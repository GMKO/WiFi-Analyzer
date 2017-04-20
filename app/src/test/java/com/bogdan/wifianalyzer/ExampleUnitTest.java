package com.bogdan.wifianalyzer;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.cglib.core.Local;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Mock
    JSONObject testObj;
    @Mock
    JSONArray dataArrayJson;
    @Mock
    JSONObject dataJson;

    @Test
    public void testRequestHandlerParseData () throws JSONException {

        //Mock a JSON object
        JSONObject testObj = new JSONObject();
        JSONArray dataArrayJson = new JSONArray();

        RequestHandler rh = new RequestHandler("","","nume",1,1);

        testObj.put("name", "nume");
        testObj.put("timestamp", rh.getTime());

        JSONObject dataJson = new JSONObject();

        dataJson.put("network", "1");
        dataJson.put("ssid", "ssid");
        dataJson.put("bssid", "bssid");
        dataJson.put("frequency", "freq");
        dataJson.put("intensity", "intensity");
        dataJson.put("capabilities", "capabilities");

        dataArrayJson.put(dataJson);
        testObj.put("data",dataArrayJson);

        //Mock a string that needs to be converted in a JSON object
        StringBuilder sb = new StringBuilder();
        sb.append("1"+"\n");
        sb.append("ssid"+"\n");
        sb.append("bssid"+"\n");
        sb.append("freq"+"\n");
        sb.append("intensity"+"\n");
        sb.append("capabilities"+"\n");

        //Check if the mock JSON mathes with the result returned by the function when called on the mock string.
        assertEquals(testObj.toString(), rh.parseData(1,"nume", sb.toString()).toString());
    }
}