#include "vertx.h"

int main(){
JsonMessage js;
js.address="vertx";
js.replyAddress="vertx";
js.type="send";
String message;
getMessage(js,message);

printf("%s\n",message);

return 0;
}
