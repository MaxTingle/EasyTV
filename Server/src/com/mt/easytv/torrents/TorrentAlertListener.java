package com.mt.easytv.torrents;

import com.frostwire.jlibtorrent.AlertListener;
import com.frostwire.jlibtorrent.Logger;
import com.frostwire.jlibtorrent.TorrentAlertAdapter;
import com.frostwire.jlibtorrent.alerts.*;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * This class is a clone of com.frostwire.jlibtorrent.TorrentAlertAdapter
 * with the torrent handler specific removed.
 *
 * @see com.frostwire.jlibtorrent.TorrentAlertAdapter
 */
public class TorrentAlertListener implements AlertListener
{
    private static final Logger                                              LOG        = Logger.getLogger(TorrentAlertAdapter.class);
    private static final Map<String, TorrentAlertListener.CallAlertFunction> CALL_TABLE = buildCallAlertTable();

    private static Map<String, TorrentAlertListener.CallAlertFunction> buildCallAlertTable() {
        HashMap map = new HashMap();
        Method[] var1 = TorrentAlertListener.class.getDeclaredMethods();
        int var2 = var1.length;

        for (int var3 = 0; var3 < var2; ++var3) {
            Method m = var1[var3];
            Class returnType = m.getReturnType();
            Class[] parameterTypes = m.getParameterTypes();
            if (isAlertMethod(returnType, parameterTypes)) {
                try {
                    Class e = parameterTypes[0];
                    TorrentAlertListener.CallAlertFunction function = new TorrentAlertListener.CallAlertFunction(m);
                    map.put(e.getName(), function);
                }
                catch (Throwable var9) {
                    LOG.warn(var9.toString());
                }
            }
        }

        return Collections.unmodifiableMap(map);
    }

    private static boolean isAlertMethod(Class<?> returnType, Class<?>[] parameterTypes) {
        return returnType.equals(Void.TYPE) && parameterTypes.length == 1 && Alert.class.isAssignableFrom(parameterTypes[0]);
    }

    public int[] types() {
        return null;
    }

    public final void alert(Alert<?> alert) {
        if (alert instanceof TorrentAlert) {
            TorrentAlertListener.CallAlertFunction function = CALL_TABLE.get(alert.getClass().getName());
            if (function != null) {
                function.invoke(this, alert);
            }
        }
    }

    public void torrentAdded(TorrentAddedAlert alert) {
    }

    public void torrentFinished(TorrentFinishedAlert alert) {
    }

    public void torrentRemoved(TorrentRemovedAlert alert) {
    }

    public void torrentUpdate(TorrentUpdateAlert alert) {
    }

    public void torrentDeleted(TorrentDeletedAlert alert) {
    }

    public void torrentPaused(TorrentPausedAlert alert) {
    }

    public void torrentResumed(TorrentResumedAlert alert) {
    }

    public void torrentChecked(TorrentCheckedAlert alert) {
    }

    public void torrentNeedCert(TorrentNeedCertAlert alert) {
    }

    public void torrentError(TorrentErrorAlert alert) {
    }

    public void addTorrent(AddTorrentAlert alert) {
    }

    public void blockFinished(BlockFinishedAlert alert) {
    }

    public void metadataReceived(MetadataReceivedAlert alert) {
    }

    public void metadataFailed(MetadataFailedAlert alert) {
    }

    public void saveResumeData(SaveResumeDataAlert alert) {
    }

    public void fastresumeRejected(FastresumeRejectedAlert alert) {
    }

    public void fileCompleted(FileCompletedAlert alert) {
    }

    public void fileRenamed(FileRenamedAlert alert) {
    }

    public void fileRenameFailed(FileRenameFailedAlert alert) {
    }

    public void fileError(FileErrorAlert alert) {
    }

    public void hashFailed(HashFailedAlert alert) {
    }

    public void trackerAnnounce(TrackerAnnounceAlert alert) {
    }

    public void trackerReply(TrackerReplyAlert alert) {
    }

    public void trackerWarning(TrackerWarningAlert alert) {
    }

    public void trackerError(TrackerErrorAlert alert) {
    }

    public void readPiece(ReadPieceAlert alert) {
    }

    public void stateChanged(StateChangedAlert alert) {
    }

    public void dhtReply(DhtReplyAlert alert) {
    }

    public void scrapeReply(ScrapeReplyAlert alert) {
    }

    public void scrapeFailed(ScrapeFailedAlert alert) {
    }

    public void lsdPeer(LsdPeerAlert alert) {
    }

    public void peerBlocked(PeerBlockedAlert alert) {
    }

    public void performance(PerformanceAlert alert) {
    }

    public void pieceFinished(PieceFinishedAlert alert) {
    }

    public void saveResumeDataFailed(SaveResumeDataFailedAlert alert) {
    }

    public void stats(StatsAlert alert) {
    }

    public void storageMoved(StorageMovedAlert alert) {
    }

    public void torrentDeleteFailed(TorrentDeleteFailedAlert alert) {
    }

    public void urlSeed(UrlSeedAlert alert) {
    }

    public void invalidRequest(InvalidRequestAlert alert) {
    }

    public void peerBan(PeerBanAlert alert) {
    }

    public void peerConnect(PeerConnectAlert alert) {
    }

    public void peerDisconnected(PeerDisconnectedAlert alert) {
    }

    public void peerError(PeerErrorAlert alert) {
    }

    public void peerSnubbed(PeerSnubbedAlert alert) {
    }

    public void peerUnsnubbe(PeerUnsnubbedAlert alert) {
    }

    public void requestDropped(RequestDroppedAlert alert) {
    }

    public void anonymousMode(AnonymousModeAlert alert) {
    }

    public void blockDownloading(BlockDownloadingAlert alert) {
    }

    public void blockTimeout(BlockTimeoutAlert alert) {
    }

    public void cacheFlushed(CacheFlushedAlert alert) {
    }

    public void storageMovedFailed(StorageMovedFailedAlert alert) {
    }

    public void trackerid(TrackeridAlert alert) {
    }

    public void unwantedBlock(UnwantedBlockAlert alert) {
    }

    public void torrentPrioritize(TorrentPrioritizeAlert alert) {
    }

    private static final class CallAlertFunction
    {
        private final Method method;

        public CallAlertFunction(Method method) {
            this.method = method;
        }

        public void invoke(TorrentAlertListener adapter, Alert<?> alert) {
            try {
                this.method.invoke(adapter, alert);
            }
            catch (Throwable var4) {
                TorrentAlertListener.LOG.warn(var4.toString());
            }

        }
    }
}
