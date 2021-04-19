#!/usr/bin/python
# Authors:
# 2016: Jayamine Alupotha https://github.com/jaymine
# 2020: Wolfgang Fahl https://github.com/WolfgangFahl
# 2021: Lin Gao https://github.com/gaol

import socket
import json
import struct
import time
from threading import Thread, Lock
from enum import IntEnum


# Errors -----------------------------------------------------------------
# 1 - connection errors
# 2 - unknown type of the received message
# 3 - invalid state errors
# 4 - registration failed error
# 5 - unknown address of un-registration
def _print_err(no, category, error):
    print(no)
    print(category)
    print(error)


class State(IntEnum):
    """
    State of EventBus Client
    """
    NEW = 0               # when created or not connected / failed
    CONNECTING = 1        # when the client is connecting to the bridge
    CONNECTED = 2         # when the client gets connected to the bridge
    CLOSING = 3           # when the client is closing the connection
    CLOSED = 4            # when the client closed the connection


class EventBus:
    """
    Vert.x TCP EventBus Client for Python
    """

    # constructor
    def __init__(self, host='localhost', port=7000, options=None):
        """
        EventBus Constructor

        Args:
            host(str): the host to connect to - default: 'localhost'
            port(int): the port to use - default: 7000
            options(dict): e.g. { ping_interval=5000, timeout=60, debug=False, connect=False}

        :raise:
           :IOError: - the socket could not be opened
           :Exception: - some other issue e.g. with starting the listening thread

        """
        self.sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.lock = Lock()
        self.state = State.NEW
        self.host = host
        self.port = port
        self.handlers = {}
        self.options = options
        self.timeout = 60  # socket timeout, in seconds
        self.ping_interval = 1000  # heart beat to ping/pong
        self.wait_timeout = 5000  # timeout waiting for the target state
        self.connect = False  # default to lazy connect
        self.debug = False
        if options is not None:
            if "timeout" in options:
                self.timeout = float(options["timeout"])
            if "ping_interval" in options:
                self.ping_interval = int(options["ping_interval"])
            if "wait_timeout" in options:
                self.wait_timeout = int(options["wait_timeout"])
            if "connect" in options:
                self.connect = bool(options["connect"])
            if "debug" in options:
                self.connect = bool(options["debug"])
        if self.connect:
            self._do_connect()

    def connect(self):
        self.lock.acquire()
        # TODO: check state !!
        self._do_connect()
        self.lock.release()

    def _do_connect(self):
        """ This method is called within the lock """
        if self.state == State.CONNECTED:
            print("Already Connected to %s:%d" % (self.host, self.port))
            return
        try:
            self.state = State.CONNECTING
            self.sock.settimeout(self.timeout)
            self.sock.connect((self.host, self.port))
            self.state = State.CONNECTED
            t1 = Thread(target=self._receiving_thread())
            t1.start()
        except IOError as e:
            _print_err(1, 'SERVER', str(e))
        except Exception as e:
            _print_err('Undefined Error', 'SEVERE', str(e))

    def wait(self, state=State.CONNECTED, time_out=5.0, time_step=0.01):
        """
        wait for the eventbus to reach the given state

        Args:
            state(State): the state to wait for - default: State.CONNECTED
            time_out(float): the timeOut in secs after which the wait fails with an Exception
            time_step(float): the timeStep in secs in which the state should be regularly checked

        :raise:
           :Exception: wait timed out
        """
        time_left = time_out
        while self.state != state and time_left > 0:
            time.sleep(time_step)
            time_left = time_left - time_step
        if time_left <= 0:
            raise Exception("wait for %s timedOut after %.3f secs" % (state.name, time_out))
        if self.debug:
            print("wait for %s successful after %.3f secs" % (state.name, time_out - time_left))

    # Connection send and receive---------------------------------------------

    def is_connected(self):
        return self.state == State.CONNECTED

    def _send_frame(self, message_s):
        message = message_s.encode('utf-8')
        frame = struct.pack('!I', len(message)) + message
        self.sock.sendall(frame)

    def receive(self):
        try:
            if self.state == State.CONNECTED:
                len_str = self.sock.recv(4)
                len1 = struct.unpack("!i", len_str)[0]
                payload = self.sock.recv(len1)
                json_message = payload.decode('utf-8')
                message = json.loads(json_message)
                # check
                if message['type'] == 'message':
                    # failure message
                    if 'address' not in message.keys():
                        _print_err('message failure', 'SEVERE', message)
                    else:
                        # handlers
                        if self.handlers[message['address']] is not None:
                            for handler in self.handlers[message['address']]:
                                handler(message)
                elif message['type'] == 'err':
                    _print_err(2, 'SEVERE', message)
                else:
                    _print_err(2, 'SEVERE', 'Unknown type')
                return True
            else:
                return False
        except socket.timeout:
            return True
        except Exception as e:
            # 1) close socket while thread is running
            # 2) function error in the client code
            _print_err('Undefined Error', 'SEVERE', str(e))
            return False
            # send error message

    def _receiving_thread(self):
        while self.state == State.CONNECTED:
            if not self.receive():
                break

    def close_connection(self, time_interval=30):
        if self.state == 1:
            self.sock.close()
            return
        self.state = 3
        time.sleep(time_interval)
        try:
            self.sock.close()
        except Exception as e:
            _print_err('Undefined Error', 'SEVERE', str(e))
        self.state = 4

    # send, receive, register, unregister ------------------------------------

    # address-string
    # body - json object
    # deliveryOption - object
    # replyHandler -function
    def send(self, address, headers=None, body=None, reply_address=None, reply_handler=None):
        if self.is_connected() is True:
            message = json.dumps({'type': 'send', 'address': address,
                                  'replyAddress': reply_address, 'headers': headers, 'body': body})
            self._send_frame(message)
        else:
            _print_err(3, 'SEVERE', 'INVALID_STATE_ERR')

    # address-string
    # body - json object
    # deliveryOption -object
    def publish(self, address, headers=None, body=None):
        if self.is_connected() is True:
            message = json.dumps({'type': 'publish', 'address': address, 'headers': headers, 'body': body, })
            self._send_frame(message)
        else:
            _print_err(3, 'SEVERE', 'INVALID_STATE_ERR')

    # address-string
    # deliveryOption -object
    # replyHandler -function
    def register_handler(self, address, handler):
        if self.is_connected() is True:
            if callable(handler):
                try:
                    if (address not in self.handlers.keys()) or (self.handlers[address] is None):
                        self.handlers[address] = []
                        message = json.dumps(
                            {'type': 'register', 'address': address, })
                        self._send_frame(message)
                        time.sleep(self.timeout)
                except KeyError:
                    self.handlers[address] = []
                try:
                    self.handlers[address].append(handler)
                except Exception as e:
                    _print_err(
                        4, 'SEVERE', 'Registration failed\n' + str(e))
            else:
                _print_err(
                    4, 'SEVERE', 'Registration failed. Function is not callable\n')
        else:
            _print_err(3, 'SEVERE', 'INVALID_STATE_ERR')

    # address-string
    # deliveryOption -object
    # replyHandler -function
    def unregister_handler(self, address):
        if self.is_connected() is True:
            try:
                if self.handlers[address] is not None:
                    if len(self.handlers) == 1:
                        message = json.dumps(
                            {'type': 'unregister', 'address': address, })
                        self._send_frame(message)
                    del self.handlers[address]
            except:
                _print_err(5, 'SEVERE', 'Unknown address:' + address)

        else:
            print('error occured: INVALID_STATE_ERR')
