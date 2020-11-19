package com.alibaba.bytekit.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

/**
 * 
 * @author hengyunabc 2020-07-31
 *
 */
public class PropertiesUtils {

    public static Properties loadNotNull(File path) {

        Properties properties = loadOrNull(path);

        if (properties == null) {
            properties = new Properties();
        }

        return properties;
    }

    public static Properties loadOrNull(File path) {

        try {
            return loadOrNull(path.toURI().toURL().openStream());
        } catch (IOException e) {
            // ignore
        }

        return null;
    }

    public static Properties loadNotNull(URL url) {

        Properties properties = loadOrNull(url);

        if (properties == null) {
            properties = new Properties();
        }

        return properties;
    }

    public static Properties loadOrNull(URL url) {

        try {
            return loadOrNull(url.openStream());
        } catch (IOException e1) {
            // ignore
        }

        return null;
    }

    public static Properties loadNotNull(InputStream inputStream) {
        Properties properties = loadOrNull(inputStream);

        if (properties == null) {
            properties = new Properties();
        }

        return properties;
    }

    public static Properties loadOrNull(InputStream inputStream) {
        try {
            Properties properties = new Properties();
            properties.load(inputStream);
            return properties;
        } catch (Throwable e) {
            // ignore
        } finally {
            IOUtils.close(inputStream);
        }

        return null;
    }
}
