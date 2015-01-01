package com.mt.easytv.torrents;

import com.frostwire.jlibtorrent.TorrentAlertAdapter;
import com.frostwire.jlibtorrent.TorrentHandle;
import com.frostwire.jlibtorrent.TorrentInfo;
import com.frostwire.jlibtorrent.Utils;
import com.frostwire.jlibtorrent.alerts.PieceFinishedAlert;
import com.frostwire.jlibtorrent.alerts.TorrentFinishedAlert;
import com.mt.easytv.Helpers;
import com.mt.easytv.Main;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TorrentDownload
{
    private TorrentInfo    _torrentInfo;
    private TorrentHandle  _torrentHandle;
    private Torrent        _torrent;
    private ProgressAction _progressAction;

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
        this._torrent._state = TorrentState.DOWNLOADING;

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
        this._torrentHandle = Main.torrentSession.addTorrent(this._torrentInfo, saveDir, null, resumeFile);
        this._torrentHandle.setDownloadLimit(Integer.parseInt(Main.config.getValue("downloadLimit")));
        this._torrentHandle.setUploadLimit(Integer.parseInt(Main.config.getValue("uploadLimit")));

        TorrentDownload self = this; //preserve scope, just as bad as javascript. IMPLEMENT DELEGATES GOD DAMNIT.

        Main.torrentSession.addListener(new TorrentAlertAdapter(this._torrentHandle)
        {
            @Override
            public void torrentFinished(TorrentFinishedAlert alert) {
                if (alert.getHandle() == self._torrentHandle) {
                    self._updateState(TorrentState.DOWNLOADED);
                }
            }

            @Override
            public void pieceFinished(PieceFinishedAlert alert) {
                if (alert.getHandle() == self._torrentHandle) {
                    self._updateState((alert.getPieceIndex() * 100) / self._torrentInfo.getNumPieces());
                }
            }
        });

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
        /* If the handle has been setup, use its status */
        if (this.getHandle() != null) {
            return this.getHandle().getStatus().isFinished();
        }

        /* Use the savedir and the files to expect */
        if (!(new File(this.getDownloadDir())).exists()) {
            return false;
        }

        TorrentInfo info = this.getTorrentInfo();
        int totalFiles = info.getFiles().geNumFiles();
        for (int i = 0; i < totalFiles; i++) {
            File file = new File(info.getFiles().getFilePath(i));

            if (!file.exists()) {
                return false; //file doesn't exists
            }
            else if (!file.isDirectory() && info.getFiles().getFileSize(i) > file.length()) {
                return false; //file size is less
            }
            else if (info.getFiles().getFileSize(i) > Helpers.getDirSize(file)) {
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

    public String getMetaPath() {
        return Main.config.concatValues(new String[]{"storage", "torrentCache"}) + this._torrent.url.replaceAll("[^0-9a-zA-Z-.,;_]", "") + ".torrent";
    }

    public String getDownloadDir() {
        return Main.config.concatValues(new String[]{"storage", "torrentFiles"});
    }

    public String getResumePath() {
        return Main.config.concatValues(new String[]{"storage", "torrentResume"}) + this._torrent.url.replaceAll("[^0-9a-zA-Z-.,;_]", "");
    }

    private void _updateState(TorrentState state) {
        this._torrent._state = state;

        if (this._progressAction != null) {
            this._progressAction.onProgress(0);
        }
    }

    private void _updateState(int percentCompleted) {
        if (this._progressAction != null) {
            this._progressAction.onProgress(percentCompleted);
        }
    }
}