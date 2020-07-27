package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static Client client;
    private static EditText commandField;
    private static EditText IpAddress;
    private static EditText fileName;
    private static TextView connectedStatus;
    private static EditText searchField;
    private static EditText pathField;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        new Thread() {
            public void run() {
                client = new Client("192.168.1.2",21375);
                client.connectClient();
                client.sendMessage("HACK_SHIT31337");
            }
        }.start();
        setContentView(R.layout.activity_main);

        Button sendButton = findViewById(R.id.sendCommand);
        Button sendFile = findViewById(R.id.sendFile);
        Button recvFile = findViewById(R.id.receiveFilebutton);
        Button searchFile = findViewById(R.id.searchButon);
        Button openActivity = findViewById(R.id.viewConnected);
        commandField = findViewById(R.id.commandText);
        IpAddress = findViewById(R.id.IpAdderssText);
        fileName = findViewById(R.id.fileNameText);
        searchField = findViewById(R.id.searchFileField);
        pathField = findViewById(R.id.pathField);
        connectedStatus = findViewById(R.id.ConnectedStatus);

        connectedStatus.setText("Connected : " + client.getConnectStatus());


        sendButton.setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.sendCommand:new Thread(new Runnable() {
                @Override
                public void run() {
                    if(client.getConnectStatus() == false)
                        return;

                    if(IpAddress.getText().toString().equals("IpAddress") || IpAddress.getText().toString().equals("")) {
                        //cta == command_to_all
                        client.sendMessage("cta");
                        client.sendMessage(commandField.getText().toString());
                    }
                    else {
                        //csp == command_specific
                        client.sendMessage("csp");
                        client.sendMessage(commandField.getText().toString());
                        client.sendMessage(IpAddress.getText().toString());
                    }

                }
            }).start();
            break;

            case R.id.sendFile:new Thread(new Runnable() {
                @Override
                public void run() {
                    if(client.getConnectStatus() == false)
                        return;

                    if(fileName.getText().toString().equals("File Name") || fileName.getText().toString().equals(""))
                        return;

                    if(IpAddress.getText().toString().equals("IpAddress") || IpAddress.getText().toString().equals("")) {
                        //fta == file_to_all
                        client.sendMessage("fta");
                        client.sendMessage(fileName.getText().toString());
                    }
                    else {
                        //fsp == file_specific
                        client.sendMessage("fsp");
                        client.sendMessage(fileName.getText().toString());
                        client.sendMessage(IpAddress.getText().toString());
                    }
                }
            }).start();
            break;

            case R.id.receiveFilebutton: new Thread(new Runnable() {
                @Override
                public void run() {
                    if(client.getConnectStatus() == false)
                        return;

                    if(fileName.getText().toString().equals("File Name") || fileName.getText().toString().equals(""))
                        return;
                    if(IpAddress.getText().toString().equals("IpAddress") || IpAddress.getText().toString().equals("")) {
                        //rfa == recv_from_all
                        client.sendMessage("rfa");
                        client.sendMessage(fileName.getText().toString());
                        client.receiveFile();
                    }
                    else {
                        //rsp == recv_specific
                        client.sendMessage("rsp");
                        client.sendMessage(fileName.getText().toString());
                        client.sendMessage(IpAddress.getText().toString());
                        client.receiveFile();



                    }
                }
            }).start();
                break;

            case R.id.searchButon: new Thread(new Runnable() {
                @Override
                public void run() {
                    if(client.getConnectStatus() == false)
                        return;

                    if(searchField.getText().toString().equals("Search File") || fileName.getText().toString().equals(""))
                        return;

                    if(pathField.getText().toString().equals(""))
                        return;

                    if(IpAddress.getText().toString().equals("IpAddress") || IpAddress.getText().toString().equals("")) {
                        //sfa == search_from_all
                        client.sendMessage("sfa");
                        client.sendMessage(searchField.getText().toString());
                        client.sendMessage(pathField.getText().toString());
                    }
                    else {
                        //sfs == search_from_specific
                        client.sendMessage("sfs");
                        client.sendMessage(searchField.getText().toString());
                        client.sendMessage(pathField.getText().toString());
                        client.sendMessage(IpAddress.getText().toString());

                    }

                }
            }).start();
            break;

            case R.id.viewConnected: new Thread(new Runnable() {
                @Override
                public void run() {
                    if(client.getConnectStatus() == false)
                        return;

                    client.sendMessage("scc");
                    OpenText();

                }
            }).start();

                break;

        }


    }

    public void OpenText() {
        Intent text = new Intent(this, TextActivity.class);
        startActivity(text);
    }


    public static Client getClient() {
        return client;
    }
}


