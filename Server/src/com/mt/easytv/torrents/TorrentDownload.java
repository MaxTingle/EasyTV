package com.mt.easytv.torrents;

import com.frostwire.jlibtorrent.TorrentAlertAdapter;
import com.frostwire.jlibtorrent.TorrentHandle;
import com.frostwire.jlibtorrent.TorrentInfo;
import com.frostwire.jlibtorrent.Utils;
import com.frostwire.jlibtorrent.alerts.BlockFinishedAlert;
import com.frostwire.jlibtorrent.alerts.TorrentFinishedAlert;
import com.mt.easytv.Helpers;
import com.mt.easytv.Main;
import com.mt.easytv.interaction.Messager;
import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;

/**
 * A wrapper around JLibTorrents torrent downloading system
 * for ease of use and sectioning
 */
public class TorrentDownload
{
    private TorrentInfo    _torrentInfo;
    private TorrentHandle  _torrentHandle;
    private Torrent        _torrent;
    private ProgressAction _progressAction;

    /**
     * Creates a new TorrentDownload wrapper and sets the Torrent it is linked too
     */
    public TorrentDownload(@NotNull Torrent torrent) {
        this._torrent = torrent;
    }

    /**
     * Downloads the torrent meta info and contents without any progress action
     * Method is non-blocking as it simply calls the JLibTorrent C++
     * class and asks it to download the torrent
     *
     * @throws Exception Torrent already being downloaded
     */
    public void download() throws Exception {
        this.download(null);
    }

    /**
     * Downloads the torrent meta info and contents with the specified progress action
     * Method is non-blocking as it simply calls the JLibTorrent C++
     * class and asks it to download the torrent
     *
     * @param progressAction The event handler to call every time the torrent's downloading status updates
     * @throws Exception Torrent already being downloaded
     */
    public void download(@Nullable ProgressAction progressAction) throws Exception {
        if (this.isDownloading()) {
            throw new Exception("Already downloading torrent");
        }

        this._progressAction = progressAction;
        this.downloadMeta();
        this.downloadTorrent();
    }

    /**
     * Downloads the meta info for a torrent if it doesn't already exist
     * This method will not update the torrent status to DOWNLOADED_META
     * if the meta is already downloaded
     * <p>
     * Method is non-blocking as it simply calls the JLibTorrent C++
     * class and asks it to download the torrent
     *
     * @throws Exception Magnet failed to download
     */
    public void downloadMeta() throws Exception {
        if (this.metaExists()) { //.torrent not downloaded, load it via magnet or url
            return;
        }

        this._updateState(TorrentState.DOWNLOADING_META);

        if (this.isMagnet()) { //magnet link uses the dht server to download the info off the DHB servers
            File file = new File(this.getMetaPath());

            byte[] data = Main.torrentDownloader.fetchMagnet(this._torrent.url, Integer.parseInt(Main.config.getValue("magnetTimeout")));

            if (data == null) {
                throw new Exception("Magnet link not found.");
            }

            Utils.writeByteArrayToFile(file, data);
        }
        else if (!this._torrent.url.endsWith(".torrent")) { //download straight from the url
            URL torrentSource = new URL(this._torrent.url);
            ReadableByteChannel byteChannel = Channels.newChannel(torrentSource.openStream());
            FileOutputStream fileOutputStream = new FileOutputStream(this.getMetaPath());
            fileOutputStream.getChannel().transferFrom(byteChannel, 0, Long.MAX_VALUE);

            fileOutputStream.flush();
            fileOutputStream.close();
            byteChannel.close();
        }
        else {
            throw new Exception("Unknown meta data source, failed to download meta data.");
        }

        this._updateState(TorrentState.DOWNLOADED_META);
    }

    /**
     * Downloads all the torrent's files, calls the progress action if
     * it is attached and updates the torrent's progress before telling
     * all attached clients about the torrent's progress update.
     *
     * Will resume from existing download point if there is one.
     *
     * Method is non-blocking as it simply calls the JLibTorrent C++
     * class and asks it to download the torrent
     *
     * @throws Exception Torrent file not found, torrent meta is not downloaded or torrent failed downloading
     */
    public void downloadTorrent() throws Exception {
        if (this._torrent.getState() != TorrentState.DOWNLOADED_META) {
            throw new Exception("Torrent meta must be downloaded to download files.");
        }

        this._updateState(TorrentState.DOWNLOADING);

        /* loading the torrent handle from the file */
        if (this._torrentHandle != null) {
            this.dispose();
        }

        TorrentInfo info = this.getTorrentInfo();
        if (info == null) {
            throw new Exception("Torrent file not found, unable to download torrent.");
        }

        File saveDir = new File(this.getDownloadDir());

        if (!saveDir.exists()) {
            if (!saveDir.mkdirs()) {
                throw new Exception("Failed to make torrent save directory structure");
            }
        }

        File resumeFile = new File(this.getResumePath());

        if (!resumeFile.exists()) {
            /* Make folder structure */
            if (!resumeFile.getParentFile().exists() && !resumeFile.getParentFile().mkdirs()) {
                throw new Exception("Failed to create resume file dir structure.");
            }

            /* Make file */
            if (!resumeFile.createNewFile()) {
                throw new Exception("Failed to create resume file");
            }
        }

        this._torrentHandle = Main.torrentSession.addTorrent(this._torrentInfo, saveDir, null, resumeFile);
        this._torrentHandle.setDownloadLimit(Integer.parseInt(Main.config.getValue("downloadLimit")));
        this._torrentHandle.setUploadLimit(Integer.parseInt(Main.config.getValue("uploadLimit")));

        Main.torrentSession.addListener(new TorrentAlertAdapter(this._torrentHandle) //torrentalertadapter checks the torrent handler for us
        {
            @Override
            public void torrentFinished(TorrentFinishedAlert alert) {
                TorrentDownload.this._updateState(TorrentState.DOWNLOADED);
                TorrentDownload.this._progressAction = null;

                if (Main.config.getValue("seedAfterDownload").equals("false")) {
                    TorrentDownload.this._torrentHandle.pause();
                }
            }

            @Override
            public void blockFinished(BlockFinishedAlert alert) {
                TorrentDownload.this._updateState(Math.round(TorrentDownload.this._torrentHandle.getStatus().getProgress() * 100));
            }
        });

        this._torrentHandle.resume();
    }

    /**
     * Checks if the torrent is currently downloading based
     * upon torrent handle's downloading status according
     * to JLibTorrent
     *
     * @return Whether or not the torrent is downloading
     */
    public boolean isDownloading() {
        return Main.torrentSession.getTorrents().contains(this._torrentHandle) &&
               Main.torrentSession.getTorrents().indexOf(this._torrentHandle) != -1 &&
               !Main.torrentSession.getTorrents().get(Main.torrentSession.getTorrents().indexOf(this._torrentHandle)).getStatus().isPaused();
    }

    /**
     * Checks whether or not the torrent is a magnet based
     * meta info download or not
     *
     * @return whether the torrent is a magnet link or not
     */
    public boolean isMagnet() {
        return this._torrent.url.startsWith("magnet:");
    }

    /**
     * Checks whether the meta info exists or not
     *
     * @return Whether the meta file exists or not
     */
    public boolean metaExists() {
        return (new File(this.getMetaPath())).exists();
    }

    /**
     * Checks whether the torrent and all its files are
     * downloaded or not
     *
     * @return Whether the torrent is completely downloaded
     */
    public boolean isDownloaded() throws Exception {
        /* Use the savedir and the files to expect */
        if (!(new File(this.getDownloadDir())).exists()) {
            return false;
        }

        TorrentInfo info = this.getTorrentInfo();

        if (info == null) {
            return false;
        }
        else if (!this.getSaveDir().exists()) { //won't be null if info isn't null
            return false;
        }

        File[] files = this.getFiles();
        for (int i = 0; i < files.length; i++) {
            File file = files[i];

            if (!file.exists()) {
                return false; //file doesn't exists
            }
            else if (!file.isDirectory() && info.getFiles().getFileSize(i) > file.length()) {
                return false; //file size is less
            }
            else if (file.isDirectory() && info.getFiles().getFileSize(i) > Helpers.getDirSize(file)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Destroys the link to JLibTorrent and pauses all downloads
     */
    public void dispose() {
        if (this._torrentHandle != null) {
            this._torrentHandle.pause();
            this._torrentHandle = null;
        }
        this._torrentInfo = null;
    }

    /**
     * Deletes all the downloaded torrent files, DOES NOT delete downloaded meta info files
     */
    public boolean deleteFiles() throws Exception {
        if (this._torrentInfo == null && this.getTorrentInfo() == null) {
            return false;
        }
        else if (this._torrentHandle != null && (this._torrentHandle.getStatus().isSeeding() || !this._torrentHandle.getStatus().isFinished())
                 && !this._torrentHandle.getStatus().isPaused()
                ) {
            return false;
        }

        File[] torrentFiles = this.getFiles();
        for (File file : torrentFiles) {
            if (file.exists() && !file.delete()) {
                return false;
            }
        }

        File saveDir = this.getSaveDir();
        File[] saveDirFiles = saveDir.listFiles();
        if (saveDir.exists() && (saveDirFiles == null || saveDirFiles.length == 0)) {
            if (!saveDir.delete()) {
                return false;
            }
        }

        this._torrent.loadState(); //so it loads back into downloaded_meta or something
        return true;
    }

    /**
     * Pauses the torrent download and saves the resume
     * data for if the torrent needs to resume after the
     * program has shutdown.
     *
     * @return Whether or not the download was successfully stopped
     */
    public boolean stopDownload() {
        if (this._torrentHandle == null) {
            return false;
        }

        this._torrentHandle.pause();
        this._torrentHandle.saveResumeData();
        this._updateState(-1);

        return true;
    }

    /**
     * Gets all the torrent files to be downloaded.
     * Does not include meta files.
     *
     * @throws Exception Failed to get the TorrentInfo
     * @return The torrent files or null if the torrent info failed to load
     */
    public File[] getFiles() throws Exception {
        if (this.getTorrentInfo() == null) {
            return null;
        }

        int fileCount = this._torrentInfo.getFiles().geNumFiles();
        File[] files = new File[fileCount];

        for (int i = 0; i < fileCount; i++) {
            files[i] = new File(this.getDownloadDir() + "\\" + this._torrentInfo.getFileAt(i).getPath());
        }

        return files;
    }

    /**
     * Gets the JLibTorrent TorrentHandle used for downloading the torrent
     *
     * @return The JLibTorrent link or null if it has not been initialized yet
     */
    public TorrentHandle getHandle() {
        return this._torrentHandle;
    }

    /**
     * Gets the JLibTorrent torrent meta information
     *
     * @throws Exception Failed to read the meta info file
     * @return The Torrent's meta info wrapper object or null if the meta info isn't downloaded
     */
    public TorrentInfo getTorrentInfo() throws Exception {
        if (this._torrentInfo == null) {
            Path path = Paths.get(this.getMetaPath());

            if (!path.toFile().exists()) {
                return null;
            }

            this._torrentInfo = TorrentInfo.bdecode(Files.readAllBytes(path));
        }

        return this._torrentInfo;
    }

    /**
     * Gets the directory that the torrent files will be downloaded into
     *
     * @throws Exception The meta info failed to load
     * @return The torrent download directory or null if the meta info isn't downloaded
     */
    public File getSaveDir() throws Exception {
        if (this.getTorrentInfo() == null) {
            return null;
        }

        return new File(this.getDownloadDir() + "\\" + this.getTorrentInfo().getName());
    }

    /**
     * Gets the path where meta data will be stored
     *
     * @return The path to the .torrent file containing the meta data
     */
    public String getMetaPath() {
        return Main.config.concatValues(new String[]{"storage", "torrentCache"}, "\\") + "\\" + this._torrent.getUniqueName() + ".torrent";
    }

    /**
     * Gets the path where all of this torrent will be downloaded into
     *
     * @return The path to the torrent's download directory
     */
    public String getDownloadDir() {
        return Main.config.concatValues(new String[]{"storage", "torrentFiles"}, "\\");
    }

    /**
     * Gets the path to the torrent's resume information used for
     * resuming downloads after the applicant has restarted
     *
     * @return The path to the torrent's resume information file
     */
    public String getResumePath() {
        return Main.config.concatValues(new String[]{"storage", "torrentResume"}, "\\") + "\\" + this._torrent.getUniqueName() + ".resume";
    }

    /**
     * Gets a description of the TorrentDownload's current downloading status
     *
     * @return A detailed description of the torrent download or null if it is not downloaded / downloading
     */
    public String toString() {
        if (this._torrentHandle == null || this._torrentInfo == null) {
            return null;
        }

        String allFiles = "";
        try {
            File[] files = this.getFiles();

            for (File file : files) {
                allFiles += (allFiles.equals("") ? "\n " : " ") + file.getPath().replace(this.getDownloadDir(), "");
            }
        }
        catch (Exception e) {
            allFiles = "Failed to load files: " + e.getMessage();
        }

        return "Created by: " + this._torrentInfo.getCreator() +
               " on " + (new Date((long) this._torrentInfo.getCreationDate() * 1000)).toString() +
               "\nTrackers: " + this._torrentInfo.getTrackers().size() +
               " peers: " + this._torrentHandle.getStatus().getNumPeers() +
               " seeds: " + this._torrentHandle.getStatus().getNumSeeds() +
               "\nDownloaded: " + Helpers.byteToMB(this._torrentHandle.getStatus().getTotalDownload()) + "MB" +
               " Downloading at: " + Helpers.byteToMB(this._torrentHandle.getStatus().getDownloadRate()) + "MB/s" +
               " limited to " + Helpers.byteToMB(this._torrentHandle.getDownloadLimit()) +
               "\nUploaded: " + Helpers.byteToMB(this._torrentHandle.getStatus().getTotalUpload()) + "MB" +
               " Uploading at: " + Helpers.byteToMB(this._torrentHandle.getStatus().getUploadRate()) + "MB/s" +
               " limited to " + Helpers.byteToMB(this._torrentHandle.getUploadLimit()) +
               "\nFiles:" + allFiles;
    }

    /**
     * Gets the percent that the torrent is downloaded
     *
     * @return The percent downloaded or -1 if it is not downloading / has failed downloading
     */
    public int getPercentDownloaded() {
        return this._torrent._percentDownloaded;
    }

    private void _updateState(TorrentState state) {
        this._torrent._setState(state);

        if (this._progressAction != null) {
            this._progressAction.onProgress(0);
        }
    }

    private void _updateState(int percentCompleted) {
        if (this._progressAction != null) {
            this._torrent._percentDownloaded = percentCompleted;
            this._progressAction.onProgress(percentCompleted);
            Messager.informClientsAboutChange(this._torrent);
        }
    }
}