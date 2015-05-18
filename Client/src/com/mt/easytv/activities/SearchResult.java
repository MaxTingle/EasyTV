package com.mt.easytv.activities;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import com.mt.easytv.R;
import com.mt.easytv.torrents.Torrent;

public class SearchResult extends Activity
{
    public static String    search;
    public static String[]  searchIn;
    public static Torrent[] results;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.search_result);


        String searchForStr = "";
        for (int i = 0; i < SearchResult.searchIn.length; i++) {
            searchForStr += i == 0 ? SearchResult.searchIn[i] : ", " + SearchResult.searchIn[i];
        }

        ((TextView) this.findViewById(R.id.lblSearchFor)).setText("Search result for '" + SearchResult.search + "' in " + searchForStr);
        ((ListView) this.findViewById(R.id.lstSearchResults)).setAdapter(
                new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, SearchResult.results)
        );
    }
}