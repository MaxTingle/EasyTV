package com.mt.easytv.activities;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import com.mt.easytv.R;
import com.mt.easytv.torrents.Torrent;
import com.mt.easytv.torrents.TorrentState;

public class ViewTorrent extends Activity
{
    public static Torrent torrent;
    private       Torrent _torrentOnCreation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.view_torrent);

        this._torrentOnCreation = ViewTorrent.torrent;
        this._fillFields();
        this._disableButtons();
        this._bindButtons();
    }

    private void _fillFields() {
        ((TextView) this.findViewById(R.id.lblTorrentName)).setText("Viewing torrent: '" + this._torrentOnCreation.getName() + "'");
        ((TextView) this.findViewById(R.id.lblTorrentInfo)).setText(
                "Seeders: " + this._torrentOnCreation.seeders + "\n" +
                "Leeches: " + this._torrentOnCreation.leechers + "\n" +
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

    private void _disableButtons() {
        //TODO: Conditionally disable the buttons based upon the state of the torrent
    }

    private void _bindButtons() {
        this.findViewById(R.id.btnPlayTorrent).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                //TODO: WRITE THIS
            }
        });

        this.findViewById(R.id.btnPlayTorrent).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                //TODO: WRITE THIS
            }
        });
    }
}