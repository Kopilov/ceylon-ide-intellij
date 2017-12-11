/********************************************************************************
 * Copyright (c) {date} Red Hat Inc. and/or its affiliates and others
 *
 * This program and the accompanying materials are made available under the 
 * terms of the Apache License, Version 2.0 which is available at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: Apache-2.0 
 ********************************************************************************/
import com.intellij.openapi.application {
    QueryExecutorBase
}
import com.intellij.psi {
    PsiReference,
    PsiElement,
    PsiClass,
    PsiMethod,
    PsiFile,
    PsiNameIdentifierOwner
}
import com.intellij.psi.search.searches {
    ReferencesSearch
}
import com.intellij.util {
    Processor
}
import org.eclipse.ceylon.compiler.typechecker.context {
    PhasedUnit
}
import org.eclipse.ceylon.compiler.typechecker.tree {
    Tree,
    Node
}
import org.eclipse.ceylon.ide.common.util {
    FindRefinementsVisitor
}
import org.eclipse.ceylon.model.typechecker.model {
    Declaration
}

import org.eclipse.ceylon.ide.intellij.model {
    concurrencyManager {
        needReadAccess
    },
    getCeylonProject,
    findProjectForFile,
    declarationFromPsiElement
}
import org.eclipse.ceylon.ide.intellij.psi {
    CeylonPsi,
    isInSourceArchive,
    CeylonFile,
    CeylonTreeUtil {
        findPsiElement,
        getDeclaringFile
    }
}

//TODO: this is currently disabled in plugin.xml, but I would like to add it in as a preference
shared class RefinementsSearch() extends
        QueryExecutorBase<PsiReference, ReferencesSearch.SearchParameters>() {

    shared actual void processQuery(ReferencesSearch.SearchParameters params, Processor<PsiReference> consumer) {
        if (is CeylonPsi.DeclarationPsi element = params.elementToSearch) {
            findImplementors(element, consumer.process);
        }
    }

    void findImplementors(PsiElement sourceElement, void consumer(PsiReference element)) {
        switch (sourceElement)
        case (is PsiClass|PsiMethod) {
            if (exists psiFile = sourceElement.containingFile,
                exists decl = declarationFromPsiElement(sourceElement),
                decl.formal || decl.default,
                exists pus = getCeylonProject(psiFile)?.typechecker?.phasedUnits) {

                scanPhasedUnits {
                    decl = decl;
                    node = null;
                    sourceElement = sourceElement;
                    consumer = consumer;
                    *pus.phasedUnits
                };
            }
        }
        else case (is CeylonPsi.DeclarationPsi) {
            if (exists node = sourceElement.ceylonNode,
                exists decl = node.declarationModel,
                decl.formal || decl.default,
                is CeylonFile ceylonFile = sourceElement.containingFile,
                exists project
                        = needReadAccess(()
                            => findProjectForFile(ceylonFile)),
                exists modules = project.modules) {

                if (exists pus = project.typechecker?.phasedUnits) {
                    scanPhasedUnits {
                        decl = decl;
                        node = node;
                        sourceElement = sourceElement;
                        consumer = consumer;
                        *pus.phasedUnits
                    };
                }

                if (isInSourceArchive(ceylonFile.realVirtualFile())) {
                    for (mod in modules) {
                        scanPhasedUnits {
                            pus = mod.phasedUnits;
                            decl = decl;
                            node = node;
                            sourceElement = sourceElement;
                            consumer = consumer;
                        };
                    }
                }
            }
        }
        else {}
    }

    void action(PsiFile declaringFile, Node dnode,
                void consumer(PsiReference element)) {
        variable CeylonReference? ref = null;
        needReadAccess(() {
            if (is PsiNameIdentifierOwner psiElement = findPsiElement(dnode, declaringFile),
                exists id = psiElement.nameIdentifier) {
                ref = CeylonReference(id);
            }
        });
        if (exists r = ref) {
            consumer(r);
        }
    }

    void scanPhasedUnits({PhasedUnit*} pus,
        Declaration decl,
        Tree.Declaration? node, PsiElement sourceElement,
        void consumer(PsiReference element)) {

        for (pu in pus) {
            value vis = FindRefinementsVisitor(decl);
            pu.compilationUnit.visit(vis);
            for (dnode in vis.declarationNodes) {
                if (exists node) {
                    if (dnode==node) {
                        continue;
                    }
                    if (is Tree.Declaration dnode,
                        dnode.declarationModel.qualifiedNameString
                            == decl.qualifiedNameString) {
                        continue;
                    }
                }

                value declaringFile = getDeclaringFile(dnode.unit, sourceElement.project);
                if (is CeylonFile declaringFile) {
                    declaringFile.doWhenAnalyzed((_) => action(declaringFile, dnode, consumer));
                } else if (exists declaringFile) {
                    action(declaringFile, dnode, consumer);
                }
            }
        }
    }
}

