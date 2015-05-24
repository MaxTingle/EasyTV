package uk.co.maxtingle.easytv.torrents.sources;

import uk.co.maxtingle.easytv.torrents.Torrent;

import java.util.ArrayList;

public interface ITorrentSource
{
    /**
     * Gets a single torrent from the url/magnet provided
     *
     * @return The torrent or null if it is not found
     */
    Torrent view(String url) throws Exception;

    /**
     * Searches all the torrents and returns all the ones that have a name similiarity
     * score greater than the config threshold
     *
     * @return The search matches
     * @throws Exception The search failed because of a critical error
     */
    ArrayList<Torrent> search(String searchTerms, boolean showProgress) throws Exception;
}