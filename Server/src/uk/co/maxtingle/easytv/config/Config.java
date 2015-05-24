package uk.co.maxtingle.easytv.config;

import com.sun.istack.internal.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;

/**
 * This is the config class for the
 * application, it handles saving, loading
 * getting and setting of config files.
 * Data is saved to XML.
 * <p>
 * It is not static, like the messager,
 * as there is the possibility of multiple
 * config files in the future.
 */
public class Config
{
    private final Properties            _properties = new Properties();
    private final ArrayList<ConfigItem> _defaults   = new ArrayList<>();
    private String _configPath;

    /**
     * Creates a new instance of the config
     * and sets the internal config path
     * variable
     *
     * @param path The path to the config xml
     *             file which is we have R/W
     *             too.
     */
    public Config(@NotNull String path) {
        this._configPath = path;
    }

    /**
     * Loads the config variables into memory
     * the config file
     *
     * @throws java.lang.Exception Failed reading the config, failed creating the config file / dirs.
     */
    public void load() throws Exception {
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

            this.loadDefaults(false);
            this.save();
        }
        else if (propertyFile.isDirectory()) {
            throw new Exception("config file path is a dir (" + propertyFile.getAbsolutePath() + ")");
        }

        FileInputStream stream = new FileInputStream(this._configPath);
        this._properties.loadFromXML(stream);
        stream.close();
    }

    /**
     * Saves the current config values
     * to the config file. Including default values
     * if they have been used or if the config file
     * did not exist before this
     *
     * @throws java.io.IOException Failed writing to the config file
     */
    public void save() throws IOException {
        FileOutputStream stream = new FileOutputStream(this._configPath);
        this._properties.storeToXML(stream, "Options for the application");
        stream.close();
    }

    /**
     * Adds a default into the list of default config items
     * Defaults are loaded when the config file doesn't exist
     * or when getValue is called on a key that doesn't exist
     *
     * @param key   The key associated with the config item, used in getValue
     * @param value Any class that implements the method toString
     */
    public void addDefault(String key, @NotNull Object value) {
        this._defaults.add(new ConfigItem(key, value.toString()));
    }

    /**
     * Removes a default from the list of default
     * config items
     *
     * @param key The key associated with the config item
     * @return Whether or not the default was found and removed
     */
    public boolean removeDefault(String key) {
        for (ConfigItem item : this._defaults) {
            if (item.key.equals(key)) {
                this._defaults.remove(item);
                return true;
            }
        }

        return false;
    }

    /**
     * Loads the all the default config items into
     * the list of config properties (Properties are saved).
     *
     * @param removeExisting Whether or not to remove all existing properties
     */
    public void loadDefaults(boolean removeExisting) {
        if (removeExisting) {
            this._properties.clear();
        }

        for (ConfigItem item : this._defaults) {
            this._properties.setProperty(item.key, item.value);
        }
    }

    /**
     * Sets the value of a property, even if it doesn't
     * already exist.
     *
     * @param key   The key associated with the config item, used in getValue
     * @param value Any class that implements the method toString
     */
    public void setValue(String key, @NotNull Object value) {
        this._properties.setProperty(key, value.toString());
    }

    /**
     * Gets the value of a property or its default value if its
     * not set.
     * If it has to load a default value it will set the property
     * value to the default value (Thus will be saved as default value)
     *
     * @param key The key associated with the property
     * @return The value or null if it's not found
     */
    public String getValue(String key) {
        String value = this._properties.getProperty(key);

        if (value == null) {
            for (ConfigItem item : this._defaults) {
                if (key.equals(item.key)) {
                    value = item.value; //set property if in defaults and not already set
                    this.setValue(key, item.value);
                    break;
                }
            }
        }

        return value;
    }

    /**
     * Gets the value of multiple properties
     *
     * @param keys The array of property keys to get the value of
     * @return One value of all the property values
     * combined.
     */
    public String concatValues(String[] keys) {
        return this.concatValues(keys, "");
    }

    /**
     * Gets the value of multiple properties with the
     * specified delimiter in-between the different
     * property items. Delimiter is not added to the end.
     *
     * @param keys The array of property keys to get the value of
     * @return One value of all the property values
     * combined.
     */
    public String concatValues(String[] keys, String delimiter) {
        String fullValue = "";

        for (String key : keys) {
            String value = this.getValue(key);

            if (value != null) {
                fullValue += (fullValue.equals("") ? "" : delimiter) + value;
            }
        }

        return fullValue;
    }
}