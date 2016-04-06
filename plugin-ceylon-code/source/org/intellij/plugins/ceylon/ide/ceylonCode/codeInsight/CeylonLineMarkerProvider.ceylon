import ceylon.interop.java {
    javaString
}

import com.intellij.codeHighlighting {
    Pass
}
import com.intellij.codeInsight.daemon {
    LineMarkerInfo,
    GutterIconNavigationHandler
}
import com.intellij.openapi.editor.markup {
    GutterIconRenderer
}
import com.intellij.pom {
    Navigatable
}
import com.intellij.psi {
    PsiElement
}
import com.intellij.util {
    Function
}
import com.redhat.ceylon.ide.common.util {
    types
}
import com.redhat.ceylon.model.typechecker.model {
    Declaration
}

import java.awt.event {
    MouseEvent
}
import java.lang {
    JString=String
}

import org.intellij.plugins.ceylon.ide.ceylonCode.psi {
    CeylonPsi,
    CeylonFile,
    CeylonCompositeElement
}
import org.intellij.plugins.ceylon.ide.ceylonCode.resolve {
    CeylonReference
}
import org.intellij.plugins.ceylon.ide.ceylonCode.util {
    ideaIcons
}

shared class CeylonLineMarkerProvider() extends MyLineMarkerProvider() {
    
    Declaration? getModel(CeylonPsi.DeclarationPsi|CeylonPsi.SpecifierStatementPsi decl) {
        if (is CeylonPsi.DeclarationPsi decl) {
            return decl.ceylonNode.declarationModel;
        }
        return decl.ceylonNode.declaration;
    }
    
    shared actual LineMarkerInfo<PsiElement>? getLineMarkerInfo(PsiElement element) {
        
        if (is CeylonFile file = element.containingFile) {
            file.ensureTypechecked();
        }
        if (is CeylonPsi.IdentifierPsi|CeylonPsi.SpecifierStatementPsi element,
            exists decl = findParentDeclaration(element),
            exists model = getModel(decl),
            model.actual,
            exists refined = types.getRefinedDeclaration(model)) {
            
            value unit = decl.ceylonNode.unit;
            assert(is Declaration parent = refined.container);
            value text = "Refines ``parent.getName(unit)``.``refined.getName(unit)``";
            value tooltip = object satisfies Function<PsiElement, JString> {
                shared actual JString fun(PsiElement? param) => javaString(text);
            };
            value icon = refined.formal then ideaIcons.refinement else ideaIcons.extendedType;

            return LineMarkerInfo(element of CeylonCompositeElement, element.textRange, icon,
                Pass.\iUPDATE_ALL, tooltip, NavigationHandler(refined),
                GutterIconRenderer.Alignment.\iLEFT);
        }
        
        return null;
    }
    
    <CeylonPsi.DeclarationPsi|CeylonPsi.SpecifierStatementPsi>? findParentDeclaration(PsiElement el) {
        if (is CeylonPsi.DeclarationPsi decl = el.parent) {
            return decl;
        } else if (is CeylonPsi.SpecifierStatementPsi el) {
            return el;
        }
        
        return null;
    }
    
    class NavigationHandler(Declaration target)
             satisfies GutterIconNavigationHandler<PsiElement> {
        
        shared actual void navigate(MouseEvent? mouseEvent, PsiElement t) {
            assert(is CeylonFile ceylonFile = t.containingFile);

            if (exists psi = CeylonReference
                    .resolveDeclaration(target, t.project),
                is Navigatable psi) {
                psi.navigate(true);
            }
        }
    }
}
