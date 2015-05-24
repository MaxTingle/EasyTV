package uk.co.maxtingle.easytv;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class Helpers
{
    /**
     * Loads a file then gets its dir size
     * Gets the total size of a directory, including sub directories and files
     *
     * @return The size or -1 if the file doesn't exist
     * @throws Exception File is not a directory or failed to get a list of dir files
     */
    public static long getDirSize(String dirPath) throws Exception {
        return Helpers.getDirSize(new File(dirPath));
    }

    /**
     * Gets the total size of a directory, including sub directories and files
     *
     * @return The size or -1 if the file doesn't exist
     * @throws Exception File is not a directory or failed to get a list of dir files
     */
    public static long getDirSize(File dir) throws Exception {
        if (!dir.exists()) {
            return -1;
        }
        else if (dir.isFile()) {
            throw new Exception("getDirSize passed file.");
        }

        File[] dirFiles = dir.listFiles();

        if (dirFiles == null) {
            throw new Exception("listFiles returned null.");
        }

        long dirSize = 0;
        for (File dirFile : dirFiles) {
            if (dirFile.isDirectory()) {
                dirSize += Helpers.getDirSize(dirFile);
            }
            else {
                dirSize += dirFile.length();
            }
        }

        return dirSize;
    }

    /**
     * Calculates the similarity (a number within 0 and 1) between two strings.
     *
     * @link Created by https://stackoverflow.com/users/1850609/acdcjunior
     * @link Reference https://stackoverflow.com/questions/955110/similarity-string-comparison-in-java/16018452#16018452
     */
    public static double similarity(String s1, String s2) {
        String longer = s1, shorter = s2;
        if (s1.length() < s2.length()) { // longer should always have greater length
            longer = s2;
            shorter = s1;
        }
        int longerLength = longer.length();
        if (longerLength == 0) {
            return 1.0; /* both strings are zero length */
        }

        return (longerLength - Helpers.editDistance(longer, shorter)) / (double) longerLength;
    }

    /**
     * Calculates the Levenshtein distance between two strings
     *
     * @link Reference & Created by http://rosettacode.org/wiki/Levenshtein_distance#Java
     */
    public static int editDistance(String s1, String s2) {
        s1 = s1.toLowerCase();
        s2 = s2.toLowerCase();

        int[] costs = new int[s2.length() + 1];
        for (int i = 0; i <= s1.length(); i++) {
            int lastValue = i;
            for (int j = 0; j <= s2.length(); j++) {
                if (i == 0) {
                    costs[j] = j;
                }
                else {
                    if (j > 0) {
                        int newValue = costs[j - 1];
                        if (s1.charAt(i - 1) != s2.charAt(j - 1)) {
                            newValue = Math.min(Math.min(newValue, lastValue),
                                                costs[j]) + 1;
                        }
                        costs[j - 1] = lastValue;
                        lastValue = newValue;
                    }
                }
            }
            if (i > 0) {
                costs[s2.length()] = lastValue;
            }
        }
        return costs[s2.length()];
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

    public static String cleanTorrentName(String torrentName) {
        return torrentName.replaceAll("[_\\.\\-\\|]", " ").replaceAll("( +)", " ").replaceAll("[^a-zA-Z0-9 ]", "");
    }

    public static float byteToMB(int bytes) {
        return Helpers.byteToMB((float) bytes);
    }

    public static float byteToMB(double bytes) {
        return Helpers.byteToMB((float) bytes);
    }

    public static float byteToMB(long bytes) {
        return Helpers.byteToMB((float) bytes);
    }

    public static float byteToMB(float bytes) {
        float converted = (bytes / 1024f) / 1024f;
        return converted < 0 ? 0 : converted;
    }
}
