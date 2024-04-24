package com.google.firebase.dataconnect.querymgr

import com.google.firebase.dataconnect.core.Logger
import com.google.firebase.dataconnect.core.debug
import com.google.firebase.dataconnect.util.ReferenceCountedSet

internal class TypedActiveQueries(val activeQuery: ActiveQuery, parentLogger: Logger) :
  ReferenceCountedSet<TypedActiveQueryKey<*>, TypedActiveQuery<*>>() {

  private val logger =
    Logger("TypedActiveQueries").apply { debug { "Created by ${parentLogger.nameWithId}" } }

  override fun valueForKey(key: TypedActiveQueryKey<*>) =
    TypedActiveQuery(
      activeQuery = activeQuery,
      dataDeserializer = key.dataDeserializer,
      logger = logger,
    )

  override fun onAllocate(entry: Entry<TypedActiveQueryKey<*>, TypedActiveQuery<*>>) {
    logger.debug(
      "Allocated ${entry.value.logger.nameWithId} (dataDeserializer=${entry.key.dataDeserializer})"
    )
  }

  override fun onFree(entry: Entry<TypedActiveQueryKey<*>, TypedActiveQuery<*>>) {
    logger.debug("Deallocated ${entry.value.logger.nameWithId}")
  }
}