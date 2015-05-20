package com.mt.easytv.torrents;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.mt.easytv.Main;
import com.mt.easytv.interaction.Messager;
import com.sun.istack.internal.NotNull;
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

    public Torrent(@NotNull SearchScore score) {
        this.score = score;
    }

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

    public TorrentDownload getDownload() {
        if (this._download == null) {
            this._download = new TorrentDownload(this);
        }

        return this._download;
    }

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

    public void pause() {
        Main.mediaPlayer.getMediaPlayer().stop();
        this._setState(TorrentState.DOWNLOADED);
    }

    public TorrentState getState() {
        return this._state;
    }

    public String getUniqueName() {
        return this.url == null ? null : this.url.replaceAll("[^0-9a-zA-Z-.,;_() ]", "");
    }

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