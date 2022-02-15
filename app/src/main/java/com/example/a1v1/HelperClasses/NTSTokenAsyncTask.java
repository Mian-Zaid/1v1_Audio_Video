package com.example.a1v1.HelperClasses;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;


import com.example.a1v1.Interfaces.IceServer;
import com.example.a1v1.Interfaces.NetworkCallback;
import com.example.a1v1.Models.WebRTCModel;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class NTSTokenAsyncTask extends AsyncTask<Void, Void, Void> {

    private static final String TAG = "NTSTokenAsyncTask";
    private String command;
    private NetworkCallback networkCallback;
    private StringBuilder result;
    private HttpURLConnection conn;
    private WebRTCModel example = new WebRTCModel();


    public NTSTokenAsyncTask(Context context, NetworkCallback networkCallback) {
        command = "https://api.confu.info/genNTSToken";
        this.networkCallback = networkCallback;
    }

    @Override
    protected Void doInBackground(Void... voids) {
        result = new StringBuilder();
        URL url = null;
        try {
            url = new URL(command);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = rd.readLine()) != null) {
                result.append(line);
            }
            rd.close();
            JSONObject jObject = new JSONObject(result.toString());
            JSONArray iceServers = jObject.getJSONArray("ice_servers");
            List<IceServer> iceServerList = new ArrayList<>();
            for (int i = 0; i < iceServers.length(); i++) {
                String ur = "";
                String urls = "";
                String username = "";
                String credential = "";
                if (iceServers.getJSONObject(i).has("url")) {
                    ur = iceServers.getJSONObject(i).getString("url");
//                    ur = ur.replace("global", "in1");
                }
                if (iceServers.getJSONObject(i).has("urls")) {
                    urls = iceServers.getJSONObject(i).getString("urls");
//                    urls = urls.replace("global", "in1");
                }
                if (iceServers.getJSONObject(i).has("username")) {
                    username = iceServers.getJSONObject(i).getString("username");
                }
                if (iceServers.getJSONObject(i).has("credential")) {
                    credential = iceServers.getJSONObject(i).getString("credential");
                }
                IceServer iceServer = new IceServer(ur, urls, username, credential);
                iceServerList.add(iceServer);
            }
            String username = jObject.getString("username");
            String password = jObject.getString("password");
            example = new WebRTCModel(username, password, iceServerList);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "THAT DIDN'T work: " + e.toString());
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void unused) {
        super.onPostExecute(unused);
        networkCallback.JsonData(example);
        networkCallback.initializeOutboundCall(example);
    }
}
