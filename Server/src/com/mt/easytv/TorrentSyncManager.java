package com.mt.easytv;

import com.frostwire.jlibtorrent.alerts.*;
import com.mt.easytv.interaction.Messager;
import com.mt.easytv.torrents.TorrentAlertListener;
import uk.co.maxtingle.communication.debug.Debugger;

public class TorrentSyncManager extends TorrentAlertListener
{
    @Override
    public void torrentError(TorrentErrorAlert alert) {
        Debugger.log("App", "Torrent error " + alert.getError().message());
        Messager.informClientsAboutError(alert.getError().message());
    }

    @Override
    public void metadataFailed(MetadataFailedAlert alert) {
        Debugger.log("App", "Meta data failed " + alert.getError().message());
        Messager.informClientsAboutError(alert.getError().message());
    }

    @Override
    public void fileRenameFailed(FileRenameFailedAlert alert) {
        Debugger.log("App", "File rename error " + alert.getError().message());
        Messager.informClientsAboutError(alert.getError().message());
    }

    @Override
    public void fileError(FileErrorAlert alert) {
        Debugger.log("App", "File error " + alert.getError().message());
        Messager.informClientsAboutError(alert.getError().message());
    }

    @Override
    public void trackerWarning(TrackerWarningAlert alert) {
        Debugger.log("App", "Tracking warning " + alert.getMsg());
    }

    @Override
    public void trackerError(TrackerErrorAlert alert) {
        Debugger.log("App", "Tracker error " + alert.getMsg() + " " + alert.getError().message());
        Messager.informClientsAboutError(alert.getError().message());
    }

    @Override
    public void scrapeFailed(ScrapeFailedAlert alert) {
        Debugger.log("App", "Scrape failed " + alert.getMsg());
        Messager.informClientsAboutError(alert.getMsg());
    }

    @Override
    public void saveResumeDataFailed(SaveResumeDataFailedAlert alert) {
        Debugger.log("App", "Saving resume data failed " + alert.getError().message());
        Messager.informClientsAboutError(alert.getError().message());
    }

    @Override
    public void torrentDeleteFailed(TorrentDeleteFailedAlert alert) {
        Debugger.log("App", "Failed to delete torrent " + alert.getError().message());
        Messager.informClientsAboutError(alert.getError().message());
    }

    @Override
    public void peerError(PeerErrorAlert alert) {
        Debugger.log("App", "Peer error " + alert.getError().message());
        Messager.informClientsAboutError(alert.getError().message());
    }
}