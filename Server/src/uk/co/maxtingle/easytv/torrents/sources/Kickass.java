package uk.co.maxtingle.easytv.torrents.sources;

import uk.co.maxtingle.easytv.torrents.Torrent;

import java.util.ArrayList;

public final class Kickass implements ITorrentSource
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
