package org.intellij.plugins.ceylon.ide.annotator;

import ceylon.language.AssertionError;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.lang.annotation.Annotation;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.util.TextRange;
import com.redhat.ceylon.compiler.typechecker.TypeChecker;
import com.redhat.ceylon.compiler.typechecker.analyzer.AnalysisError;
import com.redhat.ceylon.compiler.typechecker.analyzer.UsageWarning;
import com.redhat.ceylon.compiler.typechecker.analyzer.Warning;
import com.redhat.ceylon.compiler.typechecker.context.PhasedUnit;
import com.redhat.ceylon.compiler.typechecker.parser.CeylonLexer;
import com.redhat.ceylon.compiler.typechecker.parser.RecognitionError;
import com.redhat.ceylon.compiler.typechecker.tree.*;
import com.redhat.ceylon.ide.common.model.BaseCeylonProject;
import com.redhat.ceylon.ide.common.util.nodes_;
import org.antlr.runtime.CommonToken;
import org.apache.commons.lang.ArrayUtils;
import org.intellij.plugins.ceylon.ide.ceylonCode.correct.IdeaQuickFixData;
import org.intellij.plugins.ceylon.ide.ceylonCode.correct.ideaQuickFixManager_;
import org.intellij.plugins.ceylon.ide.ceylonCode.model.IdeaCeylonProjects;
import org.intellij.plugins.ceylon.ide.ceylonCode.psi.CeylonFile;

import java.util.ArrayList;

/**
 * A visitor that visits a compilation unit returned by {@link com.redhat.ceylon.compiler.typechecker.parser.CeylonParser}
 * to highlight errors and warnings using an {@link com.intellij.lang.annotation.AnnotationHolder}.
 */
class CeylonTypeCheckerVisitor extends Visitor {

    private AnnotationHolder annotationHolder;
    private CeylonFile file;

    private static final int[] UNRESOLVED_REFERENCE_CODES = {100, 102};

    private static final String[] UNUSED_CODES = {
            Warning.unusedDeclaration.toString(),
            Warning.unusedImport.toString()
    };

    /**
     * Creates a new visitor that will report errors and warnings in {@code annotationHolder}.
     *
     * @param annotationHolder the receiver of the annotations
     */
    public CeylonTypeCheckerVisitor(AnnotationHolder annotationHolder, CeylonFile file) {
        this.annotationHolder = annotationHolder;
        this.file = file;
    }

    @Override
    public void visitAny(Node that) {
        for (Message error : new ArrayList<>(that.getErrors())) {
            int crlfCountDiff = 0;
            if (that.getStartIndex() == null || that.getEndIndex() == null) {
                continue;
            }
            TextRange range = new TextRange(that.getStartIndex() + crlfCountDiff, that.getEndIndex() + crlfCountDiff);

            if (that instanceof Tree.Declaration) {
                Tree.Identifier id = ((Tree.Declaration) that).getIdentifier();
                if (id != null && id.getStartIndex() != null && id.getEndIndex() != null) {
                    range = new TextRange(id.getStartIndex() - crlfCountDiff, id.getEndIndex() - crlfCountDiff);
                }
            }
            if (error instanceof RecognitionError) {
                RecognitionError recognitionError = (RecognitionError) error;
                CommonToken token = (CommonToken) recognitionError.getRecognitionException().token;

                if (token != null) {
                    if (token.getType() == CeylonLexer.EOF) {
                        // we can't underline EOF, so we try to underline the last word instead
                        int lastNonWS = file.getTextLength() - 1;
                        while (lastNonWS >= 0 && Character.isWhitespace(file.getText().charAt(lastNonWS))) {
                            lastNonWS--;
                        }
                        int wordStart = lastNonWS;
                        while (wordStart >= 0 && !Character.isWhitespace(file.getText().charAt(wordStart))) {
                            wordStart--;
                        }

                        if (wordStart < lastNonWS) {
                            range = new TextRange(wordStart + 1, lastNonWS + 1);
                        } else {
                            range = new TextRange(token.getStartIndex() - 1, token.getStopIndex());
                        }
                    } else {
                        range = new TextRange(token.getStartIndex(), token.getStopIndex() + 1);
                    }
                }
            }

            Annotation annotation;
            if (error instanceof AnalysisError || error instanceof RecognitionError || error instanceof UnexpectedError) {
                annotation = annotationHolder.createErrorAnnotation(range, error.getMessage());

                if (ArrayUtils.contains(UNRESOLVED_REFERENCE_CODES, error.getCode())) {
                    annotation.setHighlightType(ProblemHighlightType.LIKE_UNKNOWN_SYMBOL);
                }
            } else if (error instanceof UsageWarning) {
                annotation = annotationHolder.createWarningAnnotation(range, error.getMessage());

                if (ArrayUtils.contains(UNUSED_CODES, ((UsageWarning) error).getWarningName())) {
                    annotation.setHighlightType(ProblemHighlightType.LIKE_UNUSED_SYMBOL);
                } else {
                    annotation.setHighlightType(ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
                }
            } else {
                annotation = annotationHolder.createInfoAnnotation(range, error.getMessage());
            }

            if (!ApplicationManager.getApplication().isUnitTestMode()) {
                addQuickFixes(range, error, annotation);
            }
        }
        super.visitAny(that);
    }

    @Override
    public void handleException(Exception e, Node that) {
        e.printStackTrace();
    }

    private void addQuickFixes(TextRange range, Message error, Annotation annotation) {
        CeylonFile file = (CeylonFile) annotationHolder.getCurrentAnnotationSession().getFile();
        Module module = ModuleUtil.findModuleForFile(file.getVirtualFile(), file.getProject());
        TypeChecker tc = TypeCheckerProvider.getFor(file);
        PhasedUnit pu = file.getPhasedUnit();
        IdeaCeylonProjects projects = file.getProject().getComponent(IdeaCeylonProjects.class);
        BaseCeylonProject project = projects.getProject(module);

        Node node = nodes_.get_().findNode(pu.getCompilationUnit(), null, range.getStartOffset(), range.getEndOffset());
        IdeaQuickFixData data = new IdeaQuickFixData(error, file.getViewProvider().getDocument(),
                pu.getCompilationUnit(), node, module, annotation, project);

        try {
            ideaQuickFixManager_.get_().addQuickFixes(data, tc, file);
        } catch (Exception|AssertionError e) {
            e.printStackTrace();
        }
    }
}
