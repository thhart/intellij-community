// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.kotlin.idea.refactoring.suggested

import com.intellij.psi.PsiElement
import com.intellij.refactoring.suggested.SuggestedChangeSignatureData
import com.intellij.refactoring.suggested.SuggestedRefactoringExecution
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.refactoring.changeSignature.*
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.types.isError
import org.jetbrains.kotlin.utils.addIfNotNull

class KotlinSuggestedRefactoringExecution(
    refactoringSupport: KotlinSuggestedRefactoringSupportBase
) : SuggestedRefactoringExecution(refactoringSupport) {

    private data class PrepareChangeSignatureResult(
        val returnTypeInfo: KotlinTypeInfo,
        val parameterTypeInfos: List<KotlinTypeInfo>
    )

    override fun prepareChangeSignature(data: SuggestedChangeSignatureData): Any {
        val declaration = data.declaration as KtCallableDeclaration
        val descriptorWithNewSignature = declaration.resolveToDescriptorIfAny() as CallableDescriptor
        val newSignature = data.newSignature

        val returnType = descriptorWithNewSignature.returnType
        val returnTypeInfo = if (returnType != null && !returnType.isError) {
            KotlinTypeInfo(type = returnType, isCovariant = true)
        } else {
            KotlinTypeInfo(text = newSignature.type, isCovariant = true)
        }

        require(descriptorWithNewSignature.valueParameters.size == newSignature.parameters.size) {
            "Number of parameters of newSignature is ${newSignature.parameters.size} and of the descriptor is ${descriptorWithNewSignature.valueParameters.size}"
        }

        val parameterTypeInfos = descriptorWithNewSignature.valueParameters.zip(newSignature.parameters)
            .map { (parameterDescriptor, parameterData) ->
                val type = parameterDescriptor.varargElementType ?: parameterDescriptor.type
                if (!type.isError) {
                    KotlinTypeInfo(type = type, isCovariant = true)
                } else {
                    KotlinTypeInfo(text = parameterData.type, isCovariant = true)
                }
            }

        return PrepareChangeSignatureResult(returnTypeInfo, parameterTypeInfos)
    }

    override fun performChangeSignature(
        data: SuggestedChangeSignatureData,
        newParameterValues: List<NewParameterValue>,
        preparedData: Any?
    ) {
        val (returnTypeInfo, parameterTypeInfos) = preparedData as PrepareChangeSignatureResult

        val declaration = data.declaration as KtDeclaration
        val descriptor = declaration.resolveToDescriptorIfAny() as CallableDescriptor
        val project = declaration.project

        val configuration = object : KotlinChangeSignatureConfiguration {
            override fun isPerformSilently(affectedFunctions: Collection<PsiElement>) = true
        }

        val changeSignature = KotlinChangeSignature(project, null, descriptor, configuration, declaration, null)
        val methodDescriptor = changeSignature.adjustDescriptor(listOf(descriptor))!!

        val parameters = mutableListOf<KotlinParameterInfo>()
        var newParameterValueIndex = 0

        val receiver: KotlinParameterInfo? = data.newSignature.receiverType?.let { newReceiverType ->
            val newTypeInfo = KotlinTypeInfo(text = newReceiverType, isCovariant = false)
            if (data.oldSignature.receiverType != null) {
                methodDescriptor.receiver!!.apply { currentTypeInfo = newTypeInfo }
            } else {
                KotlinParameterInfo(descriptor, -1, "", newTypeInfo).apply {
                    setNewParameterValue(newParameterValues[newParameterValueIndex++])
                }
            }
        }

        parameters.addIfNotNull(receiver)

        val psiFactory = KtPsiFactory(project)

        for ((index, parameter) in data.newSignature.parameters.withIndex()) {
            val initialIndex = data.oldSignature.parameterById(parameter.id)
                ?.let { data.oldSignature.parameterIndex(it) }

            val defaultValue = parameter.defaultValue?.let { psiFactory.createExpression(it) }

            val modifierList = parameter.modifiers
                .takeIf { it.isNotEmpty() }
                ?.let { psiFactory.createModifierList(it) }

            val parameterInfo = if (initialIndex == null) {
                KotlinParameterInfo(
                    callableDescriptor = descriptor,
                    name = parameter.name,
                    originalTypeInfo = parameterTypeInfos[index],
                    modifierList = modifierList,
                ).apply {
                    setDefaultValueForParameter(defaultValue)
                    // a few last added parameters may be missing in newParameterValues
                    // because they have default values and we did not offer to enter value for them
                    if (newParameterValueIndex < newParameterValues.size) {
                        setNewParameterValue(newParameterValues[newParameterValueIndex++])
                    }
                }
            } else {
                KotlinParameterInfo(
                    callableDescriptor = descriptor,
                    originalIndex = initialIndex,
                    name = parameter.name,
                    originalTypeInfo = methodDescriptor.parameters[initialIndex].originalTypeInfo,
                    modifierList = modifierList,
                ).apply {
                    setDefaultValueForParameter(defaultValue)
                    currentTypeInfo = parameterTypeInfos[index]
                }
            }
            parameters.add(parameterInfo)
        }

        val changeInfo = KotlinChangeInfo(
            methodDescriptor,
            context = declaration,
            name = data.newSignature.name,
            newReturnTypeInfo = returnTypeInfo,
            parameterInfos = parameters,
            receiver = receiver,
            checkUsedParameters = true,
        )

        val processor = KotlinChangeSignatureProcessor(project, changeInfo, "")
        processor.run()
    }

    private fun KotlinParameterInfo.setNewParameterValue(value: NewParameterValue) {
        when (value) {
            is NewParameterValue.AnyVariable -> isUseAnySingleVariable = true
            is NewParameterValue.Expression -> defaultValueForCall = value.expression as KtExpression
            is NewParameterValue.None -> defaultValueForCall = null
        }
    }
}