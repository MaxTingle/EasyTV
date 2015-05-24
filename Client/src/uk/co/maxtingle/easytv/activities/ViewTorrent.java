package uk.co.maxtingle.easytv.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import uk.co.maxtingle.communication.common.Message;
import uk.co.maxtingle.communication.debug.Debugger;
import uk.co.maxtingle.easytv.CommandArgument;
import uk.co.maxtingle.easytv.R;
import uk.co.maxtingle.easytv.ResponseCallback;
import uk.co.maxtingle.easytv.config.Config;
import uk.co.maxtingle.easytv.torrents.Torrent;
import uk.co.maxtingle.easytv.torrents.TorrentState;

import java.util.Map;

public class ViewTorrent extends Activity
{
    public static String  torrentId;
    private       String  _torrentId;
    private       Torrent _torrent;
    private       Thread  _updateThread;
    private       boolean _updateThreadRunning;
    private boolean _skipUpdateReload = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.view_torrent);

        this._torrentId = ViewTorrent.torrentId;
        this._loadTorrent();
        this._startUpdateThread();
    }

    @Override
    protected void onStop() {
        ViewTorrent.this._stopUpdateThread();
        super.onStop();
    }

    private void _loadTorrent() {
        ViewTorrent.this._skipUpdateReload = true;
        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Progressing");
        progressDialog.setMessage("Getting updated torrent information...");
        progressDialog.setCancelable(false);
        progressDialog.setIndeterminate(true);
        progressDialog.show();

        MainActivity.safeClientRequest(this, new Message("get", new Object[]{
                new CommandArgument("id", this._torrentId)
        }), new ResponseCallback()
        {
            @Override
            public void onResponse(Message reply) throws Exception {
                ViewTorrent.this._torrent = Torrent.fromMap((Map) reply.params[0]);
                MainActivity.torrents.put(ViewTorrent.this._torrent.getId(), ViewTorrent.this._torrent);
                ViewTorrent.this._fillFields();
                ViewTorrent.this._disableButtons();
                ViewTorrent.this._bindButtons();
                progressDialog.dismiss();
            }
        }, new Runnable()
        {
            @Override
            public void run() {
                ViewTorrent.this._stopUpdateThread();
                progressDialog.dismiss();
                ViewTorrent.this.finish();
            }
        });
    }

    private void _fillFields() {
        ((TextView) this.findViewById(R.id.lblTorrentName)).setText("Viewing torrent: '" + this._torrent.getName() + "'");
        ((TextView) this.findViewById(R.id.lblTorrentInfo)).setText(
                "Seeders: " + this._torrent.getSeeders() + "\n" +
                "Leeches: " + this._torrent.getLeechers() + "\n" +
                this._torrent.toDescriptiveString() +
                "\n\n" +
                "Score: " + this._torrent.getScore().getOverallScore() + "\n" +
                "   Relevance Score: " + this._torrent.getScore().getRelevanceScore() + "\n" +
                "   Seeders Score: " + this._torrent.getScore().getSeedersScore() + "\n" +
                "   Ratio Score: " + this._torrent.getScore().getRatioScore() + "\n" +
                "   Ratio: " + this._torrent.getScore().getRatio() + "\n" +
                "   Relevance: " + this._torrent.getScore().getRelevance()
        );
    }

    private void _disableButtons() {
        //reset
        this.findViewById(R.id.btnPlayTorrent).setEnabled(true);
        this.findViewById(R.id.btnDownloadTorrent).setEnabled(true);

        //update
        if (this._torrent.state == TorrentState.ACTIONED) {
            this.findViewById(R.id.btnDownloadTorrent).setEnabled(false);
            ((Button) this.findViewById(R.id.btnDownloadTorrent)).setText("Delete files");
            ((Button) this.findViewById(R.id.btnPlayTorrent)).setText("Pause");
        }
        else if (this._torrent.state == TorrentState.DOWNLOADED) {
            ((Button) this.findViewById(R.id.btnPlayTorrent)).setText("Play");
            ((Button) this.findViewById(R.id.btnDownloadTorrent)).setText("Delete files");
        }
        else if (this._torrent.state == TorrentState.DOWNLOADING || this._torrent.state == TorrentState.DOWNLOADING_META) {
            this.findViewById(R.id.btnPlayTorrent).setEnabled(false);
            ((Button) this.findViewById(R.id.btnDownloadTorrent)).setText("Stop downloading");
        }
        else { //loaded or searched
            this.findViewById(R.id.btnPlayTorrent).setEnabled(false);
            ((Button) this.findViewById(R.id.btnDownloadTorrent)).setText("Download torrent");
        }
    }

    private void _bindButtons() {
        this.findViewById(R.id.btnPlayTorrent).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                if (ViewTorrent.this._torrent.state == TorrentState.ACTIONED) {
                    ViewTorrent.this._doRequest("pause", "Paused torrent ");
                }
                else {
                    ViewTorrent.this._doRequest("play", "Started playing torrent ");
                }
            }
        });

        this.findViewById(R.id.btnDownloadTorrent).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                if (ViewTorrent.this._torrent.state == TorrentState.DOWNLOADING_META || ViewTorrent.this._torrent.state == TorrentState.DOWNLOADING) {
                    ViewTorrent.this._doRequest("cancelDownload", "Stopped download torrent ");
                }
                else if (ViewTorrent.this._torrent.state == TorrentState.DOWNLOADED) {
                    ViewTorrent.this._doRequest("delete", "Deleted torrent files for torrent ");
                }
                else {
                    ViewTorrent.this._doRequest("download", "Started downloading torrent ");
                }
            }
        });
    }

    private void _doRequest(String request, final String msgPrefix) {
        ViewTorrent.this._skipUpdateReload = true;
        MainActivity.safeClientRequest(ViewTorrent.this, new Message(request, new Object[]{
                new CommandArgument("id", ViewTorrent.this._torrent.getId())
        }), new ResponseCallback()
        {
            @Override
            public void onResponse(Message reply) throws Exception {
                ViewTorrent.this._loadTorrent();
                new AlertDialog.Builder(ViewTorrent.this)
                        .setTitle("Torrent status update")
                        .setMessage(msgPrefix + ViewTorrent.this._torrent.getName())
                        .show();
            }
        });
    }

    private void _startUpdateThread() {
        if (this._updateThreadRunning) {
            return;
        }

        this._updateThreadRunning = true;
        this._updateThread = new Thread(new Runnable()
        {
            @Override
            public void run() {
                int sleepTime = Integer.parseInt(Config.getValue("viewTorrentUpdateTime"));

                try {
                    Thread.sleep(sleepTime);
                }
                catch (Exception e) {
                    if (ViewTorrent.this._updateThreadRunning) {
                        Debugger.log("App", "Failed to do initial sleep on view torrent: " + e.toString());
                    }
                }

                while (ViewTorrent.this._updateThreadRunning) {
                    try {
                        if (!ViewTorrent.this._skipUpdateReload) {
                            ViewTorrent.this.runOnUiThread(new Runnable()
                            {
                                @Override
                                public void run() {
                                    ViewTorrent.this._loadTorrent();
                                }
                            });
                        }

                        ViewTorrent.this._skipUpdateReload = false;
                        Thread.sleep(sleepTime);
                    }
                    catch (Exception e) {
                        if (ViewTorrent.this._updateThreadRunning) {
                            Debugger.log("App", "Failed to sleep on view torrent: " + e.toString());
                        }
                    }
                }
            }
        });
        this._updateThread.setName("Torrent updater");
        this._updateThread.start();
    }

    private void _stopUpdateThread() {
        if (!this._updateThreadRunning) {
            return;
        }

        this._updateThreadRunning = false;
        if (this._updateThread != null) {
            this._updateThread.interrupt();
        }
    }
}