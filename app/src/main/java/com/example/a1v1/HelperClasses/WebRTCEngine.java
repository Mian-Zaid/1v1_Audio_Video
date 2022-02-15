package com.example.a1v1.HelperClasses;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import com.example.a1v1.Interfaces.IPeerConnectionCallback;

import com.example.a1v1.Interfaces.IceCandidateCallback;
import com.example.a1v1.Interfaces.IceConnectionEventsCallback;
import com.example.a1v1.Interfaces.SessionDescriptionCallback;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.Camera1Enumerator;
import org.webrtc.Camera2Enumerator;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoDecoderFactory;
import org.webrtc.VideoEncoderFactory;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static android.content.Context.MODE_APPEND;


public class WebRTCEngine {

    private static final String TAG = "WebRTCEngine";
    private static WebRTCEngine webRTCEngine;
    Activity activity;
    Context context;
    private List<PeerConnection.IceServer> iceServers;
    private PeerConnectionFactory factory;
    public MediaStream localMediaStream;
    public AudioTrack localAudioTrack;

    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference databaseReference;
    private boolean isRoomJoined = false, isOfferCreator = false, connectivityStatus = false, isOfferCreated = false, isReconnection = false;
    private static String DEVICE_ID = "DEVICE_ID";
    private HashMap<String, PeerClass> peerClassHashMap = new HashMap();
    private static String CURRENT_ICE_CAND_STATE = "";

    public PeerClass peerClass, peer;
    private PeerConnection peerConnection;


    //create singelton
    public static WebRTCEngine getInstance(Activity activity, Context context, List<PeerConnection.IceServer> iceServers) {
        if (webRTCEngine == null) {
            webRTCEngine = new WebRTCEngine(activity, context, iceServers);
        }
        return webRTCEngine;
    }

    private WebRTCEngine(Activity activity, Context context, List<PeerConnection.IceServer> iceServers) {
        this.activity = activity;
        this.context = context;
        this.iceServers = iceServers;
        DEVICE_ID = getUUid();
    }

    public void initWebRTCFactory() {

        PeerConnectionFactory.InitializationOptions initializationOptions = PeerConnectionFactory.
                InitializationOptions.builder(context)
                .setEnableInternalTracer(true)
                .createInitializationOptions();
        PeerConnectionFactory.initialize(initializationOptions);

        WebrtcUtils.getInstance();


        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();

        //video encoder decoder
        final VideoEncoderFactory encoderFactory;
        final VideoDecoderFactory decoderFactory;

        encoderFactory = new DefaultVideoEncoderFactory(WebrtcUtils.mRootEglBase.getEglBaseContext(),
                true, false);
        decoderFactory = new DefaultVideoDecoderFactory(WebrtcUtils.mRootEglBase.getEglBaseContext());


        //init factory
        factory = PeerConnectionFactory.builder()
                .setOptions(options)
                .setVideoEncoderFactory(encoderFactory)
                .setVideoDecoderFactory(decoderFactory)
                .createPeerConnectionFactory();


        localMediaStream = factory.createLocalMediaStream("MediaStream");

        createAndSendAudioTrack();


    }

    private String getUUid() {
        UUID uuid = UUID.randomUUID();
        DEVICE_ID = uuid.toString();
        return DEVICE_ID;
    }

    private void createAndSendAudioTrack() {
        Runnable audioRunnable = () -> {
            AudioSource audioSource = factory.createAudioSource(new MediaConstraints());
            localAudioTrack = factory.createAudioTrack("AudioTrack", audioSource);
            localAudioTrack.setEnabled(true);
            localMediaStream.addTrack(localAudioTrack);
        };
        this.activity.runOnUiThread(audioRunnable);
    }


    public DatabaseReference getDatabaseReference(String path) {
        if (firebaseDatabase == null) {
            initiateFirebase();
        }

        databaseReference = firebaseDatabase.getReference(path);
        return databaseReference;
    }


    IceCandidateCallback iceCandidateCallback = iceCandidate -> {
    };

    SessionDescriptionCallback sessionDescriptionCallback = new SessionDescriptionCallback() {
        @Override
        public void getLocalDescription(SessionDescription localSD) {

        }

        @Override
        public void getRemoteDescription(SessionDescription remoteSD) {

        }
    };


    private void initiateFirebase() {
        firebaseDatabase = FirebaseDatabase.getInstance();
    }

    public void postListenerForRoomDevices(DatabaseReference databaseReference) {
        ValueEventListener postListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                if (dataSnapshot.hasChildren()) {
                    Log.d(TAG, "onDataChange: " + dataSnapshot.getChildrenCount());
                }

                //not joined room yet.
                if (!isRoomJoined) {

                    //if already has someone In room , create offer to them

                    if (dataSnapshot.hasChildren()) {
                        isOfferCreator = true;
                    }

                    joinRoom(databaseReference);
                }

                if (dataSnapshot.hasChildren() && isRoomJoined) {
                    Log.d(TAG, "onDataChange: IceServers 02 \t\t" + iceServers.size());
                    createPeerConnectionsHashmap(dataSnapshot);
                    if (isOfferCreator) {
                        Log.d(TAG, "onDataChange: Offer Creation");
                        sendOffer();
                    } else {

                        if (isOfferCreated) { //only for offer creator
                            Toast.makeText(activity, "Offer created", Toast.LENGTH_SHORT).show();

                            isOfferCreated = false;
                            return;
                        }

                        //only for answer creator
                        if (dataSnapshot.hasChildren() && dataSnapshot.getChildrenCount() > 1) {
                            Toast.makeText(activity, "Offer not created", Toast.LENGTH_SHORT).show();

                            Log.d(TAG, "onDataChange: Ans. Creation");
                            sendAnswer();
                        }
                    }
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Getting Post failed, log a message
                Log.w(TAG, "loadPost:onCancelled", databaseError.toException());
            }
        };
        databaseReference.addValueEventListener(postListener);
    }

    private void sendAnswer() {
        for (Map.Entry mapElement : peerClassHashMap.entrySet()) {
            String key = (String) mapElement.getKey();
            PeerClass peerClass = ((PeerClass) mapElement.getValue());

            if (peerClass.getPeerConnectionId().equals(DEVICE_ID + "_" + peerClass.getPeerId())) {
                peerClass.initiatePeerConnection(false, true);
            }
        }
    }

    private void sendOffer() {
        for (Map.Entry mapElement : peerClassHashMap.entrySet()) {
            String key = (String) mapElement.getKey();
            PeerClass peerClass = ((PeerClass) mapElement.getValue());

            if (peerClass.getPeerConnectionId().equals(DEVICE_ID + "_" + peerClass.getPeerId())) {
                Log.d(TAG, "sendOffer: \t\t" + peerClass.getPeerId());
                peerClass.initiatePeerConnection(true, true);
            }
        }
        isOfferCreator = false;
        isOfferCreated = true;
    }


    IceConnectionEventsCallback iceConnectionEventsCallback = new IceConnectionEventsCallback() {
        @Override
        public void iceConnectionEvent(String event) {
            CURRENT_ICE_CAND_STATE = event;
            if (event.equalsIgnoreCase("Failed") && connectivityStatus) {

                isRoomJoined = false;
                isReconnection = true;
                peerClassHashMap.clear();

                if (isOfferCreator) {
                    databaseReference = firebaseDatabase.getReference("rooms");
                    databaseReference.setValue(null);
                    databaseReference = firebaseDatabase.getReference("connections");
                    databaseReference.setValue(null);
                }
            }
        }
    };

    IPeerConnectionCallback iPeerConnectionCallback = (peerConnection, peerClass) -> setPeerConnection(peerConnection, peerClass);

    private void setPeerConnection(PeerConnection peerConnection, PeerClass peerClass) {
        this.peerConnection = peerConnection;
        this.peerClass = peerClass;
    }

    //create a hashmap of all the peers who joined the room
    private void createPeerConnectionsHashmap(DataSnapshot dataSnapshot) {
        for (DataSnapshot dataSnapshot1 : dataSnapshot.getChildren()) {
            String id = (String) dataSnapshot1.getValue();

            if (!DEVICE_ID.equalsIgnoreCase(id) &&
                    !CURRENT_ICE_CAND_STATE.equalsIgnoreCase("DISCONNECTED")) {

                //Create a peer class object of new member who joind the room
                peer = new PeerClass(this.context, DEVICE_ID, id, iceServers, factory, DEVICE_ID + "_" + id,
                        iceConnectionEventsCallback, iPeerConnectionCallback, WebrtcUtils.mRootEglBase,
                        activity);
                peer.setLocalStream(localMediaStream);

                peer.setOnSendLogsListener(sessionDescriptionCallback, iceCandidateCallback);

                peerClassHashMap.put(DEVICE_ID + "_" + id, peer);// for offer to ans.


            }
        }

    }


    private void joinRoom(DatabaseReference dbRef) {
        if (!isRoomJoined) {
            isRoomJoined = true;
            dbRef = getDatabaseReference("rooms");
            String id = dbRef.push().getKey();
            dbRef.child(Build.MANUFACTURER + "_" + Build.DEVICE + "_" + id).setValue(DEVICE_ID);
        }
    }
}
