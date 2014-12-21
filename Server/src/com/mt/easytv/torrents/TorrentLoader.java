package com.mt.easytv.torrents;

import com.mt.easytv.torrents.sources.Kickass;
import com.mt.easytv.torrents.sources.PirateBay;
import com.mt.easytv.torrents.sources.PirateBayLocal;
import com.mt.easytv.torrents.sources.TorrentSource;

import java.util.ArrayList;

public final class TorrentLoader
{
    public static ArrayList<Torrent> search(String searchTerms, String[] searchIn) throws Exception
    {
        ArrayList<Torrent> torrents = new ArrayList<>();

        for (String sourceName : searchIn) {
            TorrentSource source = TorrentLoader._siteShortToSite(sourceName);

            if (source == null) {
                throw new Exception("Source " + sourceName + " not found");
            }

            torrents.addAll(source.search(searchTerms));
        }

        return torrents;
    }

    private static TorrentSource _siteShortToSite(String siteShort)
    {
        switch (siteShort.toLowerCase()) {
            case "kickass":
                return new Kickass();
            case "piratebay":
                return new PirateBay();
            case "piratebaylocal":
                return new PirateBayLocal();
        }

        return null;
    }
}