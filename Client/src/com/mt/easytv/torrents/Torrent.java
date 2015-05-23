package com.mt.easytv.torrents;

import java.util.Map;

public class Torrent
{
    public int progress = 0;
    //torrent info
    public  TorrentState state;
    //download info
    private int          _seeders;
    private int          _currentSeeders;
    private int          _currentPeers;
    private int          _leechers;
    private float        _downloaded; //MB
    private float        _downloadSpeed;
    private float        _uploaded;
    private float        _uploadSpeed;
    private String       _id;
    private String       _url;
    private String       _name;
    private float        _size; //MB
    private String[]     _files;
    private SearchScore  _score;

    public static Torrent[] fromMap(Object[] maps) {
        Torrent[] torrents = new Torrent[maps.length];

        for (int i = 0; i < maps.length; i++) {
            torrents[i] = Torrent.fromMap((Map) maps[i]);
        }

        return torrents;
    }

    public static Torrent fromMap(Map map) {
        Torrent torrent = new Torrent();
        torrent._id = (String) map.get("id");
        torrent._url = (String) map.get("url");
        torrent._name = (String) map.get("name");
        torrent._size = ((Number) map.get("size")).floatValue();
        torrent._seeders = ((Number) map.get("seeders")).intValue();
        torrent._leechers = ((Number) map.get("leechers")).intValue();
        torrent.progress = map.get("progress") == null ? 0 : ((Number) map.get("progress")).intValue();
        torrent.state = map.get("state") == null ? null : TorrentState.valueOf((String) map.get("state"));
        torrent._score = SearchScore.fromMap((Map) map.get("score"));

        //torrent download map
        Map downloadInfo = (Map) map.get("download");
        if (downloadInfo != null) {
            torrent._currentPeers = ((Number) downloadInfo.get("currentPeers")).intValue();
            torrent._currentSeeders = ((Number) downloadInfo.get("currentSeeders")).intValue();
            torrent._downloaded = ((Number) downloadInfo.get("downloaded")).floatValue();
            torrent._downloadSpeed = ((Number) downloadInfo.get("downloadSpeed")).floatValue();
            torrent._uploaded = ((Number) downloadInfo.get("uploaded")).floatValue();
            torrent._uploadSpeed = ((Number) downloadInfo.get("uploadSpeed")).floatValue();

            Object files = downloadInfo.get("files");
            if (files != null) {
                torrent._files = (String[]) files;
            }
        }

        return torrent;
    }

    public float getSize() {
        return this._size;
    }

    public String getId() {
        return this._id;
    }

    public int getSeeders() {
        return this._seeders;
    }

    public int getLeechers() {
        return this._leechers;
    }

    public String getUrl() {
        return this._url;
    }

    public String getName() {
        return this._name;
    }

    public SearchScore getScore() {
        return this._score;
    }

    @Override
    public String toString() {
        return this._name + "\nScore: " + this._score.getOverallScore();
    }
}