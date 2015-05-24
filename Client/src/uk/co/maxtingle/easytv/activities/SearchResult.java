package uk.co.maxtingle.easytv.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import uk.co.maxtingle.easytv.R;
import uk.co.maxtingle.easytv.torrents.Torrent;

public class SearchResult extends Activity
{
    public static String    search;
    public static String[]  searchIn;
    public static Torrent[] results;
    private Torrent[] _torrentsOnCreation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.search_result);
        this._torrentsOnCreation = SearchResult.results;

        String searchForStr = "";
        for (int i = 0; i < SearchResult.searchIn.length; i++) {
            searchForStr += i == 0 ? SearchResult.searchIn[i] : ", " + SearchResult.searchIn[i];
        }

        ((TextView) this.findViewById(R.id.lblSearchFor)).setText("Search result for '" + SearchResult.search + "' in " + searchForStr);

        ListView searchList = (ListView) this.findViewById(R.id.lstSearchResults);
        searchList.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, SearchResult.results));
        searchList.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ViewTorrent.torrentId = SearchResult.this._torrentsOnCreation[position].getId();
                SearchResult.this.startActivity(new Intent(SearchResult.this, ViewTorrent.class));
            }
        });
    }
}