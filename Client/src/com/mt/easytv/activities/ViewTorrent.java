package com.mt.easytv.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import com.mt.easytv.CommandArgument;
import com.mt.easytv.R;
import com.mt.easytv.ResponseCallback;
import com.mt.easytv.torrents.Torrent;
import com.mt.easytv.torrents.TorrentState;
import uk.co.maxtingle.communication.common.Message;

public class ViewTorrent extends Activity
{
    public static Torrent torrent;
    private       Torrent _torrentOnCreation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.view_torrent);

        this._torrentOnCreation = ViewTorrent.torrent;
        this._resetButtons();
        this._fillFields();
        this._disableButtons();
        this._bindButtons();
    }

    private void _fillFields() {
        ((TextView) this.findViewById(R.id.lblTorrentName)).setText("Viewing torrent: '" + this._torrentOnCreation.getName() + "'");
        ((TextView) this.findViewById(R.id.lblTorrentInfo)).setText(
                "Seeders: " + this._torrentOnCreation.getSeeders() + "\n" +
                "Leeches: " + this._torrentOnCreation.getLeechers() + "\n" +
                "State: " + this._torrentOnCreation.state + "\n" +
                (this._torrentOnCreation.state == TorrentState.DOWNLOADING ? "Download Progress: " + this._torrentOnCreation.progress + "\n" : "") +
                "\n\n" +
                "Score: " + this._torrentOnCreation.getScore().getOverallScore() + "\n" +
                "   Relevance Score: " + this._torrentOnCreation.getScore().getRelevanceScore() + "\n" +
                "   Seeders Score: " + this._torrentOnCreation.getScore().getSeedersScore() + "\n" +
                "   Ratio Score: " + this._torrentOnCreation.getScore().getRatioScore() + "\n" +
                "   Ratio: " + this._torrentOnCreation.getScore().getRatio() + "\n" +
                "   Relevance: " + this._torrentOnCreation.getScore().getRelevance()
        );
    }

    private void _resetButtons() {
        this.findViewById(R.id.btnDownloadTorrent).setEnabled(true);
        this.findViewById(R.id.btnPlayTorrent).setEnabled(true);
    }

    private void _disableButtons() {
        if (this._torrentOnCreation.state != TorrentState.DOWNLOADED) {
            this.findViewById(R.id.btnPlayTorrent).setEnabled(false);
        }

        if (this._torrentOnCreation.state != TorrentState.LOADED && this._torrentOnCreation.state != TorrentState.SEARCHED) {
            this.findViewById(R.id.btnDownloadTorrent).setEnabled(false);
        }
    }

    private void _bindButtons() {
        this.findViewById(R.id.btnPlayTorrent).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                MainActivity.safeClientRequest(ViewTorrent.this, new Message("play", new Object[]{
                        new CommandArgument("id", ViewTorrent.this._torrentOnCreation.getId())
                }), new ResponseCallback()
                {
                    @Override
                    public void onResponse(Message reply) throws Exception {
                        if (ViewTorrent.this._torrentOnCreation.state != TorrentState.SEARCHED) {
                            //already loaded into the system, can update its status immediately
                            Torrent torrent = MainActivity.torrents.get(ViewTorrent.this._torrentOnCreation.getId());

                            if (torrent != null) {
                                torrent.state = TorrentState.ACTIONED;
                            }
                        }

                        ViewTorrent.this._torrentOnCreation.state = TorrentState.ACTIONED;

                        new AlertDialog.Builder(ViewTorrent.this)
                                .setTitle("Torrent status update")
                                .setMessage("Started playing " + ViewTorrent.this._torrentOnCreation.getName())
                                .show();
                    }
                });
            }
        });

        this.findViewById(R.id.btnDownloadTorrent).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                MainActivity.safeClientRequest(ViewTorrent.this, new Message("download", new Object[]{
                        new CommandArgument("id", ViewTorrent.this._torrentOnCreation.getId())
                }), new ResponseCallback()
                {
                    @Override
                    public void onResponse(Message reply) throws Exception {
                        if (ViewTorrent.this._torrentOnCreation.state != TorrentState.SEARCHED) {
                            //already loaded into the system, can update its status immediately
                            Torrent torrent = MainActivity.torrents.get(ViewTorrent.this._torrentOnCreation.getId());

                            if (torrent != null) {
                                torrent.state = TorrentState.DOWNLOADING_META;
                            }
                        }

                        ViewTorrent.this._torrentOnCreation.state = TorrentState.DOWNLOADING_META;

                        new AlertDialog.Builder(ViewTorrent.this)
                                .setTitle("Torrent status update")
                                .setMessage("Started playing " + ViewTorrent.this._torrentOnCreation.getName())
                                .show();
                    }
                });
            }
        });
    }
}