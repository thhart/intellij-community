// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.workspaceModel.ide.impl.legacyBridge.facet

import com.intellij.facet.*
import com.intellij.facet.impl.FacetModelBase
import com.intellij.facet.impl.FacetUtil
import com.intellij.facet.impl.invalid.InvalidFacet
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectBundle
import com.intellij.openapi.project.isExternalStorageEnabled
import com.intellij.openapi.roots.ExternalProjectSystemRegistry
import com.intellij.openapi.util.JDOMUtil
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.workspace.jps.JpsImportedEntitySource
import com.intellij.platform.workspace.jps.entities.*
import com.intellij.platform.workspace.storage.*
import com.intellij.platform.workspace.storage.instrumentation.EntityStorageInstrumentationApi
import com.intellij.platform.workspace.storage.instrumentation.MutableEntityStorageInstrumentation
import com.intellij.workspaceModel.ide.impl.WorkspaceModelImpl
import com.intellij.workspaceModel.ide.impl.jps.serialization.BaseIdeSerializationContext
import com.intellij.workspaceModel.ide.legacyBridge.ModuleBridge
import com.intellij.workspaceModel.ide.legacyBridge.WorkspaceFacetContributor
import com.intellij.workspaceModel.ide.toExternalSource
import org.jetbrains.jps.model.serialization.facet.FacetState

class FacetManagerBridge(module: Module) : FacetManagerBase() {
  internal val module: ModuleBridge = module as ModuleBridge
  internal val model: FacetModelBridge = FacetModelBridge(this.module)

  private fun isThisModule(moduleEntity: ModuleEntity) = moduleEntity.name == module.name

  override fun checkConsistency() {
    val entityTypeToFacetContributor = WorkspaceFacetContributor.EP_NAME.extensionList.associateBy { it.rootEntityType }
    val facetRelatedEntities = entityTypeToFacetContributor
      .asSequence()
      .flatMap { module.entityStorage.current.entities(it.key) }
      .filter { entity ->
        val facetContributor = entityTypeToFacetContributor[entity.getEntityInterface()]!!
        isThisModule(facetContributor.getParentModuleEntity(entity))
      }
      .toList()
    model.checkConsistency(facetRelatedEntities, entityTypeToFacetContributor)
  }

  @OptIn(EntityStorageInstrumentationApi::class)
  override fun facetConfigurationChanged(facet: Facet<*>) {
    if (facet is FacetBridge<*, *>) {
      runWriteAction {
        val mutableEntityStorage: MutableEntityStorage = module.diff
                                                         ?: WorkspaceModel.getInstance(module.project).currentSnapshot.toBuilder()
        facet.updateInStorage(mutableEntityStorage)
        if (module.diff == null) {
          if ((mutableEntityStorage as MutableEntityStorageInstrumentation).hasChanges()) {
            WorkspaceModel.getInstance(module.project).updateProjectModel("Update facet configuration") { it.applyChangesFrom(mutableEntityStorage) }
          }
        }
      }
    } else {
      val facetEntity = model.getEntity(facet)
      if (facetEntity != null) {
        val facetConfigurationXml = FacetUtil.saveFacetConfiguration(facet)?.let { JDOMUtil.write(it) }
        if (facetConfigurationXml != facetEntity.configurationXmlTag) {
          runWriteAction {
            val change: FacetEntity.Builder.() -> Unit = { this.configurationXmlTag = facetConfigurationXml }
            module.diff?.modifyFacetEntity(facetEntity, change) ?: WorkspaceModel.getInstance(module.project)
              .updateProjectModel("Update facet configuration (not bridge)") { it.modifyFacetEntity(facetEntity, change) }
          }
        }
      }
    }
    super.facetConfigurationChanged(facet)
  }

  override fun getModel(): FacetModel = model
  override fun getModule(): Module = module
  override fun createModifiableModel(): ModifiableFacetModel {
    return createModifiableModel(module.entityStorage.current.toSnapshot().toBuilder())
  }

  fun createModifiableModel(diff: MutableEntityStorage): ModifiableFacetModel {
    return ModifiableFacetModelBridgeImpl(module.entityStorage.current, diff, module, this)
  }

  companion object {
    internal fun <F : Facet<C>, C : FacetConfiguration> createFacetFromStateRaw(module: Module, type: FacetType<F, C>,
                                                                                state: FacetState, underlyingFacet: Facet<*>?): F {
      val configuration: C = type.createDefaultConfiguration()
      val config = state.configuration
      FacetUtil.loadFacetConfiguration(configuration, config)
      val name = state.name
      val facet: F = createFacet(module, type, name, configuration, underlyingFacet)
      @Suppress("DEPRECATION")
      if (facet is com.intellij.openapi.util.JDOMExternalizable && config != null) {
        facet.readExternal(config)
      }
      val externalSystemId = state.externalSystemId
      if (externalSystemId != null) {
        facet.externalSource = ExternalProjectSystemRegistry.getInstance().getSourceById(externalSystemId)
      }
      return facet
    }

    internal fun saveFacetConfiguration(facet: Facet<*>): FacetState? {
      val facetState = createFacetState(facet, facet.module.project)
      if (facet !is InvalidFacet) {
        val config = FacetUtil.saveFacetConfiguration(facet) ?: return null
        facetState.configuration = config
      }
      return facetState
    }

    private fun createFacetState(facet: Facet<*>, project: Project): FacetState {
      if (facet is InvalidFacet) {
        return facet.configuration.facetState
      }
      else {
        val facetState = FacetState()
        val externalSource = facet.externalSource
        if (externalSource != null && project.isExternalStorageEnabled) {
          //set this attribute only if such facets will be stored separately, otherwise we will get modified *.iml files
          facetState.externalSystemId = externalSource.id
        }
        facetState.facetType = facet.type.stringId
        facetState.name = facet.name
        return facetState
      }
    }
  }
}

class FacetModelBridge(private val moduleBridge: ModuleBridge) : FacetModelBase() {

  init {
    // Initialize facet bridges after loading from cache
    val moduleEntity = (moduleBridge.diff ?: moduleBridge.entityStorage.current).resolve(moduleBridge.moduleEntityId)
                       ?: error("Module entity should be available")
    val facetTypeToSerializer = BaseIdeSerializationContext.CUSTOM_FACET_RELATED_ENTITY_SERIALIZER_EP.extensionList.associateBy { FacetEntityTypeId(it.supportedFacetType) }
    val facetMapping = facetMapping()
    val mappings = ArrayList<Pair<WorkspaceEntity, Facet<*>>>()
    for (facetContributor in WorkspaceFacetContributor.EP_NAME.extensionList) {
      if (facetContributor.rootEntityType != FacetEntity::class.java) {
        facetContributor.getRootEntitiesByModuleEntity(moduleEntity).forEach {
          if (facetMapping.getDataByEntity(it) == null) {
            mappings.add(it to facetContributor.createFacetFromEntity(it, moduleBridge))
          }
        }
      }
      else {
        for (entity in moduleEntity.facets.filter { !facetTypeToSerializer.containsKey(it.typeId) }) {
          fun initFacet(entity: FacetEntity): Facet<*> {
            val under = entity.underlyingFacet?.let { initFacet(it) }
            var existingFacet = facetMapping().getDataByEntity(entity)
            if (existingFacet == null) {
              existingFacet = createFacet(entity, under)
              mappings.add(entity to existingFacet)
            }
            return existingFacet
          }

          initFacet(entity)
        }
      }
    }

    if (mappings.isNotEmpty()) {
      updateDiffOrStorage(mappings)
    }
  }

  override fun getAllFacets(): Array<Facet<*>> {
    val moduleEntity = (moduleBridge.diff ?: moduleBridge.entityStorage.current).resolve(moduleBridge.moduleEntityId)
    if (moduleEntity == null) {
      logger<FacetModelBridge>().error("Cannot resolve module entity ${moduleBridge.moduleEntityId}")
      return emptyArray()
    }

    val facetEntities = mutableListOf<WorkspaceEntity>()
    facetEntities.addAll(moduleEntity.facets)
    for (it in WorkspaceFacetContributor.EP_NAME.extensionList) {
      if (it.rootEntityType != FacetEntity::class.java) {
        facetEntities.addAll(it.getRootEntitiesByModuleEntity(moduleEntity))
      }
    }
    return facetEntities.mapNotNull { facetMapping().getDataByEntity(it) }.toTypedArray()
  }

  internal fun getFacet(entity: FacetEntity): Facet<*>? = facetMapping().getDataByEntity(entity)

  internal fun getEntity(facet: Facet<*>): FacetEntity? = facetMapping().getEntities(facet).singleOrNull() as? FacetEntity

  internal fun createFacet(entity: FacetEntity, underlyingFacet: Facet<*>?): Facet<*> {
    val registry = FacetTypeRegistry.getInstance()
    val facetType = registry.findFacetType(entity.typeId.name)
    if (facetType == null) {
      return FacetManagerBase.createInvalidFacet(moduleBridge, FacetState().apply {
        name = entity.name
        setFacetType(entity.typeId.name)
        configuration = entity.configurationXmlTag?.let { JDOMUtil.load(it) }
      }, underlyingFacet, ProjectBundle.message("error.message.unknown.facet.type.0", entity.typeId.name), true, !PluginManagerCore.isDisabled(PluginManagerCore.ULTIMATE_PLUGIN_ID))
    }

    val configuration = facetType.createDefaultConfiguration()
    val configurationXmlTag = entity.configurationXmlTag
    val loadedConfiguration = configurationXmlTag?.let { JDOMUtil.load(it) }
    if (loadedConfiguration != null) {
      FacetUtil.loadFacetConfiguration(configuration, loadedConfiguration)
    }
    val facet = facetType.createFacet(moduleBridge, entity.name, configuration, underlyingFacet)
    facet.externalSource = (entity.entitySource as? JpsImportedEntitySource)?.toExternalSource()

    // JDOM facets should be additionally read
    @Suppress("DEPRECATION")
    if (loadedConfiguration != null && facet is com.intellij.openapi.util.JDOMExternalizable) {
      facet.readExternal(loadedConfiguration)
    }
    return facet
  }

  public override fun facetsChanged() {
    super.facetsChanged()
  }

  fun checkConsistency(facetRelatedEntities: List<ModuleSettingsFacetBridgeEntity>,
                       entityTypeToFacetContributor: Map<Class<ModuleSettingsFacetBridgeEntity>, WorkspaceFacetContributor<ModuleSettingsFacetBridgeEntity>>) {
    val facetEntitiesSet = facetRelatedEntities.toHashSet()
    for (entity in facetRelatedEntities) {
      val facet = facetMapping().getDataByEntity(entity)
      val facetName = entity.name
      if (facet == null) {
        throw IllegalStateException("No facet registered for $entity (name = $facetName)")
      }
      if (facet.name != facetName) {
        throw IllegalStateException("Different name")
      }
      val entityFromMapping = facetMapping().getEntities(facet).single() as FacetEntity
      val moduleEntity = entityFromMapping.module
      val facetsFromStorage = entityTypeToFacetContributor.values.filter { it.rootEntityType != FacetEntity::class.java }
        .flatMap { it.getRootEntitiesByModuleEntity(moduleEntity) }
        .toHashSet()
      facetsFromStorage.addAll(moduleEntity.facets.toSet())
      if (facetsFromStorage != facetEntitiesSet) {
        throw IllegalStateException("Different set of facets from $entity storage: expected $facetRelatedEntities but was $facetsFromStorage")
      }
    }
    val usedStore = moduleBridge.diff ?: moduleBridge.entityStorage.current
    val resolvedModuleEntity = usedStore.resolve(moduleBridge.moduleEntityId)!!
    val mappedFacets = entityTypeToFacetContributor.values.filter { it.rootEntityType != FacetEntity::class.java }
      .flatMap { it.getRootEntitiesByModuleEntity(resolvedModuleEntity) }
      .toMutableSet()
    mappedFacets.addAll(resolvedModuleEntity.facets.toSet())
    mappedFacets.removeAll(facetEntitiesSet)
    val staleEntity = mappedFacets.firstOrNull()
    if (staleEntity != null) {
      val facetName = staleEntity.name
      throw IllegalStateException("Stale entity $staleEntity (name = $facetName) in the mapping")
    }
  }

  private fun facetMapping(): ExternalEntityMapping<Facet<*>> {
    return moduleBridge.diff?.facetMapping() ?: moduleBridge.entityStorage.current.facetMapping()
  }

  private fun updateDiffOrStorage(mappings: List<Pair<WorkspaceEntity, Facet<*>>>) {
    val diff = moduleBridge.diff
    if (diff != null) {
      synchronized(diff) {
        mergeMappings(mappings, mutableFacetMapping(diff))
      }
    }
    else {
      (WorkspaceModel.getInstance(moduleBridge.project) as WorkspaceModelImpl).updateProjectModelSilent("Facet manager update storage") {
        mergeMappings(mappings, mutableFacetMapping(it))
      }
    }
  }

  companion object {
    private val FACET_BRIDGE_MAPPING_ID = ExternalMappingKey.create<Facet<*>>("intellij.facets.bridge")

    private fun mergeMappings(mappings: List<Pair<WorkspaceEntity, Facet<*>>>, mapping: MutableExternalEntityMapping<Facet<*>>) {
      for ((entity, data) in mappings) {
        mapping.addIfAbsent(entity, data)
      }
    }

    internal fun EntityStorage.facetMapping(): ExternalEntityMapping<Facet<*>> {
      return this.getExternalMapping(FACET_BRIDGE_MAPPING_ID)
    }

    internal fun mutableFacetMapping(mutableEntityStorage: MutableEntityStorage): MutableExternalEntityMapping<Facet<*>> {
      return mutableEntityStorage.getMutableExternalMapping(FACET_BRIDGE_MAPPING_ID)
    }
  }
}

private fun EntityStorage.toSnapshot(): ImmutableEntityStorage {
  return when (this) {
    is ImmutableEntityStorage -> this
    is MutableEntityStorage -> this.toSnapshot()
    else -> error("Unexpected storage: $this")
  }
}
