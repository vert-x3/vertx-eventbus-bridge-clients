#!/bin/sh
set -e

docker run --rm -v $(pwd):/workdir:Z -it mcr.microsoft.com/dotnet/sdk:latest /workdir/vertx-eventbus/build.sh
