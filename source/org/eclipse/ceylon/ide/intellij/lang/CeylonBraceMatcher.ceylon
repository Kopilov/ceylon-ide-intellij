/********************************************************************************
 * Copyright (c) {date} Red Hat Inc. and/or its affiliates and others
 *
 * This program and the accompanying materials are made available under the 
 * terms of the Apache License, Version 2.0 which is available at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: Apache-2.0 
 ********************************************************************************/
import com.intellij.lang {
    BracePair,
    PairedBraceMatcher
}
import com.intellij.psi {
    PsiFile,
    PsiNameIdentifierOwner
}
import com.intellij.psi.tree {
    IElementType
}

import org.eclipse.ceylon.ide.intellij.psi {
    CeylonPsi,
    TokenTypes,
    CeylonTokens
}
import java.lang {
    ObjectArray
}

shared class CeylonBraceMatcher() satisfies PairedBraceMatcher {

    pairs = ObjectArray.with {
        BracePair(TokenTypes.lparen.tokenType, TokenTypes.rparen.tokenType, false),
        BracePair(TokenTypes.lbrace.tokenType, TokenTypes.rbrace.tokenType, true),
        BracePair(TokenTypes.lbracket.tokenType, TokenTypes.rbracket.tokenType, false)
    };

    isPairedBracesAllowedBeforeType(IElementType lbraceType, IElementType contextType)
            => contextType != CeylonTokens.lidentifier
            && contextType != CeylonTokens.uidentifier
            && contextType != CeylonTokens.stringLiteral
            && contextType != CeylonTokens.astringLiteral
            && contextType != CeylonTokens.verbatimString
            && contextType != CeylonTokens.averbatimString
            && contextType != CeylonTokens.stringStart
            && contextType != CeylonTokens.charLiteral
            && contextType != CeylonTokens.floatLiteral
            && contextType != CeylonTokens.naturalLiteral
            && contextType != CeylonTokens.\isuper
            && contextType != CeylonTokens.\ithis
            && contextType != CeylonTokens.\iouter
            && contextType != CeylonTokens.ifClause
            && contextType != CeylonTokens.switchClause
            && contextType != CeylonTokens.\ilet
            && contextType != CeylonTokens.objectDefinition
            && contextType != CeylonTokens.\inonempty
            && contextType != CeylonTokens.isOp
            && contextType != CeylonTokens.\iexists
            && contextType != CeylonTokens.decrementOp
            && contextType != CeylonTokens.incrementOp;

    shared actual Integer getCodeConstructStart(PsiFile file, Integer openingBraceOffset) {
        if (exists element = file.findElementAt(openingBraceOffset),
            is CeylonPsi.BlockPsi parent = element.parent,
            is PsiNameIdentifierOwner parentParent = parent.parent) {
            return if (exists nameIdentifier = parentParent.nameIdentifier)
                then nameIdentifier.textOffset
                else parent.textOffset;
        }
        return openingBraceOffset;
    }
}
