
#include <stdio.h>
#include <string.h>
#include "parson.h"
#include "vertx.h"
#include "osi_socket.h"
#include <stdlib.h>
#include <errno.h>
#include <unistd.h>
#include <stdbool.h>
#include <pthread.h>

#ifdef _WIN32
#include <windows.h>
#include <winsock2.h>
#endif // _WIN32

#ifdef linux
#include <pthread.h>
#include <sys/socket.h>
#endif // linux

#ifndef linux
#pragma comment (lib, "Ws2_32.lib")
#pragma comment (lib, "Mswsock.lib")
#pragma comment (lib, "AdvApi32.lib")
#endif //windows
#define DEFAULT_BUFLEN 4

// Handlers -------------------------------------------------

//This is the structure for handler
typedef struct Handler{
    String address;
    void (*function)(String *);
} Handler;

// Linked list------------------------------------------------
struct node
{
   Handler data;
   int key;
   struct node *next;
};

struct node *head = NULL;
struct node *current = NULL;
int node_index=0;

//handling functions
void handle(String address,String *msg){
   struct node *ptr = head;
   while(ptr != NULL){
      if(strcmp(ptr->data.address,address)==0){
        ptr->data.function(msg);
      }
      ptr = ptr->next;
   }
}

//find handler
bool find(String address){
   struct node *ptr = head;
   while(ptr != NULL){
      if(strcmp(ptr->data.address,address)==0){
        return true;
      }
      ptr = ptr->next;
   }
   return false;
}

void printList(){
   struct node *ptr = head;
   printf("\n[ ");
   while(ptr != NULL){
      printf("(%d,%s) ",ptr->key,ptr->data.address);
      ptr = ptr->next;
   }
   printf(" ]\n");
}

//insert link at the first location
void insertFirst(int key, Handler handler){
   struct node *link = (struct node*) malloc(sizeof(struct node));
   link->key = key;
   link->data = handler;
   link->next = head;
   head = link;
}

//delete all nodes with given address
struct node* delete_node(String * address){

   struct node* current = head;
   struct node* previous = NULL;
   if(head == NULL){return NULL;}
   while(strcmp(current->data.address,*address)!= 0){
      if(current->next == NULL){
         return NULL;
      }else {
         previous = current;
         current = current->next;
      }
   }
   if(current == head) {
      head = head->next;
   }else {
      previous->next = current->next;
   }
   return current;
}


// Json Message-----------------------------------------------

//This is the structure for JsonMessage
typedef struct JsonMessage{
    String type;
    String address;
    JSON_Value *headers;
    String replyAddress;
    JSON_Value *body;
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
    json_object_set_value(root_object, "headers", jsonMessage.headers);
    json_object_set_value(root_object, "body", jsonMessage.body);
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
SOCKET SendingSocket = INVALID_SOCKET;
int FLAG;

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
     WSADATA   wsaData;
     SOCKADDR_IN   ServerAddr;
     int  RetCode;

     // Initialize Winsock version 2.2
     WSAStartup(MAKEWORD(2,2), &wsaData);

     SendingSocket = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP);
     if(SendingSocket == INVALID_SOCKET)
     {
          perror("Client: socket() failed! Error code: "+ WSAGetLastError());
          WSACleanup();
          exit(1);
     }
     setsockopt(SendingSocket,SO_RCVTIMEO, 1000,  (char*)&FLAG, sizeof(int));
     ServerAddr.sin_family = AF_INET;
     ServerAddr.sin_port = htons(PORT);
     ServerAddr.sin_addr.s_addr = inet_addr(HOST);
     RetCode = connect(SendingSocket, (SOCKADDR *) &ServerAddr, sizeof(ServerAddr));

     if(RetCode != 0)
     {
          perror("Client: connect() failed! Error code: "+WSAGetLastError());
          closesocket(SendingSocket);
          WSACleanup();
          exit(1);
     }

}

void recieve_frame(){
  fd_set active_fd_set, read_fd_set;
  FD_ZERO (&active_fd_set);
  FD_SET (SendingSocket, &active_fd_set);
    int retVal;
  //while(1){
  read_fd_set = active_fd_set;

  if (select (FD_SETSIZE, &read_fd_set, NULL, NULL, NULL) < 0){
      perror ("select");
      exit (EXIT_FAILURE);
  }

    printf("%d\n",FD_SETSIZE);

    if (FD_ISSET (SendingSocket, &read_fd_set)){
        //lock
        char length_buffer[4];
        retVal = recv(SendingSocket, length_buffer, 4, 0);
        if ( retVal > 0 ){
            printf("Bytes received: %s\n", length_buffer);
        }
        int num=0;
        length_buffer[3] = (num>>24) & 0xFF;
        length_buffer[2] = (num>>16) & 0xFF;
        length_buffer[1] = (num>>8) & 0xFF;
        length_buffer[0] = num & 0xFF;
        printf("Bytes received: %d\n",num);
        //char message[(int)length_buffer];
        //lock
    }
  //}
}

void send_frame(String * message){
    //length
    byte buffer[4];
    int length=(int)strlen(*message);
    buffer[0] = length >> 24;
    buffer[1] = length >> 16;
    buffer[2] = length >> 8;
    buffer[3] = length;
    INIT = send( SendingSocket, buffer, 4, 0 );
    if (INIT == SOCKET_ERROR) {
        printf("send failed with error: %d\n", WSAGetLastError());
        closesocket(SendingSocket);
        WSACleanup();
    }else printf("length %s %d\n",buffer,length);
    //message
    INIT = send( SendingSocket, *message, (int)strlen(*message), 0 );
    if (INIT == SOCKET_ERROR) {
        printf("send failed with error: %d\n", WSAGetLastError());
        closesocket(SendingSocket);
        WSACleanup();
    }else printf("message %s\n",*message);
}

void eventbus_send(String address,String replyAddress,String Headers,String Body){
    JsonMessage js;
    js.address=address;
    js.replyAddress=replyAddress;
    js.type="send";

    JSON_Value *body = json_parse_string(Body);
    JSON_Value *headers = json_parse_string(Headers);

    js.body=body;
    js.headers=headers;
    String message=NULL;
    getMessage(js,&message);

    printf("%s\n",message);
    send_frame(&message);
    free(message);
}

void eventbus_publish(String address,String Headers,String Body){
    JsonMessage js;
    js.address=address;
    js.type="publish";

    JSON_Value *body = json_parse_string(Body);
    JSON_Value *headers = json_parse_string(Headers);

    js.body=body;
    js.headers=headers;
    String message=NULL;
    getMessage(js,&message);

    printf("%s\n",message);
    send_frame(&message);
    free(message);
}

void eventbus_register(String address,String Headers,String Body,Handler handler){

    if(find(address)==false){
        JsonMessage js;
        js.address=address;
        js.type="register";

        JSON_Value *body = json_parse_string(Body);
        JSON_Value *headers = json_parse_string(Headers);

        js.body=body;
        js.headers=headers;
        String message=NULL;
        getMessage(js,&message);

        printf("%s\n",message);
        send_frame(&message);
        free(message);
    }
    insertFirst(node_index,handler);
    node_index++;
}

void eventbus_unregister(String address,String Headers,String Body){

    if(find(address)==false){
        JsonMessage js;
        js.address=address;
        js.type="register";

        JSON_Value *body = json_parse_string(Body);
        JSON_Value *headers = json_parse_string(Headers);

        js.body=body;
        js.headers=headers;
        String message=NULL;
        getMessage(js,&message);

        printf("%s\n",message);
        send_frame(&message);
        free(message);
    }
    delete_node(&address);
}


//test-------------------------------------------------------------------
void test(void (*func)(String *)){
    Handler h;
    h.address="pcs.status";
    String s="pcs.status";
    h.function=func;
    //handle(h,&s);

    Handler h2;
    h2.address="pcs.status1";
    h2.function=func;

    create_socket();
    eventbus_send("pcs.status","pcs.status","{\"type\":\"Maths\"}","{\"message\":\"send ok\"}");
    recieve_frame();
    eventbus_publish("pcs.status","{\"type\":\"Maths\"}","{\"message\":\"publish ok\"}");
    eventbus_register("pcs.status","{\"type\":\"Maths\"}","{\"message\":\"publish ok\"}",h);
    eventbus_register("pcs.status","{\"type\":\"Maths\"}","{\"message\":\"publish ok\"}",h2);

    printList();
    handle("pcs.status",&s);
    eventbus_unregister("pcs.status","{\"type\":\"Maths\"}","{\"message\":\"publish ok\"}");
    printList();
}




