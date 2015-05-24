package uk.co.maxtingle.easytv.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import uk.co.maxtingle.easytv.R;
import uk.co.maxtingle.easytv.torrents.Torrent;

import java.util.Collection;

public class ActiveTorrents extends Activity
{
    private String[] _descriptions;
    private String[] _torrentIds; //array of the description index and the torrent id, need this in-case the array indexes change or something

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.active_torrents);

        ((ListView) this.findViewById(R.id.lstActiveTorrents)).setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ViewTorrent.torrentId = ActiveTorrents.this._torrentIds[position];
                ActiveTorrents.this.startActivity(new Intent(ActiveTorrents.this, ViewTorrent.class));
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        this._buildTorrentDescriptions();
        ((ListView) this.findViewById(R.id.lstActiveTorrents)).setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, this._descriptions));
    }

    private void _buildTorrentDescriptions() {
        //build a different description as we don't care about the score, we want states and such
        Collection<Torrent> torrents = MainActivity.torrents.values();
        this._torrentIds = new String[torrents.size()];
        this._descriptions = new String[torrents.size()];

        for (int i = 0; i < torrents.size(); i++) {
            Torrent torrent = torrents.iterator().next();

            this._torrentIds[i] = torrent.getId();
            this._descriptions[i] = torrent.toDescriptiveString();
        }
    }
}