package com.mt.easytv.activities;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
import com.mt.easytv.R;

import java.net.InetAddress;
import java.net.InetSocketAddress;

public class DebugManager extends Activity
{
    private static String _log = "";

    public static void log(String message) {
        DebugManager._log += message + "\n\n";
    }

    public static void handleException(Exception e) {
        String stack = "";
        StackTraceElement[] stackTrace = e.getStackTrace();

        for (StackTraceElement stackItem : stackTrace) {
            stack += stackItem.getMethodName() + " in " + stackItem.getClassName() + " @ " + stackItem.getLineNumber();
        }

        DebugManager.log(e.getMessage() + "\n" + stack);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.debugger);
    }

    @Override
    protected void onStart() {
        super.onStart();

        ((TextView) this.findViewById(R.id.lblDebugLog)).setText(DebugManager._log);

        new Thread(new Runnable()
        {
            @Override
            public void run() {
                InetAddress hostAddress = MainActivity.client.getSocket().getInetAddress();
                final String host = hostAddress.getHostAddress() + " ( " + hostAddress.getCanonicalHostName() + " )";
                final int port = ((InetSocketAddress) MainActivity.client.getSocket().getLocalSocketAddress()).getPort();

                DebugManager.this.runOnUiThread(new Runnable()
                {
                    @Override
                    public void run() {
                        ((TextView) DebugManager.this.findViewById(R.id.lblConnectionIP)).setText("Server IP Address: " + host);
                        ((TextView) DebugManager.this.findViewById(R.id.lblConnectionIP)).setText("Server Port: " + port);
                    }
                });
            }
        }).start();
    }
}