package com.mt.easytv.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import com.mt.easytv.CommandArgument;
import com.mt.easytv.R;
import com.mt.easytv.ResponseCallback;
import uk.co.maxtingle.communication.common.Message;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class CommandExecutor extends Activity
{
    private ArrayList<Map<String, String>> _arguments;
    private SimpleAdapter                  _argumentsAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        this.setContentView(R.layout.command_executor);
        this._initList();
        this._bind();
        super.onCreate(savedInstanceState);
    }

    public void showCommandList() {
        final ListView listCommands = new ListView(CommandExecutor.this);
        MainActivity.safeClientRequest(this, new Message("getCommands"), new ResponseCallback()
        {
            @Override
            public void onResponse(Message reply) throws Exception {
                final ArrayList<String> commands = new ArrayList<>();

                for (Object command : reply.params) {
                    commands.add((String) command);
                }

                listCommands.setAdapter(new ArrayAdapter<>(CommandExecutor.this, android.R.layout.simple_list_item_1, commands));

                final AlertDialog dialog = new AlertDialog.Builder(CommandExecutor.this)
                        .setTitle("Select command to use")
                        .setView(listCommands)
                        .setNegativeButton("Cancel", null)
                        .show();

                listCommands.setOnItemClickListener(new AdapterView.OnItemClickListener()
                {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        EditText txtCommand = (EditText) findViewById(R.id.txtCommand);
                        txtCommand.setText(commands.get(position));
                        dialog.cancel();
                    }
                });
            }
        });
    }

    public void addArgument() {
        final EditText txtName = new EditText(this);

        new AlertDialog.Builder(this)
                .setTitle("Create new argument")
                .setMessage("Enter the argument name")
                .setView(txtName)
                .setPositiveButton("Create", new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Map<String, String> argument = new HashMap<>();
                        argument.put("Argument", txtName.getText().toString());
                        argument.put("Value", "");
                        CommandExecutor.this._arguments.add(argument);
                        CommandExecutor.this._argumentsAdapter.notifyDataSetChanged();
                        CommandExecutor.this.showEditItem(CommandExecutor.this._arguments.indexOf(argument));
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    public void updateArgument(int index, String newValue) {
        Map<String, String> current = this._arguments.get(index);
        current.put("Value", newValue);
        this._arguments.set(index, current);
    }

    public void deleteArgument(int index) {
        this._arguments.remove(index);
        this._argumentsAdapter.notifyDataSetChanged();
    }

    public void showEditItem(final int index) {
        Map<String, String> current = this._arguments.get(index);
        final EditText txtNewValue = new EditText(this);
        txtNewValue.setText(current.get("Value"));

        new AlertDialog.Builder(this)
                .setTitle("Update argument value")
                .setMessage("Update " + current.get("Argument") + " value")
                .setView(txtNewValue)
                .setPositiveButton("Save", new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        updateArgument(index, txtNewValue.getText().toString());
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    public void showItemMenu(final int index, View view) {
        PopupMenu popup = new PopupMenu(this, view);
        final MenuItem editItem = popup.getMenu().add("Edit");
        final MenuItem deleteItem = popup.getMenu().add("Delete");

        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener()
        {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item == editItem) {
                    showEditItem(index);
                    return true;
                }
                else if (item == deleteItem) {
                    deleteArgument(index);
                    return true;
                }

                return false;
            }
        });

        popup.show();
    }

    public Object[] getCommandArguments() {
        Object[] args = new Object[this._arguments.size()];

        for (Map<String, String> argument : this._arguments) {
            args[this._arguments.indexOf(argument)] = new CommandArgument(argument.get("Argument"), argument.get("Value"));
        }

        return args;
    }

    public void executeCommand() {
        final String command = ((EditText) this.findViewById(R.id.txtCommand)).getText().toString();

        MainActivity.safeClientRequest(this, new Message(command, CommandExecutor.this.getCommandArguments()), new ResponseCallback()
        {
            @Override
            public void onResponse(Message reply) throws Exception {
                CommandResponse.setResponse(reply);
                CommandExecutor.this.startActivity(new Intent(CommandExecutor.this, CommandResponse.class));
            }
        });
    }

    private void _initList() {
        this._arguments = new ArrayList<>();
        this._argumentsAdapter = new SimpleAdapter(this, this._arguments,
                                                   android.R.layout.simple_list_item_2,
                                                   new String[]{"Argument", "Value"},
                                                   new int[]{
                                                           android.R.id.text1,
                                                           android.R.id.text2
                                                   });

        ListView list = (ListView) this.findViewById(R.id.listArguments);
        list.setAdapter(this._argumentsAdapter);
    }

    private void _bind() {
        /* Get the components */
        ListView listArguments = (ListView) this.findViewById(R.id.listArguments);
        ImageButton btnAddArgument = (ImageButton) this.findViewById(R.id.btnAddArgument);
        ImageButton btnLoadCommand = (ImageButton) this.findViewById(R.id.btnLoadCommand);
        Button btnExecute = (Button) this.findViewById(R.id.btnExecute);

        /* Bind handlers */
        btnLoadCommand.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                showCommandList();
            }
        });

        btnAddArgument.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                addArgument();
            }
        });

        listArguments.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                showItemMenu(position, view);
            }
        });

        btnExecute.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                executeCommand();
            }
        });
    }
}