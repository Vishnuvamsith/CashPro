package com.cashpro.payment_service.Exceptions;

public class DuplicatePaymentException extends RuntimeException{
    String msg;
    public DuplicatePaymentException(String msg)
    {
        this.msg=msg;
    }
    public String getMessage()
    {
        return msg;
    }
}
