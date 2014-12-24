package com.mt.easytv;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Pattern;

public class Helpers
{
    public static double matchPercent(String needle, String haystack) {
        Pattern notAlphanumRegex = Pattern.compile("[^a-zA-Z0-9 \\|]"); //^ is not
        haystack = notAlphanumRegex.matcher(haystack).replaceAll("").toLowerCase();

        String[] needleParts = notAlphanumRegex.matcher(needle).replaceAll("").toLowerCase().split(" ");

        int matches = 0;
        for (String word : needleParts) {
            if (haystack.contains(word)) {
                matches++;
            }
        }

        return matches == 0 ? 0 : (matches / needleParts.length);
    }

    public static String requestPage(String urlPath) throws Exception {
        URL url = new URL(urlPath);
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
