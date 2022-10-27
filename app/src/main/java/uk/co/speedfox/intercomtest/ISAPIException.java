package uk.co.speedfox.intercomtest;

public class ISAPIException extends Exception{
    public ISAPIException(String msg)
    {
        super(msg);
    }

    public ISAPIException(String msg, Exception cause)
    {
        super(cause);
    }
}
