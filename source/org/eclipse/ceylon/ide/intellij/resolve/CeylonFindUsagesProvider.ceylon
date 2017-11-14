/********************************************************************************
 * Copyright (c) {date} Red Hat Inc. and/or its affiliates and others
 *
 * This program and the accompanying materials are made available under the 
 * terms of the Apache License, Version 2.0 which is available at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: Apache-2.0 
 ********************************************************************************/
import com.intellij.lang.cacheBuilder {
    DefaultWordsScanner
}
import com.intellij.lang.findUsages {
    FindUsagesProvider
}
import com.intellij.openapi.diagnostic {
    Logger
}
import com.intellij.psi {
    PsiElement,
    PsiNamedElement
}
import com.intellij.psi.tree {
    TokenSet
}
import org.eclipse.ceylon.compiler.typechecker.tree {
    Tree
}
import org.eclipse.ceylon.ide.common.typechecker {
    AnalysisResult
}
import org.eclipse.ceylon.ide.common.util {
    nodes
}

import java.lang {
    UnsupportedOperationException
}
import java.util.concurrent {
    TimeUnit
}

import org.eclipse.ceylon.ide.intellij.parser {
    CeylonAntlrToIntellijLexerAdapter
}
import org.eclipse.ceylon.ide.intellij.psi {
    CeylonCompositeElement,
    CeylonFile,
    CeylonPsi,
    TokenTypes,
    kind
}
import org.eclipse.ceylon.ide.intellij.psi.impl {
    DeclarationPsiNameIdOwner
}

shared class CeylonFindUsagesProvider() satisfies FindUsagesProvider {

    value logger = Logger.getInstance(`CeylonFindUsagesProvider`);

    wordsScanner
            => DefaultWordsScanner(CeylonAntlrToIntellijLexerAdapter(),
                TokenSet.create(TokenTypes.lidentifier.tokenType,
                                TokenTypes.uidentifier.tokenType),
                TokenSet.create(TokenTypes.multiComment.tokenType,
                                TokenTypes.lineComment.tokenType),
                TokenSet.create(TokenTypes.stringLiteral.tokenType));

    canFindUsagesFor(PsiElement psiElement)
            => psiElement is DeclarationPsiNameIdOwner;

    getHelpId(PsiElement psiElement)
            => "Please open an issue if you ever need this help :)";

    getType(PsiElement element) => kind(element);

    shared actual String getDescriptiveName(PsiElement element) {
        if (is CeylonCompositeElement element) {
            assert (is CeylonFile file = element.containingFile);
            Tree.CompilationUnit? typecheckedCompilationUnit;
            AnalysisResult? analysisResult;
            if (exists localAnalysisResult = file.localAnalyzer?.result) {
                analysisResult = localAnalysisResult;
                typecheckedCompilationUnit = localAnalysisResult.lastCompilationUnit;
            } else {
                analysisResult = file.waitForAnalysis(5, TimeUnit.seconds);
                typecheckedCompilationUnit = analysisResult?.typecheckedRootNode;
            }

            if (exists analysisResult,
                exists typecheckedCompilationUnit) {
                value node = nodes.findNode {
                    node = typecheckedCompilationUnit;
                    tokens = analysisResult.tokens;
                    startOffset = element.textRange.startOffset;
                    endOffset = element.textRange.endOffset;
                };
                switch (node)
                case (null) {}
                case (is Tree.InitializerParameter) {
                    return node.identifier.text;
                }
                case (is Tree.ModuleDescriptor) {
                    return node.importPath.model.nameAsString;
                }
                else if (exists id = nodes.findDeclaration(typecheckedCompilationUnit, node)?.identifier) {
                    return id.text;
                }
            }
        }

        logger.warn("Descriptive name not implemented for " + className(element));

        if (is CeylonPsi.IdentifierPsi element) {
            return element.ceylonNode.text;
        }
        return "<unknown>";
    }

    shared actual String getNodeText(PsiElement element, Boolean useFullName) {
        if (is PsiNamedElement element,
            exists name = element.name) {
            return name;
        }
        throw UnsupportedOperationException();
    }
}
