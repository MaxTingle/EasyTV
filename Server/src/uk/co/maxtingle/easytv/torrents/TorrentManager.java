package uk.co.maxtingle.easytv.torrents;

import com.frostwire.jlibtorrent.Session;
import com.frostwire.jlibtorrent.TorrentHandle;
import com.frostwire.jlibtorrent.TorrentStatus;
import com.sun.istack.internal.NotNull;
import uk.co.maxtingle.communication.debug.Debugger;
import uk.co.maxtingle.easytv.Main;
import uk.co.maxtingle.easytv.interaction.Messager;
import uk.co.maxtingle.easytv.torrents.sources.*;

import java.util.*;
import java.util.stream.Collectors;

public final class TorrentManager
{
    private static boolean _searchCleanupThreadRunning;
    private static Thread  _searchCleanupThread;
    private static HashMap<String, ArrayList<Torrent>> _previousSearches = new HashMap<>();
    private        ArrayList<Torrent>                  _torrents         = new ArrayList<>();
    private        int                                 _paginationIndex  = 0;
    private boolean _cleanupThreadRunning;
    private Thread  _cleanupThread;

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
     * Gets a single searched torrent
     *
     * @return The torrent or null if it's not found
     */
    public static Torrent getSearchedTorrent(@NotNull String torrentId) {
        for (ArrayList<Torrent> torrents : TorrentManager._previousSearches.values()) {
            for (Torrent torrent : torrents) {
                if (torrentId.equals(torrent.id)) {
                    return torrent;
                }
            }
        }

        return null;
    }

    /**
     * Searches all the torrent sources found in the searchIn array, without logging progress
     *
     * @return The results which have a title that matches more than the config match threshold
     * @throws Exception             There was an exception while searching
     * @throws TorrentSourceNotFound An invalid torrent source in the searchIn array
     */
    public static ArrayList<Torrent> search(@NotNull String searchTerms, @NotNull String[] searchIn) throws Exception {
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
    public static ArrayList<Torrent> search(@NotNull String searchTerms, @NotNull String[] searchIn, @NotNull boolean showProgress) throws Exception {
        if (TorrentManager._previousSearches.containsKey(searchTerms)) {
            return TorrentManager._previousSearches.get(searchTerms);
        }

        //load all the torrents
        ArrayList<Torrent> torrents = new ArrayList<>();
        for (String sourceName : searchIn) {
            torrents.addAll(TorrentManager._siteShortToSite(sourceName).search(searchTerms, showProgress));
        }

        ArrayList<String> usedIds = new ArrayList<>();
        for (Collection<Torrent> searchedTorrents : TorrentManager._previousSearches.values()) {
            usedIds.addAll(searchedTorrents.stream().map(t -> t.id).collect(Collectors.toList()));
        }

        TorrentManager._setIDs(torrents, usedIds);

        //calculate their scores
        //relevance score
        torrents.sort((Torrent t, Torrent t2) -> t.score.relevance == t2.score.relevance ? 0 : (t.score.relevance > t2.score.relevance ? -1 : 1));
        for (int i = 0; i < torrents.size(); i++) {
            torrents.get(i).score.relevanceScore = i;
        }

        //ratio score
        torrents.sort((Torrent t, Torrent t2) -> t.score.ratio == t2.score.ratio ? 0 : (t.score.ratio > t2.score.ratio && t.score.ratio != 0 ? -1 : 1));
        for (int i = 0; i < torrents.size(); i++) {
            torrents.get(i).score.ratioScore = i;
        }

        //seeders score
        torrents.sort((Torrent t, Torrent t2) -> t.seeders == t2.seeders ? 0 : (t.seeders > t2.seeders ? -1 : 1));
        for (int i = 0; i < torrents.size(); i++) {
            Torrent torrent = torrents.get(i);
            torrent.score.seedersScore = i;
            torrent.score.updateOverallScore();
        }

        torrents.sort((Torrent t, Torrent t2) -> t.score.getOverallScore() == t2.score.getOverallScore() ? 0 : (t.score.getOverallScore() > t2.score.getOverallScore() ? 1 : -1));

        //cache the result
        TorrentManager._previousSearches.put(searchTerms, torrents);
        return torrents;
    }

    private static ITorrentSource _siteShortToSite(String siteShort) throws TorrentSourceNotFound {
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

    private static void _setIDs(ArrayList<Torrent> torrents, ArrayList<Torrent> used) throws Exception {
        TorrentManager._setIDs(torrents, used.stream().map(t -> t.id).collect(Collectors.toList()));
    }

    private static void _setIDs(ArrayList<Torrent> torrents, List<String> usedIds) throws Exception {
        /* Generate unique ids */
        Iterator<Torrent> torrentsIterator = torrents.iterator();

        while (torrentsIterator.hasNext()) { //used while because exception gets thrown and don't want own collection
            Torrent torrent = torrentsIterator.next();
            torrent.id = TorrentManager._generateId(usedIds, 0);
            usedIds.add(torrent.id);
        }
    }

    private static String _generateId(List<String> usedIDs, int iterations) throws Exception {
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
        return usedIDs.indexOf(id) == -1 ? id : TorrentManager._generateId(usedIDs, iterations + 1);
    }

    /**
     * Removes all the search history and searched torrents from memory
     *
     */
    public static void clearSearched() {
        TorrentManager._previousSearches.clear();
    }

    /**
     * Starts the cleaning thread to dispose of unused, searched torrents
     *
     * @throws Exception Failed to start the thread
     */
    public static void startSearchCleaner() throws Exception {
        if (TorrentManager._searchCleanupThread != null && TorrentManager._searchCleanupThread.isAlive()) {
            throw new Exception("Cleaner thread already running.");
        }

        TorrentManager._searchCleanupThread = new Thread(() -> {
            TorrentManager._searchCleanupThreadRunning = true;

            try { //skip the first interval
                Thread.sleep(Integer.parseInt(Main.config.getValue("cleanInterval")));
            }
            catch (Exception e) {
                if (TorrentManager._searchCleanupThreadRunning) {
                    Messager.error("Error when doing initial search cleanup thread sleeping", e);
                }
            }

            while (TorrentManager._searchCleanupThreadRunning) {
                try {
                    TorrentManager.clearSearched();
                    Debugger.log("App", "Cleaned up searched torrents");
                    Thread.sleep(Integer.parseInt(Main.config.getValue("cleanInterval")));
                }
                catch (Exception e) {
                    if (TorrentManager._searchCleanupThreadRunning) {
                        Messager.error("Error when sleeping search cleanup thread", e);
                    }
                }
            }
        });
        TorrentManager._searchCleanupThread.setName("Searched torrent cleaner");
        TorrentManager._searchCleanupThread.start();
    }

    /**
     * Safely stops the search cleaner thread
     */
    public static void stopSearchCleaner() {
        TorrentManager._searchCleanupThreadRunning = false;

        if (TorrentManager._searchCleanupThread != null) {
            TorrentManager._searchCleanupThread.interrupt();
        }
    }

    /**
     * Loads in all the torrent instances from a JLibTorrent session
     * Useful for loading in saved states of when the server shutdown
     *
     * @param session The torrent session with torrents to load
     */
    public void loadFromSession(Session session) throws Exception {
        for (TorrentHandle handle : session.getTorrents()) {
            Torrent torrent = new Torrent(new SearchScore(1, 1));
            torrent.url = handle.makeMagnetUri();
            torrent.name = handle.getName();
            torrent.seeders = handle.getStatus().getNumSeeds();
            torrent.leechers = handle.getStatus().getNumPeers() - torrent.seeders;
            torrent.size = handle.getTorrentInfo().getTotalSize();
            torrent._percentDownloaded = Math.round(handle.getStatus().getProgress() * 100);
            torrent._state = handle.getStatus().getState() == TorrentStatus.State.DOWNLOADING_METADATA ? TorrentState.DOWNLOADED_META : TorrentState.DOWNLOADING;
            torrent.getDownload()._torrentHandle = handle;
            torrent.getDownload()._torrentInfo = handle.getTorrentInfo();
            torrent.getDownload()._updateSerailizeInfo();
            torrent.getDownload()._addSessionListener();

            this._torrents.add(torrent);
        }

        TorrentManager._setIDs(this._torrents, new ArrayList<Torrent>());
        Messager.immediateMessage("Loaded " + session.getTorrents().size() + " torrents from a previous session");
    }

    /**
     * Converts the strings into their representive enum values and calls the regular sort method
     */
    public void sort(@NotNull String sortModeStr, @NotNull String sortTypeStr) throws Exception {
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
     * Loads all the unique torrents (URL based uniqueness) into the torrents array, without showing progress
     *
     * @return The number of items loaded
     */
    public int load(@NotNull String searchTerms, @NotNull String[] searchIn) throws Exception {
        return this.load(searchTerms, searchIn, false);
    }

    /**
     * Loads all the unique torrents (URL based uniqueness) into the torrents array, with showing progress as specified
     *
     * @param showProgress Whether or not the write to the Messager when the searcher progresses
     * @return The number of items loaded
     */
    public int load(@NotNull String searchTerms, @NotNull String[] searchIn, @NotNull boolean showProgress) throws Exception {
        return this.loadSearch(searchTerms, searchIn, showProgress).size();
    }

    /**
     * Loads all the unique torrents (URL based uniqueness) into the torrents array, with showing progress as specified
     *
     * @param showProgress Whether or not the write to the Messager when the searcher progresses
     * @return The items matching from the search
     */
    public ArrayList<Torrent> loadSearch(@NotNull String searchTerms, @NotNull String[] searchIn, @NotNull boolean showProgress) throws Exception {
        ArrayList<Torrent> torrents = TorrentManager.search(searchTerms, searchIn, showProgress);

        /* Handle differences for previously loaded torrents */
        for (int i = 0; i < torrents.size(); i++) {
            boolean found = false;
            Torrent torrent = torrents.get(i);

            for (Torrent existingTorrent : this._torrents) {
                if (existingTorrent.url.equals(torrent.url)) {
                    existingTorrent.score = torrent.score; //copy over the score, it's the only thing that shuld be different
                    torrents.set(i, existingTorrent); //make sure we send them the updated torrent
                    found = true;
                    break;
                }
            }

            if (!found) {
                torrent._state = TorrentState.LOADED;
                this._torrents.add(torrent);
            }
        }

        return torrents;
    }

    /**
     * Loads a torrent that was searched statically
     *
     * @param id The id of the searched torrent
     * @throws Exception Searched torrent not found or already loaded
     */
    public void loadSearchedTorrent(@NotNull String id) throws Exception {
        Torrent searchedTorrent = TorrentManager.getSearchedTorrent(id);

        if (searchedTorrent == null) {
            throw new Exception("Searched torrent not found");
        }

        //make sure we don't already have the torrent
        for (Torrent torrent : this._torrents) {
            if (torrent.url.equals(searchedTorrent.url)) {
                throw new Exception("Torrent already loaded");
            }
        }

        //copy the torrent from the searched torrents and re-assign its id to prevent mixups
        Torrent clone = searchedTorrent.clone();
        clone.id = searchedTorrent.id; //shouldn't really be copying over the id as-well but I can't think of an alternative
        //as the client would need to know about the searched id updating to the new loaded id in everything that references the torrent

        //add it to the loaded torrents
        this._torrents.add(clone);
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
    public Torrent get(@NotNull String id) {
        Iterator<Torrent> iterator = this._torrents.iterator();

        while (iterator.hasNext()) {
            Torrent torrent = iterator.next();

            if (torrent.id != null && torrent.id.equals(id)) {
                return torrent;
            }
        }

        return null;
    }

    /**
     * Gets a single torrent based upon its jlibtorrent handle
     *
     * @param handle The jlibtorrent handle
     * @return The found torrent or null when note found
     */
    public Torrent get(@NotNull TorrentHandle handle) {
        for (Torrent torrent : this._torrents) {
            if (torrent.getDownload().getHandle() == handle) {
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
    public Torrent[] get(@NotNull int howMany) {
        return this.get(howMany, 0);
    }

    /**
     * Gets n number of torrents from the internal torrents array, starting from n. Always returns the specified number of torrents
     *
     * @param howMany How many torrents to load, if howMany + startIndex > size, startIndex will be cutdown to match
     * @return An array the size of howMany, populated with the torrents
     */
    public Torrent[] get(@NotNull int howMany, @NotNull int startIndex) {
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
    public Torrent[] next(@NotNull int size) {
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
        for (int i = this._torrents.size() - 1; i != -1; i--) {
            Torrent torrent = this._torrents.get(i);

            if (torrent.getState() == TorrentState.SEARCHED || torrent.getState() == TorrentState.LOADED) {
                torrent.getDownload().dispose();
                Messager.informClientsAboutRemoval(torrent);
                TorrentManager.this._torrents.remove(i);
            }
        }
    }

    /**
     * Starts the cleaning thread to dispose of unused, loaded torrents
     *
     * @throws Exception Failed to start the thread
     */
    public void startCleaner() throws Exception {
        if (this._cleanupThread != null && this._cleanupThread.isAlive()) {
            throw new Exception("Cleaner thread already running.");
        }

        this._cleanupThread = new Thread(() -> {
            TorrentManager.this._cleanupThreadRunning = true;

            try { //skip the first interval
                Thread.sleep(Integer.parseInt(Main.config.getValue("cleanInterval")));
            }
            catch (Exception e) {
                if (TorrentManager.this._cleanupThreadRunning) {
                    Messager.error("Error when doing initial cleanup thread sleeping", e);
                }
            }

            while (TorrentManager.this._cleanupThreadRunning) {
                try {
                    TorrentManager.this.clear();
                    Debugger.log("App", "Cleaned up loaded torrents");
                    Thread.sleep(Integer.parseInt(Main.config.getValue("cleanInterval"))); //doing this every time so it will sleep for updated config time
                }
                catch (Exception e) {
                    if (TorrentManager.this._cleanupThreadRunning) {
                        Messager.error("Error when sleeping cleanup thread", e);
                    }
                }
            }
        });
        this._cleanupThread.setName("Instance torrent cleaner");
        this._cleanupThread.start();
    }

    /**
     * Safely stops the cleaner thread
     */
    public void stopCleaner() {
        this._cleanupThreadRunning = false;

        if (this._cleanupThread != null) {
            this._cleanupThread.interrupt();
        }
    }
}