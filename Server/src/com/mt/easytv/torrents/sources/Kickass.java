package com.mt.easytv.torrents.sources;

import com.mt.easytv.torrents.Torrent;

import java.util.ArrayList;

public final class Kickass implements TorrentSource
{
    @Override
    public Torrent view(String url) {
        return null;
    }

    @Override
    public ArrayList<Torrent> search(String searchTerms, boolean showProgress) {
        return null;
    }
}
