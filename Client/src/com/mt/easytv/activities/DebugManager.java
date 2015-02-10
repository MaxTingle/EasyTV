package com.mt.easytv.activities;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
import com.mt.easytv.R;
import com.mt.easytv.config.Config;

public class DebugManager extends Activity
{
    private static String _log = Config.getValue("defaultLog");

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

        TextView txtDebugLog = (TextView) this.findViewById(R.id.txtDebugLog);
        txtDebugLog.setText(DebugManager._log);
    }
}