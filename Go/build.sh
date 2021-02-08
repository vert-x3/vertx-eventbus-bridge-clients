#!/bin/sh
set -e
cwd=$(cd `dirname $0` && pwd)
wd="${GOPATH}/src/github.com/vert-x3/vertx-eventbus-bridge-clients/Go"
mkdir -p $wd
cd $wd
[ ! -s eventbus ] && ln -s $cwd/eventbus
cd eventbus
go get
go build -v
cd $cwd/sample-vertx-go-client
go build -v
