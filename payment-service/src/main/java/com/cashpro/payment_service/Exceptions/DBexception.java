package com.cashpro.payment_service.Exceptions;

public class DBexception extends RuntimeException{
    private String msg;
    public DBexception(String msg)
    {
        this.msg=msg;
    }
    public String getMessage()
    {
        return msg;
    }
}
