package com.bogdan.wifianalyzer;

import java.util.List;

public class ScanResult {

    public String name;
    public String timestamp;
    public List<ScanData> data;

    public ScanResult(String name, String timestamp, List<ScanData> data) {
        this.name = name;
        this.timestamp = timestamp;
        this.data = data;
    }

    public ScanResult() {

    }

    public void setName(String name) {
        this.name = name;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public void setData(List<ScanData> data) {
        this.data = data;
    }

    public String getName() {
        return name;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public List<ScanData> getData() {
        return data;
    }
}
