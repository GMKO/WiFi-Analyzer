package com.bogdan.wifianalyzer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import android.Manifest;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.sheets.v4.SheetsScopes;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class Analyzer extends Activity implements EasyPermissions.PermissionCallbacks {
    GoogleAccountCredential mCredential;

    List<List<Object>> wifiResults;

    static final int REQUEST_ACCOUNT_PICKER = 1000;
    static final int REQUEST_AUTHORIZATION = 1001;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    static final int REQUEST_PERMISSION_GET_ACCOUNTS = 1003;

    private static final String PREF_ACCOUNT_NAME = "accountName";
    private static final String[] SCOPES = { SheetsScopes.SPREADSHEETS };

    Button clearButton, saveButton, scanButton, historyButton;
    TextView mainText;
    WifiManager mainWifi;
    WifiReceiver receiverWifi;
    List<ScanResult> wifiList;
    String serverAddress;
    String enableSheets;

    public void setWifiResults(List<List<Object>> data) {
        this.wifiResults = data;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        //Get the server address
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        serverAddress = extras.getString("SERVER_ADDR");
        enableSheets = extras.getString("ENABLE_SHEETS");

        if(serverAddress.equals("FALSE")) {
            Toast.makeText(getApplicationContext(), "No connection to server.",
                    Toast.LENGTH_SHORT).show();
        }

        if(enableSheets.equals("FALSE")) {
            Toast.makeText(getApplicationContext(), "Results won't be sent to a Google Spreadsheet.",
                    Toast.LENGTH_SHORT).show();
        }

        mCredential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff());

        //Check if the build version is over API23. If it is, then ask the user to give permissions
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){

            //After this point you wait for callback in onRequestPermissionsResult(int, String[], int[]) overriden method
            //Ask for location permission (to scan the WiFi)
            if((checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                || (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED))
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);

            getConnectionList();
        }else {
            //Do something, permission was previously granted; or legacy device
            getConnectionList();
        }
    }

    /**
     * Attempt to call the API, after verifying that all the preconditions are
     * satisfied. The preconditions are: Google Play Services installed, an
     * account was selected and the device currently has online access. If any
     * of the preconditions are not satisfied, the app will prompt the user as
     * appropriate.
     */
    public void acquireServices() {
        if (! isGooglePlayServicesAvailable()) {
            acquireGooglePlayServices();
        } else if (mCredential.getSelectedAccountName() == null) {
            chooseAccount();
        } else if (! isDeviceOnline()) {
            String setText = "No network connection available.";
            mainText.setText(setText);
        }
    }

    public void getResultsFromApi() {
        new MakeRequestTask(mCredential, this.wifiResults).execute();
    }


    /**
     * Attempts to set the account used with the API credentials. If an account
     * name was previously saved it will use that one; otherwise an account
     * picker dialog will be shown to the user. Note that the setting the
     * account to use with the credentials object requires the app to have the
     * GET_ACCOUNTS permission, which is requested here if it is not already
     * present. The AfterPermissionGranted annotation indicates that this
     * function will be rerun automatically whenever the GET_ACCOUNTS permission
     * is granted.
     */
    @AfterPermissionGranted(REQUEST_PERMISSION_GET_ACCOUNTS)
    private void chooseAccount() {
        if (EasyPermissions.hasPermissions(
                this, Manifest.permission.GET_ACCOUNTS)) {
            String accountName = getPreferences(Context.MODE_PRIVATE)
                    .getString(PREF_ACCOUNT_NAME, null);
            if (accountName != null) {
                mCredential.setSelectedAccountName(accountName);
                //getResultsFromApi();
            } else {
                // Start a dialog from which the user can choose an account
                startActivityForResult(
                        mCredential.newChooseAccountIntent(),
                        REQUEST_ACCOUNT_PICKER);
            }
        } else {
            // Request the GET_ACCOUNTS permission via a user dialog
            EasyPermissions.requestPermissions(
                    this,
                    "This app needs to access your Google account (via Contacts).",
                    REQUEST_PERMISSION_GET_ACCOUNTS,
                    Manifest.permission.GET_ACCOUNTS);
        }
    }

    /**
     * Called when an activity launched here (specifically, AccountPicker
     * and authorization) exits, giving you the requestCode you started it with,
     * the resultCode it returned, and any additional data from it.
     * @param requestCode code indicating which activity result is incoming.
     * @param resultCode code indicating the result of the incoming
     *     activity result.
     * @param data Intent (containing result data) returned by incoming
     *     activity result.
     */
    @Override
    protected void onActivityResult(
            int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
            case REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode != RESULT_OK) {
                    String setText = "This app requires Google Play Services. Please install " +
                            "Google Play Services on your device and relaunch this app.";
                    mainText.setText(setText);
                } else {
                    //getResultsFromApi();
                }
                break;
            case REQUEST_ACCOUNT_PICKER:
                if (resultCode == RESULT_OK && data != null &&
                        data.getExtras() != null) {
                    String accountName =
                            data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        SharedPreferences settings =
                                getPreferences(Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString(PREF_ACCOUNT_NAME, accountName);
                        editor.apply();
                        mCredential.setSelectedAccountName(accountName);
                        //getResultsFromApi();
                    }
                }
                break;
            case REQUEST_AUTHORIZATION:
                if (resultCode == RESULT_OK) {
                    //getResultsFromApi();
                }
                break;
        }
    }

    /**
     * Respond to requests for permissions at runtime for API 23 and above.
     * @param requestCode The request code passed in
     *     requestPermissions(android.app.Activity, String, int, String[])
     * @param permissions The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *     which is either PERMISSION_GRANTED or PERMISSION_DENIED. Never null.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    /**
     * Callback for when a permission is granted using the EasyPermissions
     * library.
     * @param requestCode The request code associated with the requested
     *         permission
     * @param list The requested permission list. Never null.
     */
    @Override
    public void onPermissionsGranted(int requestCode, List<String> list) {
        Log.d("PERMISSION GRANTED",Integer.valueOf(requestCode).toString());
    }

    /**
     * Callback for when a permission is denied using the EasyPermissions
     * library.
     * @param requestCode The request code associated with the requested
     *         permission
     * @param list The requested permission list. Never null.
     */
    @Override
    public void onPermissionsDenied(int requestCode, List<String> list) {
        Log.d("PERMISSION DENIED",Integer.valueOf(requestCode).toString());
    }

    /**
     * Checks whether the device currently has a network connection.
     * @return true if the device has a network connection, false otherwise.
     */
    private boolean isDeviceOnline() {
        ConnectivityManager connMgr =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    /**
     * Check that Google Play services APK is installed and up to date.
     * @return true if Google Play Services is available and up to
     *     date on this device; false otherwise.
     */
    private boolean isGooglePlayServicesAvailable() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(this);
        return connectionStatusCode == ConnectionResult.SUCCESS;
    }

    /**
     * Attempt to resolve a missing, out-of-date, invalid or disabled Google
     * Play Services installation via a user dialog, if possible.
     */
    private void acquireGooglePlayServices() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(this);
        if (apiAvailability.isUserResolvableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
        }
    }


    /**
     * Display an error dialog showing that Google Play Services is missing
     * or out of date.
     * @param connectionStatusCode code describing the presence (or lack of)
     *     Google Play Services on this device.
     */
    void showGooglePlayServicesAvailabilityErrorDialog(
            final int connectionStatusCode) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        Dialog dialog = apiAvailability.getErrorDialog(
                Analyzer.this,
                connectionStatusCode,
                REQUEST_GOOGLE_PLAY_SERVICES);
        dialog.show();
    }

    public void getConnectionList(){
        setContentView(R.layout.activity_main);
        mainText = (TextView) findViewById(R.id.mainText);
        mainText.setMovementMethod(new ScrollingMovementMethod());

        clearButton = (Button) findViewById(R.id.clearButton);
        saveButton = (Button) findViewById(R.id.saveButton);
        scanButton = (Button) findViewById(R.id.scanButton);
        historyButton = (Button) findViewById(R.id.historyButton);

        // Initiate wifi service manager
        mainWifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        // Check if wifi is disabled
        if (!mainWifi.isWifiEnabled())
        {
            // If wifi is disabled then enable it
            Toast.makeText(getApplicationContext(), "Starting WiFi...",
                    Toast.LENGTH_LONG).show();

            mainWifi.setWifiEnabled(true);
        }
        // WiFi broadcast receiver
        receiverWifi = new WifiReceiver();

        // Register broadcast receiver
        // Broadcast receiver will automatically call when the number of wifi connections changes
        registerReceiver(receiverWifi, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        mainWifi.startScan();

        String display = "Starting Scan...";
        mainText.setText(display);
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 0, 0, "Refresh");
        return super.onCreateOptionsMenu(menu);
    }

    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        mainWifi.startScan();

        String display = "Starting Scan";
        mainText.setText(display);
        return super.onMenuItemSelected(featureId, item);
    }

    protected void onPause() {
        unregisterReceiver(receiverWifi);
        super.onPause();
    }

    protected void onResume() {
        registerReceiver(receiverWifi, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        super.onResume();
    }

    //Creates the local file and writes the scan result to it
    public void saveResults(String result) throws IOException {
        try {
            //Set the fileName and the path
            String filename = "log.txt";
            String path = "/storage/emulated/0/Documents";

            //Create a file with the specified fileName at the specified path
            File filePath = new File(path, filename);
            filePath.createNewFile();

            //Clear the file of values and write the new ones.
            FileOutputStream fileOutputStream = new FileOutputStream(filePath);
            fileOutputStream.flush();
            fileOutputStream.write(result.getBytes());
            fileOutputStream.close();

            //Display the path where the file was saved in a Toast
            String showText = String.format("File saved at %s/%s", path, filename);
            Toast.makeText(getApplicationContext(), showText, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            String setText = "Can't write to local file";
            mainText.setText(setText);
            Log.d("SAVE","EXCEPTION",e);
        }
    }

    //Retrieves the system time and formats the output
    public String getTime() {
        Calendar rightNow = Calendar.getInstance();

        int hour = rightNow.get(Calendar.HOUR_OF_DAY);
        int minute = rightNow.get(Calendar.MINUTE);
        int second = rightNow.get(Calendar.SECOND);

        String h;
        if(hour < 10)
            h = String.format("0%d:",hour);
        else
            h = String.format("%d:",hour);

        String m;
        if(minute < 10)
            m = String.format("0%d:",minute);
        else
            m = String.format("%d:",minute);

        String s;
        if(second < 10)
            s = String.format("0%d",second);
        else
            s = String.format("%d",second);

        return h+m+s;
    }

    // Broadcast receiver class calls its receive method when the number of wifi connections changes
    class WifiReceiver extends BroadcastReceiver {

        // This method is called when the number of wifi connections changes
        public void onReceive(Context c, Intent intent) {
            acquireServices();
            Log.d("LOG","Updated connection list");

            StringBuilder serverData = new StringBuilder();
            final StringBuilder sb = new StringBuilder();
            List<List<Object>> values = new ArrayList<>();

            //sb = new StringBuilder();

            wifiList = mainWifi.getScanResults();
            sb.append("\nNumber of WiFi connections :"
                    + wifiList.size()+"\nScanned at: "
                    + getTime()
                    +"\n\n");

            //Set the column heads
            List<Object> data1 = new ArrayList<>();
            data1.add("Network");
            data1.add("SSID");
            data1.add("BSSID");
            data1.add("Frequency");
            data1.add("Level of Intensity");
            data1.add("Capabilities");
            values.add(data1);

            for(int i = 0; i < wifiList.size(); i++){

                //Add the results to the String Builder that is going to be displayed to the user.
                sb.append("Network #" + Integer.valueOf(i+1).toString());
                sb.append("\nSSID: " + wifiList.get(i).SSID);
                sb.append("\nBSSID: " + wifiList.get(i).BSSID);
                sb.append("\nFrequency: " + wifiList.get(i).frequency);
                sb.append("\nLevel of intensity: " + mainWifi.calculateSignalLevel((wifiList.get(i)).level,10));
                sb.append("\nCapabilities: " + wifiList.get(i).capabilities + "\n");
                sb.append("\n\n");

                //If the user chose to use Google SpreadSheets, then the data is added to a separate container.
                if(enableSheets.equals("TRUE")) {
                    //Add the results to the list that is going to be passed to the Sheets API.
                    List<Object> wifiListInfo = new ArrayList<>();
                    wifiListInfo.add(Integer.valueOf(i + 1).toString());
                    wifiListInfo.add((wifiList.get(i)).SSID);
                    wifiListInfo.add((wifiList.get(i)).BSSID);
                    wifiListInfo.add(Integer.valueOf((wifiList.get(i)).frequency).toString());
                    wifiListInfo.add(Integer.valueOf(mainWifi.calculateSignalLevel((wifiList.get(i)).level, 10)).toString());
                    wifiListInfo.add(wifiList.get(i).capabilities);
                    values.add(wifiListInfo);
                }
                //If the user chose to use a server, then the data is added to a separate container.
                if(!serverAddress.equals("FALSE")) {
                    //Add the results to the String Builder that is going to be sent to the server.
                    serverData.append(Integer.valueOf(i + 1).toString() + "\n");
                    serverData.append(wifiList.get(i).SSID + "\n");
                    serverData.append(wifiList.get(i).BSSID + "\n");
                    serverData.append(wifiList.get(i).frequency + "\n");
                    serverData.append(mainWifi.calculateSignalLevel((wifiList.get(i)).level, 10) + "\n");
                    serverData.append(wifiList.get(i).capabilities + "\n");
                }
            }

            //If the user chose to use a server, then the data is sent to the server.
            if(!serverAddress.equals("FALSE")) {
                //Send the scan result to the server.
                new RequestHandler(serverAddress, serverData.toString(), mCredential.getSelectedAccountName(), wifiList.size(), 1).execute();
            }

            //If the user chose to use Google SpreadSheets then the data is sent to a predefined SpreadSheet.
            if(enableSheets.equals("TRUE")) {
                //Send the retrieved results to the Google Spreadsheet
                setWifiResults(values);
                getResultsFromApi();
            }

            //Display the scan results to the user
            mainText.setText(sb);

            clearButton.setOnClickListener(new View.OnClickListener(){
                public void onClick(View v){
                    mainText.setText("\nNumber of WiFi connections :"
                            + wifiList.size()+"\nCleared at: "
                            + getTime()
                            +"\n\n");
                }
            });

            saveButton.setOnClickListener(new View.OnClickListener(){
                public void onClick(View v){
                    try {
                        saveResults(sb.toString());
                    } catch (Exception e) {
                        Log.d("EXCEPTION","Something broke",e);
                    }
                }
            });

            scanButton.setOnClickListener(new View.OnClickListener(){
                public void onClick(View v){
                    mainWifi.startScan();
                }
            });

            historyButton.setOnClickListener(new View.OnClickListener(){
                public void onClick(View v){
                    if (serverAddress.equals("FALSE")) {
                        Toast.makeText(getApplicationContext(), "Can't retrieve history, no connection to server.",
                                Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }
}
