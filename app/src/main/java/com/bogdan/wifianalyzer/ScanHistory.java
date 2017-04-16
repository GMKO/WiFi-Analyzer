package com.bogdan.wifianalyzer;

import java.util.List;

public class ScanHistory {

    public Integer entries;
    public List<ScanResult> history;

    public ScanHistory(Integer entries, List<ScanResult> history) {
        this.entries = entries;
        this.history = history;
    }

    public ScanHistory() {

    }

    public void setEntries(Integer entries) {
        this.entries = entries;
    }

    public void setHistory(List<ScanResult> history) {
        this.history = history;
    }

    public Integer getEntries() {
        return entries;
    }

    public List<ScanResult> getHistory() {
        return history;
    }
}

