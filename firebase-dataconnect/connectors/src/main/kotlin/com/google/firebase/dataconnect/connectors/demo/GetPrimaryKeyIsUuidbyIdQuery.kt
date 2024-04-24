@file:Suppress("SpellCheckingInspection")
@file:UseSerializers(DateSerializer::class, UUIDSerializer::class, TimestampSerializer::class)

package com.google.firebase.dataconnect.connectors.demo

import com.google.firebase.dataconnect.QueryRef
import com.google.firebase.dataconnect.QueryResult
import com.google.firebase.dataconnect.QuerySubscriptionResult
import com.google.firebase.dataconnect.generated.GeneratedQuery
import com.google.firebase.dataconnect.serializers.DateSerializer
import com.google.firebase.dataconnect.serializers.TimestampSerializer
import com.google.firebase.dataconnect.serializers.UUIDSerializer
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.serializer

public interface GetPrimaryKeyIsUuidbyIdQuery :
  GeneratedQuery<
    DemoConnector, GetPrimaryKeyIsUuidbyIdQuery.Data, GetPrimaryKeyIsUuidbyIdQuery.Variables
  > {

  @Serializable public data class Variables(val key: PrimaryKeyIsUuidKey)

  @Serializable
  public data class Data(val primaryKeyIsUUID: PrimaryKeyIsUuid?) {

    @Serializable public data class PrimaryKeyIsUuid(val id: java.util.UUID, val value: String)
  }

  public companion object {
    @Suppress("ConstPropertyName")
    public const val operationName: String = "GetPrimaryKeyIsUUIDById"
    public val dataDeserializer: DeserializationStrategy<Data> = serializer()
    public val variablesSerializer: SerializationStrategy<Variables> = serializer()
  }
}

public fun GetPrimaryKeyIsUuidbyIdQuery.ref(
  key: PrimaryKeyIsUuidKey
): QueryRef<GetPrimaryKeyIsUuidbyIdQuery.Data, GetPrimaryKeyIsUuidbyIdQuery.Variables> =
  ref(GetPrimaryKeyIsUuidbyIdQuery.Variables(key = key))

public suspend fun GetPrimaryKeyIsUuidbyIdQuery.execute(
  key: PrimaryKeyIsUuidKey
): QueryResult<GetPrimaryKeyIsUuidbyIdQuery.Data, GetPrimaryKeyIsUuidbyIdQuery.Variables> =
  ref(key = key).execute()

public fun GetPrimaryKeyIsUuidbyIdQuery.flow(
  key: PrimaryKeyIsUuidKey
): Flow<
  QuerySubscriptionResult<GetPrimaryKeyIsUuidbyIdQuery.Data, GetPrimaryKeyIsUuidbyIdQuery.Variables>
> = ref(key = key).subscribe().flow

// The lines below are used by the code generator to ensure that this file is deleted if it is no
// longer needed. Any files in this directory that contain the lines below will be deleted by the
// code generator if the file is no longer needed. If, for some reason, you do _not_ want the code
// generator to delete this file, then remove the line below (and this comment too, if you want).

// FIREBASE_DATA_CONNECT_GENERATED_FILE MARKER 42da5e14-69b3-401b-a9f1-e407bee89a78
// FIREBASE_DATA_CONNECT_GENERATED_FILE CONNECTOR demo