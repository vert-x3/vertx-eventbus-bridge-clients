#!/bin/sh
set -e
cd /workdir/vertx-eventbus
dotnet restore
dotnet build
