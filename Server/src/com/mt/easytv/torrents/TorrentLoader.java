package com.mt.easytv.torrents;

import com.mt.easytv.torrents.sources.*;

import java.util.ArrayList;

public final class TorrentLoader
{
    private ArrayList<Torrent> _torrents        = new ArrayList<>();
    private int                _paginationIndex = 0;

    public static ArrayList<Torrent> search(String searchTerms, String[] searchIn) throws Exception {
        return TorrentLoader.search(searchTerms, searchIn, false);
    }

    public static ArrayList<Torrent> search(String searchTerms, String[] searchIn, boolean showProgress) throws Exception {
        ArrayList<Torrent> torrents = new ArrayList<>();

        for (String sourceName : searchIn) {
            torrents.addAll(TorrentLoader._siteShortToSite(sourceName).search(searchTerms, showProgress));
        }

        return torrents;
    }

    private static TorrentSource _siteShortToSite(String siteShort) throws TorrentSourceNotFound {
        switch (siteShort.toLowerCase()) {
            case "kickass":
                return new Kickass();
            case "piratebay":
                return new PirateBay();
            case "piratebaylocal":
                return new PirateBayLocal();
        }

        throw new TorrentSourceNotFound(siteShort);
    }

    public int load(String searchTerms, String[] searchIn) throws Exception {
        return this.load(searchTerms, searchIn, false);
    }

    public int load(String searchTerms, String[] searchIn, boolean showProgress) throws Exception {
        ArrayList<Torrent> torrents = TorrentLoader.search(searchTerms, searchIn, showProgress);
        this._torrents.addAll(torrents);

        return torrents.size();
    }

    public boolean hasTorrents() {
        return this._torrents.size() > 0;
    }

    public ArrayList<Torrent> getTorrents() {
        return this._torrents;
    }

    public Torrent[] get(int howMany) {
        return this.get(howMany, 0);
    }

    public Torrent[] get(int howMany, int startIndex) {
        /* Requesting more than there is */
        int diff = this._torrents.size() - (startIndex + howMany);
        if (diff < 0) {
            startIndex += diff;
        }

        Torrent[] torrents = new Torrent[howMany];

        for (int loaded = 0; loaded < howMany; loaded++) {
            torrents[loaded] = this._torrents.get(startIndex + loaded);
        }

        return torrents;
    }

    public Torrent[] next(int size) {
        Torrent[] torrents = this.get(size, this._paginationIndex);

        if (this._paginationIndex + size < this._torrents.size()) {
            this._paginationIndex += size;
        }

        return torrents;
    }

    public void resetPageIndex() {
        this._paginationIndex = 0;
    }

    public void clear() {
        this._torrents.clear();
    }
}