package com.mt.easytv.torrents;

public class Torrent
{
    public  String          url;
    public  String          name;
    public  float           size; //bytes
    public  int             seeders;
    public  int             leechers;
    public  int             age;
    private TorrentDownload _download;

    public TorrentDownload getDownload() {
        if (this._download == null) {
            this._download = new TorrentDownload(this.url);
        }

        return this._download;
    }

    public String toString() {
        return this.name + " (" + this.url + ") size of " + this.size + " with " + this.seeders + "/" + this.leechers + "(S/L) ratio.";
    }
}