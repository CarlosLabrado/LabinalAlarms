package com.ocr.labinal.model;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;

/**
 * Event created from the SMS message
 */
@Table(name = "Events")
public class PlantEvent extends Model {
    @Column
    public String sensorPhoneNumber;

    @Column
    public String micrologId;

    @Column
    public String state;

    @Column
    public String powerOrigin;

    @Column
    public String upsState;

    @Column
    public int minutesOnBattery;

    @Column
    public int minutesOnTransfer;

    @Column
    public boolean plantFailure;

    @Column
    public long timeInMillis;

    public PlantEvent(String sensorPhoneNumber, String micrologId, String state, String powerOrigin, String upsState, int minutesOnBattery, int minutesOnTransfer, boolean plantFailure, long timeInMillis) {
        this.sensorPhoneNumber = sensorPhoneNumber;
        this.micrologId = micrologId;
        this.state = state;
        this.powerOrigin = powerOrigin;
        this.upsState = upsState;
        this.minutesOnBattery = minutesOnBattery;
        this.minutesOnTransfer = minutesOnTransfer;
        this.plantFailure = plantFailure;
        this.timeInMillis = timeInMillis;
    }

    public PlantEvent() {
        super();
    }

    public String getSensorPhoneNumber() {
        return sensorPhoneNumber;
    }

    public void setSensorPhoneNumber(String sensorPhoneNumber) {
        this.sensorPhoneNumber = sensorPhoneNumber;
    }

    public String getMicrologId() {
        return micrologId;
    }

    public void setMicrologId(String micrologId) {
        this.micrologId = micrologId;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getPowerOrigin() {
        return powerOrigin;
    }

    public void setPowerOrigin(String powerOrigin) {
        this.powerOrigin = powerOrigin;
    }

    public int getMinutesOnBattery() {
        return minutesOnBattery;
    }

    public void setMinutesOnBattery(int minutesOnBattery) {
        this.minutesOnBattery = minutesOnBattery;
    }

    public int getMinutesOnTransfer() {
        return minutesOnTransfer;
    }

    public void setMinutesOnTransfer(int minutesOnTransfer) {
        this.minutesOnTransfer = minutesOnTransfer;
    }

    public boolean isPlantFailure() {
        return plantFailure;
    }

    public void setPlantFailure(boolean plantFailure) {
        this.plantFailure = plantFailure;
    }

    public String getUpsState() {
        return upsState;
    }

    public void setUpsState(String upsState) {
        this.upsState = upsState;
    }

    public long getTimeInMillis() {
        return timeInMillis;
    }

    public void setTimeInMillis(long timeInMillis) {
        this.timeInMillis = timeInMillis;
    }
}


