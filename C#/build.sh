#!/bin/sh
set -e

docker run --rm -v $(pwd):/workdir:Z -it microsoft/dotnet:latest /workdir/vertx-eventbus/build.sh
