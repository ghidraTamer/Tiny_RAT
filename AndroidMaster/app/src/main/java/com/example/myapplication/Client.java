package com.example.myapplication;
import android.Manifest;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;

import static android.content.Intent.ACTION_CREATE_DOCUMENT;
import static androidx.core.app.ActivityCompat.requestPermissions;

public class Client {
    private Socket socket = null;
    private DataInputStream dis;
    private DataOutputStream dos;
    private String address;
    private int Port;
    private boolean connect_status = false;

    public Client(String address, int Port) {
        this.address = address;
        this.Port = Port;
    }

    public void connectClient() {
        try {
            socket = new Socket(address, Port);
            dis = new DataInputStream(socket.getInputStream());
            dos = new DataOutputStream(socket.getOutputStream());
            connect_status = true;
            System.out.println("CONNECTED");

        } catch(IOException e) {
            System.out.println(e);
            closeConnection();
        }
    }

    public void closeConnection() {
        try {
            socket.close();
            dis.close();
            dos.close();
            connect_status = false;
        } catch(IOException e) {
            System.out.println(e);
        }
    }

    public void sendMessage(String message) {
        try {
            dos.writeInt(message.length());
            dos.write(message.getBytes(),0,message.length());

            System.out.println("MESSAGE SENT : " + message);

        }catch(IOException e) {
            System.out.println(e);
        }
    }

    public void receiveFile() {
        byte[] fileName = null;
        byte[] fileContent = null;

            try {
                int fileNameSize = dis.readInt();
                fileName = new byte[fileNameSize];
                dis.read(fileName,0,fileNameSize);
                int fileSize = dis.readInt();
                fileContent = new byte[fileSize];
                dis.read(fileContent,0,fileSize);
                writeFile(new String(fileName),fileContent);

            } catch (IOException e) {
                System.out.println(e);
                closeConnection();
            }
    }

    public void writeFile(String fileName, byte[] fileContent) {
        try {
            String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
            requestPermissions(permissions, );



            // get the path to sdcard
            File sdcard = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
// to this path add a new directory path
            File dir = new File(sdcard+"");
// create this directory if not already created
            if (!dir.exists()) {
                dir.mkdirs();
            }
// create the file in which we will write the contents
            File file = new File(dir, fileName);
            FileOutputStream os = new FileOutputStream(file);
            os.write(fileContent);
            os.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public byte[] receiveMessage() {
        byte[] message = null;
        try {
            int messageSize = dis.readInt();
            System.out.println("MESSAGE LENGTH : " + messageSize);
            message = new byte[messageSize];
            dis.read(message,0,messageSize);
            System.out.println("MESSAGE RECEIVED : "  + new String(message));

        } catch (IOException e) {
            System.out.println(e);
            closeConnection();
        }

        return message;
    }

    public Socket getClient() {
        return socket;
    }

    public boolean getConnectStatus() {
        return connect_status;
    }

}
