package com.mt.easytv.torrents;

import com.frostwire.jlibtorrent.TorrentAlertAdapter;
import com.frostwire.jlibtorrent.TorrentHandle;
import com.frostwire.jlibtorrent.TorrentInfo;
import com.frostwire.jlibtorrent.Utils;
import com.frostwire.jlibtorrent.alerts.BlockFinishedAlert;
import com.frostwire.jlibtorrent.alerts.TorrentFinishedAlert;
import com.mt.easytv.Main;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;

public class TorrentDownload
{
    private TorrentInfo    _torrentInfo;
    private TorrentHandle  _torrentHandle;
    private Torrent        _torrent;
    private ProgressAction _progressAction;
    private int _percentDownloaded;

    public TorrentDownload(Torrent torrent) {
        this._torrent = torrent;
    }

    public void download() throws Exception {
        this.download(null);
    }

    public void download(ProgressAction progressAction) throws Exception {
        if (this.isDownloading()) {
            throw new Exception("Already downloading torrent");
        }

        this._progressAction = progressAction;
        this.downloadMeta();
        this.downloadTorrent();
    }

    public void downloadMeta() throws Exception {
        if (!this.metaExists()) { //.torrent not downloaded, load it via magnet or url
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
        }

        this._updateState(TorrentState.DOWNLOADED_META);
    }

    public void downloadTorrent() throws Exception {
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
        TorrentDownload self = this;

        Main.torrentSession.addListener(new TorrentAlertAdapter(this._torrentHandle) //torrentalertadapter checks the torrent handler for us
        {
            @Override
            public void torrentFinished(TorrentFinishedAlert alert) {
                self._updateState(TorrentState.DOWNLOADED);
                self._progressAction = null;

                if (Main.config.getValue("seedAfterDownload").equals("false")) {
                    self._torrentHandle.pause();
                }
            }

            @Override
            public void blockFinished(BlockFinishedAlert alert) {
                self._updateState(Math.round(self._torrentHandle.getStatus().getProgress() * 100));
            }
        });

        this._torrentHandle.resume();
    }

    public boolean isDownloading() {
        return Main.torrentSession.getTorrents().contains(this._torrentHandle) &&
               Main.torrentSession.getTorrents().indexOf(this._torrentHandle) != -1 &&
               !Main.torrentSession.getTorrents().get(Main.torrentSession.getTorrents().indexOf(this._torrentHandle)).getStatus().isPaused();
    }

    public boolean isMagnet() {
        return this._torrent.url.startsWith("magnet:");
    }

    public boolean metaExists() {
        return (new File(this.getMetaPath())).exists();
    }

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

    public void dispose() {
        if (this._torrentHandle != null) {
            this._torrentHandle.pause();
            this._torrentHandle = null;
        }
        this._torrentInfo = null;
    }

    public boolean deleteFiles() throws Exception {
        if (this._torrentInfo == null && this.getTorrentInfo() == null) {
            return false;
        }
        else if (this._torrentHandle != null && (this._torrentHandle.getStatus().isSeeding() || !this._torrentHandle.getStatus().isFinished()) && !this._torrentHandle.getStatus().isPaused()) {
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

    public TorrentHandle getHandle() {
        return this._torrentHandle;
    }

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

    public File getSaveDir() throws Exception {
        if (this.getTorrentInfo() == null) {
            return null;
        }

        return new File(this.getDownloadDir() + "\\" + this.getTorrentInfo().getName());
    }

    public String getMetaPath() {
        return Main.config.concatValues(new String[]{"storage", "torrentCache"}, "\\") + "\\" + this._torrent.getUniqueName() + ".torrent";
    }

    public String getDownloadDir() {
        return Main.config.concatValues(new String[]{"storage", "torrentFiles"}, "\\");
    }

    public String getResumePath() {
        return Main.config.concatValues(new String[]{"storage", "torrentResume"}, "\\") + "\\" + this._torrent.getUniqueName() + ".resume";
    }

    public String toString() {
        if (this._torrentHandle == null || this._torrentInfo == null) {
            return null;
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
               " limited to " + Helpers.byteToMB(this._torrentHandle.getUploadLimit());
    }

    public int getPercentDownloaded() {
        return this._percentDownloaded;
    }

    private void _updateState(TorrentState state) {
        this._torrent._state = state;

        if (this._progressAction != null) {
            this._progressAction.onProgress(0);
        }
    }

    private void _updateState(int percentCompleted) {
        if (this._progressAction != null) {
            this._percentDownloaded = percentCompleted;
            this._progressAction.onProgress(percentCompleted);
        }
    }
}