package com.mt.easytv;

import com.frostwire.jlibtorrent.alerts.*;
import com.mt.easytv.interaction.Messager;
import com.mt.easytv.torrents.TorrentAlertListener;

public class TorrentSyncManager extends TorrentAlertListener
{
    @Override
    public void torrentError(TorrentErrorAlert alert) {
        Messager.message("Torrent error " + alert.getError().message());
    }

    @Override
    public void metadataFailed(MetadataFailedAlert alert) {
        Messager.message("Meta data failed " + alert.getError().message());
    }

    @Override
    public void fileRenameFailed(FileRenameFailedAlert alert) {
        Messager.message("File rename error " + alert.getError().message());
    }

    @Override
    public void fileError(FileErrorAlert alert) {
        Messager.message("File error " + alert.getError().message());
    }

    @Override
    public void trackerWarning(TrackerWarningAlert alert) {
        Messager.message("Tracking warning " + alert.getMsg());
    }

    @Override
    public void trackerError(TrackerErrorAlert alert) {
        Messager.message("Tracker error " + alert.getMsg() + " " + alert.getError().message());
    }

    @Override
    public void scrapeFailed(ScrapeFailedAlert alert) {
        Messager.message("Scrape failed " + alert.getMsg());
    }

    @Override
    public void saveResumeDataFailed(SaveResumeDataFailedAlert alert) {
        Messager.message("Saving resume data failed " + alert.getError().message());
    }

    @Override
    public void torrentDeleteFailed(TorrentDeleteFailedAlert alert) {
        Messager.message("Failed to delete torrent " + alert.getError().message());
    }

    @Override
    public void peerError(PeerErrorAlert alert) {
        Messager.message("Peer error " + alert.getError().message());
    }
}