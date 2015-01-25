package com.mt.easytv.activities;

import android.app.Activity;
import android.os.Bundle;
import android.widget.EditText;
import com.mt.easytv.R;
import com.mt.easytv.connectivity.ServerMessage;

public class CommandResponse extends Activity
{
    private static ServerMessage _response;

    public static void setResponse(ServerMessage response) {
        CommandResponse._response = response;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.main);
        this._displayResponse();
    }

    private void _displayResponse() {
        EditText txtResponseMessage = (EditText) this.findViewById(R.id.txtResponseMessage);
        EditText txtResponseData = (EditText) this.findViewById(R.id.txtResponseData);

        if (CommandResponse._response == null) {
            txtResponseMessage.setText("Null response");
            txtResponseData.setText("");
            return;
        }

        if (CommandResponse._response.response == null) {
            txtResponseMessage.setText("Null message");
        }
        else {
            txtResponseMessage.setText(CommandResponse._response.response);
        }

        if (CommandResponse._response.responseData == null) {
            txtResponseData.setText("");
        }
        else {
            txtResponseData.setText("there is data"); //TODO: make this display xml encoded data
        }
    }
}