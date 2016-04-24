package com.abdalladelessa.locmanager;

/**
 * Created by Abdullah.Essa on 4/24/2016.
 */
public class LocException extends Exception {
    private int errorCode;

    public LocException(int errorCode) {
        super("Location Error Code : " + errorCode);
        this.errorCode = errorCode;
    }

    public int getErrorCode() {
        return errorCode;
    }
}
