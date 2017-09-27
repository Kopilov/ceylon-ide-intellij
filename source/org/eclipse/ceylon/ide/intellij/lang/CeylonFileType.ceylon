//import com.intellij.openapi.fileTypes {
//    LanguageFileType
//}
//
//import org.eclipse.ceylon.ide.intellij.util {
//    icons
//}
//
//shared class CeylonFileType extends LanguageFileType {
//
//    shared new instance extends LanguageFileType(ceylonLanguage) {}
//
//    name => "Ceylon";
//
//    description => "Ceylon source file";
//
//    defaultExtension => "ceylon";
//
//    icon => icons.file;
//}

shared CeylonFileType ceylonFileType = CeylonFileType.instance;
