package com.bogdan.wifianalyzer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.concurrent.ExecutionException;

/**
 * Created by Bogdan on 15-Apr-17.
 */

public class HistoryScreen extends Activity {

    EditText serverInput;
    Button nextButton, skipButton;
    TextView textView;
    Boolean proceedWithServer;
    CheckBox sheetsEnable;
    String serverAddress;
    String name;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.history_screen);

        Button clearButton = (Button) findViewById(R.id.clearButton);
        final TextView historyText = (TextView) findViewById(R.id.historyText);

        //Get the server address and username
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        serverAddress = extras.getString("SERVER_ADDR");
        name = extras.getString("NAME");

        try {
            String response = new RequestHandler(serverAddress, "Not needed", name, 0, 3).execute().get();
            historyText.setText(response);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        clearButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                try {
                    String response = new RequestHandler(serverAddress, "Not needed", name, 0, 2).execute().get();
                    historyText.setText("");
                    String showText = "Successfully cleared the history.";
                    Toast.makeText(getApplicationContext(), showText, Toast.LENGTH_SHORT).show();
                } catch (InterruptedException e) {
                    String showText = "There was an error, failed clearing the history.";
                    Toast.makeText(getApplicationContext(), showText, Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    String showText = "There was an error, failed clearing the history.";
                    Toast.makeText(getApplicationContext(), showText, Toast.LENGTH_SHORT).show();
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

}
