package com.mt.easytv.torrents.sources;

import com.mt.easytv.torrents.Torrent;

import java.util.ArrayList;

/**
 * Searches the live PirateBay website
 */
public final class PirateBay implements ITorrentSource
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
