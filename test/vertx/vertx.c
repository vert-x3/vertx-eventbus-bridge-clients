#include <stdio.h>
#include <string.h>
#include "parson.h"
#include "vertx.h"

// Json Message-----------------------------------------------
//get message
void getMessage(JsonMessage jsonMessage,String message){
    message=NULL;
    //init
    JSON_Value *root_value = json_value_init_object();
    JSON_Object *root_object = json_value_get_object(root_value);
    json_object_set_string(root_object, "type", jsonMessage.type);
    json_object_set_string(root_object, "address", jsonMessage.address);
    json_object_set_string(root_object, "replyAddress", jsonMessage.replyAddress);
    json_object_set_string(root_object, "headers", jsonMessage.headers);
    json_object_set_string(root_object, "body", jsonMessage.body);
    //to string
    message=json_serialize_to_string_pretty(root_value);
    //free
    json_value_free(root_value);
}

//
