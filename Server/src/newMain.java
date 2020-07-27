import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.atomic.AtomicInteger;

public class newMain {
    private static AtomicInteger exitCondition = new AtomicInteger(0);
    private static int response = -1;
    private static BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));
    private static String fileName;
    private static String command;
    private static String IpAddress;
    private static String Path;
    private static int ANDROID_THREAD_NUMBERS = 1;
    private static int DESKTOP_THREAD_NUMBERS = 3;
    private static Server DesktopServer = new Server(21574, DESKTOP_THREAD_NUMBERS, "Desktop", 0);
    private static Server AndroidServer = new Server(21375, ANDROID_THREAD_NUMBERS, "Android", 1);

    public static void main(String[] args) {
        Thread DesktopServerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                DesktopServer.startServer();
            }
        });
        DesktopServerThread.start();

        Thread AndroidServerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                AndroidServer.startServer();
            }
        });
        AndroidServerThread.start();

        Thread androidMessage = new Thread(new Runnable() {
            @Override
            public void run() {
                AndroidInputControll();
            }
        });
        androidMessage.start();

        UserInputControll();

    }

    public static void UserInputControll() {
        while (exitCondition.get() == 0) {
            try {
                System.out.println("\n0. Print All Connected Clients\n"
                        + "1. Send File To All Clients\n"
                        + "2. Send File To Specific Client\n"
                        + "3. Request File From All Clients\n"
                        + "4. Reqeust File From Specific Client\n"
                        + "5. Send Command To all Clients\n"
                        + "6. Send Command To Specific Client\n"
                        + "7. Search For File\n"
                        + "8. Close Server\n");


                response = Integer.parseInt(userInput.readLine());

                switch (response) {

                    case 0:
                        DesktopServer.printIpAddresses();
                        response = -1;
                        break;

                    case 1:
                        System.out.println("Enter File Name");
                        fileName = userInput.readLine();
                        DesktopServer.sendMessage("1", true, null);
                        DesktopServer.sendFile(fileName, true, null);
                        response = -1;
                        break;

                    case 2:
                        System.out.println("Enter File Name");
                        fileName = userInput.readLine();
                        System.out.println("Enter IpAddress");
                        IpAddress = userInput.readLine();
                        DesktopServer.sendMessage("1", false, IpAddress);
                        DesktopServer.sendFile(fileName, false, IpAddress);
                        response = -1;
                        break;

                    case 3:
                        System.out.println("Enter File Name");
                        fileName = userInput.readLine();
                        DesktopServer.sendMessage("2", true, null);
                        DesktopServer.requestFile(fileName, true, null);
                        response = -1;
                        break;

                    case 4:
                        System.out.println("Enter File Name");
                        fileName = userInput.readLine();
                        System.out.println("Enter Ip Address");
                        IpAddress = userInput.readLine();
                        DesktopServer.sendMessage("2", false, IpAddress);
                        DesktopServer.requestFile(fileName, false, IpAddress);
                        response = -1;
                        break;

                    case 5:
                        System.out.println("Execute File (send path)");
                        command = userInput.readLine();
                        DesktopServer.sendMessage("3", true, null);
                        DesktopServer.sendMessage(command, true, null);
                        response = -1;
                        break;

                    case 6:
                        System.out.println("Execute File (send path)");
                        command = userInput.readLine();
                        System.out.println("Enter Ip Address");
                        IpAddress = userInput.readLine();
                        DesktopServer.sendMessage("3", false, IpAddress);
                        DesktopServer.sendMessage(command, false, IpAddress);
                        response = -1;
                        break;

                    case 7:
                        System.out.println("Enter File Name : ");
                        fileName = userInput.readLine();
                        System.out.println("Enter Path (ex : C:/* , it must have a wildcard) or DEFAULT for C:/* Path");
                        String Path = userInput.readLine();
                        if (Path.equals("DEFAULT"))
                            Path = "C:/*";
                        DesktopServer.sendMessage("4", true, null);
                        DesktopServer.sendMessage(fileName, true, null);
                        DesktopServer.sendMessage(Path, true, null);
                        response = -1;
                        break;

                    case 8:
                        System.out.println("Server Is Closing...");
                        exitCondition.set(1);
                        DesktopServer.closeServer();
                        AndroidServer.closeServer();
                        break;

                }

            } catch (IOException e) {
                throw new RuntimeException("SYSTEM IN ERROR");
            }
        }
        System.out.println("Server Closed...");
    }

    public static void AndroidInputControll() {
        ClientThread[] androidClientThread = AndroidServer.getClientThreads();
        ClientThread[] desktopClientthread = DesktopServer.getClientThreads();
        byte[] message;
        String messageString;
        String IpAddress;
        String command;
        while (exitCondition.get() == 0) {
            for (int i = 0; i < ANDROID_THREAD_NUMBERS; i++) {
                if (androidClientThread[i] != null && androidClientThread[i].getConnectionStatus() == 1) {
                    message = androidClientThread[i].receiveMessage();
                    if (message != null) {
                        messageString = new String(message);

                        switch (messageString) {
                            //cta = command_to_all
                            case "cta":
                                command = new String(androidClientThread[i].receiveMessage());
                                DesktopServer.sendMessage("3", true, null);
                                DesktopServer.sendMessage(command, true, null);
                                break;
                            //csp == command_specific
                            case "csp":
                                command = new String(androidClientThread[i].receiveMessage());
                                IpAddress = new String(androidClientThread[i].receiveMessage());
                                DesktopServer.sendMessage("3", false, IpAddress);
                                DesktopServer.sendMessage(command, false, IpAddress);
                                break;
                            //fta == file_to_all
                            case "fta":
                                fileName = new String(androidClientThread[i].receiveMessage());
                                DesktopServer.sendMessage("1", true, null);
                                DesktopServer.sendFile(fileName, true, null);
                                break;
                            //fsp == file_specific
                            case "fsp":
                                fileName = new String(androidClientThread[i].receiveMessage());
                                IpAddress = new String(androidClientThread[i].receiveMessage());
                                DesktopServer.sendMessage("1", false, IpAddress);
                                DesktopServer.sendFile(fileName, false, IpAddress);
                                break;
                            //rfa == recv_from_all
                            case "rfa":
                                fileName = new String(androidClientThread[i].receiveMessage());
                                DesktopServer.sendMessage("2", true, null);
                                DesktopServer.requestFile(fileName, true, null);
                                AndroidServer.sendFile(fileName, true, null);
                                break;
                            //rsp == recv_specific
                            case "rsp":
                                fileName = new String(androidClientThread[i].receiveMessage());
                                IpAddress = new String(androidClientThread[i].receiveMessage());
                                DesktopServer.sendMessage("2", false, IpAddress);
                                DesktopServer.requestFile(fileName, false, IpAddress);
                                AndroidServer.requestFile(fileName, false, IpAddress);
                                break;
                            //sfa == search_from_all
                            case "sfa":
                                fileName = new String(androidClientThread[i].receiveMessage());
                                Path = new String(androidClientThread[i].receiveMessage());
                                if (Path.equals("Path"))
                                    Path = "C:/*";
                                DesktopServer.sendMessage("4", true, null);
                                DesktopServer.sendMessage(fileName, true, null);
                                DesktopServer.sendMessage(Path, true, null);
                                break;
                            //sfs == search_from_specific
                            case "sfs":
                                fileName = new String(androidClientThread[i].receiveMessage());
                                String Path = new String(androidClientThread[i].receiveMessage());
                                if (Path.equals("Path"))
                                    Path = "C:/*";
                                IpAddress = new String(androidClientThread[i].receiveMessage());
                                DesktopServer.sendMessage("4", false, IpAddress);
                                DesktopServer.sendMessage(fileName, false, IpAddress);
                                DesktopServer.sendMessage(Path, false, IpAddress);
                                break;
                            //scc == send_connected_clients
                            case "scc":
                                int connected_clients = 0;
                                for(int j = 0; j < DESKTOP_THREAD_NUMBERS; j++) {
                                    if(desktopClientthread[j].getConnectionStatus() == 1)
                                        connected_clients++;
                                }
                                androidClientThread[i].sendMessage(String.valueOf(connected_clients));
                                if(connected_clients == 0) {
                                    break;
                                }

                                for(int j = 0; j < DESKTOP_THREAD_NUMBERS; j++) {
                                    if(desktopClientthread[j].getConnectionStatus() == 1) {
                                        System.out.println("A PARCURS ODATA");
                                        System.out.println(desktopClientthread[j].getClient().getInetAddress().getHostAddress());
                                        androidClientThread[i].sendMessage(desktopClientthread[j].getClient().getInetAddress().getHostAddress());
                                        System.out.println(desktopClientthread[j].getComputerUserName());
                                        androidClientThread[i].sendMessage(desktopClientthread[j].getComputerUserName());
                                    }
                                }
                                break;
                        }


                    }

                }
            }

        }

        System.out.println("Android Server Closed...");
    }
}
