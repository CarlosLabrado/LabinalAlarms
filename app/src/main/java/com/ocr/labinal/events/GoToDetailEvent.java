package com.ocr.labinal.events;

import com.ocr.labinal.model.Microlog;

/**
 * Created by carlos on 2/17/16.
 */
public class GoToDetailEvent {

    private Microlog mMicrolog;

    public GoToDetailEvent(Microlog microlog) {
        mMicrolog = microlog;
    }

    public Microlog getMicrolog() {
        return mMicrolog;
    }

    public void setMicrolog(Microlog microlog) {
        mMicrolog = microlog;
    }
}
