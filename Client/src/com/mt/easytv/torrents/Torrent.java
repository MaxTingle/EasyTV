package com.mt.easytv.torrents;

public class Torrent
{
    public String id;
    public String url;
    public String name;
    public float  size; //MB
    public int    seeders;
    public int    leechers;
    public int progress = 0;
    TorrentState _state;

    @Override
    public String toString() {
        return this.name;
    }
}