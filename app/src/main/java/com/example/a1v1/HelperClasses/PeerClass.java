package com.example.a1v1.HelperClasses;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.a1v1.Interfaces.IDataChannelCallback;
import com.example.a1v1.Interfaces.IPeerConnectionCallback;

import com.example.a1v1.Interfaces.IVideoCallback;
import com.example.a1v1.Interfaces.IceCandidateCallback;
import com.example.a1v1.Interfaces.IceConnectionEventsCallback;
import com.example.a1v1.Interfaces.SessionDescriptionCallback;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.webrtc.AudioTrack;
import org.webrtc.Camera1Enumerator;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.CameraVideoCapturer;
import org.webrtc.CandidatePairChangeEvent;
import org.webrtc.DataChannel;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RTCStats;
import org.webrtc.RTCStatsReport;
import org.webrtc.RendererCommon;
import org.webrtc.RtpReceiver;
import org.webrtc.RtpTransceiver;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PeerClass implements SdpObserver, PeerConnection.Observer {

    public static final String TAG = "PeerClassTag";

    private SessionDescriptionCallback sessionDescriptionCallback;
    private IceCandidateCallback iceCandidateCallback;
    Activity activity;

    private String deviceId;
    private String peerId;
    private boolean isOffer;
    private PeerConnectionFactory peerConnectionFactory;
    private List<PeerConnection.IceServer> iceServers;
    private Context context;
    private PeerConnection peerConnection;

    private DatabaseReference databaseReference;
    private FirebaseDatabase firebaseDatabase;
    private String peerConnectionId;
    private IceConnectionEventsCallback iceConnectionEventsCallback;
    private boolean isOfferPosted, isAnswerPosted;
    DataChannel dataChannel = null;
    private IPeerConnectionCallback iPeerConnectionCallback;
    int count = 0;
    private Handler handler = new Handler();
    int iterationCount = 0;
    public AudioTrack localAudioTrack = null, remoteAudioTrack = null;


    private EglBase eglBase;
    private IVideoCallback iVideoCallback;
    private MediaStream mediaStream;


    public PeerClass(Context context, String deviceId, String id, List<PeerConnection.IceServer> iceServers, PeerConnectionFactory factory,
                     String s,
                     IceConnectionEventsCallback iceConnectionEventsCallback, IPeerConnectionCallback iPeerConnectionCallback, EglBase eglBase,
                     IVideoCallback iVideoCallback, Activity activity, VideoCapturer videoCapturer) {
        this.context = context;
        this.deviceId = deviceId;
        this.peerId = id;
        this.iceServers = iceServers;
        this.peerConnectionFactory = factory;
        this.peerConnectionId = s;
        this.eglBase = eglBase;
        this.iVideoCallback = iVideoCallback;
        this.iceConnectionEventsCallback = iceConnectionEventsCallback;
        this.iPeerConnectionCallback = iPeerConnectionCallback;
        this.activity = activity;
        Log.d(TAG, "PeerClass: Constr");
    }

    public PeerClass(Context context, String deviceId, String id, List<PeerConnection.IceServer> iceServers, PeerConnectionFactory factory,
                     String s,
                     IceConnectionEventsCallback iceConnectionEventsCallback, IPeerConnectionCallback iPeerConnectionCallback, EglBase eglBase,
                     Activity activity) {
        this.context = context;
        this.deviceId = deviceId;
        this.peerId = id;
        this.iceServers = iceServers;
        this.peerConnectionFactory = factory;
        this.peerConnectionId = s;
        this.eglBase = eglBase;
        this.iceConnectionEventsCallback = iceConnectionEventsCallback;
        this.iPeerConnectionCallback = iPeerConnectionCallback;
        this.activity = activity;
        Log.d(TAG, "PeerClass: Constr");
    }

    public void setLocalStream(MediaStream mediaStream) {
        this.mediaStream = mediaStream;
    }

    public String getPeerId() {
        return peerId;
    }


    public String getPeerConnectionId() {
        return peerConnectionId;
    }


    private PeerConnection createPeerConnection() {

        //Audio Manager
        createAudioManager();

        //[For Native WebRtc]
        Log.d(TAG, "createPeerConnection: IceServers 03 \t\t" + iceServers.size());
        PeerConnection.RTCConfiguration rtcConfig = new PeerConnection.RTCConfiguration(iceServers);
        rtcConfig.iceTransportsType = PeerConnection.IceTransportsType.ALL;
        PeerConnection peerConnection = peerConnectionFactory.createPeerConnection(rtcConfig, this);

        //[DATA CHANNEL]  zaid
        createDataChannel(peerConnection);

        //add stream to peer connection
        assert peerConnection != null;
        if (peerConnection.addStream(mediaStream)) {
            Log.d(TAG, "createPeerConnection: added stram");
        } else {
            Log.d(TAG, "createPeerConnection: not added stream");
        }

        return peerConnection;
    }

    private void createDataChannel(PeerConnection peerConnection) {
        Log.d(TAG, "createPeerConnection: onMessage,dataChannelEnable");

        DataChannel.Init dcInit = new DataChannel.Init();

        //by default
        dataChannel = peerConnection.createDataChannel("DataChannelLabel", dcInit);


        if (dataChannel == null) {
            Log.d(TAG, "createPeerConnection: DATA CHANNELLE IN NULLLLL.....");
        }


    }

    private void createAudioManager() {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        audioManager.setSpeakerphoneOn(true);
        audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
    }

    //[Logs listeners call by UI layer]
    public void setOnSendLogsListener(SessionDescriptionCallback sessionDescriptionCallback,
                                      IceCandidateCallback iceCandidateCallback) {
        this.sessionDescriptionCallback = sessionDescriptionCallback;
        this.iceCandidateCallback = iceCandidateCallback;
    }


    public void initiatePeerConnection(boolean isOffer, boolean isMediaInclude) {
        firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference("connections");

        this.isOffer = isOffer;
        peerConnection = createPeerConnection();

        if (isOffer) {
            createOffer();
        } else {
            activateOfferListener();
        }


    }

    private void createOffer() {
        MediaConstraints mediaConstraints = new MediaConstraints();
        mediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
        mediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));

        peerConnection.createOffer(this, mediaConstraints);
    }

    private void createAnswer() {
        MediaConstraints mediaConstraints = new MediaConstraints();
        mediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
        mediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));


        peerConnection.createAnswer(this, mediaConstraints);
    }

    private void parseRemoteDescription(DataSnapshot dataSnapshot) {
        for (DataSnapshot dataSnapshot1 : dataSnapshot.getChildren()) {
            String remoteDescription = (String) dataSnapshot1.getValue();
            if (isOffer) {
                peerConnection.setRemoteDescription(this, new SessionDescription(SessionDescription.Type.ANSWER, remoteDescription));
            } else {
                peerConnection.setRemoteDescription(this, new SessionDescription(SessionDescription.Type.OFFER, remoteDescription));
                createAnswer();
            }
        }
    }

    @Override
    public void onSignalingChange(PeerConnection.SignalingState signalingState) {
        Log.d(TAG, "onSignalingChange: " + signalingState.name());
    }

    @Override
    public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {

        Log.d(TAG, "onIceConnectionChange: ");


        if (iceConnectionState.name().equalsIgnoreCase("CONNECTED")) {

            iPeerConnectionCallback.getPeerConnection(peerConnection, this);
            handler.postDelayed(() -> sendData(), 5000);

        }

        iceConnectionEventsCallback.iceConnectionEvent(iceConnectionState.name());


    }

    private void sendData() {
        Log.d(TAG, "sendData: ");
    }

    @Override
    public void onStandardizedIceConnectionChange(PeerConnection.IceConnectionState newState) {
        Log.d(TAG, "onStandardizedIceConnectionChange: ");
    }

    @Override
    public void onConnectionChange(PeerConnection.PeerConnectionState newState) {

    }

    @Override
    public void onIceConnectionReceivingChange(boolean b) {
        Log.d(TAG, "onIceConnectionReceivingChange: ");
    }

    @Override
    public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {
        Log.d(TAG, "onIceGatheringChange: ");
    }

    @Override
    public void onIceCandidate(IceCandidate iceCandidate) {
        Log.d(TAG, "onIceCandidate: ");
        Map<String, Object> ice = new HashMap<>();
        ice.put("label", iceCandidate.sdpMLineIndex);
        ice.put("id", iceCandidate.sdpMid);
        ice.put("sdp", iceCandidate.sdp);

        if (isOffer) {
            databaseReference = firebaseDatabase.getReference("connections")
                    .child(deviceId + "__TO__" + peerId).child(deviceId + "__" + peerId).child("ice_cand");
        } else {
            databaseReference = firebaseDatabase.getReference("connections")
                    .child(peerId + "__TO__" + deviceId).child(deviceId + "__" + peerId).child("ice_cand");
        }

        String dbKey = databaseReference.push().getKey();
        databaseReference.child(dbKey).setValue(ice);
        iceCandidateCallback.getIceCandidate(iceCandidate);

    }

    @Override
    public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {
        Log.d(TAG, "onIceCandidatesRemoved: ");
    }

    @Override
    public void onSelectedCandidatePairChanged(CandidatePairChangeEvent event) {
        Log.d(TAG, "onSelectedCandidatePairChanged: ");
    }

    @Override
    public void onAddStream(MediaStream mediaStream) {
        if (mediaStream.audioTracks.size() > 0) {
            Log.d(TAG, "onAddStream: stream found");
            remoteAudioTrack = mediaStream.audioTracks.get(0);
            remoteAudioTrack.setEnabled(true);
        }
        Log.d(TAG, "onAddStream: remoteStream");

    }


    @Override
    public void onRemoveStream(MediaStream mediaStream) {

    }

    @Override
    public void onDataChannel(DataChannel dataChannel) {
        DCObserver dcObserver = new DCObserver(this.dataChannel, iDataChannelCallback);
        dataChannel.registerObserver(dcObserver);
        Log.d(TAG, "onDataChannel: ");

    }

    @Override
    public void onRenegotiationNeeded() {
        Log.d(TAG, "onRenegotiationNeeded: ");
    }

    @Override
    public void onAddTrack(RtpReceiver rtpReceiver, MediaStream[] mediaStreams) {
        Log.d(TAG, "onAddTrack: " + rtpReceiver.toString());
    }

    @Override
    public void onRemoveTrack(RtpReceiver receiver) {

        Log.d(TAG, "onRemoveTrack: ");
    }

    @Override
    public void onTrack(RtpTransceiver transceiver) {
        Log.d(TAG, "onTrack: " + transceiver.toString());
    }

    @Override
    public void onCreateSuccess(SessionDescription sessionDescription) {
        Log.d(TAG, "onCreateSuccess: ");
        peerConnection.setLocalDescription(this, sessionDescription);
        if (isOffer) {
            if (!isOfferPosted) {
                isOfferPosted = true;
                databaseReference = firebaseDatabase.getReference("connections")
                        .child(deviceId + "__TO__" + peerId)
                        .child(deviceId + "__" + peerId)
                        .child("offer");
                String id = databaseReference.push().getKey();
                databaseReference.child(id).setValue(sessionDescription.description);
            }
            activateAnswerListener(); // for current created offer.
        } else {
            if (!isAnswerPosted) {
                isAnswerPosted = true;
                databaseReference = firebaseDatabase.getReference("connections")
                        .child(peerId + "__TO__" + deviceId)
                        .child(deviceId + "__" + peerId)
                        .child("answer");
                String id = databaseReference.push().getKey();
                databaseReference.child(id).setValue(sessionDescription.description);
            }
        }
    }

    @Override
    public void onSetSuccess() {

        Log.d(TAG, "onSetSuccess: ");
    }

    @Override
    public void onCreateFailure(String s) {
        Log.d(TAG, "onCreateFailure: ");
    }

    @Override
    public void onSetFailure(String s) {
        Log.d(TAG, "onSetFailure: ");
    }


    ValueEventListener offerListener, answerListener, iceCandListener;

    private void postListenerForOffer(DatabaseReference mPostReference) {
        Log.d(TAG, "postListenerForOffer: ");
        offerListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.hasChildren()) {
                    parseRemoteDescription(dataSnapshot);
                } else {
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Getting Post failed, log a message
                Log.w(TAG, "loadPost:onCancelled", databaseError.toException());
            }
        };
        mPostReference.addValueEventListener(offerListener);
        activateIceCandidateListener();
    }

    private void postListenerForAnswer(DatabaseReference mPostReference) {
        Log.d(TAG, "postListenerForAnswer: ");
        answerListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.hasChildren()) {
                    parseRemoteDescription(dataSnapshot);
                } else {
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, "loadPost:onCancelled", databaseError.toException());
            }
        };
        mPostReference.addValueEventListener(answerListener);
        activateIceCandidateListener();
    }

    private void postListenerForIceCandidates(DatabaseReference mPostReference) {

        Log.d(TAG, "postListenerForIceCandidates: ");
        iceCandListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.hasChildren()) {
                    for (DataSnapshot dataSnapshot1 : dataSnapshot.getChildren()) {
                        Map<String, Object> iceCandidate = (Map<String, Object>) dataSnapshot1.getValue();
                        IceCandidate iceCandidate1 = new IceCandidate(
                                (String) iceCandidate.get("id"),
                                Integer.parseInt(iceCandidate.get("label") + ""),
                                (String) iceCandidate.get("sdp"));
                        peerConnection.addIceCandidate(iceCandidate1);
                    }
                } else {
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Getting Post failed, log a message
                Log.w(TAG, "loadPost:onCancelled", databaseError.toException());
            }
        };

        mPostReference.addValueEventListener(iceCandListener);
    }

    private void activateOfferListener() {
        Log.d(TAG, "activateOfferListener: ");
        databaseReference = firebaseDatabase.getReference("connections")
                .child(peerId + "__TO__" + deviceId).child(peerId + "__" + deviceId).child("offer");
        postListenerForOffer(databaseReference);
    }

    private void activateAnswerListener() {
        Log.d(TAG, "activateAnswerListener: ");
        databaseReference = firebaseDatabase.getReference("connections")
                .child(deviceId + "__TO__" + peerId).child(peerId + "__" + deviceId).child("answer");
        postListenerForAnswer(databaseReference);
    }

    private void activateIceCandidateListener() {

        Log.d(TAG, "activateIceCandidateListener: ");
        if (isOffer) {
            databaseReference = firebaseDatabase.getReference("connections")
                    .child(deviceId + "__TO__" + peerId).child(peerId + "__" + deviceId).child("ice_cand");
        } else {
            databaseReference = firebaseDatabase.getReference("connections")
                    .child(peerId + "__TO__" + deviceId).child(peerId + "__" + deviceId).child("ice_cand");
        }

        postListenerForIceCandidates(databaseReference);
    }

    IDataChannelCallback iDataChannelCallback = new IDataChannelCallback() {
        @Override
        public void onMessage(String message) {
            Log.d(TAG, "onMessage: Got new message : " + message);
        }

        @Override
        public void onMessage(String message, long receiveTime) {
        }
    };


    private long getSystemTimeInNano() {
        Log.d(TAG, "getSystemTimeInNano: ");
        return System.nanoTime();
    }

    public void closePeerConnection() {
        peerConnection.dispose();
        peerConnection.close();
    }

    public void disableFirebaseListeners() {
        Log.d(TAG, "disableFirebaseListeners: ");
        databaseReference.removeEventListener(offerListener);
        databaseReference.removeEventListener(answerListener);
        databaseReference.removeEventListener(iceCandListener);
    }

    public void sendMessage(String message) {
        Log.d(TAG, "sendMessage: ");
        ByteBuffer buffer = ByteBuffer.wrap(message.getBytes());


        if (dataChannel != null) {
            Log.d("sendMessage: ", buffer.toString() + " : " + message);
            Log.d(TAG, "sendMessage: Sending Nowww......");
            this.dataChannel.send(new DataChannel.Buffer(buffer, false));
        } else {
            Log.d(TAG, "sendMessage: NULL DATA CHANNEL");
        }

    }

    public PeerConnection getPeerConnection() {
        return peerConnection;
    }
}

