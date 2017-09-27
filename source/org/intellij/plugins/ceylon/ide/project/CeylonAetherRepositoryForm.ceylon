import com.intellij.openapi.fileChooser {
    FileChooserDescriptorFactory
}
import com.intellij.ide.highlighter {
    XmlFileType
}
import org.eclipse.ceylon.ide.intellij {
    CeylonBundle
}

shared class CeylonAetherRepositoryForm()
        extends AetherRepositoryForm() {

    value descriptor = FileChooserDescriptorFactory.createSingleFileDescriptor(XmlFileType.instance)
        .withFileFilter((virtualFile) => virtualFile.name.endsWith("settings.xml"));

    repoField.addBrowseFolderListener(CeylonBundle.message("project.wizard.repo.maven.selectpath"),
                                      null, null, descriptor);

}