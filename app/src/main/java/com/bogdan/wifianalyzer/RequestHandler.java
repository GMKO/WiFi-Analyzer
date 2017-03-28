package com.bogdan.wifianalyzer;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

class RequestHandler extends AsyncTask<Void, String, String> {

    private String address, data;
    private Integer mode;
    private StringBuilder response = new StringBuilder();

    RequestHandler(String address, Integer mode, String data) {
        this.address = address;
        this.mode = mode;
        this.data = data;
    }

    @Override
    public String doInBackground(Void... params) {
        if(mode == 0) {
            return getRequest(address + data);
        }
        else {
            JSONObject jsonData = parseData(data);
            try {
                return postRequest(address, jsonData);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
    }

    //Takes the input data from the scan and converts it to a JSON object.
    private JSONObject parseData(String scanData) {
        String[] splitScanData = scanData.split("\n");
        JSONObject scanJson = new JSONObject();

        try {
            scanJson.put("User",splitScanData[0]);
            scanJson.put("Network", splitScanData[1]);
            scanJson.put("SSID", splitScanData[2]);
            scanJson.put("BSSID", splitScanData[3]);
            scanJson.put("Frequency", splitScanData[4]);
            scanJson.put("Intensity", splitScanData[5]);
            scanJson.put("Capabilities", splitScanData[6]);
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
        URL object=new URL(addr + "/scan");

        HttpURLConnection con = (HttpURLConnection) object.openConnection();
        con.setDoOutput(true);
        con.setDoInput(true);
        con.setRequestProperty("Content-Type", "application/json");
        con.setRequestProperty("Accept", "application/json");
        //con.setRequestProperty("json",scan.toString());
        con.setRequestMethod("POST");

        OutputStreamWriter wr = new OutputStreamWriter(con.getOutputStream());
        wr.write(scan.toString());
        wr.flush();


        ///////////////////////////
        StringBuilder sb = new StringBuilder();
        int HttpResult = con.getResponseCode();
        if (HttpResult == HttpURLConnection.HTTP_OK) {
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(con.getInputStream(), "utf-8"));
            String line = null;
            while ((line = br.readLine()) != null) {
                sb.append(line + "\n");
            }
            br.close();
            Log.d("GOOD: ", sb.toString());
        } else {
            Log.d("BAD: ", con.getResponseMessage());
        }
        ///////////////////////////
        return null;
    }
}
