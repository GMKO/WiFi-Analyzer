package com.bogdan.wifianalyzer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.support.v7.app.AppCompatActivity;


/**
 * Created by Bogdan on 26-Mar-17.
 */

public class StartScreen extends Activity {

    EditText serverInput;
    Button nextButton;
    TextView textView;
    public static final String SERVER_ADDR = "empty";

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.start_screen);

        serverInput = (EditText) findViewById(R.id.serverInput);
        nextButton = (Button) findViewById(R.id.nextButton);
        textView = (TextView) findViewById(R.id.textView);

        nextButton.setOnClickListener(new View.OnClickListener(){

            public void onClick(View v){

                performNextStep(v);
            }
        });
    }

    public void performNextStep(View view) {

        String addr = serverInput.getText().toString();
        Intent intent = new Intent(this, Analyzer.class);
        intent.putExtra(SERVER_ADDR, addr);
        startActivity(intent);
    }
}
