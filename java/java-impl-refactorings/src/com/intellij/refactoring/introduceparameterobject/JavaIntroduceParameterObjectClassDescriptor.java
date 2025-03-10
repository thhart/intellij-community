// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.refactoring.introduceparameterobject;

import com.intellij.codeInsight.generation.GenerateMembersUtil;
import com.intellij.codeInsight.highlighting.ReadWriteAccessDetector;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.ide.util.PackageUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.pom.java.JavaFeature;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.util.PropertyUtilBase;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.psi.util.PsiUtil;
import com.intellij.psi.util.TypeConversionUtil;
import com.intellij.refactoring.MoveDestination;
import com.intellij.refactoring.changeSignature.ParameterInfoImpl;
import com.intellij.refactoring.introduceParameterObject.IntroduceParameterObjectClassDescriptor;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class JavaIntroduceParameterObjectClassDescriptor extends IntroduceParameterObjectClassDescriptor<PsiMethod, ParameterInfoImpl> {
  private static final Logger LOG = Logger.getInstance(JavaIntroduceParameterObjectClassDescriptor.class);
  private final Set<PsiTypeParameter> myTypeParameters = new LinkedHashSet<>();
  private final Map<ParameterInfoImpl, ParameterBean> myExistingClassProperties = new HashMap<>();
  private final MoveDestination myMoveDestination;

  public JavaIntroduceParameterObjectClassDescriptor(@NotNull String className,
                                                     String packageName,
                                                     MoveDestination moveDestination,
                                                     boolean useExistingClass,
                                                     boolean createInnerClass,
                                                     String newVisibility,
                                                     ParameterInfoImpl[] paramsToMerge,
                                                     PsiMethod method, boolean generateAccessors) {
    super(className, calcPackageName(packageName, createInnerClass, method), useExistingClass, createInnerClass,
          newVisibility, generateAccessors, paramsToMerge);
    myMoveDestination = moveDestination;
    PsiTypesUtil.TypeParameterSearcher searcher = new PsiTypesUtil.TypeParameterSearcher();
    for (ParameterInfoImpl parameterInfo : paramsToMerge) {
      parameterInfo.getTypeWrapper().getType(method).accept(searcher);
    }
    myTypeParameters.addAll(searcher.getTypeParameters());
  }

  private static String calcPackageName(String packageName, boolean createInnerClass, PsiMethod method) {
    if (createInnerClass) {
      final PsiClass containingClass = method.getContainingClass();
      if (containingClass != null) {
        final String qualifiedName = containingClass.getQualifiedName();
        return qualifiedName != null ? qualifiedName : "";
      }
      else {
        return packageName;
      }
    }
    return packageName;
  }

  public Set<PsiTypeParameter> getTypeParameters() {
    return myTypeParameters;
  }

  public MoveDestination getMoveDestination() {
    return myMoveDestination;
  }

  public String createFakeClassTypeText() {
    String text = StringUtil.getQualifiedName(getPackageName(), getClassName());
    if (!myTypeParameters.isEmpty()) {
      text += "<" + StringUtil.join(myTypeParameters, PsiNamedElement::getName, ", ") + ">";
    }
    return text;
  }


  @Override
  public PsiClass getExistingClass() {
    return (PsiClass)super.getExistingClass();
  }

  public String getGetter(ParameterInfoImpl param) {
    final ParameterBean bean = getBean(param);
    return bean != null ? bean.getGetter() : null;
  }

  public String getSetter(ParameterInfoImpl param) {
    final ParameterBean bean = getBean(param);
    return bean != null ? bean.getSetter() : null;
  }

  @Override
  public String getSetterName(ParameterInfoImpl parameterInfo, @NotNull PsiElement context) {
    final ParameterBean bean = getBean(parameterInfo);
    @NonNls String setter = bean != null ? bean.getSetter() : null;
    if (setter == null) {
      setter = bean != null && bean.getField() != null
               ? GenerateMembersUtil.suggestSetterName(bean.getField())
               : GenerateMembersUtil
                 .suggestSetterName(parameterInfo.getName(), parameterInfo.getTypeWrapper().getType(context),
                                    context.getProject());
    }

    return setter;
  }

  @Override
  public String getGetterName(ParameterInfoImpl paramInfo,
                              @NotNull PsiElement context,
                              ReadWriteAccessDetector.Access access) {
    final ParameterBean bean = getBean(paramInfo);
    @NonNls String getter = bean != null ? bean.getGetter() : null;
    if (getter == null) {
      if (bean != null && bean.getField() != null) {
        getter = GenerateMembersUtil.suggestGetterName(bean.getField());
      }
      else if (bean == null && access == ReadWriteAccessDetector.Access.Read && PsiUtil.isAvailable(JavaFeature.RECORDS, context)) {
        getter = paramInfo.getName();
      }
      else {
        getter = GenerateMembersUtil.suggestGetterName(paramInfo.getName(), paramInfo.getTypeWrapper().getType(context), context.getProject());
      }
    }
    return getter;
  }

  @Override
  public PsiMethod findCompatibleConstructorInExistingClass(PsiMethod method) {
    final JavaPsiFacade psiFacade = JavaPsiFacade.getInstance(method.getProject());
    final String qualifiedName = StringUtil.getQualifiedName(getPackageName(), getClassName());
    final PsiClass existingClass = psiFacade.findClass(qualifiedName, method.getResolveScope());
    setExistingClass(existingClass);
    return findCompatibleConstructor(existingClass);
  }

  public @Nullable PsiField getField(ParameterInfoImpl parameter) {
    final ParameterBean bean = getBean(parameter);
    return bean != null ? bean.getField() : null;
  }

  public ParameterBean getBean(ParameterInfoImpl param) {
    return myExistingClassProperties.get(param);
  }

  private @Nullable PsiMethod findCompatibleConstructor(@NotNull PsiClass aClass) {
    ParameterInfoImpl[] paramsToMerge = getParamsToMerge();
    if (paramsToMerge.length == 1) {
      final ParameterInfoImpl parameterInfo = paramsToMerge[0];
      final PsiType paramType = parameterInfo.getTypeWrapper().getType(aClass);
      String fqn = aClass.getQualifiedName();
      if (fqn != null && TypeConversionUtil.isPrimitiveWrapper(fqn)) {
        ParameterBean bean = new ParameterBean();
        bean.setField(aClass.findFieldByName("value", false));
        bean.setGetter(paramType.getCanonicalText() + "Value");
        myExistingClassProperties.put(parameterInfo, bean);
        for (PsiMethod constructor : aClass.getConstructors()) {
          if (isConstructorCompatible(constructor, new ParameterInfoImpl[]{parameterInfo}, aClass)) return constructor;
        }
      }
    }
    final PsiMethod[] constructors = aClass.getConstructors();
    PsiMethod compatibleConstructor = null;
    for (PsiMethod constructor : constructors) {
      if (isConstructorCompatible(constructor, paramsToMerge, aClass)) {
        compatibleConstructor = constructor;
        break;
      }
    }

    PsiField[] fields = aClass.getFields();
    if (compatibleConstructor == null && !areTypesCompatible(getParamsToMerge(), fields, aClass)) {
      return null;
    }

    final PsiVariable[] constructorParams = compatibleConstructor != null ? compatibleConstructor.getParameterList().getParameters()
                                                                          : fields;
    for (int i = 0; i < getParamsToMerge().length; i++) {
      final int oldIndex = getParamsToMerge()[i].getOldIndex();
      final ParameterInfoImpl methodParam = getParameterInfo(oldIndex);
      final ParameterBean bean = new ParameterBean();
      myExistingClassProperties.put(methodParam, bean);

      final PsiVariable var = constructorParams[i];

      final PsiField field = var instanceof PsiParameter ? findFieldAssigned((PsiParameter)var, compatibleConstructor) : (PsiField)var;
      if (field == null) {
        return null;
      }

      bean.setField(field);

      final PsiMethod getterForField = PropertyUtilBase.findGetterForField(field);
      if (getterForField != null) {
        bean.setGetter(getterForField.getName());
      }

      final PsiMethod setterForField = PropertyUtilBase.findSetterForField(field);
      if (setterForField != null) {
        bean.setSetter(setterForField.getName());
      }
    }
    return compatibleConstructor;
  }

  private static boolean isConstructorCompatible(PsiMethod constructor, ParameterInfoImpl[] paramsToMerge, PsiElement context) {
    final PsiParameterList parameterList = constructor.getParameterList();
    final PsiParameter[] constructorParams = parameterList.getParameters();
    return areTypesCompatible(paramsToMerge, constructorParams, context);
  }

  private static boolean areTypesCompatible(ParameterInfoImpl[] expected, PsiVariable[] actual, PsiElement context) {
    if (actual.length != expected.length) {
      return false;
    }
    for (int i = 0; i < actual.length; i++) {
      if (!TypeConversionUtil.isAssignable(actual[i].getType(), expected[i].getTypeWrapper().getType(context))) {
        return false;
      }
    }
    return true;
  }

  private static PsiField findFieldAssigned(PsiParameter param, PsiMethod constructor) {
    final ParamAssignmentFinder visitor = new ParamAssignmentFinder(param);
    constructor.accept(visitor);
    return visitor.getFieldAssigned();
  }

  @Override
  public PsiClass createClass(PsiMethod method, ReadWriteAccessDetector.Access[] accessors) {
    if (isUseExistingClass()) {
      return getExistingClass();
    }

    final ParameterObjectBuilder beanClassBuilder = new ParameterObjectBuilder();
    beanClassBuilder.setVisibility(isCreateInnerClass() ? PsiModifier.PRIVATE : PsiModifier.PUBLIC);
    beanClassBuilder.setProject(method.getProject());
    beanClassBuilder.setFile(method.getContainingFile());
    beanClassBuilder.setTypeArguments(getTypeParameters());
    beanClassBuilder.setClassName(getClassName());
    beanClassBuilder.setPackageName(getPackageName());
    PsiParameter[] parameters = method.getParameterList().getParameters();
    final ParameterInfoImpl[] parameterInfos = getParamsToMerge();
    for (int i = 0; i < parameterInfos.length; i++) {
      PsiParameter parameter = parameters[parameterInfos[i].getOldIndex()];
      final boolean setterRequired = accessors[i] == ReadWriteAccessDetector.Access.Write;
      final String newName = parameterInfos[i].getName();
      beanClassBuilder
        .addField(parameter, newName, parameterInfos[i].getTypeWrapper().getType(method), setterRequired);
    }

    final String classString = beanClassBuilder.buildBeanClass();

    try {
      final PsiFileFactory factory = PsiFileFactory.getInstance(method.getProject());
      final PsiJavaFile newFile =
        (PsiJavaFile)factory.createFileFromText(getClassName() + ".java", JavaFileType.INSTANCE, classString);
      if (isCreateInnerClass()) {
        final PsiClass containingClass = method.getContainingClass();
        final PsiClass[] classes = newFile.getClasses();
        assert classes.length > 0 : classString;
        final PsiClass innerClass = (PsiClass)containingClass.add(classes[0]);
        PsiUtil.setModifierProperty(innerClass, PsiModifier.STATIC, true);
        return (PsiClass)JavaCodeStyleManager.getInstance(newFile.getProject()).shortenClassReferences(innerClass);
      }
      else {
        final PsiFile containingFile = method.getContainingFile();
        final PsiDirectory containingDirectory = containingFile.getContainingDirectory();
        final PsiDirectory directory;
        final MoveDestination moveDestination = getMoveDestination();
        if (moveDestination != null) {
          directory = moveDestination.getTargetDirectory(containingDirectory);
        }
        else {
          final Module module = ModuleUtilCore.findModuleForPsiElement(containingFile);
          directory =
            PackageUtil.findOrCreateDirectoryForPackage(module, getPackageName(), containingDirectory, true, true);
        }

        if (directory != null) {

          PsiFile file = directory.findFile(newFile.getName());
          if (file == null) {
            final CodeStyleManager codeStyleManager = CodeStyleManager.getInstance(method.getManager().getProject());
            final PsiElement shortenedFile = JavaCodeStyleManager.getInstance(newFile.getProject()).shortenClassReferences(newFile);
            final PsiElement reformattedFile = codeStyleManager.reformat(shortenedFile);
            file = (PsiFile)directory.add(reformattedFile);
          }

          return ((PsiJavaFile)file).getClasses()[0];
        }
      }
    }
    catch (IncorrectOperationException e) {
      LOG.error(e);
    }
    return null;
  }


  private static class ParamAssignmentFinder extends JavaRecursiveElementWalkingVisitor {

    private final PsiParameter param;

    private PsiField fieldAssigned;

    ParamAssignmentFinder(PsiParameter param) {
      this.param = param;
    }

    @Override
    public void visitAssignmentExpression(@NotNull PsiAssignmentExpression assignment) {
      super.visitAssignmentExpression(assignment);
      final PsiExpression lhs = assignment.getLExpression();
      final PsiExpression rhs = assignment.getRExpression();
      if (!(lhs instanceof PsiReferenceExpression)) {
        return;
      }
      if (!(rhs instanceof PsiReferenceExpression)) {
        return;
      }
      final PsiElement referent = ((PsiReference)rhs).resolve();
      if (referent == null || !referent.equals(param)) {
        return;
      }
      final PsiElement assigned = ((PsiReference)lhs).resolve();
      if (!(assigned instanceof PsiField)) {
        return;
      }
      fieldAssigned = (PsiField)assigned;
    }

    public PsiField getFieldAssigned() {
      return fieldAssigned;
    }
  }

  @ApiStatus.Internal
  public static final class ParameterBean {
    private PsiField myField;
    private String myGetter;
    private String mySetter;

    public PsiField getField() {
      return myField;
    }

    public void setField(PsiField field) {
      myField = field;
    }

    public String getGetter() {
      return myGetter;
    }

    public void setGetter(@NonNls String getter) {
      myGetter = getter;
    }

    public String getSetter() {
      return mySetter;
    }

    public void setSetter(String setter) {
      mySetter = setter;
    }
  }
}
