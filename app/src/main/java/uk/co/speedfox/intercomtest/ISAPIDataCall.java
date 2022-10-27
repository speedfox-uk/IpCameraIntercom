package uk.co.speedfox.intercomtest;

import android.util.Log;
import okhttp3.*;
import okio.Timeout;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.ArrayList;

/**
 * This class reimplements Call from okhttp so that the connection is not closed automatically and is made accessible
 * for use after the call is compelte.
 * What I'm basically trying to do with this: https://github.com/electric-monk/mqtt-hikvision/blob/master/hiksound.py
 */
public class ISAPIDataCall implements Call {
    private boolean executed = false;
    private ISAPIDataConnection connection;
    private Request originalRequest;
    private Response response;
    private OkHttpClient client;

    public ISAPIDataCall(Request req, OkHttpClient client)
    {
        this.originalRequest = req;
        this.client = client;
    }

    @Override
    public void cancel() {

    }

    @NotNull
    @Override
    public Call clone() {
        return null;
    }

    @Override
    public void enqueue(@NotNull Callback callback) {

    }

    @NotNull
    @Override
    public Response execute() throws IOException {
        createConnection();
        ArrayList<Interceptor> interceptors = new ArrayList<>();
        interceptors.addAll(client.interceptors());
        //Need to add one interceptor to comlete the chain (normally CallServerInterceptor, but we can't use that because it needs a real chain)
        //interceptors.add(new CallServerInterceptor(false));
        interceptors.add(new ISAPICallServerInterceptor());
        Log.d(getClass().getName(), "Creating chain with connection " + connection);
        ISAPIInterceptorChain ric = new ISAPIInterceptorChain(
                this, interceptors, originalRequest, connection);

        return ric.proceed(originalRequest);
    }

    private void createConnection() {
        Proxy proxy = client.proxy();
        if(null == proxy)
        {
            proxy = Proxy.NO_PROXY;
        }
        Address address = new Address(
                originalRequest.url().host(),
                originalRequest.url().port(),
                client.dns(),
                client.socketFactory(), //SocketFactory
                client.sslSocketFactory(), //sslSocketFactory//
                client.hostnameVerifier(), //hostNameVeriffier
                client.certificatePinner(), //certificate pinner
                client.proxyAuthenticator(),
                proxy,
                client.protocols(),
                client.connectionSpecs(),
                client.proxySelector()
                );
        InetSocketAddress isockAddr = InetSocketAddress.createUnresolved(address.url().host(), address.url().port());
        Route route = new Route(address, proxy, isockAddr);
        connection = new ISAPIDataConnection(route);
    }

    @Override
    public boolean isCanceled() {
        return false;
    }

    @Override
    public boolean isExecuted() {
        return executed;
    }

    @NotNull
    @Override
    public Request request() {
        return originalRequest;
    }

    public void setResponse(Response resp){
        this.response = resp;
    }

    public Response response(){
        return response;
    }

    @NotNull
    @Override
    public Timeout timeout() {
        return null;
    }

    public ISAPIDataConnection getConnection(){
        return connection;
    }
}
