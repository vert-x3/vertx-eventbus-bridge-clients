/**
 * Created by itersh on 25.10.16.
 */
/*
 *   Copyright (c) 2011-2015 The original author or authors
 *   ------------------------------------------------------
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   and Apache License v2.0 which accompanies this distribution.
 *
 *       The Eclipse Public License is available at
 *       http://www.eclipse.org/legal/epl-v10.html
 *
 *       The Apache License v2.0 is available at
 *       http://www.opensource.org/licenses/apache2.0.php
 *
 *   You may elect to redistribute this code under either of these licenses.
 */
var net = require('net');
var makeUUID = require('node-uuid').v4;
var tls = require('tls');

function mergeHeaders(defaultHeaders, headers) {
  if (defaultHeaders) {
    if (!headers) {
      return defaultHeaders;
    }

    for (var headerName in defaultHeaders) {
      if (defaultHeaders.hasOwnProperty(headerName)) {
        // user can overwrite the default headers
        if (typeof headers[headerName] === 'undefined') {
          headers[headerName] = defaultHeaders[headerName];
        }
      }
    }
  }

  // headers are required to be a object
  return headers || {};
}

/**
 * WireEncode the message to the bridge
 */
function send(transport, message) {
  message = Buffer.from(message, "utf-8");
  var msgLen = message.length;

  var buffer = new Buffer(4);
  buffer.writeInt32BE(msgLen, 0);

  transport.write(Buffer.concat([buffer, message], 4 + msgLen));
}

/**
 * EventBus
 *
 * @param {String} host
 * @param {Number} port
 * @param {Object} options
 * @constructor
 */
var EventBus = function (host, port, options) {

  var self = this;

  options = options || {};

  var pingInterval = options.vertxbus_ping_interval || 5000;
  var pingTimerID;

  var sendPing = function () {
    send(self.transport, JSON.stringify({ type: 'ping' }));
  };

  var callback = function (err) {
    if (err) {
      self.onerror(err);
    }

    // Send the first ping then send a ping every pingInterval milliseconds
    sendPing();
    pingTimerID = setInterval(sendPing, pingInterval);
    self.state = EventBus.OPEN;
    self.onopen && self.onopen();
  }
  // if user use certificate need use tls module
  var connectionModule = options.hasOwnProperty('pfx') || options.hasOwnProperty('cert') ? tls : net;

  // attributes
  this.transport = connectionModule.connect(port, host, options, callback);

  this.state = EventBus.CONNECTING;
  this.handlers = {};
  this.replyHandlers = {};
  this.defaultHeaders = null;

  // default event handlers
  this.onerror = console.error;

  // message buffer
  var buffer = new Buffer(0);
  var len = 0;

  this.transport.on('close', function () {
    self.state = EventBus.CLOSED;
    if (pingTimerID) {
      clearInterval(pingTimerID);
    }
    self.onclose && self.onclose();
  });

  this.transport.on('error', self.onerror);

  this.transport.on('data', function (chunk) {
    buffer = Buffer.concat([buffer, chunk], buffer.length + chunk.length);
    // we need to loop since there can be several messages in a chunk
    do {
      !len && (len = buffer.readInt32BE(0));

      if (len && buffer.length >= len + 4) {
        // we have a full message
        var message = buffer.slice(4, len + 4);
        // slice the buffer to consume the next message
        buffer = buffer.slice(len + 4);
        len = 0;

        var json;

        try {
          json = JSON.parse(message.toString('utf8'));
        } catch (e) {
          self.onerror(e);
          return;
        }

        // define a reply function on the message itself
        if (json.replyAddress) {
          Object.defineProperty(json, 'reply', {
            value: function (message, headers, callback) {
              self.send(json.replyAddress, message, headers, callback);
            }
          });
        }

        if (self.handlers[json.address]) {
          // iterate all registered handlers
          var handlers = self.handlers[json.address];
          for (var i = 0; i < handlers.length; i++) {
            if (json.type === 'err') {
              handlers[i]({ failureCode: json.failureCode, failureType: json.failureType, message: json.message });
            } else {
              handlers[i](null, json);
            }
          }
        } else if (self.replyHandlers[json.address]) {
          // Might be a reply message
          var handler = self.replyHandlers[json.address];
          delete self.replyHandlers[json.address];
          if (json.type === 'err') {
            handler({ failureCode: json.failureCode, failureType: json.failureType, message: json.message });
          } else {
            handler(null, json);
          }
        } else {
          if (json.type === 'err') {
            self.onerror(json);
          } else {
            console.warn('No handler found for message: ', json);
          }
        }
      }   // if data chunked into few frames need concatenate into buffer
    } while (buffer.length > 4 && !len)
  });
};

/**
 * Send a message
 *
 * @param {String} address
 * @param {Object} message
 * @param {Object} [headers]
 * @param {Function} [callback]
 */
EventBus.prototype.send = function (address, message, headers, callback) {
  // are we ready?
  if (this.state != EventBus.OPEN) {
    throw new Error('INVALID_STATE_ERR');
  }

  if (typeof headers === 'function') {
    callback = headers;
    headers = {};
  }

  var envelope = {
    type: 'send',
    address: address,
    headers: mergeHeaders(this.defaultHeaders, headers),
    body: message
  };

  if (callback) {
    var replyAddress = makeUUID();
    envelope.replyAddress = replyAddress;
    this.replyHandlers[replyAddress] = callback;
  }

  send(this.transport, JSON.stringify(envelope));
};

/**
 * Publish a message
 *
 * @param {String} address
 * @param {Object} message
 * @param {Object} [headers]
 */
EventBus.prototype.publish = function (address, message, headers) {
  // are we ready?
  if (this.state != EventBus.OPEN) {
    throw new Error('INVALID_STATE_ERR');
  }

  send(this.transport, JSON.stringify({
    type: 'publish',
    address: address,
    headers: mergeHeaders(this.defaultHeaders, headers),
    body: message
  }));
};

/**
 * Register a new handler
 *
 * @param {String} address
 * @param {Object} [headers]
 * @param {Function} callback
 */
EventBus.prototype.registerHandler = function (address, headers, callback) {
  // are we ready?
  if (this.state != EventBus.OPEN) {
    throw new Error('INVALID_STATE_ERR');
  }

  if (typeof headers === 'function') {
    callback = headers;
    headers = {};
  }

  // ensure it is an array
  if (!this.handlers[address]) {
    this.handlers[address] = [];
    // First handler for this address so we should register the connection
    send(this.transport, JSON.stringify({
      type: 'register',
      address: address,
      headers: mergeHeaders(this.defaultHeaders, headers)
    }));
  }

  this.handlers[address].push(callback);
};

/**
 * Unregister a handler
 *
 * @param {String} address
 * @param {Object} [headers]
 * @param {Function} callback
 */
EventBus.prototype.unregisterHandler = function (address, headers, callback) {
  // are we ready?
  if (this.state != EventBus.OPEN) {
    throw new Error('INVALID_STATE_ERR');
  }

  var handlers = this.handlers[address];

  if (handlers) {

    if (typeof headers === 'function') {
      callback = headers;
      headers = {};
    }

    var idx = handlers.indexOf(callback);
    if (idx != -1) {
      handlers.splice(idx, 1);
      if (handlers.length === 0) {
        // No more local handlers so we should unregister the connection
        send(this.transport, JSON.stringify({
          type: 'unregister',
          address: address,
          headers: mergeHeaders(this.defaultHeaders, headers)
        }));

        delete this.handlers[address];
      }
    }
  }
};

/**
 * Closes the connection to the EvenBus Bridge.
 */
EventBus.prototype.close = function () {
  this.state = EventBus.CLOSING;
  this.transport.close();
};

EventBus.CONNECTING = 0;
EventBus.OPEN = 1;
EventBus.CLOSING = 2;
EventBus.CLOSED = 3;

module.exports = EventBus;
