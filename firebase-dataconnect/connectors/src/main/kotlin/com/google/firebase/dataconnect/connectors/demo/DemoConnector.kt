@file:Suppress("SpellCheckingInspection")

package com.google.firebase.dataconnect.connectors.demo

import com.google.firebase.FirebaseApp
import com.google.firebase.dataconnect.ConnectorConfig
import com.google.firebase.dataconnect.DataConnectSettings
import com.google.firebase.dataconnect.FirebaseDataConnect
import com.google.firebase.dataconnect.getInstance
import java.util.WeakHashMap

public interface DemoConnector {
  public val dataConnect: FirebaseDataConnect

  public val deleteFoo: DeleteFooMutation

  public val deleteFoosByBar: DeleteFoosByBarMutation

  public val getFooById: GetFooByIdQuery

  public val getFoosByBar: GetFoosByBarQuery

  public val getHardcodedFoo: GetHardcodedFooQuery

  public val getOneNonNullStringFieldById: GetOneNonNullStringFieldByIdQuery

  public val getOneNullableStringFieldById: GetOneNullableStringFieldByIdQuery

  public val getOneStringListFieldById: GetOneStringListFieldByIdQuery

  public val insertFoo: InsertFooMutation

  public val insertOneNonNullStringField: InsertOneNonNullStringFieldMutation

  public val insertOneNullableStringField: InsertOneNullableStringFieldMutation

  public val insertOneStringListField: InsertOneStringListFieldMutation

  public val updateFoo: UpdateFooMutation

  public val updateFoosByBar: UpdateFoosByBarMutation

  public val upsertFoo: UpsertFooMutation

  public val upsertHardcodedFoo: UpsertHardcodedFooMutation

  public companion object {
    @Suppress("MemberVisibilityCanBePrivate")
    public val config: ConnectorConfig =
      ConnectorConfig(
        connector = "demo",
        location = "us-central1",
        serviceId = "local",
      )

    public fun getInstance(dataConnect: FirebaseDataConnect): DemoConnector =
      synchronized(instances) { instances.getOrPut(dataConnect) { DemoConnectorImpl(dataConnect) } }

    private val instances = WeakHashMap<FirebaseDataConnect, DemoConnectorImpl>()
  }
}

public val DemoConnector.Companion.instance: DemoConnector
  get() = getInstance(FirebaseDataConnect.getInstance(config))

public fun DemoConnector.Companion.getInstance(
  settings: DataConnectSettings = DataConnectSettings()
): DemoConnector = getInstance(FirebaseDataConnect.getInstance(config, settings))

public fun DemoConnector.Companion.getInstance(
  app: FirebaseApp,
  settings: DataConnectSettings = DataConnectSettings()
): DemoConnector = getInstance(FirebaseDataConnect.getInstance(app, config, settings))

private class DemoConnectorImpl(override val dataConnect: FirebaseDataConnect) : DemoConnector {

  override val deleteFoo by lazy(LazyThreadSafetyMode.PUBLICATION) { DeleteFooMutationImpl(this) }

  override val deleteFoosByBar by
    lazy(LazyThreadSafetyMode.PUBLICATION) { DeleteFoosByBarMutationImpl(this) }

  override val getFooById by lazy(LazyThreadSafetyMode.PUBLICATION) { GetFooByIdQueryImpl(this) }

  override val getFoosByBar by
    lazy(LazyThreadSafetyMode.PUBLICATION) { GetFoosByBarQueryImpl(this) }

  override val getHardcodedFoo by
    lazy(LazyThreadSafetyMode.PUBLICATION) { GetHardcodedFooQueryImpl(this) }

  override val getOneNonNullStringFieldById by
    lazy(LazyThreadSafetyMode.PUBLICATION) { GetOneNonNullStringFieldByIdQueryImpl(this) }

  override val getOneNullableStringFieldById by
    lazy(LazyThreadSafetyMode.PUBLICATION) { GetOneNullableStringFieldByIdQueryImpl(this) }

  override val getOneStringListFieldById by
    lazy(LazyThreadSafetyMode.PUBLICATION) { GetOneStringListFieldByIdQueryImpl(this) }

  override val insertFoo by lazy(LazyThreadSafetyMode.PUBLICATION) { InsertFooMutationImpl(this) }

  override val insertOneNonNullStringField by
    lazy(LazyThreadSafetyMode.PUBLICATION) { InsertOneNonNullStringFieldMutationImpl(this) }

  override val insertOneNullableStringField by
    lazy(LazyThreadSafetyMode.PUBLICATION) { InsertOneNullableStringFieldMutationImpl(this) }

  override val insertOneStringListField by
    lazy(LazyThreadSafetyMode.PUBLICATION) { InsertOneStringListFieldMutationImpl(this) }

  override val updateFoo by lazy(LazyThreadSafetyMode.PUBLICATION) { UpdateFooMutationImpl(this) }

  override val updateFoosByBar by
    lazy(LazyThreadSafetyMode.PUBLICATION) { UpdateFoosByBarMutationImpl(this) }

  override val upsertFoo by lazy(LazyThreadSafetyMode.PUBLICATION) { UpsertFooMutationImpl(this) }

  override val upsertHardcodedFoo by
    lazy(LazyThreadSafetyMode.PUBLICATION) { UpsertHardcodedFooMutationImpl(this) }

  override fun toString() = "DemoConnectorImpl(dataConnect=$dataConnect)"
}

private class DeleteFooMutationImpl(override val connector: DemoConnectorImpl) : DeleteFooMutation {
  override fun toString() = "DeleteFooMutationImpl(connector=$connector)"
}

private class DeleteFoosByBarMutationImpl(override val connector: DemoConnectorImpl) :
  DeleteFoosByBarMutation {
  override fun toString() = "DeleteFoosByBarMutationImpl(connector=$connector)"
}

private class GetFooByIdQueryImpl(override val connector: DemoConnectorImpl) : GetFooByIdQuery {
  override fun toString() = "GetFooByIdQueryImpl(connector=$connector)"
}

private class GetFoosByBarQueryImpl(override val connector: DemoConnectorImpl) : GetFoosByBarQuery {
  override fun toString() = "GetFoosByBarQueryImpl(connector=$connector)"
}

private class GetHardcodedFooQueryImpl(override val connector: DemoConnectorImpl) :
  GetHardcodedFooQuery {
  override fun toString() = "GetHardcodedFooQueryImpl(connector=$connector)"
}

private class GetOneNonNullStringFieldByIdQueryImpl(override val connector: DemoConnectorImpl) :
  GetOneNonNullStringFieldByIdQuery {
  override fun toString() = "GetOneNonNullStringFieldByIdQueryImpl(connector=$connector)"
}

private class GetOneNullableStringFieldByIdQueryImpl(override val connector: DemoConnectorImpl) :
  GetOneNullableStringFieldByIdQuery {
  override fun toString() = "GetOneNullableStringFieldByIdQueryImpl(connector=$connector)"
}

private class GetOneStringListFieldByIdQueryImpl(override val connector: DemoConnectorImpl) :
  GetOneStringListFieldByIdQuery {
  override fun toString() = "GetOneStringListFieldByIdQueryImpl(connector=$connector)"
}

private class InsertFooMutationImpl(override val connector: DemoConnectorImpl) : InsertFooMutation {
  override fun toString() = "InsertFooMutationImpl(connector=$connector)"
}

private class InsertOneNonNullStringFieldMutationImpl(override val connector: DemoConnectorImpl) :
  InsertOneNonNullStringFieldMutation {
  override fun toString() = "InsertOneNonNullStringFieldMutationImpl(connector=$connector)"
}

private class InsertOneNullableStringFieldMutationImpl(override val connector: DemoConnectorImpl) :
  InsertOneNullableStringFieldMutation {
  override fun toString() = "InsertOneNullableStringFieldMutationImpl(connector=$connector)"
}

private class InsertOneStringListFieldMutationImpl(override val connector: DemoConnectorImpl) :
  InsertOneStringListFieldMutation {
  override fun toString() = "InsertOneStringListFieldMutationImpl(connector=$connector)"
}

private class UpdateFooMutationImpl(override val connector: DemoConnectorImpl) : UpdateFooMutation {
  override fun toString() = "UpdateFooMutationImpl(connector=$connector)"
}

private class UpdateFoosByBarMutationImpl(override val connector: DemoConnectorImpl) :
  UpdateFoosByBarMutation {
  override fun toString() = "UpdateFoosByBarMutationImpl(connector=$connector)"
}

private class UpsertFooMutationImpl(override val connector: DemoConnectorImpl) : UpsertFooMutation {
  override fun toString() = "UpsertFooMutationImpl(connector=$connector)"
}

private class UpsertHardcodedFooMutationImpl(override val connector: DemoConnectorImpl) :
  UpsertHardcodedFooMutation {
  override fun toString() = "UpsertHardcodedFooMutationImpl(connector=$connector)"
}

// The lines below are used by the code generator to ensure that this file is deleted if it is no
// longer needed. Any files in this directory that contain the lines below will be deleted by the
// code generator if the file is no longer needed. If, for some reason, you do _not_ want the code
// generator to delete this file, then remove the line below (and this comment too, if you want).

// FIREBASE_DATA_CONNECT_GENERATED_FILE MARKER 42da5e14-69b3-401b-a9f1-e407bee89a78
// FIREBASE_DATA_CONNECT_GENERATED_FILE CONNECTOR demo