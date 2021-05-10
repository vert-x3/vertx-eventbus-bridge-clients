#!/usr/bin/python
# Authors:
# 2016: Jayamine Alupotha https://github.com/jaymine
# 2020: Wolfgang Fahl https://github.com/WolfgangFahl
# 2021: Lin Gao https://github.com/gaol

from enum import IntEnum
import errno
import logging
import json
import socket
import struct
from threading import Thread
import time
import uuid

LOGGER = logging.getLogger(__name__)


def create_message(
    msg_type="ping", address=None, headers=None, body=None, reply_address=None
):
    msg = {"type": msg_type}
    if "ping" != msg_type and address is None:
        raise Exception("address of the message must be provided")
    if address is not None:
        msg["address"] = address
    if reply_address is not None:
        msg["replyAddress"] = reply_address
    if headers is not None:
        msg["headers"] = headers
    if body is not None:
        msg["body"] = body
    return json.dumps(msg)


def create_err_message(address, failure_code, message):
    """
    message type of the error message from client to bridge is always `send`
    """
    if address is None or failure_code is None or message is None:
        raise Exception("All address, failure_code and message are required.")
    return json.dumps(
        {
            "type": "send",
            "address": address,
            "failureCode": failure_code,
            "message": message,
        }
    )


class _State(IntEnum):
    """
    State of EventBus Client
    """

    NEW = 0  # when created or not connected / failed
    CONNECTING = 1  # when the client is connecting to the bridge
    CONNECTED = 2  # when the client gets connected to the bridge
    CLOSING = 3  # when the client is closing the connection
    CLOSED = 4  # when the client closed the connection
    BROKEN = 5  # when the client connection is broken


class EventBus:
    """
    Vert.x TCP EventBus Client for Python
    """

    def __init__(self, host="localhost", port=7000, options=None, err_handler=None):
        """
        EventBus Constructor

        Args:
            host(str): the host to connect to - default: 'localhost'
            port(int): the port to use - default: 7000
            options(dict): e.g. { ping_interval=5, timeout=60, debug=False, connect=False}

        :raise:
           :IOError: - the socket could not be opened
           :Exception: - some other issue e.g. with starting the listening thread

        """
        self.socket = None
        self.last_pong = None
        self._state = _State.NEW
        self.host = host
        self.port = port
        self.handlers = {}
        self.options = options
        self.timeout = 60  # socket timeout, in seconds
        self.ping_interval = 5  # heart beat for ping/pong
        self._err_handler = err_handler
        self.auto_connect = True
        self.max_reconnect = 5
        if self._err_handler is None:
            self._err_handler = EventBus._default_err_handler
        if options is not None:
            if "timeout" in options:
                self.timeout = int(options["timeout"])
            if "ping_interval" in options:
                self.ping_interval = int(options["ping_interval"])
            if "auto_connect" in options:
                self.auto_connect = bool(options["auto_connect"])
            if "max_reconnect" in options:
                self.max_reconnect = int(options["max_reconnect"])
            if "connect" in options and bool(options["connect"]):
                self.connect()

    def __enter__(self):
        return self

    def __exit__(self, *args):
        self.close()

    @staticmethod
    def _default_err_handler(message):
        LOGGER.error("Got Error Message: %s from server", message)

    def connect(self):
        if self._state == _State.CLOSED:
            LOGGER.debug("Client has been closed")
            return None
        num_of_tries = self.max_reconnect if self.auto_connect else 1
        for i in range(num_of_tries):
            try:
                if self._state != _State.CONNECTED:
                    self._state = _State.CONNECTING
                    self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
                    self.socket.settimeout(self.timeout)
                    self.socket.connect((self.host, self.port))
                    self._state = _State.CONNECTED
                    receiving_thread = Thread(target=self._receive)
                    receiving_thread.setDaemon(True)
                    receiving_thread.start()
                    ping_thread = Thread(target=self._ping)
                    ping_thread.setDaemon(True)
                    ping_thread.start()
                    for address, handler in self.handlers.items():
                        if handler.is_at_server():
                            message = create_message("register", address)
                            self._send_frame(message)
                    break
            except IOError as e:
                LOGGER.warning(
                    "Tried to connect %d times, try again.", i + 1, exc_info=e
                )
        else:
            self._state = _State.CLOSED
            raise Exception("Failed to connect after %d times try" % num_of_tries)

    def _ping(self):
        while self.is_connected():
            try:
                self.check_last_pong()
                ping_message = create_message()
                LOGGER.debug("send ping message")
                self._send_frame(ping_message)
                time.sleep(self.ping_interval)
            except Exception as e:
                LOGGER.debug("Error on sending ping message.", exc_info=e)

    def check_last_pong(self):
        # check last pong
        if self.last_pong is not None:
            if time.time() - self.last_pong > self.ping_interval * 2:
                LOGGER.warning("ping/pong packet is slow")

    def _receive_chunked(self, total_read=4096, step=2048):
        bytes_recd = 0
        chunks = []
        while bytes_recd < total_read:
            chunk = self.socket.recv(min(total_read - bytes_recd, step))
            if chunk == b"":
                return chunk
            chunks.append(chunk)
            bytes_recd = bytes_recd + len(chunk)
        return b"".join(chunks)

    def _receive(self):
        """
        This method gets running in receiving thread
        """
        while self.is_connected():
            try:
                len_str = self._receive_chunked(4, 4)
                if len_str == b"":
                    self._state = _State.CLOSED
                    break
                len1 = struct.unpack("!i", len_str)[0]
                payload = self._receive_chunked(len1)
                if payload == b"":
                    self._state = _State.CLOSED
                    break
                json_message = payload.decode("utf-8")
                message = json.loads(json_message)
                if message["type"] == "message":  # message
                    if "address" not in message:
                        self._err_handler(message)
                    else:
                        if message["address"] in self.handlers:
                            for handler in self.handlers[
                                message["address"]
                            ].all_handlers():
                                handler(message)
                        else:
                            LOGGER.warning(
                                "No handler found on address %s", message["address"]
                            )
                elif message["type"] == "err":  # err
                    self._err_handler(message)
                elif message["type"] == "pong":  # ping/pong
                    self.last_pong = time.time()
                    LOGGER.debug("get pong response")
                else:  # unknown message type
                    self._err_handler(message)
            except socket.timeout:
                LOGGER.debug("timeout, try again")
                continue
            except Exception as e:
                if self._state == _State.CLOSED:
                    LOGGER.debug("client has been closed")
                else:
                    if e.args[0] == errno.ECONNRESET:
                        self._state = _State.CLOSED
                        self.handlers.clear()
                        LOGGER.debug("connection reset by server")
                    else:
                        self._state = _State.BROKEN
                        LOGGER.error("Connection was broken", exc_info=e)
                break
        if self.auto_connect and self._state != _State.CLOSED:
            self.connect()

    def is_connected(self):
        return self._state == _State.CONNECTED

    def close(self):
        if self._state != _State.CLOSED:
            try:
                self._state = _State.CLOSING
                self.socket.close()
                self._state = _State.CLOSED
                self.handlers.clear()
            except Exception as e:
                LOGGER.error("Failed to close the socket", exc_info=e)

    def _check_closed(self):
        if not self.is_connected():
            if self.auto_connect and self._state != _State.CLOSED:
                self.connect()
            else:
                raise Exception("socket has been closed.")

    # send, receive, register, unregister ------------------------------------

    def _send_frame(self, message_s):
        message = message_s.encode("utf-8")
        frame = struct.pack("!I", len(message)) + message
        self.socket.sendall(frame)

    def send(
        self, address, headers=None, body=None, reply_address=None, reply_handler=None
    ):
        self._check_closed()
        if reply_handler is not None:
            reply_address = reply_address or str(uuid.uuid1())
            self._register_local(reply_address, reply_handler, False)
        message = create_message("send", address, headers, body, reply_address)
        self._send_frame(message)

    def publish(self, address, headers=None, body=None):
        self._check_closed()
        message = create_message("publish", address, headers, body)
        self._send_frame(message)

    def _register_local(self, address, handler, at_server=True):
        if address in self.handlers:
            self.handlers[address].append_handler(handler, at_server)
        else:
            self.handlers[address] = _MessageHandlers(handler, at_server)

    def _address_registered_at_server(self, address):
        return address in self.handlers and self.handlers[address].is_at_server()

    def register_handler(self, address, handler):
        """
        Registers a handler on the address
        
        :param address: the address on which a handler gets registered
        :param handler: the handler to register
        """
        if callable(handler):
            if not self._address_registered_at_server(address):
                try:
                    self._check_closed()
                    message = create_message("register", address)
                    self._send_frame(message)
                except Exception as e:
                    LOGGER.error("Registration failed", exc_info=e)
                    raise e
            self._register_local(address, handler, True)
        else:
            raise Exception("Registration failed. Function is not callable")

    def unregister_handler(self, address, handler=None):
        """
        Un-registers handlers with the address, if handler is not specified, all handlers with same address will be
        cleared
        
        :param address: the address of the handlers
        :param handler: the optional handler to be un-registered
        """
        if address in self.handlers:
            the_handler = self.handlers[address]
            if handler is None:
                the_handler.clear()
                del self.handlers[address]
            else:
                the_handler.del_handler(handler)
            if the_handler.is_at_server() and the_handler.is_empty():
                try:
                    self._check_closed()
                    message = create_message("unregister", address)
                    self._send_frame(message)
                except Exception as e:
                    LOGGER.error("Unregistering failed", exc_info=e)
                    raise e


class _MessageHandlers:
    """
    Handlers that get registered in client or/and in server
    Only one handler with same address needs to get registered at server side, other handlers with same address
    are in client side only, once message is back, all handlers with same address will be called in sequence.
    """

    def __init__(self, handler, at_server=True):
        self._handlers = [handler]
        self.at_server = at_server

    def append_handler(self, handler, at_server=True):
        """
        Appends the handler if it is not in the list yet, return True if it gets appended, False otherwise
        """
        if at_server:
            self.at_server = True
        if not self.has_handler(handler):
            self._handlers.append(handler)

    def del_handler(self, handler):
        if self.has_handler(handler):
            self._handlers.remove(handler)

    def has_handler(self, handler):
        return handler in self._handlers

    def is_at_server(self):
        return self.at_server

    def clear(self):
        self._handlers = []

    def is_empty(self):
        return len(self._handlers) == 0

    def all_handlers(self):
        return self._handlers
