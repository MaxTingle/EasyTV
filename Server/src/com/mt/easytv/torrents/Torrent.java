package com.mt.easytv.torrents;

import com.mt.easytv.Main;
import com.mt.easytv.interaction.Messager;

public class Torrent
{
    public String id;
    public String url;
    public String name;
    public float  size; //MB
    public int    seeders;
    public int    leechers;
    TorrentState _state;
    private TorrentDownload _download;

    public void loadState() throws Exception {
        if (this.getDownload().isDownloaded()) {
            this._state = TorrentState.DOWNLOADED;
        }
        else if (this.getDownload().isDownloading()) {
            this._state = TorrentState.DOWNLOADING;
        }
        else if (this.getDownload().metaExists()) {
            this._state = TorrentState.DOWNLOADED_META;
        }
        else {
            this._state = TorrentState.LOADED;
        }
    }

    public TorrentDownload getDownload() {
        if (this._download == null) {
            this._download = new TorrentDownload(this);
        }

        return this._download;
    }

    public void play(String file) {
        if (Main.playingTorrent != null) {
            Main.playingTorrent.pause();
        }

        Main.displayFrame.setVisible(true);
        Main.mediaPlayer.getMediaPlayer().setFullScreen(true);
        Main.mediaPlayer.getMediaPlayer().playMedia(file);
        this._state = TorrentState.ACTIONED;
    }

    public void pause() {
        Main.mediaPlayer.getMediaPlayer().stop();
        this._state = TorrentState.DOWNLOADED;
    }

    public TorrentState getState() {
        return this._state;
    }

    public String getUniqueName() {
        return this.url == null ? null : this.url.replaceAll("[^0-9a-zA-Z-.,;_() ]", "");
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
}