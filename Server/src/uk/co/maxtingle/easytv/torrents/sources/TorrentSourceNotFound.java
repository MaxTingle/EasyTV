package uk.co.maxtingle.easytv.torrents.sources;

public class TorrentSourceNotFound extends Exception
{
    private String _source;

    public TorrentSourceNotFound(String source) {
        this._source = _source;
    }

    @Override
    public String getMessage() {
        return "Torrent source '" + this._source + "' not found.";
    }
}