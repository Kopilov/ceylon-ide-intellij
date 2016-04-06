package org.intellij.plugins.ceylon.ide.annotator;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.facet.FacetManager;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleComponent;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.psi.PsiElement;
import com.redhat.ceylon.compiler.typechecker.TypeChecker;
import com.redhat.ceylon.compiler.typechecker.context.PhasedUnit;
import com.redhat.ceylon.compiler.typechecker.context.PhasedUnits;
import com.redhat.ceylon.ide.common.model.BaseIdeModelLoader;
import com.redhat.ceylon.ide.common.model.BaseIdeModuleManager;
import com.redhat.ceylon.ide.common.platform.Status;
import com.redhat.ceylon.ide.common.platform.platformUtils_;
import com.redhat.ceylon.ide.common.typechecker.IdePhasedUnit;
import org.intellij.plugins.ceylon.ide.ceylonCode.ITypeCheckerProvider;
import org.intellij.plugins.ceylon.ide.ceylonCode.model.IdeaCeylonProject;
import org.intellij.plugins.ceylon.ide.ceylonCode.model.IdeaCeylonProjects;
import org.intellij.plugins.ceylon.ide.ceylonCode.model.parsing.ProgressIndicatorMonitor;
import org.intellij.plugins.ceylon.ide.ceylonCode.platform.ideaPlatformServices_;
import org.intellij.plugins.ceylon.ide.ceylonCode.psi.CeylonFile;
import org.intellij.plugins.ceylon.ide.facet.CeylonFacet;
import org.intellij.plugins.ceylon.runtime.CeylonRuntime;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TypeCheckerProvider implements ModuleComponent, ITypeCheckerProvider {

    private Module module;
    private TypeChecker typeChecker;
    private IdeaCeylonProjects ceylonModel;
    private boolean isReady;

    public TypeCheckerProvider(Module module) {
        this.module = module;
    }

    @Nullable
    public static TypeChecker getFor(PsiElement element) {
        if (element.getContainingFile() instanceof CeylonFile) {
            CeylonFile ceylonFile = (CeylonFile) element.getContainingFile();

            if (ceylonFile.getPhasedUnit() instanceof IdePhasedUnit) {
                IdePhasedUnit phasedUnit = (IdePhasedUnit) ceylonFile.getPhasedUnit();
                return phasedUnit.getTypeChecker();
            }

            //LOGGER.warn("CeylonFile has no IdePhasedUnit: " + ceylonFile.getVirtualFile().getCanonicalPath());

            // TODO .ceylon files loaded from .src archives don't belong to any module, what should we do?
            Module module = ModuleUtil.findModuleForFile(ceylonFile.getVirtualFile(), ceylonFile.getProject());

            if (module != null) {
                ITypeCheckerProvider provider = module.getComponent(ITypeCheckerProvider.class);

                if (((TypeCheckerProvider) provider).isReady) {
                    return provider.getTypeChecker();
                }
            }
        }

        return null;
    }

    @Override
    public void initComponent() {
    }

    @Override
    public TypeChecker getTypeChecker() {
        return typeChecker;
    }

    @Override
    public void disposeComponent() {
        typeChecker = null;
        ceylonModel = null;
        module = null;
    }

    @NotNull
    @Override
    public String getComponentName() {
        return "TypeCheckerProvider";
    }

    @Override
    public void projectOpened() {
    }

    public void typecheck() {
        if (ceylonModel == null) {
            return; // the module was just created, moduleAdded() will typecheck again
        }
        if (typeChecker == null) {
            ideaPlatformServices_.get_().register();
            final IdeaCeylonProject ceylonProject = (IdeaCeylonProject) ceylonModel.getProject(module);

            ProgressManager.getInstance().run(new Task.Backgroundable(module.getProject(),
                    "Preparing typechecker for module " + module.getName()) {
                @Override
                public void run(@NotNull ProgressIndicator indicator) {
                    setupCeylonLanguageSrc(ceylonProject);

                    final ProgressIndicatorMonitor mon = new ProgressIndicatorMonitor(ProgressIndicatorMonitor.wrap_, indicator);
                    ceylonProject.parseCeylonModel(mon);
                    typeChecker = ceylonProject.getTypechecker();

                    mon.subTask("Typechecking sources files...");
                    fullTypeCheck();

                    isReady = true;

                    ApplicationManager.getApplication().invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            DaemonCodeAnalyzer.getInstance(module.getProject()).restart();
                        }
                    });
                }
            });
        } else {
            DaemonCodeAnalyzer.getInstance(module.getProject()).restart();
        }
    }

    private void fullTypeCheck() {
        List<PhasedUnit> phasedUnitsOfDependencies = new ArrayList<>();

        for (PhasedUnits phasedUnits : typeChecker.getPhasedUnitsOfDependencies()) {
            for (PhasedUnit pu : phasedUnits.getPhasedUnits()) {
                phasedUnitsOfDependencies.add(pu);
            }
        }

        for (PhasedUnit pu : phasedUnitsOfDependencies) {
            pu.scanDeclarations();
        }

        for (PhasedUnit pu : phasedUnitsOfDependencies) {
            pu.scanTypeDeclarations();
        }

        for (PhasedUnit pu : phasedUnitsOfDependencies) {
            pu.analyseTypes();
        }

        BaseIdeModuleManager mm = (BaseIdeModuleManager) typeChecker.getPhasedUnits().getModuleManager();
        BaseIdeModelLoader loader = mm.getModelLoader();
        loader.loadPackage(loader.getLanguageModule(), "com.redhat.ceylon.compiler.java.metadata", true);
        loader.loadPackage(loader.getLanguageModule(), "ceylon.language", true);
        loader.loadPackage(loader.getLanguageModule(), "ceylon.language.descriptor", true);
        loader.loadPackageDescriptors();

        List<PhasedUnit> phasedUnits = typeChecker.getPhasedUnits().getPhasedUnits();
        for (PhasedUnit pu : phasedUnits) {
            if (!pu.isDeclarationsScanned()) {
                pu.validateTree();
                pu.scanDeclarations();
            }
        }
        for (PhasedUnit pu : phasedUnits) {
            if (!pu.isTypeDeclarationsScanned()) {
                pu.scanTypeDeclarations();
            }
        }
        for (PhasedUnit pu : phasedUnits) {
            if (!pu.isRefinementValidated()) {
                pu.validateRefinement();
            }
        }
        for (PhasedUnit pu : phasedUnits) {
            if (!pu.isFullyTyped()) {
                pu.analyseTypes();
                pu.analyseUsage();
            }
        }
        for (PhasedUnit pu : phasedUnits) {
            pu.analyseFlow();
        }
    }

    @Override
    public void projectClosed() {
        typeChecker = null;

        isReady = false;
        if (ceylonModel != null) {
            ceylonModel.removeProject(module);
        }
    }

    @Override
    public void moduleAdded() {
        if (FacetManager.getInstance(module).getFacetByType(CeylonFacet.ID) == null) {
            return;
        }

        if (ceylonModel == null) {
            ceylonModel = module.getProject().getComponent(IdeaCeylonProjects.class);
        }
        ceylonModel.addProject(module);

        StartupManager.getInstance(module.getProject()).runWhenProjectIsInitialized(
                new Runnable() {
                    @Override
                    public void run() {
                        typecheck();
                    }
                }
        );
    }

    @Nullable
    private File findCeylonLanguageArchive() {
        PluginId pluginId = PluginManager.getPluginByClassName(CeylonRuntime.class.getName());
        IdeaPluginDescriptor descriptor = PluginManager.getPlugin(pluginId);
        File lib = new File(descriptor.getPath(), "lib");

        if (lib.isDirectory()) {
            File[] files = lib.listFiles();
            if (files != null) {
                for (File child : files) {
                    if (child.getName().startsWith("ceylon.language-")) {
                        return child;
                    }
                }
            }
        }
        return null;
    }

    private void setupCeylonLanguageSrc(final IdeaCeylonProject ceylonProject) {
        final File car = findCeylonLanguageArchive();

        if (car != null) {
            ApplicationManager.getApplication().invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    String path;
                    try {
                        path = car.getCanonicalPath();
                    } catch (IOException e) {
                        platformUtils_.get_().log(Status.getStatus$_ERROR(),
                                "Can't add ceylon language to classpath", e);
                        return;
                    }

                    ceylonProject.addLibrary(path, true);
                }
            }, ModalityState.any());

            DumbService.getInstance(module.getProject()).waitForSmartMode();
        } else {
            Logger.getInstance(TypeCheckerProvider.class).error("Could not locate ceylon.language.car");
        }
    }
}
