package com.ocr.labinal.model;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;

/**
 * Temperatures
 */
@Table(name = "SetPoints")
public class SetPoint extends Model {

    @Column
    public String phoneNumber;

    @Column
    public String micrologId;

    @Column
    public int setPointNumber;

    @Column
    public double tempInFahrenheit;

    @Column
    public long timestamp;

    @Column
    public boolean verified;

    public SetPoint(String phoneNumber, String micrologId, int setPointNumber, double tempInFahrenheit, long timestamp, boolean verified) {
        this.phoneNumber = phoneNumber;
        this.micrologId = micrologId;
        this.setPointNumber = setPointNumber;
        this.tempInFahrenheit = tempInFahrenheit;
        this.timestamp = timestamp;
        this.verified = verified;
    }

    public SetPoint() {
        super();
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getMicrologId() {
        return micrologId;
    }

    public void setMicrologId(String micrologId) {
        this.micrologId = micrologId;
    }

    public int getSetPointNumber() {
        return setPointNumber;
    }

    public void setSetPointNumber(int setPointNumber) {
        this.setPointNumber = setPointNumber;
    }

    public double getTempInFahrenheit() {
        return tempInFahrenheit;
    }

    public void setTempInFahrenheit(double tempInFahrenheit) {
        this.tempInFahrenheit = tempInFahrenheit;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }
}
