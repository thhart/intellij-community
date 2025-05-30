// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.jetbrains.performancePlugin.utils

import com.jetbrains.performancePlugin.commands.CommandArguments
import com.sampullara.cli.Args

fun CommandArguments.parse(text: String)  {
  Args.parse(this, text.split("|").flatMap { it.split(" ", limit = 2) }.toTypedArray(), false)
}
