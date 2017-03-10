# TCP-eventbus-client-C
This is a TCP eventbus implementation for C clients.

example:
```c

#include <stdio.h>
#include <stdlib.h>
#include "vertx.h"

void function(String *msg);
int i=0;

int main(){
    create_eventbus();
    start_eventbus();

    //register
    eventbus_register("pcs.status",function);
    eventbus_register("pcs.status.c",function);
    //send
    eventbus_send("pcs.status","pcs.status","{\"type\":\"Maths\"}","{\"message\":\"i++\"}");
    eventbus_send("pcs.status","pcs.status.c","{\"type\":\"Maths\"}","{\"message\":\"i++\"}");

    sleep(1);

    //unregister
    eventbus_unregister("pcs.status.c");
    //send
    eventbus_publish("pcs.status","{\"type\":\"Maths\"}","{\"message\":\"i++\"}");
    eventbus_send("pcs.status","pcs.status.c","{\"type\":\"Maths\"}","{\"message\":\"i++\"}");

    sleep(1);

    close_eventbus();
    return 0;
}

void function(String *msg){
    printf("%s",*msg);
    i++;
}

```

In order to use it in your project just link against the static lib `eventbus`. To build it use the provided `Makefile` and `Docker`. E.g.:

```sh
# Linux64
docker run --rm -v $(pwd):/workdir:Z -e CROSS_TRIPLE=x86_64-linux-gnu -it multiarch/crossbuild make
# Win32
docker run --rm -v $(pwd):/workdir:Z -e CROSS_TRIPLE=i686-w64-mingw32 -it multiarch/crossbuild make
# Win64
docker run --rm -v $(pwd):/workdir:Z -e CROSS_TRIPLE=x86_64-w64-mingw32 -it multiarch/crossbuild make
# OSX
docker run --rm -v $(pwd):/workdir:Z -e CROSS_TRIPLE=x86_64-apple-darwin -it multiarch/crossbuild make
```

There is a working example under `test/test.c`.
