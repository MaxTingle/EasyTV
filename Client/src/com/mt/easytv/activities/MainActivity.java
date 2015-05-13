package com.mt.easytv.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import com.mt.easytv.R;
import com.mt.easytv.config.Config;
import uk.co.maxtingle.communication.client.Client;
import uk.co.maxtingle.communication.debug.Debugger;
import uk.co.maxtingle.communication.debug.EventLogger;

import java.net.Socket;

public class MainActivity extends Activity
{
    public static Client client;
    private static Thread _clientThread;

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

        /* Setup config */
        Config.addDefault("port", "8080");
        Config.addDefault("address", "192.168.1.113");
        Config.addDefault("connectionTimeout", 500);
        Config.load();

        /* Setup debugger */
        Debugger.setLogger(new EventLogger()
        {
            @Override
            public void log(String category, String message) {
                DebugManager.log("[" + category + "] " + message);
            }
        });

        /* Start client */
        MainActivity._clientThread = new Thread(new Runnable()
        {
            @Override
            public void run() {
                try {
                    MainActivity.client = new Client(new Socket(Config.getValue("address"), Integer.parseInt(Config.getValue("port"))));
                }
                catch (final Exception e) {
                    MainActivity.this.runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run() {
                            new AlertDialog.Builder(MainActivity.this)
                                    .setTitle("Error connecting to server")
                                    .setMessage(e.getMessage())
                                    .setOnDismissListener(new DialogInterface.OnDismissListener()
                                    {
                                        @Override
                                        public void onDismiss(DialogInterface dialog) {
                                            MainActivity.this.finish();
                                        }
                                    }).show();
                        }
                    });
                }
            }
        });
        MainActivity._clientThread.start();
    }
}