import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.util.concurrent.atomic.AtomicInteger;

public class Server {
    private ServerSocket serverSocket;
    private ClientThread[] clientThreads;
    private int NUM_THREADS = 0;
    private int PORT;
    private int supportedClientType;
    private boolean[] ClientMap;
    private AtomicInteger exitCondition = new AtomicInteger(0);
    private String threadGeneralName;

    public Server(int PORT, int NUM_THREADS,String threadGeneralName, int supportedClientType) {
      this.PORT = PORT;
      this.NUM_THREADS = NUM_THREADS;
      this.threadGeneralName = threadGeneralName;
      this.clientThreads = new ClientThread[NUM_THREADS];
      this.ClientMap = new boolean[NUM_THREADS];
      this.supportedClientType = supportedClientType;
    }

    public void startServer() {
        try {
            serverSocket = new ServerSocket(PORT);

        } catch (IOException e){
            System.out.println("SERVER COULD NOT BE CREATED");
        }

        for(int i = 0; i < NUM_THREADS; i++) {
            ClientMap[i] = false;
        }

        while(exitCondition.get() == 0) {

            for(int i = 0; i < NUM_THREADS; i++) {
                if(ClientMap[i] == false) {
                    clientThreads[i] = new ClientThread(serverSocket, i, ClientMap,threadGeneralName,supportedClientType);
                    clientThreads[i].start();
                    ClientMap[i] = true;
                }
            }
        }

        for(int i = 0; i < NUM_THREADS; i++) {
            if(clientThreads[i].getConnectionStatus() == 1)
                clientThreads[i].closeClient();
        }

    }

    public void sendMessage(String message, boolean toAll, String IpAddress) {
        if(toAll == false) {
            int clientIndex = getClientIndex(IpAddress);
            if(clientIndex == -1) {
                System.out.println("IP ADDRESS ISNT IN THE SET OR ISNT ONLINE");
                return;
            }

            clientThreads[clientIndex].sendMessage(message);
            return;
        }

        for (int i = 0; i < NUM_THREADS; i++) {
            if (clientThreads[i].getConnectionStatus() == 1) {
                clientThreads[i].sendMessage(message);
            }
        }

    }

    public void sendFile(String filename,boolean toAll,String IpAddress) {
        if(checkFileExistence(filename) == false)
            return;

        if(toAll == false) {

            int clientIndex = getClientIndex(IpAddress);
            if(clientIndex == -1) {
                System.out.println("IP ADDRESS ISNT IN THE SET OR ISNT ONLINE");
                return;
            }

            clientThreads[clientIndex].sendFile(filename);
            return;
        }


        for (int i = 0; i < NUM_THREADS; i++) {
            if (clientThreads[i].getConnectionStatus() == 1) {
                clientThreads[i].sendFile(filename);
            }
        }
    }

    public void requestFile(String filename, boolean toAll, String IpAddress) {
        if(toAll == false) {

            int clientIndex = getClientIndex(IpAddress);
            if(clientIndex == -1) {
                System.out.println("IP ADDRESS ISNT IN THE SET OR ISNT ONLINE");
                return;
            }
            clientThreads[clientIndex].sendMessage(filename);
            clientThreads[clientIndex].receiveFile();

            return;

        }

        for (int i = 0; i < NUM_THREADS; i++) {
            if (clientThreads[i].getConnectionStatus() == 1) {
                clientThreads[i].sendMessage(filename);
                clientThreads[i].receiveFile();
            }
        }
    }

    public void printIpAddresses() {
        for(int i = 0;i < NUM_THREADS; i++){
            if(clientThreads[i].getConnectionStatus() == 1)
                System.out.println(clientThreads[i].getName() +" : "+ clientThreads[i].getClient().getInetAddress());
        }
    }

    public void closeServer() {
        exitCondition.set(1);
    }

    public int getClientIndex(String IpAddress) {
        for(int i = 0; i < NUM_THREADS; i++) {
            if(clientThreads[i].getConnectionStatus() == 1) {
                if ((clientThreads[i].getClient()).getInetAddress().toString().equals("/"+IpAddress))
                    return i;
            }
        }
        return -1;
    }

    public ClientThread[] getClientThreads() {
        return clientThreads;
    }

    public boolean checkFileExistence(String fileName) {
        File file = new File(fileName);
        return file.isFile();
    }

    public boolean[] getClientMap() {
        return ClientMap;
    }

    
}


