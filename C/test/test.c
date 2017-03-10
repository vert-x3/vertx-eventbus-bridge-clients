#ifdef _WIN32
#include <windows.h>
#endif
#include <stdio.h>
#include <stdlib.h>

#include "vertx.h"


void function(String *msg);
int i=0;

int main(){
    //test(function);
    create_eventbus();
    start_eventbus();

    printf("TESTS STARTED\n");

    //register
    eventbus_register("pcs.status",function);
    eventbus_register("pcs.status.c",function);
    //send
    eventbus_send("pcs.status","pcs.status","{\"type\":\"Maths\"}","{\"message\":\"i++\"}");
    eventbus_send("pcs.status","pcs.status.c","{\"type\":\"Maths\"}","{\"message\":\"i++\"}");

    #ifdef _WIN32
	  Sleep(20); Sleep(20);
    #endif // _WIN32
    #ifdef __unix__
    sleep(1);
    #endif // linux
	printf("%d\n",i);
    if(i==2){
        printf("TEST -1- Passed\n");
    }else{
        printf("TEST -1- Failed\n");
    }

    printf("TEST - 2\n");
    //unregister
    eventbus_unregister("pcs.status.c");
	eventbus_unregister("pcs.status");
    eventbus_publish("pcs.status","{\"type\":\"Maths\"}","{\"message\":\"i++\"}");
    eventbus_send("pcs.status","pcs.status","{\"type\":\"Maths\"}","{\"message\":\"i++\"}");
    #ifdef _WIN32
	Sleep(20); Sleep(20);
    #endif // _WIN32
    #ifdef __unix__
    sleep(1);
    #endif // linux
	printf("%d\n",i);
    if(i==2){
        printf("TEST -2- Passed\n");
    }else{
        printf("TEST -2- Failed\n");
    }


    close_eventbus();
    return 0;
}

void function(String *msg){
    printf("%s",*msg);
    i++;
}

