package com.mt.easytv.torrents.sources;

import com.mt.easytv.Main;
import com.mt.easytv.torrents.Torrent;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public abstract class TorrentSource
{
    public abstract Torrent view(String url);

    public abstract ArrayList<Torrent> search(String searchTerms);

    protected String _requestPage(String urlPath) throws Exception
    {
        URL url = new URL(String.format(Main.Config.getValue("torrentSearch"), urlPath));
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        int responseCode = connection.getResponseCode();
        if (responseCode != 200) {
            throw new Exception("Error loading page: " + responseCode + connection.getResponseMessage());
        }

        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String inputLine;
        String response = "";

        while ((inputLine = in.readLine()) != null) {
            response += inputLine;
        }

        in.close();
        return response;
    }
}