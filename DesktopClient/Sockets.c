#include <string.h>
#include <stdio.h>
#include <string.h>
#include <WinSock2.h>
#include <ws2tcpip.h>
#include <winbase.h>
#include <Windows.h>

// Need to link with Ws2_32.lib, Mswsock.lib, and Advapi32.lib
#pragma comment (lib, "Ws2_32.lib")
#pragma comment (lib, "Mswsock.lib")
#pragma comment (lib, "AdvApi32.lib")
#pragma warning (disable :4996)


SOCKET* __stdcall makeConnection(char* IpAddress, char* PORT) {
    SOCKET* clientSocket = calloc(1, sizeof(SOCKET));
    struct addrinfo hints, * results, * iteration;
    int iResult = -1;

    memset(&hints, 0, sizeof(hints));
    hints.ai_family = AF_UNSPEC;
    hints.ai_socktype = SOCK_STREAM;
    hints.ai_protocol = IPPROTO_TCP;

    iResult = getaddrinfo(IpAddress, PORT, &hints, &results);
    if (iResult != 0) {
        printf("GetAddrInfo : ERROR\n");

        return NULL;
    }

    for (iteration = results; iteration != NULL; iteration = iteration->ai_next) {
        *clientSocket = socket(iteration->ai_family, iteration->ai_socktype, iteration->ai_protocol);
        if (*clientSocket == SOCKET_ERROR) {
            printf("SOCKET : ERROR\n");
            continue;
        }

        iResult = connect(*clientSocket, iteration->ai_addr, iteration->ai_addrlen);
        if (iResult != 0) {
            printf("Connect : Error\n");
            continue;
        }

        break;
    }

    if (iResult != 0) {
        printf("Connection Error..\n");
        return NULL;
    }

    freeaddrinfo(results);

    printf("Connected...\n");
    return clientSocket;

}

int sendMessage(SOCKET ClientSocket, char* message) {
    int iResult = -1;
    int messageLength = htonl(strlen(message));
    int total = 0;

    iResult = send(ClientSocket, &messageLength, sizeof(int), 0);
    if (iResult <= 0) {
        printf("Send : ERROR\n");
        return -1;
    }

    while (total != strlen(message)) {
        total += send(ClientSocket, message, strlen(message), 0);
        if (total == -1) {
            printf("Send : ERROR\n");
            return -1;
        }
    }


    return total;
}