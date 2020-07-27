package com.example.myapplication;

import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.view.View;
import android.widget.TextView;

public class TextActivity extends AppCompatActivity {
    private static TextView text;
    private static Client client = MainActivity.getClient();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_text);
        text = findViewById(R.id.connectedField);
        getConnected();


    }

    public void onBackPressed() {
        finish();
    }

    public void getConnected() {
        new Thread(new Runnable() {
            @Override
            public void run() {


                int connectedPeers = Integer.valueOf(new String(client.receiveMessage()));
                if(connectedPeers == 0)
                   return;


                 text.setText("CONNECTED : \n\n");
                 for(int i = 0; i < connectedPeers; i++) {
                   text.append(new String(client.receiveMessage()));
                   text.append("  :  ");
                   text.append(new String(client.receiveMessage()));
                   text.append("\n\n");

        }

            }}).start();


    }
}
