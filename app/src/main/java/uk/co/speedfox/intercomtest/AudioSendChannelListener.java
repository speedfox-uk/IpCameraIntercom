package uk.co.speedfox.intercomtest;

import java.io.OutputStream;

public interface AudioSendChannelListener {
    void audioChannelOpen(OutputStream os);

    void audioChannelClosing();
}
