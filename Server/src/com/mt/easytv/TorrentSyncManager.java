package com.mt.easytv;

import com.frostwire.jlibtorrent.alerts.*;
import com.mt.easytv.interaction.Messager;
import com.mt.easytv.torrents.TorrentAlertListener;
import uk.co.maxtingle.communication.common.Message;

public class TorrentSyncManager extends TorrentAlertListener
{
    @Override
    public void torrentError(TorrentErrorAlert alert) {
        Messager.message("Torrent error " + alert.getError().message());
        this._messageError(alert.getError().message());
    }

    @Override
    public void metadataFailed(MetadataFailedAlert alert) {
        Messager.message("Meta data failed " + alert.getError().message());
        this._messageError(alert.getError().message());
    }

    @Override
    public void fileRenameFailed(FileRenameFailedAlert alert) {
        Messager.message("File rename error " + alert.getError().message());
        this._messageError(alert.getError().message());
    }

    @Override
    public void fileError(FileErrorAlert alert) {
        Messager.message("File error " + alert.getError().message());
        this._messageError(alert.getError().message());
    }

    @Override
    public void trackerWarning(TrackerWarningAlert alert) {
        Messager.message("Tracking warning " + alert.getMsg());
    }

    @Override
    public void trackerError(TrackerErrorAlert alert) {
        Messager.message("Tracker error " + alert.getMsg() + " " + alert.getError().message());
        this._messageError(alert.getError().message());
    }

    @Override
    public void scrapeFailed(ScrapeFailedAlert alert) {
        Messager.message("Scrape failed " + alert.getMsg());
        this._messageError(alert.getMsg());
    }

    @Override
    public void saveResumeDataFailed(SaveResumeDataFailedAlert alert) {
        Messager.message("Saving resume data failed " + alert.getError().message());
        this._messageError(alert.getError().message());
    }

    @Override
    public void torrentDeleteFailed(TorrentDeleteFailedAlert alert) {
        Messager.message("Failed to delete torrent " + alert.getError().message());
        this._messageError(alert.getError().message());
    }

    @Override
    public void peerError(PeerErrorAlert alert) {
        Messager.message("Peer error " + alert.getError().message());
        this._messageError(alert.getError().message());
    }

    private void _messageError(String error) {
        try {
            Messager.messageAllClients(new Message("__TORRENT_ERROR__", new String[]{error}));
        }
        catch (Exception e) {
            Messager.message("Failed to inform clients about the error '" + error + "', " + e.getMessage());
        }
    }
}