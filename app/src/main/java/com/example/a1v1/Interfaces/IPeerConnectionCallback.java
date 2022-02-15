package com.example.a1v1.Interfaces;

import com.example.a1v1.HelperClasses.PeerClass;

import org.webrtc.PeerConnection;

public interface IPeerConnectionCallback {
    void getPeerConnection(PeerConnection peerConnection, PeerClass peerClass);

}
