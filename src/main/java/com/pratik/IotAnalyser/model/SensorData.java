package com.pratik.IotAnalyser.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "sensor_data")
public class SensorData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sensor_value", nullable = false)
    private Double value;

    @Column(name = "sensor_type", length = 100, nullable = false)
    private String sensorType;

    @Column(name = "unit", length = 50)
    private String unit;

    @Column(name = "event_timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "anomaly")
    private Boolean anomaly;

    @ManyToOne(fetch = FetchType.LAZY) // avoid loading full Device unless needed
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }

    public String getSensorType() {
        return sensorType;
    }

    public void setSensorType(String sensorType) {
        this.sensorType = sensorType;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Boolean getAnomaly() {
        return anomaly;
    }

    public void setAnomaly(Boolean anomaly) {
        this.anomaly = anomaly;
    }

    public Device getDevice() {
        return device;
    }

    public void setDevice(Device device) {
        this.device = device;
    }
}
