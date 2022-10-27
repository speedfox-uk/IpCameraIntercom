package uk.co.speedfox.intercomtest;

import android.util.Log;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;

/**
 * Replicates the call chain inside okhttp
 */
public class ISAPICallServerInterceptor implements Interceptor {

    OutputStream outStream;
    @NotNull
    @Override
    public Response intercept(@NotNull Chain chain) throws IOException {
        ISAPIDataConnection connection = (ISAPIDataConnection)(chain.connection());
        connection.connect();
        outStream = connection.socket().getOutputStream();
        Request req = chain.request();
        for(String headerName: req.headers().names())
        {
            Log.d(getClass().getName(), "HEADER-"+ headerName + ":" + req.headers().get(headerName));
        }

        long sentTime = System.currentTimeMillis();
        startRequest(req);
        long length = -1;
        if(null != req.body())
        {
            writeHeadersForBody(req.headers(), req.body().contentLength());
        }
        else
        {
            writeHeadersNoBody(req.headers());
        }
        finishRequest();
        long completeTime = System.currentTimeMillis();

        InputStream inStream = connection.socket().getInputStream();
        String firstLine = new String(readLine(inStream));
        String[] firstParts = firstLine.split("\\s+");
        int code = Integer.parseInt(firstParts[1]);

        HashMap<String, String> headerList = new HashMap<>();
        String line = new String(readLine(inStream));
        while (line.length()>2){
            String[] splitLine = line.split(":");
            if(splitLine.length == 2)
            {
                headerList.put(splitLine[0], splitLine[1]);
            }
            line = new String(readLine(inStream));
        }
        Headers headers = Headers.of(headerList);
        return new Response(req, Protocol.HTTP_2, "", code, null, headers, null, null, null, null, sentTime, completeTime, null);
    }

    private byte[] readLine(InputStream inStream) throws IOException {
        byte[] lineBuf = new byte[255];
        int i;
        for(i=0; i < lineBuf.length; i++){
            inStream.read(lineBuf, i, 1);
            if(i>0 && lineBuf[i-1] == (byte)13   && lineBuf[i] == 10)  {
                break;
            }
        }
        return Arrays.copyOf(lineBuf,  i);
    }

    private void writeHeadersForBody(Headers headers, long contentLength) throws IOException {
        boolean needContentLength = contentLength >= 0;
        for(String headerName: headers.names())
        {
            wrtieLine(String.format("%s: %s", headerName, headers.get(headerName)));
            needContentLength = needContentLength || headerName.equals("Content-Length");
        }

        wrtieLine("Connection: keep-alive");
        if(needContentLength)
        {
            wrtieLine(String.format("Content-Length: %d", contentLength));
        }
        wrtieLine("Content-Type: application/octet-stream");
    }


    private void writeHeadersNoBody(Headers headers) throws IOException {
        for(String headerName: headers.names())
        {
            wrtieLine(String.format("%s: %s", headerName, headers.get(headerName)));

        }
    }

    private void startRequest(Request req) throws IOException {
        wrtieLine(String.format("%s %s HTTP/1.1", req.method(), req.url().encodedPath()));
        wrtieLine(String.format("HOST: %s", req.url().host()));
    }


    private void wrtieLine(String s) throws IOException {
        Log.d(getClass().getName(), "REQ:" + s);
        outStream.write(String.format("%s\r\n", s).getBytes());
    }

    private void finishRequest() throws IOException {
        outStream.write("\r\n".getBytes());
    }
}
