package com.bogdan.wifianalyzer;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;

class RequestHandler extends AsyncTask<Void, String, String> {

    private String address, data, name;
    private Integer mode, size;
    private StringBuilder response = new StringBuilder();

    //Address - the address of the server
    //Mode - 0 represents a basic GET request, checks if the server is online
    //     - 1 represents a POST request, creates a JSON object form the received data and sends it
    //     - 2 represents a DELETE request, used to clear a log file on the server
    //     - 3 represents a GET request, retrieves the history log from the server
    //Data - a string containing data that needs to be sent to the server
    //Size - the number of scan results in a data string, used for the POST request
    //Name - the name of the current user
    RequestHandler(String address, String data, String name, Integer size, Integer mode) {
        this.address = address;
        this.mode = mode;
        this.data = data;
        this.name = name;
        this.size = size;
    }

    @Override
    public String doInBackground(Void... params) {
        if(mode == 0) {
            return getRequest(address + data);
        }
        if(mode == 1) {
            JSONObject jsonData = parseData(size, name, data);
            try {
                return postRequest(address, jsonData);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if(mode == 2) {
            return null;
        }
        if(mode == 3) {
            return getHistoryRequest(address, name);
        }
        return null;
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
    }

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

    //Takes the input data from the scan and converts it to a JSON object.
    private JSONObject parseData(Integer size, String name, String scanData) {
        String[] splitScanData = scanData.split("\n");
        JSONObject scanJson = new JSONObject();

        try {
            JSONArray dataArrayJson = new JSONArray();
            scanJson.put("name", name);
            scanJson.put("timestamp", getTime());

            for(Integer i=0; i<size*6; i+=6) {

                JSONObject dataJson = new JSONObject();

                dataJson.put("Network", splitScanData[i]);
                dataJson.put("SSID", splitScanData[i+1]);
                dataJson.put("BSSID", splitScanData[i+2]);
                dataJson.put("Frequency", splitScanData[i+3]);
                dataJson.put("Intensity", splitScanData[i+4]);
                dataJson.put("Capabilities", splitScanData[i+5]);

                dataArrayJson.put(dataJson);
            }

            scanJson.put("data",dataArrayJson);
        } catch (JSONException e) {
            Log.d("REQ", "unexpected JSON exception", e);
        }

        //Log.d("JSON", scanJson.toString());
        return scanJson;
    }

    //Makes a basic GET request at the specified address, for the "data" resource
    private String getRequest(String addr) {
        URL url;
        HttpURLConnection urlConnection = null;
        try {
            url = new URL(addr);
            urlConnection = (HttpURLConnection) url.openConnection();
            InputStream in = urlConnection.getInputStream();
            InputStreamReader isw = new InputStreamReader(in);

            int data = isw.read();
            while (data != -1) {
                char current = (char) data;
                data = isw.read();
                response.append(current);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
        //Log.d("LOG", response.toString());
        return response.toString();
    }

    //Makes a post request at the address, with the JSON object.
    private String postRequest(String addr, JSONObject scan) throws IOException {
        URL url = new URL(addr + "/scan");

        //Open the connection to the specified URL.
        HttpURLConnection con = (HttpURLConnection) url.openConnection();

        //Set the connection properties.
        con.setDoOutput(true);
        con.setDoInput(true);

        //Set the request headers.
        con.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        con.setRequestProperty("Accept", "application/json");
        con.setRequestMethod("POST");

        //Send the data.
        OutputStreamWriter wr = new OutputStreamWriter(con.getOutputStream());
        wr.write(scan.toString());
        wr.flush();

        //Return the response code.
        return String.format("%d", con.getResponseCode());
    }

    private String getHistoryRequest(String addr, String name) {

        URL url;
        HttpURLConnection urlConnection = null;
        try {
            url = new URL(addr + "/scan?name=" + name);
            urlConnection = (HttpURLConnection) url.openConnection();
            InputStream in = urlConnection.getInputStream();
            InputStreamReader isw = new InputStreamReader(in);

            int data = isw.read();
            while (data != -1) {
                char current = (char) data;
                data = isw.read();
                response.append(current);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
        Log.d("LOG", response.toString());
        return response.toString();
    }

    private String clearHistoryRequest(String addr, String name) {

        return null;
    }
}
