
CC = gcc
CFLAGS = -w -g

INCLUDES = -I./include \

LFLAGS = -L./lib \

#this is for windows. Comment this if you are using Unix/Linux
LIBS = -lws2_32 --enable-stdcall-fixup
#this is for Unix/Linux. Comment this if you are using Windows
#LIBS = -lpthread --enable-stdcall-fixup

SRCS = vertx.c lib/parson.c test/test.c lib/osi_socket.c 

OBJS = $(SRCS:.c=.o)

MAIN = test

.PHONY: depend clean

all:    $(MAIN)

$(MAIN): $(OBJS) 
		$(CC) $(CFLAGS) $(INCLUDES) -o $(MAIN) $(OBJS) $(LFLAGS) $(LIBS)

.c.o:
		$(CC) $(CFLAGS) $(INCLUDES) -c $<  -o $@

clean:
		$(RM) *.o *~ $(MAIN)

depend: $(SRCS)
		makedepend $(INCLUDES) $^

# DO NOT DELETE THIS LINE -- make depend needs it