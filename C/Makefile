OBJS = target/$(CROSS_TRIPLE)/parson.o target/$(CROSS_TRIPLE)/osi_socket.o target/$(CROSS_TRIPLE)/vertx.o

LDFLAGS := -static-libgcc -Wl,-Bstatic
LDEXTRA := -Wl,-Bdynamic -lpthread -s

ifeq ($(CROSS_TRIPLE), x86_64-w64-mingw32)
	LDFLAGS := -Wl,--enable-stdcall-fixup
	LDEXTRA := -lws2_32 -s
endif
ifeq ($(CROSS_TRIPLE), i686-w64-mingw32)
	LDFLAGS := -Wl,--enable-stdcall-fixup
	LDEXTRA := -lws2_32 -s
endif

ifeq ($(CROSS_TRIPLE), x86_64-apple-darwin14)
    PATH := /usr/x86_64-apple-darwin14/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/usr/osxcross/bin
    CC := o64-clang
    LDFLAGS :=
    LDEXTRA := -lpthread
endif

target/$(CROSS_TRIPLE)/example.exe: init target/$(CROSS_TRIPLE)/libeventbus.a target/$(CROSS_TRIPLE)/example.o
	$(CC) -W -Ltarget/$(CROSS_TRIPLE) -o $@ $(LDFLAGS) target/$(CROSS_TRIPLE)/example.o -leventbus $(LDEXTRA)

target/$(CROSS_TRIPLE)/example.o: test/test.c
	$(CC) -W -Iinclude -c -o $@ $< $(CFLAGS)

target/$(CROSS_TRIPLE)/libeventbus.a: init $(OBJS)
	$(AR) crv $@ $(OBJS)

target/$(CROSS_TRIPLE)/%.o: lib/%.c
	$(CC) -W -Iinclude -c -o $@ $< $(CFLAGS)

.PHONY: clean

init:
	@mkdir -p target/$(CROSS_TRIPLE)

clean:
	@rm -rf target/$(CROSS_TRIPLE)
