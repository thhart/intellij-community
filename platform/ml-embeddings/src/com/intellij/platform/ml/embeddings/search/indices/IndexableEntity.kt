package com.intellij.platform.ml.embeddings.search.indices

interface IndexableEntity {
  val id: EntityId
  val indexableRepresentation: String
}