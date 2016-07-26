#ifndef VERTX_H_
#define VERTX_H_

typedef char * String;

//This is the structure for JsonMessage
typedef struct JsonMessage{
    String type;
    String address;
    String headers;
    String replyAddress;
    String body;
} JsonMessage;
void getMessage(JsonMessage jsonMessage,String *message);

#endif
