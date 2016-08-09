# TCP-eventbus-client-C
This is a TCP eventbus implementation for C clients. The protocol is quite simple:

* 4bytes int32 message length (big endian encoding)
* json string
* built-in keys
        
        1) type: (String, required) One of "send", "publish", "register", "unregister".
        
        2) headers: (Object, optional) Headers with JSON format.
        
        3) body: (Object, optional) Message content in JSON format.
        
        4) address: (String, required) Destination address
        
        5) replyAddress: (String, optional) Address for replying to.
        
- NOTE : Please edit Makefile according to your OS.
example:

```c

#include <stdio.h>
#include <stdlib.h>
#include "vertx/vertx.h"
#include "vertx/parson.h"

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

    #ifdef _WIN32
    Sleep(1000);
    #endif // _WIN32
    #ifdef __unix__
    sleep(1);
    #endif // linux

    //unregister
    eventbus_unregister("pcs.status.c");
    //send
    eventbus_publish("pcs.status","{\"type\":\"Maths\"}","{\"message\":\"i++\"}");
    eventbus_send("pcs.status","pcs.status.c","{\"type\":\"Maths\"}","{\"message\":\"i++\"}");
    
    #ifdef _WIN32
    Sleep(1000);
    #endif // _WIN32
    #ifdef __unix__
    sleep(1);
    #endif // linux

    close_eventbus();
    return 0;
}

void function(String *msg){
    printf("%s",*msg);
    i++;
}

```
