package com.example.anu.classicalmusicquizapp.model;

import android.renderscript.Sampler;
import android.util.JsonReader;

import java.io.IOException;

/**
 * Created by Design on 23-12-2017.
 */

public class Music {

    private int musicId;
    private String musicTitle;
    private String composerName;
    private String uri;
    private String musicArtId;

    public int getMusicId() {
        return musicId;
    }

    public Music(int musicId, String musicTitle, String composerName, String uri, String musicArtId) {
        this.musicId = musicId;
        this.musicTitle = musicTitle;
        this.composerName = composerName;
        this.uri = uri;
        this.musicArtId = musicArtId;
    }

    public void setMusicId(int musicId) {
        this.musicId = musicId;
    }

    public String getMusicTitle() {
        return musicTitle;
    }

    public void setMusicTitle(String musicTitle) {
        this.musicTitle = musicTitle;
    }

    public String getComposerName() {
        return composerName;
    }

    public void setComposerName(String composerName) {
        this.composerName = composerName;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getMusicArtId() {
        return musicArtId;
    }

    public void setMusicArtId(String musicArtId) {
        this.musicArtId = musicArtId;
    }

}
