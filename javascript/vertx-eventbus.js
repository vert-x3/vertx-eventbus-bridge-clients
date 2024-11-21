'use strict'
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

let init = () => {

  if (typeof require === 'function' && typeof module !== 'undefined') {
    // CommonJS loader
    const SockJS = require('sockjs-client');
    if (!SockJS) {
      throw new Error('vertx-eventbus.js requires sockjs-client, see http://sockjs.org');
    }
  } else if (typeof define === 'function' && define.amd) {
    // AMD loader
    define('vertx-eventbus', ['sockjs'], () => { });
  } else {
    // plain old include
    if (typeof this.SockJS === 'undefined') {
      throw new Error('vertx-eventbus.js requires sockjs-client, see http://sockjs.org');
    }
  }
};

const makeUUID = () => {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function (a, b) {
    return b = Math.random() * 16, (a == 'y' ? b & 3 | 8 : b | 0).toString(16);
  });
}

const mergeHeaders = (defaultHeaders, headers) => {
  if (defaultHeaders) {
    if (!headers) {
      return defaultHeaders;
    }

    for (let headerName in defaultHeaders) {
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


const EventBus = class EventBus {

  constructor(url, options) {
    this.self = this;
    this.SockJS = SockJS;

    options = options || {};
    this.url = url;

    this.CONNECTING = 0;
    this.OPEN = 1;
    this.CLOSING = 2;
    this.CLOSED = 3;

    // attributes
    this.pingInterval = options.vertxbus_ping_interval || 5000;
    this.pingTimerID = null;

    this.reconnectEnabled = false;
    this.reconnectAttempts = 0;
    this.reconnectTimerID = null;
    // adapted from backo
    this.maxReconnectAttempts = options.vertxbus_reconnect_attempts_max || Infinity;
    this.reconnectDelayMin = options.vertxbus_reconnect_delay_min || 1000;
    this.reconnectDelayMax = options.vertxbus_reconnect_delay_max || 5000;
    this.reconnectExponent = options.vertxbus_reconnect_exponent || 2;
    this.randomizationFactor = options.vertxbus_randomization_factor || 0.5;

    this.defaultHeaders = null;

    this.setupSockJSConnection();
  }

  getReconnectDelay() {
    let ms = this.reconnectDelayMin * Math.pow(this.reconnectExponent, this.reconnectAttempts);
    if (this.randomizationFactor) {
      const rand = Math.random();
      const deviation = Math.floor(rand * this.randomizationFactor * ms);
      ms = (Math.floor(rand * 10) & 1) === 0 ? ms - deviation : ms + deviation;
    }
    return Math.min(ms, this.reconnectDelayMax) | 0;
  }

  // default event handlers
  onerror(err) {
    try {
      console.error(err);
    } catch (e) {
      // dev tools are disabled so we cannot use console on IE
    }
  }


  onevent(event, message) {
    return false; // return false to signal that this message is not processed
  }

  onunhandled(json) {
    try {
      if (json.type === 'err')
        onerror(json);
      else if (json.event) {
        console.warn('No handler found for event: %o. Message: %O', json.event, json);
      } else {
        console.warn('No handler found for message: ', json);
      }
    } catch (e) {
      // dev tools are disabled so we cannot use console on IE
    }
  }

  setupSockJSConnection() {
    this.sockJSConn = new SockJS(this.url, null, this.options);
    this.state = this.CONNECTING;

    // handlers and reply handlers are tied to the state of the socket
    // they are added onopen or when sending, so reset when reconnecting
    this.handlers = {};
    this.replyHandlers = {};

    this.sockJSConn.onopen = () => {
      this.enablePing(true);
      this.state = this.OPEN;
      this.onopen && this.onopen();
      if (this.reconnectTimerID) {
        this.reconnectAttempts = 0;
        // fire separate event for reconnects
        // consistent behavior with adding handlers onopen
        this.onreconnect && onreconnect();
      }
    };

    this.sockJSConn.onclose = (e) => {
      this.state = this.CLOSED;
      if (this.pingTimerID) clearInterval(this.pingTimerID);
      if (this.reconnectEnabled && this.reconnectAttempts < this.maxReconnectAttempts) {
        this.sockJSConn = null;
        // set id so users can cancel
        this.reconnectTimerID = setTimeout(setupSockJSConnection, getReconnectDelay());
        ++this.reconnectAttempts;
      }
      this.onclose && this.onclose(e);
    };

    this.sockJSConn.onmessage = (e) => {
      let json;

      try {
        json = JSON.parse(e.data);
      } catch (ex) {
        json = {
          type: 'err',
          failureType: ex.toString(),
          message: e.data
        };
      }

      // define a reply function on the message itself
      if (json.replyAddress) {
        Object.defineProperty(json, 'reply', {
          value: (message, headers, callback) => {
            send(json.replyAddress, message, headers, callback);
          }
        });
      }

      if (this.handlers[json.address]) {
        // iterate all registered handlers
        const handlers = this.handlers[json.address];
        for (let i = 0; i < handlers.length; i++) {
          if (json.type === 'err') {
            handlers[i]({ failureCode: json.failureCode, failureType: json.failureType, message: json.message });
          } else {
            handlers[i](null, json);
          }
        }
      } else if (this.replyHandlers[json.address]) {
        // Might be a reply message
        const handler = this.replyHandlers[json.address];
        delete this.replyHandlers[json.address];
        if (json.type === 'err') {
          handler({ failureCode: json.failureCode, failureType: json.failureType, message: json.message });
        } else {
          handler(null, json);
        }
      } else {
        if (!json.event || !this.onevent(json.event, json.message)) {
          this.onunhandled(json)
        }
      }
    }
  }

  /**
   * Send a message
   *
   * @param {String} address
   * @param {Object} message
   * @param {Object} [headers]
   * @param {Function} [callback]
   */
  send(address, message, headers, callback) {
    // are we ready?
    if (this.state !== this.OPEN) {
      throw new Error('INVALID_STATE_ERR');
    }

    if (typeof headers === 'function') {
      callback = headers;
      headers = {};
    }

    const envelope = {
      type: 'send',
      address: address,
      headers: mergeHeaders(this.defaultHeaders, headers),
      body: message
    };

    if (callback) {
      const replyAddress = makeUUID();
      envelope.replyAddress = replyAddress;
      this.replyHandlers[replyAddress] = callback;
    }

    this.sockJSConn.send(JSON.stringify(envelope));
  }

  /**
  * Publish a message
  *
  * @param {String} address
  * @param {Object} message
  * @param {Object} [headers]
  */
  publish(address, message, headers) {
    // are we ready?
    if (this.state !== this.OPEN) {
      throw new Error('INVALID_STATE_ERR');
    }

    this.sockJSConn.send(JSON.stringify({
      type: 'publish',
      address: address,
      headers: mergeHeaders(this.defaultHeaders, headers),
      body: message
    }));
  }

  /**
   * Register a new handler
   *
   * @param {String} address
   * @param {Object} [headers]
   * @param {Function} callback
   */
  registerHandler(address, headers, callback) {
    // are we ready?
    if (this.state !== this.OPEN) {
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
      this.sockJSConn.send(JSON.stringify({
        type: 'register',
        address: address,
        headers: mergeHeaders(this.defaultHeaders, headers)
      }));
    }

    this.handlers[address].push(callback);
  }

  /**
   * Unregister a handler
   *
   * @param {String} address
   * @param {Object} [headers]
   * @param {Function} callback
   */
  unregisterHandler(address, headers, callback) {
    // are we ready?
    if (this.state !== this.OPEN) {
      throw new Error('INVALID_STATE_ERR');
    }

    const handlers = this.handlers[address];

    if (handlers) {

      if (typeof headers === 'function') {
        callback = headers;
        headers = {};
      }

      const idx = handlers.indexOf(callback);
      if (idx !== -1) {
        handlers.splice(idx, 1);
        if (handlers.length === 0) {
          // No more local handlers so we should unregister the connection
          this.sockJSConn.send(JSON.stringify({
            type: 'unregister',
            address: address,
            headers: mergeHeaders(this.defaultHeaders, headers)
          }));

          delete this.handlers[address];
        }
      }
    }
  }

  /**
   * Closes the connection to the EventBus Bridge,
   * preventing any reconnect attempts
   */
  close() {
    this.state = this.CLOSING;
    this.enableReconnect(false);
    this.sockJSConn.close();
  }

  enablePing(enable) {
    let self = this;

    if (enable) {
      const sendPing = function () {
        self.sockJSConn.send(JSON.stringify({ type: 'ping' }));
      };

      if (self.pingInterval > 0) {
        // Send the first ping then send a ping every pingInterval milliseconds
        sendPing();
        self.pingTimerID = setInterval(sendPing, self.pingInterval);
      }
    } else {
      if (self.pingTimerID) {
        clearInterval(self.pingTimerID);
        self.pingTimerID = null;
      }
    }
  }

  enableReconnect(enable) {
    this.reconnectEnabled = enable;

    if (!enable && this.reconnectTimerID) {
      clearTimeout(this.reconnectTimerID);
      this.reconnectTimerID = null;
      this.reconnectAttempts = 0;
    }
  }
}

if (typeof exports !== 'undefined') {
  if (typeof module !== 'undefined' && module.exports) {
    exports = module.exports = EventBus;
  } else {
    exports.EventBus = EventBus;
  }
}

init();

