package com.mt.easytv.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import com.mt.easytv.R;
import com.mt.easytv.config.Config;
import uk.co.maxtingle.communication.client.Client;

import java.net.Socket;

public class MainActivity extends Activity
{
    public static Client client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.main);
        this._initialInit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            default:
                return super.onOptionsItemSelected(item);
            case R.id.menu_run_command:
                this.startActivity(new Intent(this, CommandExecutor.class));
                return true;
            case R.id.menu_view_debug:
                this.startActivity(new Intent(this, DebugManager.class));
                return true;
        }
    }

    private void _initialInit() {
        if (MainActivity.client != null) {
            return;
        }

        this._setDefaultConfig();
        this._initClient();
    }

    private void _initClient() {
        try {
            MainActivity.client = new Client(new Socket(Config.getValue("address"), Integer.parseInt(Config.getValue("port"))));
        }
        catch (Exception e) {
            new AlertDialog.Builder(this)
                    .setTitle("Error connecting to server")
                    .setMessage(e.getMessage())
                    .show();
        }
    }

    private void _setDefaultConfig() {
        Config.addDefault("defaultLog", "--EASYTV CLIENT LOG--\n\n\n");
        Config.addDefault("port", "8080");
        Config.addDefault("address", "localhost");
        Config.addDefault("connectionTimeout", 500);
    }
}