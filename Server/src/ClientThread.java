import com.sun.org.apache.xpath.internal.operations.Neg;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class ClientThread extends Thread {
    private final ServerSocket serverSocket;
    private Socket client;
    private DataOutputStream dOut;
    private DataInputStream in;
    private String ComputerUserName = "";
    private int threadNumber;
    private int isConnected = 0;
    private int clientType;
    private boolean[] clientMap;

    public ClientThread(ServerSocket serverSocket, int threadNumber,
                        boolean[] clientMap,String threadGeneralName,
                        int clientType) {

        this.serverSocket = serverSocket;
        this.threadNumber = threadNumber;
        this.clientMap = clientMap;
        this.clientType = clientType;
        setName( threadGeneralName + " Thread " + threadNumber);
    }

    public Socket getClient() {
        return client;
    }

    public int getConnectionStatus() {
        return isConnected;
    }

    public void run() {
        try {
            System.out.println(getName() + " Waiting...");
            synchronized (serverSocket) {
                client = serverSocket.accept();
            }


            in = new DataInputStream(new BufferedInputStream(client.getInputStream()));
            dOut = new DataOutputStream(client.getOutputStream());
            ComputerUserName += new String(receiveMessage(),"UTF-8");
            System.out.println("Starting Client Connetion with " + ComputerUserName + " At "+ client.getInetAddress());
            createDirectory(ComputerUserName);
            isConnected = 1;


        } catch(IOException | NullPointerException e) {
            System.out.println("CLIENT ERROR");
            closeClient();
        }
    }

    public void closeClient() {
        try {

            client.close();
            dOut.close();
            in.close();
            clientMap[threadNumber] = false;
            isConnected = 0;
        } catch(IOException e) {
            throw new RuntimeException("ERROR CLOSING CLIENT");
        }
    }

    public void sendMessage(String message) {
        if(isConnected == 1) {
            try {
                dOut.writeInt(message.length());
                dOut.write(message.getBytes(),0,message.length());
            } catch(IOException e) {
                closeClient();
            }
        }
    }

    public void sendFile(String filename) {
        try {

            byte[] fileContent = readFile(filename);
            if(fileContent == null)
                return;

            //sending the size of the title
            dOut.writeInt(filename.length());
            dOut.write(filename.getBytes(),0,filename.length());
            dOut.writeInt(fileContent.length);
            dOut.write(fileContent,0,fileContent.length);

            System.out.println(filename);
            System.out.println(fileContent.length);
            System.out.println("FILE SENT SUCCESFULLY");


        } catch(IOException e) {
            closeClient();
            e.printStackTrace();

        }
    }

    public void receiveFile() {
        try {


            int filenameSize = in.readInt();
            byte[] fileName = new byte[filenameSize];
            in.read(fileName,0,filenameSize);
            String newFileName = new String(fileName);
            System.out.println(newFileName);

            int sizeOfFile = in.readInt();
            if(sizeOfFile == 0)
                return;

            byte[] fileContent = new byte[sizeOfFile];

            //this part reads in a loop untill the total bytes received are >= than the total filesize received
            int total = 0;
            int count;

            while((count = in.read(fileContent)) > 0) {
                total += count;

                if(total >= sizeOfFile)
                    break;

            }
            System.out.println("THE TITLE OF THE FILE IS " + newFileName);
            System.out.println("THE SIZE OF THE FILE IS " + sizeOfFile);
            System.out.println("CONTENT OF FILE IS " + fileContent.length + " BYTES");

            //writes the content of the file to an actual file

            if(!ComputerUserName.equals("")) {
                createDirectory(ComputerUserName);
                newFileName = ComputerUserName + "/" + newFileName;
                System.out.println("FILE PATH IS : " + newFileName);
        }

            writeFile(new String(fileName),fileContent);

        }catch(IOException e) {
            e.printStackTrace();
            closeClient();

        }catch(NegativeArraySizeException n){
            n.printStackTrace();
        }

    }

    public byte[] readFile(String filename) {
        byte[] allBytes = null;
        try {
            // create a reader
            File file = new File(filename);
            if(!file.isFile()) {
                System.out.println("FILE DOESNT EXIST");
                return null;
            }
            FileInputStream fis = new FileInputStream(new File(filename));
            allBytes = new byte[(int)file.length()];
            fis.read(allBytes);
            fis.close();

        } catch (IOException ex) {
            ex.printStackTrace();
            closeClient();
        }

        return allBytes;

    }

    public void writeFile(String fileName,byte[] fileContent) {
        try {
            FileOutputStream fos = new FileOutputStream(new File(fileName));
            fos.write(fileContent);
            fos.close();
        } catch(IOException e) {
            e.printStackTrace();

        }
    }

    public byte[] receiveMessage() {
        byte[] message = null;
        int messageLength = 0;

        try {
            messageLength = in.readInt();

            message = new byte[messageLength];
            in.read(message,0,messageLength);

        } catch (IOException e) {
            System.out.println(e);
            closeClient();

        }

        return message;

    }

    public String getComputerUserName() {
        return ComputerUserName;
    }

    public void createDirectory(String name) {
        new File(name).mkdirs();
    }




}
