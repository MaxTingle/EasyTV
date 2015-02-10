package com.mt.easytv.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import com.mt.easytv.R;
import com.mt.easytv.config.Config;
import com.mt.easytv.connectivity.Client;

public class MainActivity extends Activity
{
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.main);
        this._setDefaultConfig();
        this._initClient();
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

    private void _initClient() {
        try {
            Client.startup();
        }
        catch (Exception e) {
            new AlertDialog.Builder(this)
                    .setTitle("Error connecting to server")
                    .setMessage(e.getMessage())
                    .show();
        }
    }

    private void _setDefaultConfig() {
        Config.addDefault("defaultLog", "");
        Config.addDefault("port", "8080");
        Config.addDefault("restrictIp", "localhost"); //TODO: Change to null upon publish
        Config.addDefault("connectionTimeout", 500);
    }
}