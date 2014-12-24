package com.mt.easytv.torrents;

public class TorrentDownload
{
    private String _fileName;
    private String _path;
    private String _torrentUrl;

    public TorrentDownload(String torrentPath) {
        this._torrentUrl = torrentPath;
    }

    public void download() {
        this.downloadTorrentFile();
        this.downloadTorrent();
    }

    public void downloadTorrent() {

    }

    public void downloadTorrentFile() {

    }
}
