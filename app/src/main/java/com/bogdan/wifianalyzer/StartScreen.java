package com.bogdan.wifianalyzer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.concurrent.ExecutionException;

public class StartScreen extends Activity {

    EditText serverInput;
    Button nextButton;
    TextView textView;
    CheckBox serverToggle;
    Boolean proceedWithServer = true;
    public static final String SERVER_ADDR = "empty";

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.start_screen);

        serverInput = (EditText) findViewById(R.id.serverInput);
        nextButton = (Button) findViewById(R.id.nextButton);
        textView = (TextView) findViewById(R.id.textView);
        serverToggle = (CheckBox) findViewById(R.id.serverToggle);

        //Makes a connection to a server or proceeds without it, depending on user preference.
        nextButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                try {
                    performNextStep(v);
                } catch (ExecutionException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        //This gives the user the option to proceed without a connection to a server
        serverToggle.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                if(serverToggle.isChecked())
                    proceedWithServer = true;
                else
                    proceedWithServer = false;
            }
        });
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 0, 0, "Refresh");
        return super.onCreateOptionsMenu(menu);
    }

    protected void onPause() {
        super.onPause();
    }

    protected void onResume() {
        super.onResume();
    }

    //Method to call the next activity and pass it the input from the EditText
    public void performNextStep(View view) throws ExecutionException, InterruptedException {
        String addr = serverInput.getText().toString();
        //String addr = "http://eb9b71c8.ngrok.io";
        String status = new RequestHandler(addr, 0, "/greeting").execute().get();

        //Check if the user wants to proceed with a connection to a server.
        //Unless the user provides a valid server (gets a respons back), the user will not be able to proceed
        //Otherwise, a connection will be established and the next activity will be started
        if(proceedWithServer == true) {
            if (status.isEmpty()) {
                //Display a message when failing to connect to a server in a Toast
                String showText = String.format("Can't establish a connection to the server.");
                Toast.makeText(getApplicationContext(), showText, Toast.LENGTH_LONG).show();
            } else {
                //Display a message when connected to a server in a Toast
                String showText = String.format("Connected!");
                Toast.makeText(getApplicationContext(), showText, Toast.LENGTH_LONG).show();

                //Proceed with the next activity.
                Intent intent = new Intent(this, Analyzer.class);
                intent.putExtra(SERVER_ADDR, addr);
                startActivity(intent);
            }
        }
        //If a user wants to proceed without connection to a server, the address "FALSE" will be sent
        //as a parameter to the next activity, meaning that the scan results won't be sent to a server.
        else
        {
            addr = "FALSE";
            Intent intent = new Intent(this, Analyzer.class);
            intent.putExtra(SERVER_ADDR, addr);
            startActivity(intent);
        }
    }
}
