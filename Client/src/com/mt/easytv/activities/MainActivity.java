package com.mt.easytv.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import com.mt.easytv.CommandArgument;
import com.mt.easytv.R;
import com.mt.easytv.ResponseCallback;
import com.mt.easytv.config.Config;
import com.mt.easytv.torrents.Torrent;
import uk.co.maxtingle.communication.client.Client;
import uk.co.maxtingle.communication.common.BaseClient;
import uk.co.maxtingle.communication.common.Message;
import uk.co.maxtingle.communication.common.events.MessageReceived;
import uk.co.maxtingle.communication.debug.Debugger;
import uk.co.maxtingle.communication.debug.EventLogger;

import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends Activity
{
    public static  HashMap<String, Torrent> torrents;
    public static  Client                   client;
    private static Thread                   _clientThread;

    public static void safeClientRequest(final Activity cls, final Message message, final ResponseCallback callback) {
        MainActivity.safeClientRequest(cls, message, callback, null);
    }

    public static void safeClientRequest(final Activity cls, final Message message, final ResponseCallback callback, final Runnable errorCallback) {
        /* Can't do networking on the main thread */
        new Thread(new Runnable()
        {
            @Override
            public void run() {
                try {
                    /* Catch any message exceptions and show them to the user */
                    message.onReply(new MessageReceived()
                    {
                        @Override
                        public void onMessageReceived(BaseClient baseClient, final Message message) throws Exception {
                            /* Run the callback on ui thread and give it the reply to stop thread usage violations */
                            cls.runOnUiThread(new Runnable()
                            {
                                @Override
                                public void run() {
                                    if (!message.success) {
                                        if (errorCallback != null) {
                                            errorCallback.run();
                                        }

                                        new AlertDialog.Builder(cls)
                                                .setTitle("Request failed")
                                                .setMessage(message.request)
                                                .show();
                                        return;
                                    }
                                    else if (message.params == null) { //for convenience set params to object array if it's not set
                                        message.params = new Object[0];
                                    }

                                    try {
                                        callback.onResponse(message);
                                    }
                                    catch (Exception e) {
                                        if (errorCallback != null) {
                                            errorCallback.run();
                                        }

                                        new AlertDialog.Builder(cls)
                                                .setTitle("Request Failed")
                                                .setMessage(e.getMessage())
                                                .show();
                                    }
                                }
                            });
                        }
                    });
                    MainActivity.client.sendMessage(message);
                }
                catch (final Exception e) {
                    cls.runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run() {
                            if (errorCallback != null) {
                                errorCallback.run();
                            }

                            new AlertDialog.Builder(cls)
                                    .setTitle("Request failed")
                                    .setMessage(e.getMessage())
                                    .show();
                        }
                    });
                }
            }
        }).start();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.main);
        this._initialInit();
        this._bindButtons();
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

    private void _bindButtons() {
        this.findViewById(R.id.btnSearch).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                /* Build the request */
                final ArrayList<String> searchIn = new ArrayList<>();
                final String search = ((EditText) MainActivity.this.findViewById(R.id.txtSearchTerms)).getText().toString();

                if (((CheckBox) MainActivity.this.findViewById(R.id.chkPirateBayLocal)).isChecked()) {
                    searchIn.add("piratebaylocal");
                }
                if (((CheckBox) MainActivity.this.findViewById(R.id.chkPirateBay)).isChecked()) {
                    searchIn.add("piratebay");
                }
                if (((CheckBox) MainActivity.this.findViewById(R.id.chkKickass)).isChecked()) {
                    searchIn.add("kickass");
                }

                /* Searches can take ages, make a dialog to hold them until it's done */
                final ProgressDialog progressDialog = new ProgressDialog(MainActivity.this);
                progressDialog.setTitle("Processing...");
                progressDialog.setMessage("Searching, this could take up to 30 seconds depending upon settings!");
                progressDialog.setCancelable(false);
                progressDialog.setIndeterminate(true);
                progressDialog.show();

                MainActivity.safeClientRequest(MainActivity.this, new Message("search", new CommandArgument[]{
                        new CommandArgument("search", search),
                        new CommandArgument("searchIn", searchIn)
                }), new ResponseCallback()
                {
                    @Override
                    public void onResponse(Message reply) throws Exception {
                        progressDialog.dismiss();

                        /* Show the results screen */
                        SearchResult.searchIn = searchIn.toArray(new String[searchIn.size()]);
                        SearchResult.search = search;
                        SearchResult.results = Torrent.fromMap(reply.params);
                        MainActivity.this.startActivity(new Intent(MainActivity.this, SearchResult.class));
                    }
                }, new Runnable()
                {
                    @Override
                    public void run() {
                        progressDialog.dismiss();
                    }
                });
            }
        });
    }

    private void _initialInit() {
        if (MainActivity.client != null) {
            return;
        }

        /* Setup config */
        Config.addDefault("port", "8080");
        Config.addDefault("address", "192.168.1.151");
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