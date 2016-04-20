package org.intellij.plugins.ceylon.ide.startup;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.ide.plugins.cl.PluginClassLoader;
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.extensions.PluginId;
import com.redhat.ceylon.cmr.api.ArtifactContext;
import com.redhat.ceylon.cmr.api.RepositoryManager;
import com.redhat.ceylon.cmr.api.RepositoryManagerBuilder;
import com.redhat.ceylon.cmr.impl.CMRJULLogger;
import com.redhat.ceylon.cmr.impl.FileContentStore;
import com.redhat.ceylon.cmr.impl.FlatRepository;
import com.redhat.ceylon.cmr.spi.StructureBuilder;
import com.redhat.ceylon.common.Constants;
import com.redhat.ceylon.compiler.java.runtime.metamodel.Metamodel;
import com.redhat.ceylon.model.cmr.ArtifactResult;

import java.io.*;
import java.math.BigInteger;
import java.net.Proxy;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

abstract class AbstractMetamodelEnricher implements ApplicationComponent {

    private static Map<String, String> registeredModules = new HashMap<>();
    private static final Pattern moduleArchivePattern = Pattern.compile("(.+)-([^\\-]+)\\.(j|J|c|C)ar");

    @Override
    public void initComponent() {
        // Creates a repository manager
        IdeaPluginDescriptor pluginDescriptor = getPluginDescriptor(getClass());
        File archiveDirectory = getArchiveDirectory(pluginDescriptor);
        RepositoryManagerBuilder builder = new RepositoryManagerBuilder(archiveDirectory,
                new CMRJULLogger(), true, Constants.DEFAULT_TIMEOUT, Proxy.NO_PROXY);
        configureRepositories(archiveDirectory, builder);
        RepositoryManager repoManager = builder.buildRepository();

        // Registers Ceylon modules in the flat repository `lib`
        for (ArtifactContext searchedModule : getModuleArtifacts()) {
            ArtifactResult result = repoManager.getArtifactResult(searchedModule);
            if (result == null) {
                System.err.println("Ceylon Metamodel Registering failed : module '"
                        + searchedModule.getName() + "' could not be registered for the plugin '"
                        + getClass().getName() + "'");
            } else {
                registerModule(result, getClassLoader());
            }
        }
    }

    protected void configureRepositories(File archiveDirectory, RepositoryManagerBuilder builder) {
        StructureBuilder structureBuilder = new FileContentStore(archiveDirectory);
        FlatRepository flatRepository = new FlatRepository(structureBuilder.createRoot());
        builder.addRepository(flatRepository);
    }

    @Override
    public void disposeComponent() {
        Metamodel.resetModuleManager();
        registeredModules.clear();
    }

    private PluginClassLoader getClassLoader() {
        ClassLoader cl = getClass().getClassLoader();
        if (cl instanceof PluginClassLoader) {
            return (PluginClassLoader) cl;
        }
        return null;
    }

    private IdeaPluginDescriptor getPluginDescriptor(Class<?> startupClass) {
        PluginId pluginId = PluginManager.getPluginByClassName(startupClass.getName());
        return PluginManager.getPlugin(pluginId);
    }

    /**
     * @param pluginDescriptor the plugin descriptor
     * @return the <code>lib</code> folder for this plugin
     */
    private static File getArchiveDirectory(IdeaPluginDescriptor pluginDescriptor) {
        final File pluginDirectory = pluginDescriptor.getPath();
        return new File(pluginDirectory, "lib");
    }

    protected File[] getArchives(IdeaPluginDescriptor pluginDescriptor, FilenameFilter fileNameFilter) {
        final File archiveDirectory = getArchiveDirectory(pluginDescriptor);
        if (archiveDirectory.exists()) {
            return archiveDirectory.listFiles(fileNameFilter);
        }
        return new File[0];
    }

    private ArtifactContext[] getModuleArtifacts() {
        IdeaPluginDescriptor pluginDescriptor = getPluginDescriptor(getClass());
        return getModuleArtifacts(pluginDescriptor);
    }

    private ArtifactContext[] getModuleArtifacts(final IdeaPluginDescriptor pluginDescriptor) {
        final Map<String, ArtifactContext> artifacts = new HashMap<>();
        File[] modulesArchives = getArchives(pluginDescriptor, new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                Matcher matcher = moduleArchivePattern.matcher(name);
                if (!"ceylon-bootstrap.jar".equals(name)
                        && !name.equals(pluginDescriptor.getPath().getName() + ".jar")
                        && matcher.matches()) {
                    String moduleName = matcher.group(1);
                    String moduleVersion = matcher.group(2);
                    String moduleType = matcher.group(3).equalsIgnoreCase("C") ?
                            ArtifactContext.CAR : ArtifactContext.JAR;
                    if (artifacts.containsKey(moduleName)) {
                        throw new RuntimeException("Ceylon Metamodel Registering failed : several versions of the module '" + moduleName + "' are referenced from the plugin '" + getClass().getName() + "'");
                    }

                    artifacts.put(moduleName, new ArtifactContext(moduleName, moduleVersion, moduleType));
                    return true;
                }
                return false;
            }
        });
        return artifacts.values().toArray(new ArtifactContext[modulesArchives.length]);
    }

    static void registerModule(ArtifactResult moduleArtifact, ClassLoader classLoader) {
        String artifactFileName = moduleArtifact.artifact().getName();
        String artifactMD5 = "";
        try {
            artifactMD5 = generateBufferedHash(moduleArtifact.artifact());
        } catch (NoSuchAlgorithmException | IOException e) {
            e.printStackTrace();
        }

        String alreadyRegisteredModuleMD5 = registeredModules.get(artifactFileName);
        if (alreadyRegisteredModuleMD5 == null) {
            Metamodel.loadModule(moduleArtifact.name(), moduleArtifact.version(), moduleArtifact, classLoader);
            registeredModules.put(artifactFileName, artifactMD5);
        } else if (alreadyRegisteredModuleMD5.isEmpty() ||
                !alreadyRegisteredModuleMD5.equals(artifactMD5)) {
            String originMessage = "";
            if (classLoader instanceof PluginClassLoader) {
                originMessage = " of plugin '" + ((PluginClassLoader) classLoader).getPluginId().toString() + "'";
            }
            throw new RuntimeException("Ceylon Metamodel Registering failed : the module '" +
                    moduleArtifact.name() + "/" + moduleArtifact.version() +
                    "'" + originMessage + " cannot be registered since it has already been registered " +
                    "by another plugin with a different binary archive");
        }
        // In other cases, we don't need to register again
    }

    private static String generateBufferedHash(File file)
            throws NoSuchAlgorithmException,
            IOException {

        MessageDigest md = MessageDigest.getInstance("MD5");

        InputStream is = new FileInputStream(file);

        byte[] buffer = new byte[8192];
        int read;

        while ((read = is.read(buffer)) > 0)
            md.update(buffer, 0, read);

        byte[] md5 = md.digest();
        BigInteger bi = new BigInteger(1, md5);

        return bi.toString(16);
    }
}
