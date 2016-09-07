package org.intellij.plugins.ceylon.ide.runner;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.*;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.CompilerModuleExtension;
import com.intellij.openapi.roots.CompilerProjectExtension;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.openapi.util.io.FileUtil;
import com.redhat.ceylon.common.Backend;
import org.apache.commons.lang.ObjectUtils;
import org.intellij.plugins.ceylon.ide.startup.CeylonIdePlugin;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;

import static org.intellij.plugins.ceylon.ide.ceylonCode.model.findModuleByName_.findModuleByName;

/**
 * Run configuration for Ceylon files.
 */
class CeylonRunConfiguration extends ModuleBasedConfiguration<RunConfigurationModule> {

    public static Backend dartBackend = Backend.registerBackend("Dart", "dart");

    private String ceylonModule;

    /** Full top level name, including the package, including the module */
    private String topLevelNameFull;

    private Backend backend = Backend.Java;

    CeylonRunConfiguration(String name, RunConfigurationModule configurationModule, ConfigurationFactory factory) {
        super(name, configurationModule, factory);
    }

    @Override
    public Collection<Module> getValidModules() {
        Module[] modules = ModuleManager.getInstance(getProject()).getModules();

        return Arrays.asList(modules);
    }

    @NotNull
    @Override
    public SettingsEditor<? extends CeylonRunConfiguration> getConfigurationEditor() {
        return new CeylonRunConfigurationEditor(getProject());
    }

    @Nullable
    @Override
    public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment env) throws ExecutionException {
        // todo: validate

        return new JavaCommandLineState(env) {
            @Override
            protected JavaParameters createJavaParameters() throws ExecutionException {
                Sdk projectJdk = getProjectSdk();
                JavaParameters params = new JavaParameters();
                params.setJdk(projectJdk);
                final String repoDir = CeylonIdePlugin.getEmbeddedCeylonRepository().getAbsolutePath();
                params.getVMParametersList().add("-Dceylon.system.repo=" + repoDir);

                params.setMainClass("com.redhat.ceylon.launcher.Bootstrap");
                params.getClassPath().add(getBootstrapJarPath());
                params.getProgramParametersList().add(
                        backend == Backend.JavaScript ? "run-js" :
                        backend == dartBackend ? "run" :
                        "run");

                // For dart, we can't use run-dart since it won't be available in the
                // embeddedDist. So, use the "runDart" top-level, which works similarly
                // to run-dart. For now, there is a limitation requiring '=' be used
                // for args like --run, --rep, etc.
                if (backend == dartBackend) {
                    // It would be *much* better if we could just call runDart() from the
                    // current process! It's very slow to launch a separate Ceylon/JVM
                    // process here.
                    final String supplementalRepo = CeylonIdePlugin.getSupplementalCeylonRepository().getAbsolutePath();
                    params.getProgramParametersList().add("--rep=" + supplementalRepo); // for ceylon-cli
                    params.getProgramParametersList().add("--run=com.vasileff.ceylon.dart.cli.runDart"); // ~= run-dart
                    params.getProgramParametersList().add("com.vasileff.ceylon.dart.cli/1.3.0-DP3");
                    params.getProgramParametersList().add("--sysrep=" + repoDir); // jvm & js may want this too?
                    params.getProgramParametersList().add("--rep=" + supplementalRepo); // for ceylon-dart runtime modules
                }

                params.getProgramParametersList().add("--run=" + topLevelNameFull);
                final Iterable<String> outputPaths = getOutputPaths(CeylonRunConfiguration.this.getProject());
                if (backend == dartBackend) {
                    params.getProgramParametersList().add("--rep=" +
                            CeylonIdePlugin.getSupplementalCeylonRepository().getAbsolutePath());
                }
                for (String outputPath : outputPaths) {
                    params.getProgramParametersList().add("--rep=" + outputPath);
                }
                params.getProgramParametersList().add(getCeylonModuleOrDft());

                params.setWorkingDirectory(getProject().getBasePath()); // todo: make settable
                return params;
            }

            @NotNull
            private String getBootstrapJarPath() {
                File bootstrapJar = new File(CeylonIdePlugin.getEmbeddedCeylonDist(),
                        FileUtil.join("lib", "ceylon-bootstrap.jar"));
                assert bootstrapJar.exists() && !bootstrapJar.isDirectory();
                return bootstrapJar.getAbsolutePath();
            }
        };
    }

/*
    public static File getPluginDir() {
        return PluginManager.getPlugin(PluginId.getId("org.intellij.plugins.ceylon")).getPath();
    }
*/

    @Override
    public void readExternal(Element element) throws InvalidDataException {
        super.readExternal(element);

        setCeylonModule(element.getAttributeValue("ceylon-module"));
        setTopLevelNameFull(element.getAttributeValue("top-level"));
        setBackend(Backend.fromAnnotation(element.getAttributeValue("backend")));
    }

    @Override
    public void writeExternal(Element element) throws WriteExternalException {
        super.writeExternal(element);

        element.setAttribute("ceylon-module", (String) ObjectUtils.defaultIfNull(getCeylonModule(), ""));
        element.setAttribute("top-level", (String) ObjectUtils.defaultIfNull(getTopLevelNameFull(), ""));
        element.setAttribute("backend", (String) ObjectUtils.defaultIfNull(getBackend().nativeAnnotation, ""));
    }

    private static Iterable<String> getOutputPaths(Project project) {
        final Collection<String> paths = new LinkedHashSet<>();
        final CompilerProjectExtension cpe = CompilerProjectExtension.getInstance(project);
        if (cpe != null && cpe.getCompilerOutput() != null) {
            paths.add(cpe.getCompilerOutput().getCanonicalPath());
        }
        final Module[] modules = ModuleManager.getInstance(project).getModules();
        for (Module module : modules) {
            final CompilerModuleExtension cme = CompilerModuleExtension.getInstance(module);
            if (cme != null&& cme.getCompilerOutputPath() != null) {
                paths.add(cme.getCompilerOutputPath().getCanonicalPath());
            }
        }
        return paths;
    }

    private Sdk getProjectSdk() {
        Module module = getConfigurationModule().getModule();

        if (module != null) {
            return ModuleRootManager.getInstance(module).getSdk();
        }

        return ProjectRootManager.getInstance(getConfigurationModule().getProject()).getProjectSdk();
    }

    void setTopLevelNameFull(String topLevelNameFull) {
        this.topLevelNameFull = topLevelNameFull;
    }

    String getTopLevelNameFull() {
        return topLevelNameFull;
    }

    String getCeylonModule() {
        return ceylonModule;
    }

    private String getCeylonModuleOrDft() {
        String moduleName;
        if (isDefaultModule()) {
            moduleName = "default";
        } else {
            moduleName = ceylonModule;

            if (!moduleName.contains("/")) {
                com.redhat.ceylon.model.typechecker.model.Module mod
                        = findModuleByName(getProject(), ceylonModule);

                if (mod != null) {
                    moduleName += "/" + mod.getVersion();
                }
            }
        }

        return moduleName;
    }

    private boolean isDefaultModule() {
        return ceylonModule == null || ceylonModule.isEmpty();
    }

    void setCeylonModule(String ceylonModule) {
        this.ceylonModule = ceylonModule;
    }

    Backend getBackend() {
        return backend;
    }

    void setBackend(Backend backend) {
        if (backend == null) {
            backend = Backend.Java;
        }
        this.backend = backend;
    }
}
