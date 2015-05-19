package com.mt.easytv.torrents;

import java.util.Map;

public class Torrent
{
    public int seeders;
    public int leechers;
    public int progress = 0;
    public  TorrentState state;
    private String       _id;
    private String       _url;
    private String       _name;
    private float        _size; //MB
    private SearchScore _score;

    public Torrent(String id, String url, String name, float size, int seeders, int leechers, int progress, TorrentState state, SearchScore score) {
        this._id = id;
        this._url = url;
        this._name = name;
        this._size = size;
        this.seeders = seeders;
        this.leechers = leechers;
        this.progress = progress;
        this.state = state;
        this._score = score;
    }

    public static Torrent[] fromMap(Object[] maps) {
        Torrent[] torrents = new Torrent[maps.length];

        for (int i = 0; i < maps.length; i++) {
            torrents[i] = Torrent.fromMap((Map) maps[i]);
        }

        return torrents;
    }

    public static Torrent fromMap(Map map) {
        return new Torrent((String) map.get("id"), (String) map.get("url"), (String) map.get("name"), ((Number) map.get("size")).floatValue(),
                           ((Number) map.get("seeders")).intValue(), ((Number) map.get("leechers")).intValue(),
                           map.get("progress") == null ? 0 : ((Number) map.get("progress")).intValue(),
                           map.get("state") == null ? null : TorrentState.valueOf((String) map.get("state")), SearchScore.fromMap((Map) map.get("score")));
    }

    public float getSize() {
        return this._size;
    }

    public String getId() {
        return this._id;
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