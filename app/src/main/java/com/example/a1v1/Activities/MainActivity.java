package com.example.a1v1.Activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.example.a1v1.HelperClasses.NTSTokenAsyncTask;
import com.example.a1v1.HelperClasses.WebRTCEngine;
import com.example.a1v1.Interfaces.IDisplayVideoCallBack;
import com.example.a1v1.Interfaces.NetworkCallback;
import com.example.a1v1.Models.WebRTCModel;
import com.example.a1v1.R;
import com.google.firebase.database.DatabaseReference;

import org.jetbrains.annotations.NotNull;
import org.webrtc.PeerConnection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import io.sentry.Sentry;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity implements
        View.OnClickListener,
        EasyPermissions.PermissionCallbacks,
        NetworkCallback {

    private static final String TAG = "MainActivity";
    LinearLayout btnLayout;
    Button btnCall, btn_end;
    List<DtoStream> streamList = new ArrayList<>();
    StreamsAdapter streamsAdapter = null;
    RecyclerView recyclerView_Streams;
    HashMap<String, Boolean> streamHashMap = new HashMap<>();
    LinearLayout layout_controls;
    ImageView mic, swap;


    private PeerConnection.IceServer.Builder iceServerBuilder;
    private final List<PeerConnection.IceServer> iceServers = new LinkedList<>();
    WebRTCEngine webRTCEngine;

    private DatabaseReference databaseReference;


    private static final int RC_PERMISSION_LIST = 1001;
    private static final String[] PERMISSION_LIST = {
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE};

    Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initComponents();
        initListeners();
        initStreamsAdapterGrid();

        initPeerConnection(iceServers);
    }

    private void initStreamsAdapterGrid() {
        streamsAdapter = new StreamsAdapter(getApplicationContext(), MainActivity.this, streamList);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        recyclerView_Streams.setLayoutManager(linearLayoutManager);
        recyclerView_Streams.setAdapter(streamsAdapter);
    }

    private void initPeerConnection(List<PeerConnection.IceServer> iceServers) {
        Log.d(TAG, "run: createScreenCapturer,screenCapturer init");
        webRTCEngine = WebRTCEngine.getInstance(MainActivity.this
                , getApplicationContext(), iceServers);

        webRTCEngine.initWebRTCFactory();

        webRTCEngine.setDisplayVideoCallback(iDisplayVideoCallBack);

        methodRequiresCameraAndMicPermissions();
    }

    private void initListeners() {
        btnCall.setOnClickListener(this);
        btnLayout.setOnClickListener(this);
        btn_end.setOnClickListener(this);
        mic.setOnClickListener(this);
        swap.setOnClickListener(this);

    }

    private void initComponents() {
        context = getApplicationContext();

        btnLayout = findViewById(R.id.btn_layout);
        btnCall = findViewById(R.id.btn_call);
        btn_end = findViewById(R.id.btn_end);

        mic = findViewById(R.id.imgView_Mic);
        swap = findViewById(R.id.imgView_CameraSwitch);

        layout_controls = findViewById(R.id.layout_controls);
        recyclerView_Streams = findViewById(R.id.recyclerView_Streams);

    }


    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_layout:
                break;
            case R.id.btn_call:
                joinRoom();
                break;
            case R.id.btn_end:
                endCall();
                break;
            case R.id.imgView_CameraSwitch:
                switchCamera();
                break;
            case R.id.imgView_Mic:
                toggleMic();
                break;
        }
    }

    private void toggleMic() {
        WebRTCEngine.getInstance(this, getApplicationContext(), null).toggleMic();
    }

    private void switchCamera() {
        WebRTCEngine.getInstance(this, getApplicationContext(), null)
                .toggleCamera();
    }

    private void endCall() {
        databaseReference = webRTCEngine.getDatabaseReference("rooms");
        databaseReference.setValue(null);

        databaseReference = webRTCEngine.getDatabaseReference("connections");
        databaseReference.setValue(null);
        switchUI(false);
    }


    private void joinRoom() {
        Toast.makeText(context, "Joining", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "joinRoom: Joining");
        databaseReference = webRTCEngine.getDatabaseReference("rooms");
        webRTCEngine.postListenerForRoomDevices(databaseReference);
        switchUI(true);

    }

    private void switchUI(boolean b) {
        if (b) {
            btnCall.setVisibility(View.GONE);
            layout_controls.setVisibility(View.VISIBLE);
        }
    }

    //permissions

    @AfterPermissionGranted(RC_PERMISSION_LIST)
    private void methodRequiresCameraAndMicPermissions() {
        if (EasyPermissions.hasPermissions(this, PERMISSION_LIST)) {
            NTSTokenAsyncTask ntsToken = new NTSTokenAsyncTask(context, this);
            ntsToken.execute();
        } else {
            // Do not have permissions, request them now
            EasyPermissions.requestPermissions(
                    this,
                    "Mic Permission",
                    RC_PERMISSION_LIST, PERMISSION_LIST);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String @NotNull [] permissions, int @NotNull [] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {

    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            new AppSettingsDialog.Builder(this).build().show();
        }
    }


    //Network Callback methods
    @Override
    public void JsonData(WebRTCModel example) {

    }

    @Override
    public void initializeOutboundCall(WebRTCModel example) {
        try {
            for (int i = 0; i < example.iceServers.size(); ++i) {

                if (!example.iceServers.get(i).getUsername().isEmpty()) {
                    iceServerBuilder = PeerConnection.IceServer.builder(example.iceServers.get(i).getUrl())
                            .setUsername(example.iceServers.get(i).getUsername())
                            .setPassword(example.iceServers.get(i).getCredentials());
                } else {
                    iceServerBuilder = PeerConnection.IceServer.builder(example.iceServers.get(i).getUrl());
                }

                iceServers.add(iceServerBuilder.createIceServer());
                Log.d(TAG, "initializeOutboundCall: IceServers 01 \t\t" + iceServers.size());


            }
        } catch (Exception e) {
            Log.e(TAG, "ERROR: " + e.getMessage());

        }
    }


    IDisplayVideoCallBack iDisplayVideoCallBack = new IDisplayVideoCallBack() {
        @Override
        public void setDisplayVideo(boolean isLocalVideo, String deviceId, View view) {
            handleStreamHashMap(deviceId, view);
        }
    };

    private void handleStreamHashMap(String deviceId, View view) {
        if (!streamHashMap.containsKey(deviceId)) {
            streamHashMap.put(deviceId, true);
            DtoStream dtoStream = new DtoStream(deviceId, view);
            streamsAdapter.addStreamToAdapter(dtoStream);
        }
    }
}