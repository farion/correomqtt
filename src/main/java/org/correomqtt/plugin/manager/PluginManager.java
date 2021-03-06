package org.correomqtt.plugin.manager;

import org.correomqtt.business.provider.ConfigProvider;
import org.correomqtt.plugin.spi.BaseExtensionPoint;
import org.correomqtt.plugin.spi.ExtensionId;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.pf4j.ExtensionFactory;
import org.pf4j.JarPluginManager;
import org.pf4j.ManifestPluginDescriptorFinder;
import org.pf4j.PluginDescriptorFinder;
import org.pf4j.PluginFactory;
import org.pf4j.PluginLoader;
import org.pf4j.PluginState;
import org.pf4j.PluginWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class PluginManager extends JarPluginManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(PluginManager.class);

    private static PluginManager instance;

    private PluginProtocolParser pluginProtocolParser;

    private final HashMap<String, List> extensionsCache = new HashMap<>();
    private final HashMap<String, List<Task>> taskCache = new HashMap<>();

    private PluginManager() {
        // private constructor
        super(Path.of(ConfigProvider.getInstance().getPluginJarPath()));
        try {
            pluginProtocolParser = new PluginProtocolParser();
        } catch (IOException | JDOMException e) {
            LOGGER.error("Cant't parse the protocol, please check the protocol.xml file.");
        }
    }

    @Override
    protected PluginFactory createPluginFactory() {
        return new PermissionPluginFactory();
    }

    @Override
    protected PluginLoader createPluginLoader() {
        // load only jar plugins
        return new PermissionJarPluginLoader(this);
    }

    @Override
    protected PluginDescriptorFinder createPluginDescriptorFinder() {
        // read plugin descriptor from jar's manifest
        return new ManifestPluginDescriptorFinder();
    }

    @Override
    protected ExtensionFactory createExtensionFactory() {
        return new PluginExtensionFactory();
    }

    public static PluginManager getInstance() {
        if (instance == null) {
            instance = new PluginManager();
        }
        return instance;
    }

    public static void resetInstance() {
        instance = new PluginManager();
    }

    public <T> List<T> getConfiguredExtensions(Class<T> type) {
        if (pluginProtocolParser == null) return Collections.emptyList();

        List<ProtocolExtension> declaredExtensionsForClass = pluginProtocolParser.getProtocolExtensions(type);
        if (declaredExtensionsForClass.isEmpty()) {
            return Collections.emptyList();
        } else {
            return createExtensions(type, declaredExtensionsForClass);
        }
    }

    @Override
    public <T> List<T> getExtensions(Class<T> type) {
        if (!extensionsCache.containsKey(type.getSimpleName())) {
            extensionsCache.put(type.getSimpleName(), loadUserDefinedExtensions(type));
        }

        return extensionsCache.get(type.getSimpleName());
    }

    /**
     * plugins can use this method to load other plugins
     *
     * @param type
     * @param root root element inside which plugins may be nested
     * @param <T>
     * @return list of declared extensions or an empty list if none were found
     */
    public <T> List<T> getExtensions(Class<T> type, Element root) {
        if (pluginProtocolParser == null) return super.getExtensions(type);

        List<ProtocolExtension> declaredExtensionsForClass = pluginProtocolParser.getProtocolExtensions(root);
        if (declaredExtensionsForClass.isEmpty()) {
            return Collections.emptyList();
        } else {
            return createExtensions(type, declaredExtensionsForClass);
        }
    }

    private <T> List<T> loadUserDefinedExtensions(Class<T> type) {
        if (pluginProtocolParser == null) return super.getExtensions(type);

        List<ProtocolExtension> declaredExtensionsForClass = pluginProtocolParser.getProtocolExtensions(type);
        if (declaredExtensionsForClass.isEmpty()) {
            return super.getExtensions(type);
        } else {
            return createExtensions(type, declaredExtensionsForClass);
        }
    }

    public <T> List<Task<T>> getTasks(Class<T> type) {
        if (!taskCache.containsKey(type.getSimpleName())) {
            if (pluginProtocolParser == null) {
                taskCache.put(type.getSimpleName(), Collections.emptyList());
            } else {
                List<ProtocolTask> declaredTasks = pluginProtocolParser.getDeclaredTasks(type);
                if (declaredTasks.isEmpty()) {
                    taskCache.put(type.getSimpleName(), Collections.emptyList());
                } else {
                    taskCache.put(type.getSimpleName(), declaredTasks
                            .stream()
                            .map(t -> createTask(type, t))
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList()));
                }
            }
        }

        return taskCache.get(type.getSimpleName()).stream().map(t -> (Task<T>) t).collect(Collectors.toList());
    }

    private <T> Task<T> createTask(Class<T> type, ProtocolTask protocolTask) {
        List<T> extensions = createExtensions(type, protocolTask.getTasks());
        if (extensions.size() == protocolTask.getTasks().size()) {
            return new Task<>(protocolTask.getId(), extensions);
        } else {
            LOGGER.warn("Can't find all declared extensions for task {} in {}", protocolTask.getId(), type.getSimpleName());
            return null;
        }
    }

    private <T> List<T> createExtensions(Class<T> type, List<ProtocolExtension> declaredExtensionsForClass) {
        return declaredExtensionsForClass
                .stream()
                .map(pe -> createExtensionWithConfig(type, pe))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private <T> T createExtensionWithConfig(Class<T> type, ProtocolExtension pe) {
        T baseExtensionPoint = getExtensionById(type, pe.getPluginName(), pe.getExtensionId());
        if (baseExtensionPoint != null) {
            ((BaseExtensionPoint) baseExtensionPoint).onConfigReceived(pe.getPluginConfig());
        }
        return baseExtensionPoint;
    }

    private <T> T getExtensionById(Class<T> type, String pluginId, String extensionId) {
        return super.getExtensions(type, pluginId)
                .stream()
                .filter(e -> isExtensionIdResolved(e, extensionId))
                .findFirst()
                .orElseGet(() -> {
                    logInvalidPluginDeclaration(type, pluginId, extensionId);
                    return null;
                });
    }

    private <T> boolean isExtensionIdResolved(T e, String id) {
        if (e.getClass().isAnnotationPresent(ExtensionId.class)) {
            return e.getClass().getAnnotation(ExtensionId.class).value().equals(id);
        } else return true;
    }

    private <T> void logInvalidPluginDeclaration(Class<T> type, String pluginId, String extensionId) {
        Optional<T> defaultExtension = super.getExtensions(type, pluginId).stream().findFirst();
        if (defaultExtension.isPresent()) {
            if (extensionId == null) {
                LOGGER.info("Plugin {} declared for {} offers multiple valid extensions, please specify an extensionId", pluginId, type.getSimpleName());
            } else {
                LOGGER.info("Plugin {} declared for {} has no extension named: {}", pluginId, type.getSimpleName(), extensionId);
            }
        } else {
            PluginWrapper pluginWrapper = getPlugin(pluginId);

            if (pluginWrapper != null && getPlugin(pluginId).getPluginState().equals(PluginState.STARTED)) {
                LOGGER.warn("Plugin {} declared for {} has no valid extension", pluginId, type.getSimpleName());
            } else {
                LOGGER.warn("Plugin {} declared for {} is not started", pluginId, type.getSimpleName());
            }
        }
    }

    public void unloadPlugins() {
        LOGGER.debug("Unload Plugins");
        List<String> pluginIds = resolvedPlugins.stream().map(PluginWrapper::getPluginId).collect(Collectors.toList());
        for (String pluginId : pluginIds) {
            LOGGER.debug("Unload Plugin \"{}\"", pluginId);
            unloadPlugin(pluginId);
        }

    }
}
