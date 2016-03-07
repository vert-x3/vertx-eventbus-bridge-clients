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

package eventbusintegrationtest

import (
	"github.com/jponge/vertx-go-tcp-eventbus-bridge/eventbus"
	"github.com/stretchr/testify/assert"
	"testing"
	"time"
)

func sampleData() map[string]string {
	return map[string]string{
		"title":  "A blog post",
		"author": "John B R",
		"content": `This is an **awesome**

blog post that will set a new
standard in blogging.`,
	}
}

func connect(t *testing.T) *eventbus.EventBus {
	eventBus, err := eventbus.NewEventBus("localhost:7000")
	if err != nil {
		t.Error("Connecting to the event bus failed", err)
	}
	return eventBus
}

func TestSimplePublish(t *testing.T) {
	eventBus := connect(t)
	defer eventBus.Close()
	err := eventBus.Publish("sample.dumb.inbox", nil, sampleData())
	if err != nil {
		t.Error("Publishing failed", err)
	}
}

func TestSimpleSend(t *testing.T) {
	eventBus := connect(t)
	defer eventBus.Close()
	err := eventBus.Send("sample.dumb.inbox", nil, sampleData())
	if err != nil {
		t.Error("Sending failed", err)
	}
}

func consumeAndPush(eventBus *eventbus.EventBus, t *testing.T, c chan<- *eventbus.Message) {
	m, err := eventBus.Receive()
	if err != nil {
		t.Error("Receiving failed", err)
	}
	c <- m
}

func TestSimpleRegistrationThenUnregistration(t *testing.T) {

	eventBus := connect(t)
	defer eventBus.Close()

	err := eventBus.Register("sample.clock.ticks")
	if err != nil {
		t.Error("Registration failed", err)
	}

	mChan := make(chan *eventbus.Message, 1)
	go consumeAndPush(eventBus, t, mChan)
	select {
	case m := <-mChan:
		assert.Equal(t, "message", m.Type)
	case <-time.After(time.Second * 2):
		t.Error("Timeout, no tick message received")
	}

	eventBus.Unregister("sample.clock.ticks")
	go consumeAndPush(eventBus, t, mChan)
	select {
	case <-mChan:
		t.Error("No further message shall have been received")
	case <-time.After(time.Millisecond * 1500):
		t.Log("Good, no more messages")
	}
}

func TestSendThenGetReply(t *testing.T) {

	eventBus := connect(t)
	defer eventBus.Close()

	data := sampleData()
	err := eventBus.SendWithReplyAddress("sample.echo", "sample.echo.goclient", nil, data)
	if err != nil {
		t.Error("Sending data failed")
	}

	reply, err := eventBus.Receive()
	if err != nil {
		t.Error("Receiving failed", err)
	}
	t.Log(reply)
	assert.Equal(t, "message", reply.Type)
	assert.Equal(t, data["content"], reply.Body.(map[string]interface{})["content"])
}

func TestDispatcher(t *testing.T) {

	eventBus := connect(t)
	defer eventBus.Close()

	dispatcher := eventbus.NewDispatcher(eventBus)
	dispatcher.Start()

	ticksChan, ticksId, err := dispatcher.Register("sample.clock.ticks", 8)
	if err != nil {
		t.Error("Registering failed")
	}
	otherTicksChan, otherTicksId, err := dispatcher.Register("sample.clock.ticks", 8)
	if err != nil {
		t.Error("Registering failed")
	}

	m := <-ticksChan
	assert.NotNil(t, m)

	m = <-otherTicksChan
	assert.NotNil(t, m)

	dispatcher.Unregister("sample.clock.ticks", ticksId)
	for m = range ticksChan {
	}
	t.Log("ticksChan is closed")

	m = <-otherTicksChan
	assert.NotNil(t, m)

	dispatcher.Unregister("sample.clock.ticks", otherTicksId)
	for m = range otherTicksChan {
	}
	t.Log("otherTicksChan is closed")

	dispatcher.Stop()
}
