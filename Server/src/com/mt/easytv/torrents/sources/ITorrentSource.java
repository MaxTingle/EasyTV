package com.mt.easytv.torrents.sources;

import com.mt.easytv.torrents.Torrent;

import java.util.ArrayList;

public interface ITorrentSource
{
    Torrent view(String url) throws Exception;

    ArrayList<Torrent> search(String searchTerms, boolean showProgress) throws Exception;
}