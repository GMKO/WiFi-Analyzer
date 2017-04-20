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

    /**
     * @param address the address of the server.
     * @param data a string containing data that needs to be sent to the server.
     * @param name the name of the current user.
     * @param size the number of networks in a data string, used for the POST request.
     * @param mode the type of request that is going to be sent to the server.
     *          - 0 GET request, checks if the server is online.
     *          - 1 POST request, creates a JSON object form the received data and sends it.
     *          - 2 DELETE request, used to clear a log file on the server.
     *          - 3 GET request, retrieves the history log from the server.
     */
    RequestHandler(String address, String data, String name, Integer size, Integer mode) {
        this.address = address;
        this.data = data;
        this.name = name;
        this.size = size;
        this.mode = mode;
    }

    @Override
    /**
     * Checks the value of the mode variable and makes the appropriate request based on it.
     */
    public String doInBackground(Void... params) {
        if(mode == 0) {
            return getRequest(address + data);
        }

        if(mode == 1) {
            //Creates a JSON object based on the scan data.
            JSONObject jsonData = parseData(size, name, data);
            try {
                //The resulted JSON is then sent to the server in a POST request.
                return postRequest(address, jsonData);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if(mode == 2) {
            try {
                return clearHistoryRequest(address, name);
            } catch (IOException e) {
                e.printStackTrace();
            }
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

    /**
     * Retrieves the system time and formats it in a more
     * user friendly mode (hh:mm:ss).
     * @return the current time
     */
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

    /**
     * Takes the input data from the scan and converts it to a JSON object.
     * @param size the number of networks present in the scanData string
     * @param name the name of the current user. Used to identify the resource that corresponds
     *             to the user on the server.
     * @param scanData the data returned by the WifiManager after a scan. It has been formatted in
     *                 such a way so that only the necessary information is present in the string.
     * @return a JSON representation of the data given as input.
     */
    public JSONObject parseData(Integer size, String name, String scanData) {
        String[] splitScanData = scanData.split("\n");
        JSONObject scanJson = new JSONObject();

        try {
            scanJson.put("name", name);
            scanJson.put("timestamp", getTime());
            JSONArray dataArrayJson = new JSONArray();

            for(Integer i=0; i<size*6; i+=6) {

                JSONObject dataJson = new JSONObject();

                dataJson.put("network", splitScanData[i]);
                dataJson.put("ssid", splitScanData[i+1]);
                dataJson.put("bssid", splitScanData[i+2]);
                dataJson.put("frequency", splitScanData[i+3]);
                dataJson.put("intensity", splitScanData[i+4]);
                dataJson.put("capabilities", splitScanData[i+5]);

                dataArrayJson.put(dataJson);
            }

            scanJson.put("data",dataArrayJson);
        } catch (JSONException e) {
            Log.d("REQ", "unexpected JSON exception", e);
        }

        return scanJson;
    }

    /**
     * Makes a basic GET request at the specified address. This will only be used to
     * check if the server is online or the specified address is a valid one. This will not be used
     * to retrieve any meaningful information. Tha call to this method is done with "address+data"
     * as a parameter where "data" is a resource on the server.
     * @param addr the server address with a path to a certain resource specified.
     * @return the response code of the server.
     */
    private String getRequest(String addr) {
        URL url;
        HttpURLConnection urlConnection = null;
        try {
            //Opens a connection to the URL.
            url = new URL(addr);
            urlConnection = (HttpURLConnection) url.openConnection();
            InputStream in = urlConnection.getInputStream();
            InputStreamReader isw = new InputStreamReader(in);

            //Parses the response.
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
        return response.toString();
    }

    /**
     * Makes a POST request at the address.
     * @param addr the address of the server
     * @param scan the JSON object containing all the scan data.
     * @return the response code of the server.
     * @throws IOException
     */
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

    /**
     * Makes a GET request at the address for the history log assigned to the current user.
     * @param addr the server address.
     * @param name the name of the user making the request.
     * @return a serialized JSON object that represents the server's response.
     */
    private String getHistoryRequest(String addr, String name) {
        HttpURLConnection urlConnection = null;

        try {
            //Open the connection to the specified URL.
            URL url = new URL(addr + "/scan?name=" + name);
            urlConnection = (HttpURLConnection) url.openConnection();
            InputStream in = urlConnection.getInputStream();
            InputStreamReader isw = new InputStreamReader(in);

            //Parse the retrieved data.
            int data = isw.read();
            while (data != -1) {
                char current = (char) data;
                data = isw.read();
                response.append(current);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            //Close the connection when the response has been finished parsing.
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }

        return response.toString();
    }

    /**
     * Makes a DELETE request at the address. This cleans the history log on the server that
     * corresponds to the user making the request.
     * @param addr the address of the server.
     * @param name the name of the user making the request.
     * @return the response code from the server.
     * @throws IOException
     */
    private String clearHistoryRequest(String addr, String name) throws IOException {
        //Open the connection to the specified URL.
        URL url = new URL(addr + "/scan?name=" + name);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("DELETE");

        //Send the DELETE request.
        return  String.format("%d", connection.getResponseCode());
    }
}
