# A Go Client for the Eclipse Vert.x TCP EventBus Bridge

This project is a client API for interacting with
[Eclipse Vert.x](http://vertx.io/) applications from Go applications using the
[TCP EventBus bridge](http://vertx.io/docs/vertx-tcp-eventbus-bridge/java/) from _Vert.x_.

## Usage

You will need the following `import`:

```go
import (
  // ...
  "github.com/jponge/vertx-go-tcp-eventbus-bridge/eventbus"
  // ...
)
```

and `go get github.com/jponge/vertx-go-tcp-eventbus-bridge/eventbus`

The `eventbus` package essentially provides 2 types:

1. `eventbus.EventBus` that encapsulates the TCP bridge protocol, and
2. `eventbus.Dispatcher` that helps receiving messages from the event bus, and dispatch them to
   Go channels.

Here is an (incomplete) snippet showing the usage of these 2 types:

```go
// Get an event bus connection
eventBus, err := eventbus.NewEventBus("localhost:7000")
if err != nil {
  log.Fatal("Connection to the Vert.x bridge failed: ", err)
}

// Get a dispatcher for incoming messages
dispatcher := eventbus.NewDispatcher(eventBus)
dispatcher.Start()

// Get a queue of length 8 for the messages on 'sample.clock.ticks'
ticks, id, err := dispatcher.Register("sample.clock.ticks", 8)
if err != nil {
  log.Fatal("Registration on sample.clock.ticks failed: ", err)
}
log.Println("sample.clock.ticks channel registered with ID=", id)

// Print all ticks from a goroutine
go func() {
  for {
    <-ticks
    log.Println("[tick]")
  }
}()

// Post messages to 'sample.echo', and expect replies on 'sample.echo.reply'
go func() {
  for {
    <-time.After(time.Second * 3)
    eventBus.SendWithReplyAddress("sample.echo", "sample.echo.reply", nil, map[string]string{
      "source": "Go Client",
      "what": "A tick!",
    })
  }
}()

// Get replies on 'sample.echo.reply'
go func() {
  for reply := range(echoReply) {
    log.Println("Echo, got back: ", reply)
  }
}()
```

## Documentation

You should read the API documentation to understand all operations, and especially the concurrency
assumptions and requirements:
[https://godoc.org/github.com/jponge/vertx-go-tcp-eventbus-bridge/eventbus](https://godoc.org/github.com/jponge/vertx-go-tcp-eventbus-bridge/eventbus)

A sample Vert.x / Java application is given in [vertx-sample-app/](vertx-sample-app/).

It can be used to run against the sample Go client from
[sample-vertx-go-client/](sample-vertx-go-client/).

Finally, the integration tests in [eventbus/integration-test/](eventbus/integration-test/) can be
run using `go test` in that folder, and the Vert.x / Java application above needs to be running.

## License

    Copyright 2016 Julien Ponge

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
