package com.ocr.labinal.model;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;

/**
 * Temperatures
 */
@Table(name = "Temperatures")
public class Temperature extends Model {

    @Column
    public String sensorPhoneNumber;

    @Column
    public String micrologId;

    @Column
    public String status;

    @Column
    public double tempInFahrenheit;

    @Column
    public double humidity;

    @Column
    public long timestamp;


    public Temperature(String sensorPhoneNumber, String micrologId, String status, double tempInFahrenheit, double humidity, long timestamp) {
        this.sensorPhoneNumber = sensorPhoneNumber;
        this.micrologId = micrologId;
        this.status = status;
        this.tempInFahrenheit = tempInFahrenheit;
        this.humidity = humidity;
        this.timestamp = timestamp;
    }

    public Temperature() {
        super();
    }
}
