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
	"encoding/json"
	"github.com/stretchr/testify/assert"
	"net"
	"strings"
	"testing"
)

func TestProperJsonEncoding(t *testing.T) {

	m := newSendMessage("foo.bar", "anywhere", nil, map[string]string{
		"text": "hello",
	})

	bytes, err := json.Marshal(m)
	if err != nil {
		t.Error("Marshalling failed", err)
	}

	jsonText := string(bytes)
	t.Log(jsonText)
	assert.True(t, strings.Contains(jsonText, `"address":"foo.bar"`))
	assert.True(t, strings.Contains(jsonText, `"body":{"text":"hello"`))
	assert.True(t, strings.Contains(jsonText, `"replyAddress":"anywhere"`))

	m = newSendMessage("foo.bar", nil, nil, map[string]string{
		"text": "hello",
	})
	if bytes, err = json.Marshal(m); err != nil {
		t.Error("Marshalling failed", err)
	}
	jsonText = string(bytes)
	t.Log(jsonText)
	assert.False(t, strings.Contains(jsonText, "replyAddress"))
}

func TestMessageSend(t *testing.T) {

	listener, err := net.Listen("tcp", "localhost:7000")
	if err != nil {
		t.Error("Starting server failed", err)
	}
	defer listener.Close()

	go func(t *testing.T) {
		eventBus, err := NewEventBus("localhost:7000")
		if err != nil {
			t.Error("Event bus creation failed", err)
		}
		defer eventBus.Close()
		msg := newRegisterMessage("foo.bar")
		if err = eventBus.send(msg); err != nil {
			t.Error("Message sending failed", err)
		}
	}(t)

	conn, err := listener.Accept()
	if err != nil {
		t.Error("Accept() failed")
	}
	msg, err := receive(conn)
	if err != nil {
		t.Error("Bad response", err)
	}
	t.Log(msg)

	assert.Equal(t, "register", msg.Type)
	assert.Equal(t, "foo.bar", msg.Address)
	assert.Equal(t, nil, msg.Body)

}
