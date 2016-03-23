package com.ocr.labinal.events;

/**
 * Created by Vazh on 22/3/2016.
 */
public class MarkerClickedEvent {

    String phoneNumber;

    public MarkerClickedEvent(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
}
