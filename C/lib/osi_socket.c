#include <stdio.h>
#include "osi_socket.h"

#if defined _WIN32
void osi_perror(const char *s)
{
	unsigned int winerr;

	winerr = WSAGetLastError();

	switch(winerr)
	{
    case WSAEINTR: printf("%s: Interrupted system call\n", s); return;
    case WSAEBADF: printf("%s: Bad file number\n", s); return;
    case WSAEACCES: printf("%s: Permission denied\n", s); return;
    case WSAEFAULT: printf("%s: Bad address\n", s); return;
    case WSAEINVAL: printf("%s: Invalid argument\n", s); return;
    case WSAEMFILE: printf("%s: Too many open sockets\n", s); return;
    case WSAEWOULDBLOCK: printf("%s: Operation would block\n", s); return;
    case WSAEINPROGRESS: printf("%s: Operation now in progress\n", s); return;
    case WSAEALREADY: printf("%s: Operation already in progress\n", s); return;
    case WSAENOTSOCK: printf("%s: Socket operation on non-socket\n", s); return;
    case WSAEDESTADDRREQ: printf("%s: Destination address required\n", s); return;
    case WSAEMSGSIZE: printf("%s: Message too long\n", s); return;
    case WSAEPROTOTYPE: printf("%s: Protocol wrong type for socket\n", s); return;
    case WSAENOPROTOOPT: printf("%s: Bad protocol option\n", s); return;
    case WSAEPROTONOSUPPORT: printf("%s: Protocol not supported\n", s); return;
    case WSAESOCKTNOSUPPORT: printf("%s: Socket type not supported\n", s); return;
    case WSAEOPNOTSUPP: printf("%s: Operation not supported on socket\n", s); return;
    case WSAEPFNOSUPPORT: printf("%s: Protocol family not supported\n", s); return;
    case WSAEAFNOSUPPORT: printf("%s: Address family not supported\n", s); return;
    case WSAEADDRINUSE: printf("%s: Address already in use\n", s); return;
    case WSAEADDRNOTAVAIL: printf("%s: Can't assign requested address\n", s); return;
    case WSAENETDOWN: printf("%s: Network is down\n", s); return;
    case WSAENETUNREACH: printf("%s: Network is unreachable\n", s); return;
    case WSAENETRESET: printf("%s: Net connection reset\n", s); return;
    case WSAECONNABORTED: printf("%s: Software caused connection abort\n", s); return;
    case WSAECONNRESET: printf("%s: Connection reset by peer\n", s); return;
    case WSAENOBUFS: printf("%s: No buffer space available\n", s); return;
    case WSAEISCONN: printf("%s: Socket is already connected\n", s); return;
    case WSAENOTCONN: printf("%s: Socket is not connected\n", s); return;
    case WSAESHUTDOWN: printf("%s: Can't send after socket shutdown\n", s); return;
    case WSAETOOMANYREFS: printf("%s: Too many references, can't splice\n", s); return;
    case WSAETIMEDOUT: printf("%s: Connection timed out\n", s); return;
    case WSAECONNREFUSED: printf("%s: Connection refused\n", s); return;
    case WSAELOOP: printf("%s: Too many levels of symbolic links\n", s); return;
    case WSAENAMETOOLONG: printf("%s: File name too long\n", s); return;
    case WSAEHOSTDOWN: printf("%s: Host is down\n", s); return;
    case WSAEHOSTUNREACH: printf("%s: No route to host\n", s); return;
    case WSAENOTEMPTY: printf("%s: Directory not empty\n", s); return;
    case WSAEPROCLIM: printf("%s: Too many processes\n", s); return;
    case WSAEUSERS: printf("%s: Too many users\n", s); return;
    case WSAEDQUOT: printf("%s: Disc quota exceeded\n", s); return;
    case WSAESTALE: printf("%s: Stale NFS file handle\n", s); return;
    case WSAEREMOTE: printf("%s: Too many levels of remote in path\n", s); return;
    case WSASYSNOTREADY: printf("%s: Network system is unavailable\n", s); return;
    case WSAVERNOTSUPPORTED: printf("%s: Winsock version out of range\n", s); return;
    case WSANOTINITIALISED: printf("%s: WSAStartup not yet called\n", s); return;
    case WSAEDISCON: printf("%s: Graceful shutdown in progress\n", s); return;
    case WSAHOST_NOT_FOUND: printf("%s: Host not found\n", s); return;
    case WSANO_DATA: printf("%s: No host data of that type was found\n", s); return;
	}
	perror(s);
}
#endif

