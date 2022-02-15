package com.example.a1v1.Interfaces;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class IceServer {
    @SerializedName("url")
    @Expose
    private String url;
    @SerializedName("urls")
    @Expose
    private String urls;
    @SerializedName("username")
    @Expose
    private String username;
    @SerializedName("credentials")
    @Expose
    private String credentials;

    public IceServer(String url, String urls, String username, String credentials) {
        this.url = url;
        this.urls = urls;
        this.username = username;
        this.credentials = credentials;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUrls() {
        return urls;
    }

    public void setUrls(String urls) {
        this.urls = urls;
    }

    public String getCredentials() {
        return credentials;
    }

    public void setCredentials(String credentials) {
        this.credentials = credentials;
    }
}