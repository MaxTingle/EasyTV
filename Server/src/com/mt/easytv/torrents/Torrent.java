package com.mt.easytv.torrents;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.mt.easytv.Main;
import com.mt.easytv.interaction.Messager;
import com.sun.istack.internal.Nullable;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class Torrent
{
    private static final List<String> mediaTypes = Arrays.asList("mp4", "wmv", "webm", "avi", "mkv");
    public String id;
    public String url;
    public String name;
    public float  size; //MB
    public int    seeders;
    public int    leechers;
    public SearchScore score;

    @Expose
    @SerializedName("state")
    TorrentState _state = TorrentState.SEARCHED;

    @Expose
    @SerializedName("progress")
    int _percentDownloaded;

    private TorrentDownload _download;

    /**
     * Creates a new instance of a Torrent and sets the score variable
     *
     * @param score The score the torrent got when being searched
     */
    public Torrent(@Nullable SearchScore score) {
        this.score = score;
    }

    /**
     * Loads the state of the torrent based upon its previous files and downloading files
     *
     * @throws Exception Failed checking the downloading status
     */
    public void loadState() throws Exception {
        if (this.getDownload().isDownloaded()) {
            this._setState(TorrentState.DOWNLOADED);
        }
        else if (this.getDownload().isDownloading()) {
            this._setState(this._state = TorrentState.DOWNLOADING);
        }
        else if (this.getDownload().metaExists()) {
            this._setState(TorrentState.DOWNLOADED_META);
        }
        else {
            this._state = TorrentState.LOADED; //clients don't care about loaded
        }
    }

    /**
     * Returns the associated torrent download or intializes it
     *
     * @return The TorrentDownload download
     */
    public TorrentDownload getDownload() {
        if (this._download == null) {
            this._download = new TorrentDownload(this);
        }

        return this._download;
    }

    /**
     * Plays the torrent's video media file
     *
     * @param fileName The specific file in the torrent's files to play, if null will attempt to play the largest video file
     * @throws Exception Torrent not in correct state or failed to play
     */
    public void play(@Nullable String fileName) throws Exception {
        if (this._state == TorrentState.ACTIONED) {
            throw new Exception("Torrent is already playing.");
        }
        else if (this._state != TorrentState.DOWNLOADED) {
            throw new Exception("Torrent is not downloaded yet");
        }
        else if (fileName == null) {
            float largestFileSize = 0;
            String largestFileName = null;

            for (File file : this._download.getFiles()) {
                String name = file.getName();
                String ext = name.substring(name.lastIndexOf(".")).replace(".", "");
                float fileSize = file.length();

                if (Torrent.mediaTypes.contains(ext) && fileSize > largestFileSize) {
                    largestFileName = name;
                    largestFileSize = fileSize;
                }
            }

            if (largestFileName == null) {
                throw new Exception("No media files found to play.");
            }

            fileName = largestFileName;
        }

        if (Main.playingTorrent != null) {
            Main.playingTorrent.pause();
        }

        Main.displayFrame.setVisible(true);
        Main.mediaPlayer.getMediaPlayer().setFullScreen(true);
        Main.mediaPlayer.getMediaPlayer().playMedia(fileName);
        this._setState(TorrentState.ACTIONED);
    }

    /**
     * Pauses the torrent playing if it is playing and updates the state
     */
    public void pause() {
        if (this.getState() != TorrentState.ACTIONED) {
            return; //not playing
        }

        Main.mediaPlayer.getMediaPlayer().stop();
        this._setState(TorrentState.DOWNLOADED);
    }

    /**
     * Gets the torrent's current state
     *
     * @return The torrent state or null if it for some reason hasn't been intialized
     */
    public TorrentState getState() {
        return this._state;
    }

    /**
     * Returns a stripped down version of the torrent's url for use as a unique identifier
     *
     * @return A unique identifier for the torrent or null if it doesn't have a url
     */
    public String getUniqueName() {
        return this.url == null ? null : this.url.replaceAll("[^0-9a-zA-Z-.,;_() ]", "");
    }

    /**
     * Gets a clone of the entire torrent, excluding its ids
     *
     * @return A clone of this torrent
     */
    @Override
    public Torrent clone() {
        Torrent torrent = new Torrent(this.score);
        torrent.url = this.url;
        torrent.name = this.name;
        torrent.seeders = this.seeders;
        torrent.leechers = this.leechers;
        torrent.size = this.size;
        torrent._state = this._state;

        return torrent;
    }

    /**
     * Gets a string representation of the torrent and its download progress if it's downloading
     *
     * @return The torrent description or null if the torrent's state isn't set and failed to load
     */
    public String toString() {
        if (this._state == null) {
            try {
                this.loadState();
            }
            catch (Exception e) {
                Messager.error("Error converting torrent to string ", e);
                return null;
            }
        }

        String str = this.id + ": " + this.name + " (" + this.url + ") size of " + this.size + "MB with " + this.seeders + "/" + this.leechers + "(S/L) ratio. At state: " + this._state;

        if (this._download != null && this._state == TorrentState.DOWNLOADING) {
            str += "\n" + this._download.toString();
        }

        return str;
    }

    void _setState(TorrentState state) {
        this._state = state;
        Messager.informClientsAboutChange(this);
    }
}