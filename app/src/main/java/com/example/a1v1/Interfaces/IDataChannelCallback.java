package com.example.a1v1.Interfaces;

public interface IDataChannelCallback {

    void onMessage(String message);

    void onMessage(String message, long receiveTime);
}
