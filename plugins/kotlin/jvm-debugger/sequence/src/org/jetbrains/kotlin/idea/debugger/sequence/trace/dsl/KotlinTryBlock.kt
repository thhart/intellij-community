// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package org.jetbrains.kotlin.idea.debugger.sequence.trace.dsl

import com.intellij.debugger.streams.core.trace.dsl.CodeBlock
import com.intellij.debugger.streams.core.trace.dsl.StatementFactory
import com.intellij.debugger.streams.core.trace.dsl.impl.common.TryBlockBase

class KotlinTryBlock(private val block: CodeBlock, statementFactory: StatementFactory) : TryBlockBase(statementFactory) {
    override fun toCode(indent: Int): String {
        val descriptor = myCatchDescriptor ?: error("catch block must be specified")
        return "try {\n".withIndent(indent) +
                block.toCode(indent + 1) +
                "} catch(${descriptor.variable.name} : ${descriptor.variable.type.variableTypeName}) {\n" +
                descriptor.block.toCode(indent + 1) +
                "}".withIndent(indent)
    }
}