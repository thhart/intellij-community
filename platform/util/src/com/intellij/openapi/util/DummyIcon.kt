// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.openapi.util

import com.intellij.ui.icons.IconPathProvider
import org.jetbrains.annotations.ApiStatus
import javax.swing.Icon

@ApiStatus.Internal
interface DummyIcon : Icon, IconPathProvider
