package com.example.a1v1.Interfaces;


import com.example.a1v1.Models.WebRTCModel;

public interface NetworkCallback {
    void JsonData(WebRTCModel example);

    void initializeOutboundCall(WebRTCModel example);
}
