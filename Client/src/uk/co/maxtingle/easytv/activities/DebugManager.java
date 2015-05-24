package uk.co.maxtingle.easytv.activities;

import android.app.Activity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.widget.TextView;
import uk.co.maxtingle.easytv.R;
import uk.co.maxtingle.easytv.config.Config;

import java.net.InetAddress;

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
        ((TextView) this.findViewById(R.id.lblDebugLog)).setMovementMethod(new ScrollingMovementMethod());
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

                DebugManager.this.runOnUiThread(new Runnable()
                {
                    @Override
                    public void run() {
                        ((TextView) DebugManager.this.findViewById(R.id.lblConnectionIP)).setText("Server IP Address: " + host);
                        ((TextView) DebugManager.this.findViewById(R.id.lblConnectionPort)).setText("Server Port: " + Config.getValue("port"));
                    }
                });
            }
        }).start();
    }
}