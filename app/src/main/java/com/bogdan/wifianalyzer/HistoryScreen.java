package com.bogdan.wifianalyzer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import java.util.concurrent.ExecutionException;

public class HistoryScreen extends Activity {

    String serverAddress;
    String name;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.history_screen);

        Button clearButton = (Button) findViewById(R.id.clearButton);
        final TextView historyText = (TextView) findViewById(R.id.historyText);
        historyText.setMovementMethod(new ScrollingMovementMethod());

        //Get the server address and username from the past activity.
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        serverAddress = extras.getString("SERVER_ADDR");
        name = extras.getString("NAME");

        try {
            //Make a GET request to the server for the history log.
            Gson gson = new Gson();
            StringBuilder sb = new StringBuilder();
            String response = new RequestHandler(serverAddress, "Not needed", name, 0, 3).execute().get();

            //Map the JSON response from the server to a ScanHistory object that matches the structure of the JSON response.
            ScanHistory history = gson.fromJson(response,ScanHistory.class);

            //Format the text that will be displayed on the screen based on the information contained in the ScanHistory object.
            sb.append(String.format("Number of scans: %d\n\n",history.getEntries()));

            for(int i=0; i<history.getEntries(); i++) {
                ScanResult res = history.getHistory().get(i);
                sb.append(String.format("Scan %d, performed at %s \n\n", i+1, res.getTimestamp()));

                for(ScanData data : res.getData()) {
                    sb.append("Network #" + data.getNetwork() + "\n");
                    sb.append("SSID: " + data.getSSID() + "\n");
                    sb.append("BSSID: " + data.getBSSID() + "\n");
                    sb.append("Frequency: " + data.getFrequency() + "\n");
                    sb.append("Intensity: " + data.getIntensity() + "\n");
                    sb.append("Capabilities: " + data.getCapabilities() + "\n\n");
                }
                sb.append("\n");
            }
            historyText.setText(sb);

        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        //Clear history button listener. Sends a DELETE request to the server with the name of the current user.
        clearButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                try {
                    String response = new RequestHandler(serverAddress, "Not needed", name, 0, 2).execute().get();
                    historyText.setText("Number of scans: 0");

                    if(response.equals("200")) {
                        String showText = "Successfully cleared the history.";
                        Toast.makeText(getApplicationContext(), showText, Toast.LENGTH_SHORT).show();
                    }
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
