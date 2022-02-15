package com.example.a1v1.Interfaces;

import android.view.View;

public interface IVideoCallback {
    void getVideoView(boolean isLocalVideo, View view);

    void getVideoView(boolean isLocalVideo, View view, String deviceId);
}