// Copyright 2000-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.jetbrains.python.sdk.poetry

import com.intellij.execution.ExecutionException
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.webcore.packaging.RepoPackage
import com.jetbrains.python.packaging.PyPIPackageUtil
import com.jetbrains.python.packaging.PyPackageManagerUI
import com.jetbrains.python.packaging.PyRequirementParser
import com.jetbrains.python.packaging.ui.PyPackageManagementService


/**
 *  This source code is edited by @koxudaxi Koudai Aono <koxudaxi@gmail.com>
 */

class PyPoetryPackageManagementService(project: Project, sdk: Sdk) : PyPackageManagementService(project, sdk) {
  override fun getAllRepositories(): List<String>? = null

  override fun canInstallToUser() = false

  override fun getAllPackages(): List<RepoPackage> {
    PyPIPackageUtil.INSTANCE.loadAdditionalPackages(poetrySources, false)
    return allPackagesCached
  }

  override fun reloadAllPackages(): List<RepoPackage> {
    PyPIPackageUtil.INSTANCE.loadAdditionalPackages(poetrySources, true)
    return allPackagesCached
  }

  override fun getAllPackagesCached(): List<RepoPackage> =
    PyPIPackageUtil.INSTANCE.getAdditionalPackages(poetrySources)

  override fun installPackage(repoPackage: RepoPackage,
                              version: String?,
                              forceUpgrade: Boolean,
                              extraOptions: String?,
                              listener: Listener,
                              installToUser: Boolean) {
    val ui = PyPackageManagerUI(project, sdk, object : PyPackageManagerUI.Listener {
      override fun started() {
        listener.operationStarted(repoPackage.name)
      }

      override fun finished(exceptions: List<ExecutionException?>?) {
        listener.operationFinished(repoPackage.name, toErrorDescription(exceptions, sdk))
      }
    })
    val requirement = when {
                        version != null -> PyRequirementParser.fromLine("${repoPackage.name}==$version")
                        else -> PyRequirementParser.fromLine(repoPackage.name)
                      } ?: return
    val extraArgs = extraOptions?.split(" +".toRegex()) ?: emptyList()
    ui.install(listOf(requirement), extraArgs)
  }

  /**
   * The URLs of package sources configured in the Pipfile.lock of the module associated with this SDK.
   */
  private val poetrySources: List<String>
    // TODO parse pyproject.toml for tool.poetry.source.url
    get() = listOf(POETRY_DEFAULT_SOURCE_URL)
}