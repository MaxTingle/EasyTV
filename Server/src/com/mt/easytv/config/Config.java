package com.mt.easytv.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;

public final class Config
{
    private final Properties            _properties = new Properties();
    private final ArrayList<ConfigItem> _defaults   = new ArrayList<>();
    private String _configPath;

    public Config(String path)
    {
        this._configPath = path;
    }

    public void load() throws Exception
    {
        File propertyFile = new File(this._configPath);
        File propertyDir = new File(propertyFile.getParent());

        if (!propertyDir.exists()) {
            if (!propertyDir.mkdirs()) {
                throw new Exception("Failed to create config file folder structure.");
            }
        }

        if (!propertyFile.exists()) {
            if (!propertyFile.createNewFile()) {
                throw new Exception("Failed to create config file.");
            }

            this.setDefault(false);
            this.save();
        } else if (propertyFile.isDirectory()) {
            throw new Exception("Config file path is a dir (" + propertyFile.getAbsolutePath() + ")");
        }

        FileInputStream stream = new FileInputStream(this._configPath);
        this._properties.load(stream);
        stream.close();
    }

    public void save() throws IOException
    {
        FileOutputStream stream = new FileOutputStream(this._configPath);
        this._properties.store(stream, "Options for the application");
        stream.close();
    }

    public void addDefault(String key, java.io.Serializable value)
    {
        this._defaults.add(new ConfigItem(key, value.toString()));
    }

    public void removeDefault(String key) throws Exception
    {
        for (ConfigItem item : this._defaults) {
            if (item.key.equals(key)) {
                this._defaults.remove(item);
                return;
            }
        }

        throw new Exception("Config item not found.");
    }

    public void setDefault(boolean removeExisting)
    {
        if (removeExisting) {
            this._properties.clear();
        }

        for (ConfigItem item : this._defaults) {
            this._properties.setProperty(item.key, item.value);
        }
    }

    public void setValue(String key, java.io.Serializable value)
    {
        this._properties.setProperty(key, value.toString());
    }

    public String getValue(String key)
    {
        return this._properties.getProperty(key);
    }
}