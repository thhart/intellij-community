// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.kotlin.idea.imports

import com.intellij.lang.ImportOptimizer
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicatorProvider
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiRecursiveVisitor
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.base.codeInsight.KotlinOptimizeImportsFacility
import org.jetbrains.kotlin.idea.base.projectStructure.languageVersionSettings
import org.jetbrains.kotlin.idea.base.projectStructure.moduleInfo.ModuleSourceInfo
import org.jetbrains.kotlin.idea.base.projectStructure.moduleInfoOrNull
import org.jetbrains.kotlin.idea.base.resources.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.caches.resolve.safeAnalyzeNonSourceRootCode
import org.jetbrains.kotlin.idea.core.script.v1.ScriptModuleInfo
import org.jetbrains.kotlin.idea.formatter.kotlinCustomSettings
import org.jetbrains.kotlin.idea.references.*
import org.jetbrains.kotlin.idea.util.getResolutionScope
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.references.fe10.base.KtFe10Reference
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.ImportPath
import org.jetbrains.kotlin.resolve.calls.util.getResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.getImportableDescriptor
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.scopes.HierarchicalScope
import org.jetbrains.kotlin.resolve.scopes.utils.*
import org.jetbrains.kotlin.types.error.ErrorFunctionDescriptor

class KotlinImportOptimizer : ImportOptimizer {
    override fun supports(file: PsiFile) = file is KtFile

    override fun processFile(file: PsiFile): ImportOptimizer.CollectingInfoRunnable {
        val ktFile = (file as? KtFile) ?: return DO_NOTHING
        val (add, remove, imports) = prepareImports(ktFile) ?: return DO_NOTHING

        return object : ImportOptimizer.CollectingInfoRunnable {
            override fun getUserNotificationInfo(): String = if (remove == 0)
                KotlinBundle.message("import.optimizer.text.zero")
            else
                KotlinBundle.message(
                    "import.optimizer.text.non.zero",
                    remove,
                    KotlinBundle.message("import.optimizer.text.import", remove),
                    add,
                    KotlinBundle.message("import.optimizer.text.import", add)
                )

            override fun run() = KotlinOptimizeImportsFacility.getInstance().replaceImports(ktFile, imports)
        }
    }

    // The same as com.intellij.pom.core.impl.PomModelImpl.isDocumentUncommitted
    // Which is checked in com.intellij.pom.core.impl.PomModelImpl.startTransaction
    private val KtFile.isDocumentUncommitted: Boolean
        get() {
            val documentManager = PsiDocumentManager.getInstance(project)
            val cachedDocument = documentManager.getCachedDocument(this)
            return cachedDocument != null && documentManager.isUncommited(cachedDocument)
        }

    private fun prepareImports(file: KtFile): OptimizeInformation? {
        ApplicationManager.getApplication().assertReadAccessAllowed()

        // Optimize imports may be called after command
        // And document can be uncommitted after running that command
        // In that case we will get ISE: Attempt to modify PSI for non-committed Document!
        if (file.isDocumentUncommitted) return null

        val moduleInfo = file.moduleInfoOrNull
        if (moduleInfo !is ModuleSourceInfo && moduleInfo !is ScriptModuleInfo) return null

        val oldImports = file.importDirectives
        if (oldImports.isEmpty()) return null

        //TODO: keep existing imports? at least aliases (comments)

        val descriptorsToImport = collectDescriptorsToImport(file, true)

        val imports = prepareOptimizedImports(file, descriptorsToImport) ?: return null
        val intersect = imports.intersect(oldImports.map { it.importPath })
        return OptimizeInformation(
            add = imports.size - intersect.size,
            remove = oldImports.size - intersect.size,
            imports = imports
        )
    }

    private data class OptimizeInformation(val add: Int, val remove: Int, val imports: List<ImportPath>)

    private class CollectUsedDescriptorsVisitor(file: KtFile) : KtVisitorVoid(), PsiRecursiveVisitor {
        private val currentPackageName = file.packageFqName
        private val aliases: Map<FqName, List<Name>> = if (file.hasImportAlias()) {
            file.importDirectives
              .asSequence()
              .filter { !it.isAllUnder && it.alias != null }
              .mapNotNull { it.importPath }
              .groupBy(keySelector = { it.fqName }, valueTransform = { it.importedName as Name })
        } else {
            emptyMap()
        }

        private val descriptorsToImport = hashSetOf<OptimizedImportsBuilder.ImportableDescriptor>()
        private val namesToImport = hashMapOf<FqName, MutableSet<Name>>()
        private val abstractRefs = ArrayList<OptimizedImportsBuilder.AbstractReference>()
        private val unresolvedNames = hashSetOf<Name>()

        val data: OptimizedImportsBuilder.InputData
            get() = OptimizedImportsBuilder.InputData(
                descriptorsToImport,
                namesToImport,
                abstractRefs,
                unresolvedNames,
            )

        override fun visitElement(element: PsiElement) {
            ProgressIndicatorProvider.checkCanceled()
            element.acceptChildren(this)
        }

        override fun visitImportList(importList: KtImportList) {}

        override fun visitPackageDirective(directive: KtPackageDirective) {}

        override fun visitKtElement(element: KtElement) {
            super.visitKtElement(element)
            if (element is KtLabelReferenceExpression) return

            val references = element.references.ifEmpty { return }
            val bindingContext = element.safeAnalyzeNonSourceRootCode(BodyResolveMode.PARTIAL)
            val isResolved = hasResolvedDescriptor(element, bindingContext)

            for (reference in references) {
                if (reference !is KtReference) continue

                ProgressIndicatorProvider.checkCanceled()
                abstractRefs.add(AbstractReferenceImpl(reference))

                val names = reference.resolvesByNames
                if (!isResolved) {
                    unresolvedNames += names
                }

                for (target in reference.targets(bindingContext)) {
                    val importableDescriptor = target.getImportableDescriptor()
                    val importableFqName = target.importableFqName ?: continue
                    val parentFqName = importableFqName.parent()
                    if (target is PackageViewDescriptor && parentFqName == FqName.ROOT) continue // no need to import top-level packages

                    if (target !is PackageViewDescriptor && parentFqName == currentPackageName && (importableFqName !in aliases)) continue

                    ProgressIndicatorProvider.checkCanceled()
                    if (!reference.canBeResolvedViaImport(target, bindingContext)) continue

                    if (importableDescriptor.name in names && isAccessibleAsMember(importableDescriptor, element, bindingContext)) {
                        continue
                    }

                    val descriptorNames = (aliases[importableFqName].orEmpty() + importableFqName.shortName()).intersect(names)
                    namesToImport.getOrPut(importableFqName) { hashSetOf() } += descriptorNames
                    descriptorsToImport += OptimizedImportsBuilder.ImportableDescriptor(importableDescriptor, importableFqName)
                }
            }
        }

        private fun isAccessibleAsMember(target: DeclarationDescriptor, place: KtElement, bindingContext: BindingContext): Boolean {
            if (target.containingDeclaration !is ClassDescriptor) return false

            fun isInScope(scope: HierarchicalScope): Boolean {
                ProgressIndicatorProvider.checkCanceled()

                return when (target) {
                    is FunctionDescriptor ->
                        scope.findFunction(target.name, NoLookupLocation.FROM_IDE) { it == target } != null
                                && bindingContext[BindingContext.DEPRECATED_SHORT_NAME_ACCESS, place] != true

                    is PropertyDescriptor ->
                        scope.findVariable(target.name, NoLookupLocation.FROM_IDE) { it == target } != null
                                && bindingContext[BindingContext.DEPRECATED_SHORT_NAME_ACCESS, place] != true

                    is ClassDescriptor ->
                        scope.findClassifier(target.name, NoLookupLocation.FROM_IDE) == target
                                && bindingContext[BindingContext.DEPRECATED_SHORT_NAME_ACCESS, place] != true

                    else -> false
                }
            }

            val resolutionScope = place.getResolutionScope(bindingContext, place.getResolutionFacade())
            val noImportsScope = resolutionScope.replaceImportingScopes(null)

            if (isInScope(noImportsScope)) return true
            // classes not accessible through receivers, only their constructors
            return if (target is ClassDescriptor) false
            else resolutionScope.getImplicitReceiversHierarchy().any { isInScope(it.type.memberScope.memberScopeAsImportingScope()) }
        }

        private class AbstractReferenceImpl(private val reference: KtReference) : OptimizedImportsBuilder.AbstractReference {
            override val element: KtElement
                get() = reference.element

            override val dependsOnNames: Collection<Name>
                get() {
                    val resolvesByNames = reference.resolvesByNames
                    if (reference is KtInvokeFunctionReference) {
                        val additionalNames =
                            (reference.element.calleeExpression as? KtNameReferenceExpression)?.mainReference?.resolvesByNames

                        if (additionalNames != null) {
                            return resolvesByNames + additionalNames
                        }
                    }

                    return resolvesByNames
                }

            override fun resolve(bindingContext: BindingContext) = reference.resolveToDescriptors(bindingContext)

            @OptIn(KaImplementationDetail::class)
            override fun toString(): String {
                return if (reference is SyntheticPropertyAccessorReference && reference is KtFe10Reference) {
                    reference.toString().replace(
                        "Fe10SyntheticPropertyAccessorReference",
                        if (reference.getter) "Getter" else "Setter"
                    )
                }
                else reference.toString().replace("Fe10", "")
            }
        }
    }

    companion object {
        fun collectDescriptorsToImport(file: KtFile, inProgressBar: Boolean = false): OptimizedImportsBuilder.InputData {
            val progressIndicator = if (inProgressBar) ProgressIndicatorProvider.getInstance().progressIndicator else null
            progressIndicator?.text = KotlinBundle.message("import.optimizer.progress.indicator.text.collect.imports.for", file.name)
            progressIndicator?.isIndeterminate = true

            val visitor = CollectUsedDescriptorsVisitor(file)
            file.accept(visitor)
            return visitor.data
        }

        fun prepareOptimizedImports(file: KtFile, data: OptimizedImportsBuilder.InputData): List<ImportPath>? {
            val settings = file.kotlinCustomSettings
            val options = OptimizedImportsBuilder.Options(
                settings.NAME_COUNT_TO_USE_STAR_IMPORT,
                settings.NAME_COUNT_TO_USE_STAR_IMPORT_FOR_MEMBERS,
                isInPackagesToUseStarImport = { fqName -> fqName.asString() in settings.PACKAGES_TO_USE_STAR_IMPORTS }
            )

            return OptimizedImportsBuilder(file, data, options, file.languageVersionSettings.apiVersion).buildOptimizedImports()
        }

        private fun KtReference.targets(bindingContext: BindingContext): Collection<DeclarationDescriptor> {
            //class qualifiers that refer to companion objects should be considered (containing) class references
            return bindingContext[BindingContext.SHORT_REFERENCE_TO_COMPANION_OBJECT, element as? KtReferenceExpression]?.let { listOf(it) }
                ?: resolveToDescriptors(bindingContext)
        }

        private val DO_NOTHING = object : ImportOptimizer.CollectingInfoRunnable {
            override fun run() = Unit

            override fun getUserNotificationInfo() = KotlinBundle.message("import.optimizer.notification.text.unused.imports.not.found")
        }
    }
}

private fun hasResolvedDescriptor(element: KtElement, bindingContext: BindingContext): Boolean =
    if (element is KtCallElement)
        element.getResolvedCall(bindingContext) != null
    else
        element.mainReference?.resolveToDescriptors(bindingContext)?.let { descriptors ->
            descriptors.isNotEmpty() && descriptors.none { it is ErrorFunctionDescriptor }
        } == true
