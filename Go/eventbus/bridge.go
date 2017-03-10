// Copyright 2016 Julien Ponge
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

// This package provides a client API to the Vert.x TCP EventBus bridge, See
// http://vertx.io/docs/vertx-tcp-eventbus-bridge/java/ for pointers to the reference protocol.
package eventbus

import (
	"encoding/binary"
	"encoding/json"
	"net"
	"sync"
)

// A connection to an event bus TCP bridge.
//
// In all publishing methods, the headers and body parameters can be anything that the
// encoding/json package knows how to map. It is recommended to use either struct or map objects.
// The headers parameter can also be nil in case no header is useful.
type EventBus struct {
	connection net.Conn
	sendMutex  sync.Mutex
}

// Connects to a remote Vert.x application over the event bus TCP bridge.
//
// The address shall be specified as for 'net.Dial' connections, like 'somewhere.tld:port'.
//
// An EventBus pointer or an error are returned.
func NewEventBus(address string) (*EventBus, error) {
	connection, err := net.Dial("tcp", address)
	if err != nil {
		return nil, err
	}
	return &EventBus{connection, sync.Mutex{}}, nil
}

// Closes the connection to a remote Vert.x application.
func (eventBus *EventBus) Close() error {
	return eventBus.connection.Close()
}

// Receive an incoming message from the remote Vert.x application event bus.
//
// The message can be for any destination, it is up to the caller to decide how to dispatch it.
// You may want to use eventbus.Dispatcher to use Go channels.
//
// This method is blocking and it is not safe to use from concurrent goroutines.
func (eventBus *EventBus) Receive() (*Message, error) {
	return receive(eventBus.connection)
}

// Publish a message to an address.
//
// This method is safe to use from concurrent goroutines.
func (eventBus *EventBus) Publish(address string, headers, body interface{}) error {
	return eventBus.send(newPublishMessage(address, nil, body))
}

// Send a message to an address.
//
// This method is safe to use from concurrent goroutines.
func (eventBus *EventBus) Send(address string, headers, body interface{}) error {
	return eventBus.send(newSendMessage(address, nil, nil, body))
}

// Send a message to an address, and also specify a destination for an expected response.
// The response will be eventually fetched from the Receive() method.
//
// This method is safe to use from concurrent goroutines.
func (eventBus *EventBus) SendWithReplyAddress(address, replyAddress string, headers, body interface{}) error {
	return eventBus.send(newSendMessage(address, replyAddress, nil, body))
}

// Register this client to receive messages on a destination.
//
// This method is safe to use from concurrent goroutines.
func (eventBus *EventBus) Register(address string) error {
	return eventBus.send(newRegisterMessage(address))
}

// Unregisters this client from receiving messages from a destination.
//
// This method is safe to use from concurrent goroutines.
func (eventBus *EventBus) Unregister(address string) error {
	return eventBus.send(newUnregisterMessage(address))
}

func (eventBus *EventBus) send(message *Message) error {
	bytes, err := json.Marshal(message)
	if err != nil {
		return err
	}
	eventBus.sendMutex.Lock()
	defer eventBus.sendMutex.Unlock()
	size := int32(len(bytes))
	if err = binary.Write(eventBus.connection, binary.BigEndian, size); err != nil {
		return err
	}
	if _, err = eventBus.connection.Write(bytes); err != nil {
		return err
	}
	return nil
}

func receive(connection net.Conn) (*Message, error) {
	var size int32 = 0
	if err := binary.Read(connection, binary.BigEndian, &size); err != nil {
		return nil, err
	}
	bytes := make([]byte, size)
	_, err := connection.Read(bytes)
	if err != nil {
		return nil, err
	}
	var message Message
	if err = json.Unmarshal(bytes, &message); err != nil {
		return nil, err
	}
	return &message, nil
}
