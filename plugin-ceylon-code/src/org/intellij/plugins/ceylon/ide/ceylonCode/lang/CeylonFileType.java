package org.intellij.plugins.ceylon.ide.ceylonCode.lang;

import javax.swing.Icon;

import org.intellij.plugins.ceylon.ide.ceylonCode.util.ideaIcons_;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.intellij.openapi.fileTypes.LanguageFileType;

public class CeylonFileType extends LanguageFileType {

    public static final CeylonFileType INSTANCE = new CeylonFileType();
    public static final String DEFAULT_EXTENSION = "ceylon";

    protected CeylonFileType() {
        super(CeylonLanguage.INSTANCE);
    }

    @NotNull
    @Override
    public String getName() {
        return "Ceylon";
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Ceylon source file";
    }

    @NotNull
    @Override
    public String getDefaultExtension() {
        return DEFAULT_EXTENSION;
    }

    @Nullable
    @Override
    public Icon getIcon() {
        return ideaIcons_.get_().getFile();
    }
}
