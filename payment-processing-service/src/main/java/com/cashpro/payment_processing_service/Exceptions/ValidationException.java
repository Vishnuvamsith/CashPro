package com.cashpro.payment_processing_service.Exceptions;

public class ValidationException extends RuntimeException {
    String msg;
    public ValidationException(String msg)
    {
        this.msg=msg;
    }

    public String getMsg() {
        return msg;
    }
}
