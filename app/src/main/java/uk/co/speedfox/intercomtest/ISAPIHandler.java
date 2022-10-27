package uk.co.speedfox.intercomtest;

import android.util.Log;
import com.burgstaller.okhttp.AuthenticationCacheInterceptor;
import com.burgstaller.okhttp.CachingAuthenticatorDecorator;
import com.burgstaller.okhttp.digest.CachingAuthenticator;
import com.burgstaller.okhttp.digest.DigestAuthenticator;
import okhttp3.*;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * This class handles the communication to the camera, both the API calls and sending and receiving audio. It does not
 * handle receiving video, which is done via
 */
public class ISAPIHandler
{
    OkHttpClient client;
    String hostName;
    String userName;
    String password;
    String protocol = "http://";
    AudioSendChannelListener sendListener;
    AudioReceiver receiveListener;
    private final ExecutorService pool;
    DocumentBuilder builder;
    private boolean sendAudio = false;
    private boolean receiveAudio = false;

    private volatile ISAPIDataConnection sendAudioConnection = null;

    public ISAPIHandler(String host, String userName, String password) throws ParserConfigurationException, IOException {
        Log.d("GOT HOST", host);
        this.hostName = host;
        this.userName = userName;
        this.password = password;
        this.pool = Executors.newFixedThreadPool(5);

        DigestAuthenticator authenticator = new DigestAuthenticator(new com.burgstaller.okhttp.digest.Credentials(this.userName, this.password));
        ConcurrentHashMap authCache = new ConcurrentHashMap<String, CachingAuthenticator>();
        CachingAuthenticatorDecorator decorator = new CachingAuthenticatorDecorator(authenticator, authCache);
        AuthenticationCacheInterceptor interceptor = new AuthenticationCacheInterceptor(authCache);
        client = new OkHttpClient();
        client = client.newBuilder().authenticator(decorator).addInterceptor(interceptor).connectionPool(new ConnectionPool(0, 1L, TimeUnit.MICROSECONDS)).build();

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(false);
        factory.setIgnoringElementContentWhitespace(true);
        builder = factory.newDocumentBuilder();

        sendListener = new AudioSender();
        receiveListener = new AudioReceiver();
    }

    public void startAudio(boolean sendAudio, boolean receiveAudio) throws IOException {
        this.sendAudio = sendAudio;
        this.receiveAudio = receiveAudio;
        Runnable startReq = new Runnable() {
            @Override
            public void run() {

                try {
                    String channelId = getChannelId();
                    Log.d(ISAPIHandler.class.getName(), "Got channel ID:" + channelId);
                    openChannel(channelId);
                    Log.d(ISAPIHandler.class.getName(), "Audio open.");
                    //Log.d(getClass().getName(), "Started sending audio data");
                } catch (Exception e) {
                    e.printStackTrace();
                }

                Log.d(getClass().getName(), "*** Finished sending audio thread.");
            }
        };

        pool.execute(startReq);
    }

    private void sendAudioData(String channelId) throws IOException {

        String audioData = protocol + hostName + "/ISAPI/System/TwoWayAudio/channels/" + channelId + "/audioData";
        Request req = (new Request.Builder())
                .url(audioData)
                .put(RequestBody.create(null, new byte[0]))
                .build();
        sendAudioConnection = doISAPIDataCall(req).getConnection();
        sendListener.audioChannelOpen(sendAudioConnection.socket().getOutputStream());
        Log.d(getClass().getName(), "*** Audio sending connection established");
    }

    private void receiveAudioData(String channelId) throws IOException {

        String audioData = protocol + hostName + "/ISAPI/System/TwoWayAudio/channels/" + channelId + "/audioData";
        Request req = (new Request.Builder())
                .url(audioData)
                .get()
                .build();
        sendAudioConnection = doISAPIDataCall(req).getConnection();
        receiveListener.audioChannelOpen(sendAudioConnection.socket().getInputStream());
        Log.d(getClass().getName(), "*** Audio receiving connection established");
    }

    private ISAPIDataCall doISAPIDataCall(Request req) throws IOException {
        ISAPIDataCall idc = new ISAPIDataCall(req, client);
        Response resp = idc.execute();
        Log.d(getClass().getName(), "*** ISAPI CALL DONE:" + resp.code());
        return idc;
    }

    private void openChannel(String channelId) throws IOException, ISAPIException {
        Log.d(getClass().getName(), "Opening channel");
        String openChannel = protocol + hostName + "/ISAPI/System/TwoWayAudio/channels/" + channelId + "/open";

        Request req = (new Request.Builder()).url(openChannel).put(RequestBody.create(null, new byte[0])).build();
        Response resp = sendRequest(req);
        if(resp.code() == 200) {
            Log.d(ISAPIHandler.class.getName(), "Channel open.");
            Runnable audioSendStartTask = new Runnable() {
                @Override
                public void run() {
                    try {
                        sendAudioData(channelId);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            };


            Runnable audioReceiveStartTask = new Runnable() {
                @Override
                public void run() {
                    try {
                        receiveAudioData(channelId);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            };

            if(sendAudio) {
                pool.execute(audioSendStartTask);
            }

            if(receiveAudio) {
                Log.d(getClass().getSimpleName(), "***** Starting getting the audio now!");
                pool.execute(audioReceiveStartTask);
            }
        }
    }

    private String getChannelId() throws IOException, ISAPIException {
        Log.d(getClass().getName(), "Getting Channel ID");
        String getChannels = protocol + hostName + "/ISAPI/System/TwoWayAudio/channels";
        Request req = (new Request.Builder()).url(getChannels).get().build();
        Response resp = sendRequest(req);
        InputStream content = resp.body().byteStream();
        try {
            Document doc = builder.parse(content);
            NodeList twacl = doc.getFirstChild().getChildNodes(); //TwoWayAudioChannelList;
            for(int i=0; i<twacl.getLength(); i++)
            {
                Node nd1 = twacl.item(i);
                if(nd1.getNodeName().equalsIgnoreCase("TwoWayAudioChannel"))
                {
                    NodeList twacParts = nd1.getChildNodes();
                    Log.d(ISAPIHandler.class.getName(), "" + twacParts.getLength() + " from " + nd1.getNodeName());
                    for(int j=0; j < twacParts.getLength(); j++)
                    {
                        Node nd = twacParts.item(i);
                        if(nd.getNodeName().equalsIgnoreCase("id"))
                        {
                            return nd.getTextContent();
                        }
                    }

                }

            }

            throw new ISAPIException("Failed to find a channel");

        } catch (SAXException e) {
            throw new ISAPIException("Failed parsing get channels response", e);
        }
        finally {
            resp.close();
        }
    }

    private Response sendRequest(Request req) throws IOException, ISAPIException {
        Log.d(ISAPIHandler.class.getName(), "REQ:" + req.toString());
        Call call = client.newCall(req);
        Log.d(ISAPIHandler.class.getName(), "REQ headers:" + call.request().headers());
        Response resp = call.execute();
        Log.d(ISAPIHandler.class.getName(), resp.toString());
        if(resp.code() != 200) {
            //Go some error handling
            Log.d(ISAPIHandler.class.getName(), "BAD RESP");
            throw new ISAPIException("Bad response getting channels:" + resp.code());
        }
        return resp;
    }


    public void stopAudioOutput(String channelId) throws ISAPIException {
        if(null != sendListener) {
            sendListener.audioChannelClosing();
        }

        receiveListener.stop();

        if(null != sendAudioConnection)
        {
            Log.d(getClass().getName(), "***** CLOSING CONNECTION *****");
            try {
                sendAudioConnection.disconnect();
            }
            catch (IOException e)
            {
                //ignore
            }
            sendAudioConnection = null;
        }

        Runnable stopReq = new Runnable() {
            @Override
            public void run() {
                String closeChannel = protocol + hostName + "/ISAPI/System/TwoWayAudio/channels/" + channelId + "/close";
                try {
                    Request req = (new Request.Builder()).url(closeChannel).put(RequestBody.create(null, new byte[0])).build();
                    sendRequest(req);
                } catch (IOException | ISAPIException e) {
                    e.printStackTrace();
                }

                Log.d(ISAPIHandler.class.getName(), "*** Audio closed.");
            }
        };

        pool.execute(stopReq);
    }

    /*private void sendRequesttoConnection(HttpUriRequestBase req) {
        OutputStream outputStream = null;
        req.setEntity();
    }*/

    public void setAudioSendListener(AudioSendChannelListener audioSendListener){
        this.sendListener = audioSendListener;
    }

}
