package com.mt.easytv.torrents;

public enum TorrentState
{
    LOADED,
    DOWNLOADING_META,
    DOWNLOADED_META,
    DOWNLOADING,
    DOWNLOADED,
    ACTIONED
}