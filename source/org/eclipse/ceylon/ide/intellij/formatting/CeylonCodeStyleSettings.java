/********************************************************************************
 * Copyright (c) {date} Red Hat Inc. and/or its affiliates and others
 *
 * This program and the accompanying materials are made available under the 
 * terms of the Apache License, Version 2.0 which is available at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: Apache-2.0 
 ********************************************************************************/
package org.eclipse.ceylon.ide.intellij.formatting;

import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CustomCodeStyleSettings;

/**
 * Reads/writes settings from/to a configuration file. Also holds the settings
 * for the current project.
 *
 * This class is written in Java because {@link com.intellij.psi.codeStyle.CodeStyleSettingsCustomizable#showCustomOption(Class, String, String, String, Object...)}
 * only works on (public?) fields.
 */
public class CeylonCodeStyleSettings extends CustomCodeStyleSettings {

    public boolean spaceBeforePositionalArgs;
    public boolean spaceBeforeAnnotationPositionalArgs = true;
    public boolean spaceInSatisfiesAndOf = true;
    public boolean spaceAroundEqualsInImportAlias;
    public boolean spaceAfterTypeParam = true;
    public boolean spaceAfterTypeArg;
    public boolean spaceAroundEqualsInTypeArgs;
    public boolean spaceAfterKeyword = true;

    public boolean spaceBeforeParamListOpen;
    public boolean spaceAfterParamListOpen;
    public boolean spaceBeforeParamListClose;
    public boolean spaceAfterParamListClose = true;

    public boolean spaceAfterIterableEnumOpen = true;
    public boolean spaceBeforeIterableEnumClose = true;

    public boolean spaceAfterIteratorInLoopOpen;
    public boolean spaceBeforeIteratorInLoopClose;

    protected CeylonCodeStyleSettings(CodeStyleSettings container) {
        super("CeylonCodeStyleSettings", container);
    }

    // TODO see if we can synchronize this config with a ceylon.formatter.options::FormattingOptions
    // if the settings apply to a givne project
}
