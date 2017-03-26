package com.bogdan.wifianalyzer;

import android.os.AsyncTask;
import android.util.Log;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

class RequestHandler extends AsyncTask<Void, String, String> {

    String address, data;
    Integer mode;
    StringBuilder response = new StringBuilder();

    public RequestHandler(String address, Integer mode, String data) {
        this.address = address;
        this.mode = mode;
        this.data = data;
    }

    @Override
    public String doInBackground(Void... params) {

        if(mode == 0) {
            String result = getRequest(address + data);
            return result;
        }
        else {
            return null;
        }
    }

    private String getRequest(String adr) {
        URL url;
        HttpURLConnection urlConnection = null;
        try {
            url = new URL(adr);
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

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
    }
}
