package com.mt.easytv.torrents;

public class Torrent
{
    public  String          url;
    public  String          name;
    public  int             peers;
    public  int             seeds;
    public  int             leechers;
    public  int             age;
    private TorrentDownload _download;

    public TorrentDownload getDownload()
    {
        if (this._download == null) {
            this._download = new TorrentDownload(this.url);
        }

        return this._download;
    }
}