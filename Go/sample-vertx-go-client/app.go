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

package main

import (
	"github.com/jponge/vertx-go-tcp-eventbus-bridge/eventbus"
	"log"
	"time"
)

func main() {

	stop := make(chan int, 1)

	eventBus, err := eventbus.NewEventBus("localhost:7000")
	if err != nil {
		log.Fatal("Connection to the Vert.x bridge failed: ", err)
	}

	dispatcher := eventbus.NewDispatcher(eventBus)
	dispatcher.Start()

	ticks, id, err := dispatcher.Register("sample.clock.ticks", 8)
	if err != nil {
		log.Fatal("Registration on sample.clock.ticks failed: ", err)
	}
	log.Println("sample.clock.ticks channel registered with ID=", id)
	go func() {
		for {
			<-ticks
			log.Println("[tick]")
		}
	}()

	echoReply, eid, err := dispatcher.Register("sample.echo.reply", 8)
	if err != nil {
		log.Fatal("Registration on sample.echo.reply failed: ", err)
	}
	log.Println("sample.echo.reply channel registered with ID=", eid)

	go func() {
		for {
			<-time.After(time.Second * 3)
			eventBus.SendWithReplyAddress("sample.echo", "sample.echo.reply", nil, map[string]string{
				"source": "Go Client",
				"what":   "A tick!",
			})
		}
	}()

	go func() {
		for reply := range echoReply {
			log.Println("Echo, got back: ", reply)
		}
	}()

	<-stop
}
