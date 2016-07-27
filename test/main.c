#include <stdio.h>
#include <stdlib.h>
#include "vertx/vertx.h"

void test(void (*func)(String *));

void function(String *msg);
int main(){
    //test(function);
    create_eventbus();
    start_eventbus();
    printf("1");
    eventbus_send("pcs.status","pcs.status","{\"type\":\"Maths\"}","{\"message\":\"send1 ok\"}");
    eventbus_send("pcs.status.c","pcs.status.c","{\"type\":\"Maths\"}","{\"message\":\"send2 ok\"}");
    eventbus_publish("pcs.status","{\"type\":\"Maths\"}","{\"message\":\"publish ok\"}");
    eventbus_register("pcs.status","{\"type\":\"Maths\"}","{\"message\":\"publish ok\"}",function);
    eventbus_register("pcs.status.c","{\"type\":\"Maths\"}","{\"message\":\"publish ok\"}",function);
    printList();
    eventbus_unregister("pcs.status.c","{\"type\":\"Maths\"}","{\"message\":\"publish ok\"}");
    printList();
    Sleep(10);
    close_eventbus(4000);
    return 0;
}

void function(String *msg){
printf("%s\n",*msg);
}

