package com.abdalladelessa.rxlocmanager;

/**
 * Created by Abdullah.Essa on 4/24/2016.
 */
public class RxLocException extends Exception {
    private int errorCode;

    public RxLocException(int errorCode) {
        super("Location Error Code : " + errorCode);
        this.errorCode = errorCode;
    }

    public int getErrorCode() {
        return errorCode;
    }
}
