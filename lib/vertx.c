#include <stdio.h>
#include <string.h>
#include "parson.h"
#include "vertx.h"
#include "osi_socket.h"
#include <stdlib.h>
#include <errno.h>
#include <unistd.h>
#include <pthread.h>
#include <windows.h>
#include <ws2tcpip.h>
#include <winsock2.h>

#define WIN32_LEAN_AND_MEAN
#define DEFAULT_BUFLEN 4

// Need to link with Ws2_32.lib, Mswsock.lib, and Advapi32.lib
//@https://msdn.microsoft.com/en-us/library/windows/desktop/ms737591(v=vs.85).aspx

// Handlers -------------------------------------------------

//This is the structure for handler
typedef struct Handler{
    String address;
    void (*function)(String *);
} Handler;

void handle(Handler h, String *msg){
    h.function(msg);
}


// Json Message-----------------------------------------------

//This is the structure for JsonMessage
typedef struct JsonMessage{
    String type;
    String address;
    String headers;
    String replyAddress;
    String body;
} JsonMessage;

//get message
void getMessage(JsonMessage jsonMessage,String* message){
    *message=NULL;
    //init
    JSON_Value *root_value = json_value_init_object();
    JSON_Object *root_object = json_value_get_object(root_value);
    json_object_set_string(root_object, "type", jsonMessage.type);
    json_object_set_string(root_object, "address", jsonMessage.address);
    json_object_set_string(root_object, "replyAddress", jsonMessage.replyAddress);
    json_object_set_string(root_object, "headers", jsonMessage.headers);
    json_object_set_string(root_object, "body", jsonMessage.body);
    //to string
    *message=json_serialize_to_string_pretty(root_value);
    //free
    json_value_free(root_value);
}

/*Eventbus constructor
#	input parameters
#		1) host	- String
#		2) port	- integer(>2^10-1)
#		3) TimeOut - int- receive TimeOut
#	inside parameters
#		1) socket
#		2) handlers - List<address,Handlers>
#		3) state -integer
#		4) ReplyHandler - <address,function>
#		5) ErrorNumber
#       6) fileLock - object
#Eventbus state
#	0 - not connected/failed
#	1 - connecting
#	2 - connected /open
#	3 - closing
#	4 - closed*/

String HOST="127.0.0.1";
int PORT=7000;
int TIMEOUT=1000;
int INIT;
int STATE =0;

void setHost(String host){
    HOST=host;
}

void setPort(int port){
    PORT=port;
}

void setTimeOut(int timeout){
    TIMEOUT=timeout;
}

void create_socket(){

    WSADATA wsaData;
    SOCKET ConnectSocket = INVALID_SOCKET;


    // Initialize Winsock
    INIT = WSAStartup(0x202, &wsaData);
    if (INIT != 0) {
        perror("WSAStartup failed with error: %d\n");
        return 1;
    }


}




//test-------------------------------------------------------------------
void test(void (*func)(String *)){
    Handler h;
    h.address="vertx";
    String s="vertx";
    h.function=func;
    handle(h,&s);

    JsonMessage js;
    js.address="vertx";
    js.replyAddress="vertx";
    js.type="send";
    String message=NULL;
    getMessage(js,&message);

    printf("%s\n",message);


}


