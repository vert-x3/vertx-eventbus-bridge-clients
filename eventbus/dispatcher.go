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

package eventbus

import (
	"github.com/hashicorp/go-uuid"
	"sync"
)

type channelRegistry map[string]map[string]chan *Message

// A dispatcher provides an idiomatic way to receive messages from remote Vert.x application
// event buses, and consume messages from Go channels.
//
// The ErrChan channel is useful to be notified of an error while messages are being received.
// Only 1 error may be sent to that channel, in which case the dispatcher stops and will
// not receive further messages.
type Dispatcher struct {
	eventBus *EventBus
	channels channelRegistry
	mutex    sync.Mutex
	stop     chan int
	ErrChan  chan error
}

// Makes a new dispatcher on top of an EventBus object.
func NewDispatcher(eventBus *EventBus) *Dispatcher {
	return &Dispatcher{
		eventBus,
		channelRegistry{},
		sync.Mutex{},
		make(chan int, 1),
		make(chan error, 1),
	}
}

// Starts the dispatcher.
//
// Messages will be received from a goroutine that this method starts.
// The corresponding goroutine runs until either:
//
// - no more messages arrive, and Stop() has been called, or
//
// - an error is detected while receiving messages, in which case the said error is sent to the
// ErrChan field of this object.
func (dispatcher *Dispatcher) Start() {
	go func() {
		for {
			select {
			case <-dispatcher.stop:
				return
			default:
				message, err := dispatcher.eventBus.Receive()
				if err != nil {
					dispatcher.ErrChan <- err
					return
				}
				dispatcher.mutex.Lock()
				for _, c := range dispatcher.channels[message.Address] {
					go func(target chan *Message) {
						target <- message
					}(c)
				}
				dispatcher.mutex.Unlock()
			}
		}
	}()
}

// Stop this dispatcher.
func (dispatcher *Dispatcher) Stop() {
	dispatcher.stop <- 1
	close(dispatcher.stop)
	for _, channels := range dispatcher.channels {
		for _, channel := range channels {
			close(channel)
		}
	}
}

// Register a channel of size chanSize to listener on a destination.
//
// The method returns a channel of messages, a registration key, or possibly a non-nil error.
//
// This method is safe to use from concurrent goroutines.
func (dispatcher *Dispatcher) Register(address string, chanSize uint32) (<-chan *Message, string, error) {
	id, err := uuid.GenerateUUID()
	if err != nil {
		return nil, "", err
	}
	channel := make(chan *Message, chanSize)
	dispatcher.mutex.Lock()
	defer dispatcher.mutex.Unlock()
	if _, present := dispatcher.channels[address]; !present {
		err = dispatcher.eventBus.Register(address)
		if err != nil {
			return nil, "", err
		}
		dispatcher.channels[address] = map[string]chan *Message{}
	}
	dispatcher.channels[address][id] = channel
	return channel, id, nil
}

// Unregistration based on an address and a registration key.
//
// This method simply returns nil shall the address or channelId values not match current
// registrations.
//
// This method is safe to use from concurrent goroutines.
func (dispatcher *Dispatcher) Unregister(address, channelId string) error {
	dispatcher.mutex.Lock()
	defer dispatcher.mutex.Unlock()
	if _, present := dispatcher.channels[address]; !present {
		return nil
	}
	if c, present := dispatcher.channels[address][channelId]; present {
		close(c)
	} else {
		return nil
	}
	delete(dispatcher.channels[address], channelId)
	if len(dispatcher.channels[address]) == 0 {
		delete(dispatcher.channels, address)
		err := dispatcher.eventBus.Unregister(address)
		if err != nil {
			return err
		}
	}
	return nil
}
