package com.example.a1v1.Interfaces;

import org.webrtc.SessionDescription;

public interface SessionDescriptionCallback {
    void getLocalDescription(SessionDescription localSD);

    void getRemoteDescription(SessionDescription remoteSD);
}
