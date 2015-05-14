package com.mt.easytv.activities;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
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
        TextView txtResponseMessage = (TextView) this.findViewById(R.id.lblResponseMessage);
        TextView txtResponseData = (TextView) this.findViewById(R.id.lblResponseData);

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
            String objectDescription = "";

            for (Object object : CommandResponse._response.params) {
                objectDescription += object.toString() + "\n";

                /*Field[] attributes =  object.getClass().getDeclaredFields();
                for(Field field : attributes) {
                    String value;

                    try {
                        value = field.get(object).toString();
                    }
                    catch(IllegalAccessException e) {
                        value = "FAILED_READING: " + e.getMessage();
                    }

                    objectDescription += "  (" + field.getType().getSimpleName() + ") " + field.getName() + ": " + value;
                }*/
            }

            txtResponseData.setText(objectDescription);
        }
    }
}