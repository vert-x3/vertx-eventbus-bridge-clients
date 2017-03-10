#!/bin/sh
set -e
mkdir -p /go/src/github.com/vert-x3/vertx-eventbus-bridge-clients/Go
cd /go/src/github.com/vert-x3/vertx-eventbus-bridge-clients/Go
ln -s /workdir/eventbus
cd eventbus
go get
go build -v
cd /workdir/sample-vertx-go-client
go build -v
