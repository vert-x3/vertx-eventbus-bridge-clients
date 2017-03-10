#ifndef VERTX_H_
#define VERTX_H_

typedef char * String;


void setHost(String host);
void setPort(int port);
void setTimeOut(int timeout);
void create_eventbus();
void close_eventbus();
void start_eventbus();
void eventbus_send(String address,String replyAddress,String Headers,String Body);
void eventbus_publish(String address,String Headers,String Body);
void eventbus_register(String address,void (*func)(String *));
void eventbus_unregister(String address);

#endif
