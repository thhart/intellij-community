// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.workspaceModel.ide.legacyBridge

import com.intellij.openapi.components.impl.stores.ComponentStoreOwner
import com.intellij.openapi.components.impl.stores.IComponentStore
import com.intellij.openapi.module.impl.ModuleEx
import com.intellij.platform.workspace.jps.entities.ModuleId
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.VersionedEntityStorage
import com.intellij.platform.workspace.storage.url.VirtualFileUrl
import com.intellij.serviceContainer.PrecomputedExtensionModel
import org.jetbrains.annotations.ApiStatus
import java.nio.file.Path

@ApiStatus.Internal
interface ModuleBridge : ModuleEx, ComponentStoreOwner {
  val moduleEntityId: ModuleId

  /**
   * Entity store used by this module and related components like root manager.
   * It may change on module transition from modifiable module model to regular module in ModuleManager.
   */
  var entityStorage: VersionedEntityStorage

  /**
   * Specifies a diff where module related changes should be written (like root changes).
   * If it's null related changes should be written directly with updateProjectModel.
   * It may change on module transition from modifiable module model to regular module in ModuleManager.
   */
  var diff: MutableEntityStorage?

  /**
   * This is an internal function used by the platform to update the module instance when it's renamed.
   * To rename a module, use [com.intellij.openapi.module.ModifiableModuleModel.renameModule].
   */
  fun rename(newName: String, newModuleFileUrl: VirtualFileUrl?, notifyStorage: Boolean)

  fun onImlFileMoved(newModuleFileUrl: VirtualFileUrl)

  fun initServiceContainer(precomputedExtensionModel: PrecomputedExtensionModel)

  fun markContainerAsCreated()
}

@ApiStatus.Internal
interface ModuleStore : IComponentStore {
  fun setPath(path: Path, isNew: Boolean)
}