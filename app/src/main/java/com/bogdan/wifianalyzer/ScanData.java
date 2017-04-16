package com.bogdan.wifianalyzer;

public class ScanData {

    public String network;
    public String ssid;
    public String bssid;
    public String frequency;
    public String intensity;
    public String capabilities;

    public ScanData(String Network, String SSID, String BSSID, String Frequency, String Intensity, String Capabilities) {
        this.network = Network;
        this.ssid = SSID;
        this.bssid = BSSID;
        this.frequency = Frequency;
        this.intensity = Intensity;
        this.capabilities = Capabilities;
    }

    public ScanData() {

    }

    public void setNetwork(String Network) {
        this.network = Network;
    }

    public void setSSID(String SSID) {
        this.ssid = SSID;
    }

    public void setBSSID(String BSSID) {
        this.bssid = BSSID;
    }

    public void setFrequency(String Frequency) {
        this.frequency = Frequency;
    }

    public void setIntensity(String Intensity) {
        this.intensity = Intensity;
    }

    public void setCapabilities(String Capabilities) {
        this.capabilities = Capabilities;
    }

    public String getNetwork() {
        return network;
    }

    public String getSSID() {
        return ssid;
    }

    public String getBSSID() {
        return bssid;
    }

    public String getFrequency() {
        return frequency;
    }

    public String getIntensity() {
        return intensity;
    }

    public String getCapabilities() {
        return capabilities;
    }
}


