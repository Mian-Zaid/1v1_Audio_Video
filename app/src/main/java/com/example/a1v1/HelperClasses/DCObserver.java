package com.example.a1v1.HelperClasses;


import com.example.a1v1.Interfaces.IDataChannelCallback;

import org.webrtc.DataChannel;

import java.nio.ByteBuffer;

public class DCObserver implements DataChannel.Observer {
    public static final String TAG = "DCObserver";
    private DataChannel dataChannel;
    private int count = 0;
    private IDataChannelCallback iDataChannelCallback;

    public DCObserver() {

    }

    public DCObserver(DataChannel dataChannel) {
        this.dataChannel = dataChannel;
    }

    public DCObserver(DataChannel dataChannel, IDataChannelCallback iDataChannelCallback) {
        this.dataChannel = dataChannel;
        this.iDataChannelCallback = iDataChannelCallback;
    }

    @Override
    public void onBufferedAmountChange(long l) {

    }

    @Override
    public void onStateChange() {

    }

    @Override
    public void onMessage(DataChannel.Buffer buffer) {
        long responseTime = System.nanoTime();
//        Log.d(TAG, "NTP_Process Ping On Message Received Time ==> : " + String.valueOf(responseTime));
        ByteBuffer data = buffer.data;
        byte[] bytes = new byte[data.remaining()];
        data.get(bytes);
        final String command = new String(bytes);
        iDataChannelCallback.onMessage(command);
    }

    public void sendMessage(String message) {
        ByteBuffer buffer = ByteBuffer.wrap(message.getBytes());
        this.dataChannel.send(new DataChannel.Buffer(buffer, false));
    }

}
