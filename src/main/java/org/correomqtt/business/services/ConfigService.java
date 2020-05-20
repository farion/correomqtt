package org.correomqtt.business.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

//TODO check invalid configs

public class ConfigService extends BaseUserFileService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigService.class);

    private static final String PLUGIN_FOLDER = "plugins";
    private static final String PLUGIN_JAR_FOLDER = "jars";
    private static final String PLUGIN_CONFIG_FOLDER = "config";
    private static final String PROTOCOL_XML = "protocol.xml";

    private static ConfigService instance = null;

    public static synchronized ConfigService getInstance() {
        if (instance == null) {
            instance = new ConfigService();
            return instance;
        } else {
            return instance;
        }
    }

    public String getPluginRootPath() {
        String pluginPath = getTargetDirectoryPath() + File.separator + PLUGIN_FOLDER;
        File pluginFolder = new File(pluginPath);
        if (!pluginFolder.exists() && !pluginFolder.mkdir()) return null;
        return pluginPath;
    }

    public String getPluginJarPath() {
        String pluginPath = getPluginRootPath() + File.separator + PLUGIN_JAR_FOLDER;
        File pluginFolder = new File(pluginPath);
        if (!pluginFolder.exists() && !pluginFolder.mkdir()) return null;
        return pluginPath;
    }

    public String getPluginConfigPath(String pluginId) {
        String pluginConfigFolderPath = getPluginRootPath() + File.separator + PLUGIN_CONFIG_FOLDER + File.separator + pluginId;
        File pluginConfigFolder = new File(pluginConfigFolderPath);
        if (!pluginConfigFolder.exists() && !pluginConfigFolder.mkdirs()) return null;
        return pluginConfigFolderPath;
    }

    public String getPluginProtocol() {
        return getPluginRootPath() + File.separator + PROTOCOL_XML;
    }

}
