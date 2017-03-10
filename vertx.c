
#include <stdio.h>
#include <string.h>
#include "parson.h"
#include "vertx.h"
#include "osi_socket.h"
#include <stdlib.h>
#include <errno.h>
#include <unistd.h>
#include <stdbool.h>
 
#define DEFAULT_BUFLEN 4

// Handlers -------------------------------------------------

//This is the structure for handler
typedef struct Handler_t{
    String address;
    void (*function)(String *);
} handler_t;


// Linked list-----------------------------------------------------
struct node
{
   handler_t data;
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
void insertFirst(int key, handler_t handler){
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

//This is the structure for jsonMessage_t
typedef struct jsonMessage_t{
    String type;
    String address;
    JSON_Value *headers;
    String replyAddress;
    JSON_Value *body;
} jsonMessage_t;

//get message - string
void getMessage(jsonMessage_t jsonMessage,String* message){
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

/*Event bus constructor
#	input parameters
#		1) host	- String
#		2) port	- integer(>2^10-1)
#		3) TimeOut - int- receive TimeOut
#	inside parameters
#		1) socket
#		2) handlers - Linked List
#		3) state -integer
#       4) INIT,FLAG
#		5) STATE_MUTEX
#       6) receive_thread
#		7) OS - 1=windows 0=unix
#Event bus state
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

#ifdef _WIN32
	HANDLE receive_thread;
#endif // _WIN32
#ifdef __unix__
    pthread_t receive_thread;
	pthread_mutex_t STATE_MUTEX = PTHREAD_MUTEX_INITIALIZER; 
#endif // linux

void setHost(String host){
    HOST=host;
}

void setPort(int port){
    PORT=port;
}

void setTimeOut(int timeout){
    TIMEOUT=timeout;
}

void create_eventbus(){
#ifdef _WIN32
    SOCKADDR_IN   ServerAddr;
#endif // _WIN32
#ifdef __unix__
    struct sockaddr_in  ServerAddr;
#endif // linux
     int  RetCode;
     osi_socket_startup;

     SendingSocket = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP);
	 
	 #ifdef _WIN32
     WaitForSingleObject(STATE_MUTEX, INFINITE);
	 #endif
	 #ifdef __unix__
     pthread_mutex_lock(&STATE_MUTEX);
	 #endif 
     STATE = 1; //connecting
     #ifdef _WIN32
     ReleaseMutex(STATE_MUTEX);
	 #endif
	 #ifdef __unix__
     pthread_mutex_unlock(&STATE_MUTEX);
	 #endif
	 
     if(SendingSocket == INVALID_SOCKET)
     {
          perror("Client: socket() failed\n");
          exit(1);
     }
     setsockopt(SendingSocket,SO_RCVTIMEO, 1000,  (char*)&FLAG, sizeof(int));
     ServerAddr.sin_family = AF_INET;
     ServerAddr.sin_port = htons(PORT);
     ServerAddr.sin_addr.s_addr = inet_addr(HOST);

#ifdef _WIN32
     RetCode = connect(SendingSocket, (SOCKADDR *) &ServerAddr, sizeof(ServerAddr));
#endif // _WIN32
#ifdef __unix__
     RetCode = connect(SendingSocket, &ServerAddr, sizeof(ServerAddr));
#endif // linux


     if(RetCode != 0)
     {
          perror("Client: connect() failed\n");
          osi_socket_close(SendingSocket);
          exit(1);
     }
     #ifdef _WIN32
     WaitForSingleObject(STATE_MUTEX, INFINITE);
	 #endif
	 #ifdef __unix__
     pthread_mutex_lock(&STATE_MUTEX);
	 #endif 
     STATE = 2; //connected
	 #ifdef _WIN32
     ReleaseMutex(STATE_MUTEX);
	 #endif
	 #ifdef __unix__
     pthread_mutex_unlock(&STATE_MUTEX);
	 #endif
}

void recieve_frame(void * i){
  fd_set active_fd_set, read_fd_set;
  FD_ZERO (&active_fd_set);
  FD_SET (SendingSocket, &active_fd_set);
  int retVal;
  const String type_="type",address_="address",type_message_="message",type_err_="err";
  while(1){
     #ifdef _WIN32
     WaitForSingleObject(STATE_MUTEX, INFINITE);
	 #endif
	 #ifdef __unix__
     pthread_mutex_lock(&STATE_MUTEX);
	 #endif
      if(STATE==2){
		 #ifdef _WIN32
		 ReleaseMutex(STATE_MUTEX);
		 #endif
		 #ifdef __unix__
		 pthread_mutex_unlock(&STATE_MUTEX);
		 #endif
          read_fd_set = active_fd_set;
          if (select (FD_SETSIZE, &read_fd_set, NULL, NULL, NULL) < 0){
              perror ("select");
              exit (EXIT_FAILURE);
          }
		  
            if (FD_ISSET (SendingSocket, &read_fd_set)){
                //lock
                char length_buffer[4];
                retVal = osi_socket_read(SendingSocket, length_buffer, 4);
                if ( retVal > 0 ){
                    unsigned int num=0,num1=0,num2=0,num3=0;
                    num=(length_buffer[3]);
                    num1=(length_buffer[2]);
                    num2=(length_buffer[1]);
                    num3=(length_buffer[0]);
                    num=num | (num1<<8);
                    num=num | (num2<<16);
                    num=num | (num3<<24);
                    char message_buffer[num];
                    //get message
                    retVal = osi_socket_read(SendingSocket, message_buffer, (num));
					//printf("%s\n",message_buffer);
                    if ( retVal > 0 ){
                        String type,address;
                        //type
                        type=json_object_get_string(json_object(json_parse_string(message_buffer)),type_);
                        //message
                        if(strcmp(type,type_message_)==0){
                            //address
                            address=json_object_get_string(json_object(json_parse_string(message_buffer)),address_);
                            String message=json_serialize_to_string_pretty(json_parse_string(message_buffer));
                            handle(address,&message);
                            free(message);
                        }
                        //error
                        else if(strcmp(type,type_err_)==0){
                            String message=json_serialize_to_string_pretty(json_parse_string(message_buffer));
                            perror(strcat( "Error occurred ",message));
                            free(message);
                        }
                        free(type);
                        free(address);
                    }
                }
            }
      }else{
             #ifdef _WIN32
			 ReleaseMutex(STATE_MUTEX);
			 #endif
			 #ifdef __unix__
			 pthread_mutex_unlock(&STATE_MUTEX);
			 #endif
            return;
      }
  }
}

void send_frame(String * message){
    //length
    char buffer[4];
    int length=(int)strlen(*message);
    buffer[0] = length >> 24;
    buffer[1] = length >> 16;
    buffer[2] = length >> 8;
    buffer[3] = length;
    INIT = osi_socket_write( SendingSocket, buffer, 4);
    INIT = osi_socket_write( SendingSocket, *message, (int)strlen(*message));
    // printf("\n----------------message %s------------------------\n\n",*message);
} 

void start_eventbus(){
    if(STATE!=2){
        perror("socket not connected\n");
        return;
    }
    
	#ifdef __unix__
    if(pthread_create(&receive_thread, NULL, recieve_frame,NULL)) {
        fprintf(stderr, "Error creating thread\n");
        return;
    }
	#endif
	#ifdef _WIN32
	receive_thread = CreateThread(NULL, 0, recieve_frame, NULL, 0, NULL);
	#endif
}
//close socket
void close_eventbus(){
    if(STATE==1 ){
        osi_socket_close(SendingSocket);
    }else{
     #ifdef _WIN32
     WaitForSingleObject(STATE_MUTEX, INFINITE);
	 #endif
	 #ifdef __unix__
     pthread_mutex_lock(&STATE_MUTEX);
	 #endif
        STATE=3; //closing socket
     #ifdef _WIN32
     ReleaseMutex(STATE_MUTEX);
	 #endif
	 #ifdef __unix__
     pthread_mutex_unlock(&STATE_MUTEX);
	 #endif
	 
		#ifdef _WIN32
		CloseHandle(receive_thread);
		#endif
		#ifdef __unix__
        while(pthread_cancel(receive_thread)!=0){
			sleep(1);
        }
		#endif
		
        if(osi_socket_close(SendingSocket)!=0){
            perror("Error occurred at closing socket");
        }
        STATE=4; //closed	
    }
	#ifdef _WIN32
	CloseHandle(STATE_MUTEX);
	#endif
}


// send publish register unregister-------------------------------------------------------------

void eventbus_send(String address,String replyAddress,String Headers,String Body){
	
    jsonMessage_t js;
    js.address=address;
    js.replyAddress=replyAddress;
    js.type="send";

    JSON_Value *body = json_parse_string(Body);
    JSON_Value *headers = json_parse_string(Headers);

    js.body=body;
    js.headers=headers;
    String message=NULL;
    getMessage(js,&message);
    send_frame(&message);
    free(message);
}

void eventbus_publish(String address,String Headers,String Body){
    jsonMessage_t js;
    js.address=address;
    js.type="publish";
	js.replyAddress=NULL;
    JSON_Value *body = json_parse_string(Body);
    JSON_Value *headers = json_parse_string(Headers);

    js.body=body;
    js.headers=headers;
    String message=NULL;
    getMessage(js,&message);
    send_frame(&message);
    free(message);
}

void eventbus_register(String address,void (*func)(String *)){
    handler_t handler;
    handler.address=address;
    handler.function=func;
	
    if(find(address)==false){
        jsonMessage_t js;
        js.address=address;
        js.type="register";
		js.replyAddress=NULL;
        JSON_Value *body = json_parse_string(NULL);
        JSON_Value *headers = json_parse_string(NULL);

        js.body=body;
        js.headers=headers;
        String message=NULL;
        getMessage(js,&message);
		
        send_frame(&message);
	
        free(message);
    }
    insertFirst(node_index,handler);
    node_index++;
}

void eventbus_unregister(String address){

    if(find(address)==false){
        jsonMessage_t js;
        js.address=address;
        js.type="register";
		js.replyAddress=NULL;
        JSON_Value *body = json_parse_string(NULL);
        JSON_Value *headers = json_parse_string(NULL);

        js.body=body;
        js.headers=headers;
        String message=NULL;
        getMessage(js,&message);
        send_frame(&message);
        free(message);
    }
    delete_node(&address);
}





