package uk.co.speedfox.intercomtest;

import android.util.Log;
import okhttp3.Connection;
import okhttp3.Handshake;
import okhttp3.Protocol;
import okhttp3.Route;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * An implementation of the connection class that creates a new tcp/ip connection every time. This is because you need
 * separate connections for sending and receiving the audio, so should not use a chaced connection.
 */
public class ISAPIDataConnection implements Connection {

    private Route route;
    private Protocol protocol = null;
    private Socket sock;

    ISAPIDataConnection(Route route){
        this.route = route;
    }

    @Nullable
    @Override
    public Handshake handshake() {
        return null;
    }

    @NotNull
    @Override
    public Protocol protocol() {
        return protocol;
    }

    @NotNull
    @Override
    public Route route() {
        return route;
    }

    @NotNull
    @Override
    public Socket socket() {
        return sock;
    }

    public void connect() throws IOException {
        if(sock != null)
            return;

        sock = new Socket();
        Log.d(ISAPIDataConnection.class.getName(), String.format("HOST '%s'", route.socketAddress().getHostName()));
        InetSocketAddress addr = route.socketAddress();

        if(addr.getHostName().matches("[0-9]*\\.[0-9]*\\.[0-9]*\\.[0-9]*")){
            Log.d(ISAPIDataConnection.class.getName(), "GOT IP");
            String[] parts = addr.getHostName().split("\\.");
            byte[] addrBytes = new byte[4];
            for(int i=0; i<4; i++)
            {
                addrBytes[i] = (byte) Integer.parseInt(parts[i]);
            }
            InetAddress inet = InetAddress.getByAddress(addrBytes);
            addr = new InetSocketAddress(inet, addr.getPort());

        }

        sock.connect(addr);

    }

    public void disconnect() throws IOException {
        sock.close();
    }
}
