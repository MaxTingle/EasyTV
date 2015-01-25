package com.mt.easytv.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import com.mt.easytv.R;
import com.mt.easytv.connectivity.Client;

public class MainActivity extends Activity
{
    public static Client client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.main);
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

            case R.id.action_settings:
                this.startActivity(new Intent(this, CommandExecutor.class));
                return true;
        }
    }

    private void _initClient() {
        if (MainActivity.client != null) {
            return;
        }

        MainActivity.client = new Client();
    }
}