package com.mt.easytv.torrents;

import com.mt.easytv.torrents.sources.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Random;

public final class TorrentManager
{
    private ArrayList<Torrent> _torrents        = new ArrayList<>();
    private int                _paginationIndex = 0;

    public enum SortMode
    {
        SEEDERS,
        LEECHERS,
        SIZE
    }

    public enum SortDirection
    {
        ASC,
        DESC
    }

    /**
     * Searches all the torrent sources found in the searchIn array, without logging progress
     *
     * @return The results which have a title that matches more than the config match threshold
     * @throws Exception             There was an exception while searching
     * @throws TorrentSourceNotFound An invalid torrent source in the searchIn array
     */
    public static ArrayList<Torrent> search(String searchTerms, String[] searchIn) throws Exception {
        return TorrentManager.search(searchTerms, searchIn, false);
    }

    /**
     * Searches all the torrent sources found in the searchIn array, with the logging process option
     *
     * @param showProgress Whether or not the write to the Messager when the searcher progresses
     * @return The results which have a title that matches more than the config match threshold
     * @throws Exception             There was an exception while searching
     * @throws TorrentSourceNotFound An invalid torrent source in the searchIn array
     */
    public static ArrayList<Torrent> search(String searchTerms, String[] searchIn, boolean showProgress) throws Exception {
        ArrayList<Torrent> torrents = new ArrayList<>();

        for (String sourceName : searchIn) {
            torrents.addAll(TorrentManager._siteShortToSite(sourceName).search(searchTerms, showProgress));
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

    /**
     * Loads all the unique torrents (URL based uniqueness) into the torrents array, without showing progress
     *
     * @return The number of items loaded
     */
    public int load(String searchTerms, String[] searchIn) throws Exception {
        return this.load(searchTerms, searchIn, false);
    }

    /**
     * Converts the strings into their representive enum values and calls the regular sort method
     */
    public void sort(String sortModeStr, String sortTypeStr) throws Exception {
        SortMode sortMode;
        SortDirection sortDirection;

        switch (sortModeStr.toLowerCase()) {
            default:
                throw new Exception("Invalid sort mode.");
            case "seeders":
                sortMode = SortMode.SEEDERS;
                break;
            case "leechers":
                sortMode = SortMode.LEECHERS;
                break;
            case "size":
                sortMode = SortMode.SIZE;
                break;
        }

        switch (sortTypeStr.toLowerCase()) {
            default:
                throw new Exception("Invalid sort type (Direction)");
            case "asc":
                sortDirection = SortDirection.ASC;
                break;
            case "desc":
                sortDirection = SortDirection.DESC;
                break;
        }

        this.sort(sortMode, sortDirection);
    }

    /**
     * Sorts the current loaded torrents in the order given by the item given
     */
    public void sort(SortMode sortMode, SortDirection sortDirection) {
        Comparator<Torrent> comparator = null;
        int greaterSort = sortDirection == SortDirection.ASC ? 1 : -1;
        int lesserSort = -greaterSort;

        switch (sortMode) {
            case SEEDERS:
                comparator = (Torrent t, Torrent t2) -> t.seeders == t2.seeders ? 0 : (t.seeders > t2.seeders ? greaterSort : lesserSort);
                break;
            case LEECHERS:
                comparator = (Torrent t, Torrent t2) -> t.leechers == t2.leechers ? 0 : (t.leechers > t2.leechers ? greaterSort : lesserSort);
                break;
            case SIZE:
                comparator = (Torrent t, Torrent t2) -> t.size == t2.size ? 0 : (t.size > t2.size ? greaterSort : lesserSort);
                break;
        }

        this._torrents.sort(comparator);
    }

    /**
     * Loads all the unique torrents (URL based uniqueness) into the torrents array, with showing progress as specified
     *
     * @param showProgress Whether or not the write to the Messager when the searcher progresses
     * @return The number of items loaded
     */
    public int load(String searchTerms, String[] searchIn, boolean showProgress) throws Exception {
        ArrayList<Torrent> torrents = TorrentManager.search(searchTerms, searchIn, showProgress);
        this._setIDs(torrents);

        ArrayList<String> urls = new ArrayList<>();
        this._torrents.forEach((Torrent t) -> urls.add(t.url));

        torrents.forEach((Torrent t) -> {
            if (urls.indexOf(t.url) == -1) {
                this._torrents.add(t);
            }
        });

        return torrents.size();
    }

    /**
     * Checks if any torrents are currently loaded
     *
     * @return Whether or not any torrents are loaded (torrents array size > 0)
     */
    public boolean hasTorrents() {
        return this._torrents.size() > 0;
    }

    /**
     * Gets the array list used for pagination and such, by the torrents instance
     *
     * @return All the loaded torrents
     */
    public ArrayList<Torrent> getTorrents() {
        return this._torrents;
    }

    public Torrent[] getDownloadingTorrents() {
        ArrayList<Torrent> downloadingTorrents = new ArrayList<>();

        this._torrents.forEach((Torrent torrent) -> {
            if (torrent.getState() == TorrentState.DOWNLOADING || torrent.getState() == TorrentState.DOWNLOADING_META) {
                downloadingTorrents.add(torrent);
            }
        });

        return downloadingTorrents.toArray(new Torrent[downloadingTorrents.size()]);
    }

    /**
     * Gets a single Torrent from the internal torrents array, based upon id
     *
     * @param id the Torrent.id to search for
     * @return The torrent or null if it's not found
     */
    public Torrent get(String id) {
        Iterator<Torrent> iterator = this._torrents.iterator();

        while (iterator.hasNext()) {
            Torrent torrent = iterator.next();

            if (torrent.id.equals(id)) {
                return torrent;
            }
        }

        return null;
    }

    /**
     * Gets n number of torrents from the internal torrents array, starting from 0
     *
     * @return The array of torrents to the size of howMany
     */
    public Torrent[] get(int howMany) {
        return this.get(howMany, 0);
    }

    /**
     * Gets n number of torrents from the internal torrents array, starting from n. Always returns the specified number of torrents
     *
     * @param howMany How many torrents to load, if howMany + startIndex > size, startIndex will be cutdown to match
     * @return An array the size of howMany, populated with the torrents
     */
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

    /**
     * For pagination. Gets the next amount of torrents from the internal torrents array. Based upon the size you specify and the previously shown torrents.
     * Then increments the paginationIndex if doing so will not push it > the count
     *
     * @return The torrents paginated
     */
    public Torrent[] next(int size) {
        Torrent[] torrents = this.get(size, this._paginationIndex);

        if (this._paginationIndex + size < this._torrents.size()) {
            this._paginationIndex += size;
        }

        return torrents;
    }

    /**
     * Resets the current pagination index, ie: The index of the last shown torrent.
     */
    public void resetPageIndex() {
        this._paginationIndex = 0;
    }

    /**
     * Calls dispose on all torrent downloads, then clears the internal torrents array
     */
    public void clear() {
        this._torrents.forEach((Torrent t) -> t.getDownload().dispose());
        this._torrents.clear();
    }

    private void _setIDs(ArrayList<Torrent> torrents) throws Exception {
        /* Prebuild array of already used ids so not done every loop */
        ArrayList<String> usedIDs = new ArrayList<>();
        this._torrents.forEach((Torrent t) -> usedIDs.add(t.id));

        /* Generate unique ids */
        Iterator<Torrent> torrentsIterator = torrents.iterator();

        while (torrentsIterator.hasNext()) { //used while because exception gets thrown and don't want own collection
            Torrent torrent = torrentsIterator.next();
            torrent.id = this._generateId(usedIDs, 0);
            usedIDs.add(torrent.id);
        }
    }

    private String _generateId(ArrayList<String> usedIDs, int iterations) throws Exception {
        if (iterations > 30) {
            throw new Exception("Max id iteration reached. Probably out of unique IDs");
        }

        char[] chars = "qwertyuiopasdfghjklzxcvbnm1234567890".toCharArray();
        StringBuilder sb = new StringBuilder();
        Random random = new Random();

        for (int i = 0; i < 20; i++) {
            sb.append(chars[random.nextInt(chars.length)]);
        }

        String id = sb.toString();
        return usedIDs.indexOf(id) == -1 ? id : this._generateId(usedIDs, iterations + 1);
    }
}