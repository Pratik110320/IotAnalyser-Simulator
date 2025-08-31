package com.pratik.IotAnalyser.simulator;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "simulator")
public class SimulatorConfig {

    private boolean enabled;
    private int deviceCount;
    private List<String> sensorTypes;
    private long dataPushInterval;
    private double anomalyProbability;
    private double disconnectProbability;
    private double reconnectProbability;
    private String serverUrl;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public int getDeviceCount() { return deviceCount; }
    public void setDeviceCount(int deviceCount) { this.deviceCount = deviceCount; }

    public List<String> getSensorTypes() { return sensorTypes; }
    public void setSensorTypes(List<String> sensorTypes) { this.sensorTypes = sensorTypes; }

    public long getDataPushInterval() { return dataPushInterval; }
    public void setDataPushInterval(long dataPushInterval) { this.dataPushInterval = dataPushInterval; }

    public double getAnomalyProbability() { return anomalyProbability; }
    public void setAnomalyProbability(double anomalyProbability) { this.anomalyProbability = anomalyProbability; }

    public double getDisconnectProbability() { return disconnectProbability; }
    public void setDisconnectProbability(double disconnectProbability) { this.disconnectProbability = disconnectProbability; }

    public double getReconnectProbability() { return reconnectProbability; }
    public void setReconnectProbability(double reconnectProbability) { this.reconnectProbability = reconnectProbability; }

    public String getServerUrl() { return serverUrl; }
    public void setServerUrl(String serverUrl) { this.serverUrl = serverUrl; }
}
