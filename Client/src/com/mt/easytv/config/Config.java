package com.mt.easytv.config;

import java.util.ArrayList;
import java.util.prefs.Preferences;

public class Config
{
    private static final ArrayList<ConfigItem> _defaults = new ArrayList<>();
    private static Preferences _preferences;

    /**
     * Loads the config variables into memory
     */
    public static void load() {
        Config._preferences = Preferences.userNodeForPackage(Config.class);
    }

    /**
     * Adds a default into the list of default config items
     * Defaults are loaded when the config file doesn't exist
     * or when getValue is called on a key that doesn't exist
     *
     * @param key   The key associated with the config item, used in getValue
     * @param value Any class that implements the method toString
     */
    public static void addDefault(String key, Object value) {
        Config._defaults.add(new ConfigItem(key, value.toString()));
    }

    /**
     * Removes a default from the list of default
     * config items
     *
     * @param key The key associated with the config item
     * @return Whether or not the default was found and removed
     */
    public static boolean removeDefault(String key) {
        for (ConfigItem item : Config._defaults) {
            if (item.key.equals(key)) {
                Config._defaults.remove(item);
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
    public static void loadDefaults(boolean removeExisting) throws Exception {
        if (removeExisting) {
            Config._preferences.clear();
        }

        for (ConfigItem item : Config._defaults) {
            Config._preferences.put(item.key, item.value);
        }
    }

    /**
     * Sets the value of a property, even if it doesn't
     * already exist.
     *
     * @param key   The key associated with the config item, used in getValue
     * @param value Any class that implements the method toString
     */
    public static void setValue(String key, Object value) {
        Config._preferences.put(key, value.toString());
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
    public static String getValue(String key) {
        String value = Config._preferences.get(key, null);

        if (value == null) {
            for (ConfigItem item : Config._defaults) {
                if (key.equals(item.key)) {
                    value = item.value; //set property if in defaults and not already set
                    Config.setValue(key, item.value);
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
    public static String concatValues(String[] keys) {
        return Config.concatValues(keys, "");
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
    public static String concatValues(String[] keys, String delimiter) {
        String fullValue = "";

        for (String key : keys) {
            String value = Config.getValue(key);

            if (value != null) {
                fullValue += (fullValue.equals("") ? "" : delimiter) + value;
            }
        }

        return fullValue;
    }
}