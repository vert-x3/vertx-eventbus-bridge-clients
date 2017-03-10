#ifndef __OS_INDEPENDENT_SOCKETS_WIN32_COMPAT
#define __OS_INDEPENDENT_SOCKETS_WIN32_COMPAT
#include <errno.h>

#if defined _WIN32
#include <conio.h>
#include <stdio.h>
#include <windows.h>
#include <process.h>
#define STATE_MUTEX CreateMutex( NULL, FALSE, NULL)
#include <winsock2.h>
/* This is a broken replacement since I am ignoring the 3rd argument */
#define setenv(a,b,c) SetEnvironmentVariable(a,b)
#define unsetenv(a) SetEnvironmentVariable(a,NULL)

void osi_perror(const char *s);

#define osi_socket_startup WSADATA wsaData; (void) WSAStartup(MAKEWORD(2,2), &wsaData)

#define osi_socket_write(a,b,c) send(a,b,c,0)
#define osi_socket_read(a,b,c) recv(a,(char*)b,c,0)
#define osi_socket_close(a) closesocket(a)

#else
#include <sys/select.h>
#include <sys/socket.h>
#include <fcntl.h>
#include <netinet/in.h>
#include <netinet/tcp.h>
#include <netdb.h>
#include <unistd.h>
#include <pthread.h>
#define osi_socket_startup

#define osi_perror(a) perror(a)
#define osi_socket_write(a,b,c) write(a,b,c)
#define osi_socket_read(a,b,c) read(a,b,c)
#define osi_socket_close(a) close(a)

#define INVALID_SOCKET -1
#define SOCKET int

#endif

#endif
