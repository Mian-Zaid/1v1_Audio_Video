package com.example.a1v1.HelperClasses;


import org.webrtc.EglBase;

public class WebrtcUtils {

    public static final String TAG = "WebrtcUtils";
    private static WebrtcUtils webrtcUtils;
    public static EglBase mRootEglBase;

    private WebrtcUtils() {
        mRootEglBase = EglBase.create();
    }

    public static WebrtcUtils getInstance() {
        if (webrtcUtils == null) {
            synchronized (WebrtcUtils.class) {
                webrtcUtils = new WebrtcUtils();
            }
        }
        return webrtcUtils;
    }
}
