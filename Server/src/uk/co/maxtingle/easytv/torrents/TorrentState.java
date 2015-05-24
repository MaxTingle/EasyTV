package uk.co.maxtingle.easytv.torrents;

public enum TorrentState
{
    SEARCHED,
    LOADED,
    DOWNLOADING_META,
    DOWNLOADED_META,
    DOWNLOADING,
    DOWNLOADED,
    ACTIONED
}