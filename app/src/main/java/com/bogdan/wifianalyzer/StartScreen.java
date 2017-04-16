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
    Button nextButton, skipButton;
    TextView textView;
    Boolean proceedWithServer;
    CheckBox sheetsEnable;
//    public static final String SERVER_ADDR = "";
//    public static final String ENABLE_SHEETS = "";

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.start_screen);

        serverInput = (EditText) findViewById(R.id.serverInput);
        nextButton = (Button) findViewById(R.id.nextButton);
        textView = (TextView) findViewById(R.id.textView);
        skipButton = (Button) findViewById(R.id.skipButton);
        sheetsEnable = (CheckBox) findViewById(R.id.sheetsEnable);


        //Makes a connection to a server or proceeds without it, depending on user preference.
        nextButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    proceedWithServer = true;
                    performNextStep(v);
                } catch (ExecutionException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        //This gives the user the option to proceed without a connection to a server
        skipButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    proceedWithServer = false;
                    performNextStep(v);
                } catch (ExecutionException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
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
        //Server override//
        ///////////////////////////////////////////////
        //String addr = serverInput.getText().toString();
        String addr = "http://8f8732cf.ngrok.io";
        ///////////////////////////////////////////////

        //Check if the user wants to proceed with a connection to a server.
        //Unless the user provides a valid server (gets a response back), the user will not be able to proceed
        //Otherwise, a connection will be established and the next activity will be started
        if (proceedWithServer) {
            String status = new RequestHandler(addr, "/greeting", "name", 0, 0).execute().get();
            if (status.isEmpty()) {
                //Display a message when failing to connect to a server in a Toast
                String showText = "Can't establish a connection to the server.";
                Toast.makeText(getApplicationContext(), showText, Toast.LENGTH_SHORT).show();
            } else {
                //Display a message when connected to a server in a Toast
                String showText = "Connected to server.";
                Toast.makeText(getApplicationContext(), showText, Toast.LENGTH_SHORT).show();

                //Proceed with the next activity.
                Intent intent = new Intent(this, Analyzer.class);
                Bundle extras = new Bundle();
                extras.putString("SERVER_ADDR", addr);

                //Check if the user wants to have the Google Sheets functionality enabled.
                if(sheetsEnable.isChecked())
                    extras.putString("ENABLE_SHEETS", "TRUE");
                else
                    extras.putString("ENABLE_SHEETS", "FALSE");

                intent.putExtras(extras);
                startActivity(intent);
            }
        }
        //If a user wants to proceed without connection to a server, the address "FALSE" will be sent
        //as a parameter to the next activity, meaning that the scan results won't be sent to a server.
        else {
            //Uncomment this, used to bypass manual server input
            addr = "FALSE";

            //Proceed with the next activity.
            Intent intent = new Intent(this, Analyzer.class);
            Bundle extras = new Bundle();
            extras.putString("SERVER_ADDR", addr);

            //Check if the user wants to have the Google Sheets functionality enabled.
            if(sheetsEnable.isChecked())
                extras.putString("ENABLE_SHEETS", "TRUE");
            else
                extras.putString("ENABLE_SHEETS", "FALSE");

            intent.putExtras(extras);
            startActivity(intent);
        }
    }
}
