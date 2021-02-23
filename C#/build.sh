#!/bin/sh
set -e

docker run --rm -it -v $(pwd):/workdir:Z mcr.microsoft.com/dotnet/sdk:latest /bin/bash -x /workdir/vertx-eventbus/build.sh $@
