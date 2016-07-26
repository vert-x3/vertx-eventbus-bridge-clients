#include <stdio.h>
#include <stdlib.h>
#include "vertx/vertx.h"

void function(String *msg);
int main(){
    test(function);
    return 0;
}

void function(String *msg){
printf("%s\n",*msg);
}

