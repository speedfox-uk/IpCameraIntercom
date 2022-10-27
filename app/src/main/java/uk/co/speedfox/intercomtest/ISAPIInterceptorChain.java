package uk.co.speedfox.intercomtest;

import android.util.Log;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class ISAPIInterceptorChain implements Interceptor.Chain  {

    private ISAPIDataCall call;
    private Request req;
    private int idx;
    private ArrayList<Interceptor> interceptors;
    private ISAPIDataConnection connection;
    
    private ISAPIInterceptorChain(ISAPIInterceptorChain prev, int idx){
        this.idx = idx;
        this.call = prev.call;
        this.interceptors = prev.interceptors;
        this.req = prev.req;
        this.connection = prev.connection;
    }

    public ISAPIInterceptorChain(ISAPIDataCall isapiDataCall, ArrayList<Interceptor> interceptors, Request originalRequest, ISAPIDataConnection connection) {
        this.call = isapiDataCall;
        this.interceptors = interceptors;
        this.req = originalRequest;
        this.idx =0;
        this.connection = connection;
    }


    @NotNull
    @Override
    public Call call() {
        return call;
    }

    @Override
    public int connectTimeoutMillis() {
        return 0;
    }

    @Nullable
    @Override
    public Connection connection() {
        return connection;
    }

    @NotNull
    @Override
    public Response proceed(@NotNull Request request) throws IOException {

        this.req = request;
        ISAPIInterceptorChain next = new ISAPIInterceptorChain(this, idx+1);
        Log.d(getClass().getName(), "On intercetpor " + idx + " of " + interceptors.size());
        Interceptor interceptor = interceptors.get(idx);
        Log.d(getClass().getName(), "intercetor:" + interceptor.getClass().getName());
        Response response = interceptor.intercept(next);
        call.setResponse(response);
        return response;
    }

    @Override
    public int readTimeoutMillis() {
        return 0;
    }

    @NotNull
    @Override
    public Request request() {
        return req;
    }

    @NotNull
    @Override
    public Interceptor.Chain withConnectTimeout(int i, @NotNull TimeUnit timeUnit) {
        return this;
    }

    @NotNull
    @Override
    public Interceptor.Chain withReadTimeout(int i, @NotNull TimeUnit timeUnit) {
        return this;
    }

    @NotNull
    @Override
    public Interceptor.Chain withWriteTimeout(int i, @NotNull TimeUnit timeUnit) {
        return this;
    }

    @Override
    public int writeTimeoutMillis() {
        return 0;
    }

}
