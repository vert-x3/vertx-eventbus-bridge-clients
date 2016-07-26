#ifndef VERTX_H_
#define VERTX_H_

typedef char * String;

void setHost(String host);
void setPort(int port);
void setTimeOut(int timeout);
void create_socket();
void test(void (*func)(String *));
#endif
