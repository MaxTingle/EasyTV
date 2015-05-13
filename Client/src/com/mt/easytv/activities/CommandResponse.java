package com.mt.easytv.activities;

import android.app.Activity;
import android.os.Bundle;
import android.widget.EditText;
import com.mt.easytv.R;
import uk.co.maxtingle.communication.common.Message;

public class CommandResponse extends Activity
{
    private static Message _response;

    public static void setResponse(Message response) {
        CommandResponse._response = response;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        this.setContentView(R.layout.command_response);
        this._displayResponse();
        super.onCreate(savedInstanceState);
    }

    private void _displayResponse() {
        EditText txtResponseMessage = (EditText) this.findViewById(R.id.txtResponseMessage);
        EditText txtResponseData = (EditText) this.findViewById(R.id.txtResponseData);

        if (CommandResponse._response == null) {
            txtResponseMessage.setText("Null response");
            txtResponseData.setText("");
            return;
        }

        if (CommandResponse._response.request == null) {
            txtResponseMessage.setText("Null message");
        }
        else {
            txtResponseMessage.setText(CommandResponse._response.request);
        }

        if (CommandResponse._response.params == null) {
            txtResponseData.setText("");
        }
        else {
            txtResponseData.setText("there is data"); //TODO: make this display xml encoded data
        }
    }
}