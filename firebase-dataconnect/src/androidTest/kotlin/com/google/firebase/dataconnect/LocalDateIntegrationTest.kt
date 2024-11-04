/*
 * Copyright 2024 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:OptIn(ExperimentalKotest::class)
@file:UseSerializers(UUIDSerializer::class)

package com.google.firebase.dataconnect

import com.google.firebase.dataconnect.serializers.UUIDSerializer
import com.google.firebase.dataconnect.testutil.DataConnectIntegrationTestBase
import com.google.firebase.dataconnect.testutil.NullableReference
import com.google.firebase.dataconnect.testutil.property.arbitrary.DataConnectArb
import com.google.firebase.dataconnect.testutil.property.arbitrary.EdgeCases
import com.google.firebase.dataconnect.testutil.property.arbitrary.dataConnect
import com.google.firebase.dataconnect.testutil.property.arbitrary.orNullableReference
import com.google.firebase.dataconnect.testutil.requestTimeAsDate
import com.google.firebase.dataconnect.testutil.shouldContainWithNonAbuttingText
import com.google.firebase.dataconnect.testutil.toTheeTenAbpJavaLocalDate
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.common.ExperimentalKotest
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.EdgeConfig
import io.kotest.property.PropTestConfig
import io.kotest.property.RandomSource
import io.kotest.property.Sample
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.next
import io.kotest.property.arbitrary.orNull
import io.kotest.property.arbitrary.withEdgecases
import io.kotest.property.asSample
import io.kotest.property.checkAll
import java.util.UUID
import kotlin.random.nextInt
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.serializer
import org.junit.Test

class LocalDateIntegrationTest : DataConnectIntegrationTestBase() {

  private val dataConnect: FirebaseDataConnect by lazy {
    val connectorConfig = testConnectorConfig.copy(connector = "demo")
    dataConnectFactory.newInstance(connectorConfig)
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////
  // Tests for inserting into and querying this table:
  // type DateNonNullable @table { value: Date!, tag: String }
  //////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  fun dateNonNullable_MutationVariable() =
    runTest(timeout = 1.minutes) {
      checkAll(propTestConfig, Arb.dataConnect.localDate()) { localDate ->
        val insertResult = nonNullableDate.insert(localDate)
        val queryResult = nonNullableDate.getByKey(insertResult.data.key)
        queryResult.data.item?.value shouldBe localDate
      }
    }

  @Test
  fun dateNonNullable_QueryVariable() =
    runTest(timeout = 1.minutes) {
      checkAll(propTestConfig, Arb.dataConnect.tag(), Arb.dataConnect.threeNonNullLocalDates()) {
        tag,
        localDates ->
        val (localDate1, localDate2, localDate3) = localDates
        val insertResult = nonNullableDate.insert3(tag, localDate1, localDate2, localDate3)
        val queryResult = nonNullableDate.getAllByTagAndValue(tag, localDates.selected)
        val matchingIds = localDates.idsMatchingSelected(insertResult)
        queryResult.data.items.map { it.id } shouldContainExactlyInAnyOrder matchingIds
      }
    }

  @Test
  fun dateNonNullable_NullMutationVariableShouldThrow() = runTest {
    val exception = shouldThrow<DataConnectException> { nonNullableDate.insert(null) }
    assertSoftly {
      exception.message shouldContainWithNonAbuttingText "\$value"
      exception.message shouldContainWithNonAbuttingText "is null"
    }
  }

  @Test
  fun dateNonNullable_NullQueryVariableShouldThrow() = runTest {
    val tag = Arb.dataConnect.tag().next(rs)
    val exception =
      shouldThrow<DataConnectException> { nonNullableDate.getAllByTagAndValue(tag, null) }
    assertSoftly {
      exception.message shouldContainWithNonAbuttingText "\$value"
      exception.message shouldContainWithNonAbuttingText "is null"
    }
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////
  // Tests for inserting into and querying this table:
  // type DateNullable @table { value: Date, tag: String }
  //////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  fun dateNullable_MutationVariable() =
    runTest(timeout = 1.minutes) {
      val localDates = Arb.dataConnect.localDate().orNull(nullProbability = 0.2)
      checkAll(propTestConfig, localDates) { localDate ->
        val insertResult = nullableDate.insert(localDate)
        val queryResult = nullableDate.getByKey(insertResult.data.key)
        queryResult.data.item?.value shouldBe localDate
      }
    }

  @Test
  fun dateNullable_QueryVariable() =
    runTest(timeout = 1.minutes) {
      checkAll(
        propTestConfig,
        Arb.dataConnect.tag(),
        Arb.dataConnect.threePossiblyNullLocalDates()
      ) { tag, localDates ->
        val (localDate1, localDate2, localDate3) = localDates
        val insertResult = nullableDate.insert3(tag, localDate1, localDate2, localDate3)
        val queryResult = nullableDate.getAllByTagAndValue(tag, localDates.selected)
        val matchingIds = localDates.idsMatchingSelected(insertResult)
        queryResult.data.items.map { it.id } shouldContainExactlyInAnyOrder matchingIds
      }
    }

  //////////////////////////////////////////////////////////////////////////////////////////////////
  // Tests for default non-nullable `Date` values.
  //////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  fun dateNonNullable_MutationVariableDefaults() = runTest {
    val insertResult = nonNullableDate.insertWithDefaults()
    val queryResult = nonNullableDate.getInsertedWithDefaultsByKey(insertResult.data.key)
    val item = withClue("queryResult.data.item") { queryResult.data.item.shouldNotBeNull() }

    assertSoftly {
      withClue(item) {
        withClue("valueWithVariableDefault") {
          item.valueWithVariableDefault shouldBe LocalDate(6904, 11, 30)
        }
        withClue("valueWithSchemaDefault") {
          item.valueWithSchemaDefault shouldBe LocalDate(2112, 1, 31)
        }
        withClue("epoch") { item.epoch shouldBe EdgeCases.dates.zero.localDate }
        withClue("requestTime") { item.requestTime1 shouldBe item.requestTime2 }
      }
    }

    withClue("requestTime validation") {
      val today = dataConnect.requestTimeAsDate().toTheeTenAbpJavaLocalDate()
      val yesterday = today.minusDays(1)
      val tomorrow = today.plusDays(1)
      val requestTime = item.requestTime1.toTheeTenAbpJavaLocalDate()
      requestTime.shouldBeIn(yesterday, today, tomorrow)
    }
  }

  @Test
  fun dateNonNullable_QueryVariableDefaults() =
    runTest(timeout = 1.minutes) {
      val defaultLocalDate = LocalDate(2692, 5, 21)
      val localDateArb = Arb.dataConnect.localDate().withEdgecases(defaultLocalDate)
      checkAll(
        propTestConfig,
        Arb.dataConnect.threeNonNullLocalDates(localDateArb),
        Arb.dataConnect.tag()
      ) { localDates, tag ->
        val (localDate1, localDate2, localDate3) = localDates
        val insertResult = nonNullableDate.insert3(tag, localDate1, localDate2, localDate3)
        val queryResult = nonNullableDate.getAllByTagAndValue(tag, defaultLocalDate)
        val matchingIds = localDates.idsMatching(insertResult, defaultLocalDate)
        queryResult.data.items.map { it.id } shouldContainExactlyInAnyOrder matchingIds
      }
    }

  //////////////////////////////////////////////////////////////////////////////////////////////////
  // Tests for default non-nullable `Date` values.
  //////////////////////////////////////////////////////////////////////////////////////////////////

  @Test
  fun dateNullable_MutationVariableDefaults() = runTest {
    val insertResult = nullableDate.insertWithDefaults()
    val queryResult = nullableDate.getInsertedWithDefaultsByKey(insertResult.data.key)
    val item = withClue("queryResult.data.item") { queryResult.data.item.shouldNotBeNull() }

    assertSoftly {
      withClue(item) {
        withClue("valueWithVariableDefault") {
          item.valueWithVariableDefault shouldBe LocalDate(8113, 2, 9)
        }
        withClue("valueWithVariableNullDefault") {
          item.valueWithVariableNullDefault.shouldBeNull()
        }
        withClue("valueWithSchemaDefault") {
          item.valueWithSchemaDefault shouldBe LocalDate(1921, 12, 2)
        }
        withClue("valueWithSchemaNullDefault") { item.valueWithSchemaNullDefault.shouldBeNull() }
        withClue("valueWithNoDefault") { item.valueWithNoDefault.shouldBeNull() }
        withClue("epoch") { item.epoch shouldBe EdgeCases.dates.zero.localDate }
        withClue("requestTime") { item.requestTime1 shouldBe item.requestTime2 }
      }
    }

    withClue("requestTime validation") {
      val today = dataConnect.requestTimeAsDate().toTheeTenAbpJavaLocalDate()
      val yesterday = today.minusDays(1)
      val tomorrow = today.plusDays(1)
      val requestTime = item.requestTime1.toTheeTenAbpJavaLocalDate()
      requestTime.shouldBeIn(yesterday, today, tomorrow)
    }
  }

  @Test
  fun dateNullable_QueryVariableDefaults() =
    runTest(timeout = 1.minutes) {
      val defaultLocalDate = LocalDate(1771, 10, 28)
      val localDateArb = Arb.dataConnect.localDate().withEdgecases(defaultLocalDate)
      checkAll(
        propTestConfig,
        Arb.dataConnect.threeNonNullLocalDates(localDateArb),
        Arb.dataConnect.tag()
      ) { localDates, tag ->
        val (localDate1, localDate2, localDate3) = localDates
        val insertResult = nullableDate.insert3(tag, localDate1, localDate2, localDate3)
        val queryResult = nullableDate.getAllByTagAndValue(tag, defaultLocalDate)
        val matchingIds = localDates.idsMatching(insertResult, defaultLocalDate)
        queryResult.data.items.map { it.id } shouldContainExactlyInAnyOrder matchingIds
      }
    }

  //////////////////////////////////////////////////////////////////////////////////////////////////
  // Helper methods and classes.
  //////////////////////////////////////////////////////////////////////////////////////////////////

  private data class ThreeLocalDates(
    val localDate1: LocalDate?,
    val localDate2: LocalDate?,
    val localDate3: LocalDate?,
    private val index: Int,
  ) {
    init {
      require(index in 0..2) { "invalid index: $index (error code shfwcz4j4w)" }
    }

    val all: List<LocalDate?>
      get() = listOf(localDate1, localDate2, localDate3)

    val selected =
      when (index) {
        0 -> localDate1
        1 -> localDate2
        2 -> localDate3
        else -> throw Exception("internal error: unknown index: $index")
      }

    fun idsMatchingSelected(data: MutationResult<ThreeKeysData, *>): List<UUID> =
      idsMatching(data, selected)

    fun idsMatching(data: MutationResult<ThreeKeysData, *>, localDate: LocalDate?): List<UUID> {
      val ids = listOf(data.data.key1, data.data.key2, data.data.key3).map { it.id }
      return ids.filterIndexed { index, _ -> all[index] == localDate }
    }
  }

  @Serializable private data class SingleKeyVariables(val key: Key)

  @Serializable private data class SingleKeyData(val key: Key)

  @Serializable
  private data class MultipleKeysData(val items: List<Item>) {
    @Serializable data class Item(val id: UUID)
  }

  @Serializable private data class ThreeKeysData(val key1: Key, val key2: Key, val key3: Key)

  @Serializable private data class InsertVariables(val value: LocalDate?)

  @Serializable
  private data class Insert3Variables(
    val tag: String,
    val value1: LocalDate?,
    val value2: LocalDate?,
    val value3: LocalDate?,
  )

  @Serializable private data class TagVariables(val tag: String)

  @Serializable private data class TagAndValueVariables(val tag: String, val value: LocalDate?)

  @Serializable
  private data class QueryData(val item: Item?) {
    @Serializable data class Item(val value: LocalDate?)
  }

  @Serializable
  private data class GetInsertedWithDefaultsByKeyQueryData(val item: Item?) {
    @Serializable
    data class Item(
      val valueWithVariableDefault: LocalDate,
      val valueWithVariableNullDefault: LocalDate?,
      val valueWithSchemaDefault: LocalDate,
      val valueWithSchemaNullDefault: LocalDate?,
      val valueWithNoDefault: LocalDate?,
      val epoch: LocalDate,
      val requestTime1: LocalDate,
      val requestTime2: LocalDate,
    )
  }

  @Serializable private data class Key(val id: UUID)

  /** Operations for querying and mutating the table that stores non-nullable Date scalar values. */
  private val nonNullableDate =
    Operations(
      getByKeyQueryName = "DateNonNullable_GetByKey",
      getAllByTagAndValueQueryName = "DateNonNullable_GetAllByTagAndValue",
      getAllByTagAndDefaultValueQueryName = "DateNonNullable_GetAllByTagAndDefaultValue",
      insertMutationName = "DateNonNullable_Insert",
      insert3MutationName = "DateNonNullable_Insert3",
      insertWithDefaultsMutationName = "DateNonNullableWithDefaults_Insert",
      getInsertedWithDefaultsByKeyQueryName = "DateNonNullableWithDefaults_GetByKey",
    )

  /** Operations for querying and mutating the table that stores nullable Date scalar values. */
  private val nullableDate =
    Operations(
      getByKeyQueryName = "DateNullable_GetByKey",
      getAllByTagAndValueQueryName = "DateNullable_GetAllByTagAndValue",
      getAllByTagAndDefaultValueQueryName = "DateNullable_GetAllByTagAndDefaultValue",
      insertMutationName = "DateNullable_Insert",
      insert3MutationName = "DateNullable_Insert3",
      insertWithDefaultsMutationName = "DateNullableWithDefaults_Insert",
      getInsertedWithDefaultsByKeyQueryName = "DateNullableWithDefaults_GetByKey",
    )

  private inner class Operations(
    getByKeyQueryName: String,
    getAllByTagAndValueQueryName: String,
    getAllByTagAndDefaultValueQueryName: String,
    insertMutationName: String,
    insert3MutationName: String,
    insertWithDefaultsMutationName: String,
    getInsertedWithDefaultsByKeyQueryName: String,
  ) {

    suspend fun insert(localDate: LocalDate?): MutationResult<SingleKeyData, InsertVariables> =
      insert(InsertVariables(localDate))

    suspend fun insert(variables: InsertVariables): MutationResult<SingleKeyData, InsertVariables> =
      mutations.insert(variables).execute()

    suspend fun insert3(
      tag: String,
      value1: LocalDate?,
      value2: LocalDate?,
      value3: LocalDate?,
    ): MutationResult<ThreeKeysData, Insert3Variables> =
      insert3(Insert3Variables(tag = tag, value1 = value1, value2 = value2, value3 = value3))

    suspend fun insert3(
      variables: Insert3Variables
    ): MutationResult<ThreeKeysData, Insert3Variables> = mutations.insert3(variables).execute()

    suspend fun getByKey(key: Key): QueryResult<QueryData, SingleKeyVariables> =
      getByKey(SingleKeyVariables(key))

    suspend fun getByKey(
      variables: SingleKeyVariables
    ): QueryResult<QueryData, SingleKeyVariables> = queries.getByKey(variables).execute()

    suspend fun getAllByTagAndValue(
      tag: String,
      value: LocalDate?
    ): QueryResult<MultipleKeysData, TagAndValueVariables> =
      getAllByTagAndValue(TagAndValueVariables(tag, value))

    suspend fun getAllByTagAndValue(
      variables: TagAndValueVariables
    ): QueryResult<MultipleKeysData, TagAndValueVariables> =
      queries.getAllByTagAndValue(variables).execute()

    suspend fun getAllByTagAndDefaultValue(
      tag: String
    ): QueryResult<MultipleKeysData, TagVariables> = getAllByTagAndDefaultValue(TagVariables(tag))

    suspend fun getAllByTagAndDefaultValue(
      variables: TagVariables
    ): QueryResult<MultipleKeysData, TagVariables> =
      queries.getAllByTagAndDefaultValue(variables).execute()

    suspend fun insertWithDefaults(): MutationResult<SingleKeyData, Unit> =
      mutations.insertWithDefaults().execute()

    suspend fun getInsertedWithDefaultsByKey(
      key: Key
    ): QueryResult<GetInsertedWithDefaultsByKeyQueryData, SingleKeyVariables> =
      getInsertedWithDefaultsByKey(SingleKeyVariables(key))

    suspend fun getInsertedWithDefaultsByKey(
      variables: SingleKeyVariables
    ): QueryResult<GetInsertedWithDefaultsByKeyQueryData, SingleKeyVariables> =
      queries.getInsertedWithDefaultsByKey(variables).execute()

    private val queries =
      object {
        fun getByKey(variables: SingleKeyVariables): QueryRef<QueryData, SingleKeyVariables> =
          dataConnect.query(
            getByKeyQueryName,
            variables,
            serializer(),
            serializer(),
          )

        fun getAllByTagAndValue(
          variables: TagAndValueVariables
        ): QueryRef<MultipleKeysData, TagAndValueVariables> =
          dataConnect.query(
            getAllByTagAndValueQueryName,
            variables,
            serializer(),
            serializer(),
          )

        fun getAllByTagAndDefaultValue(
          variables: TagVariables
        ): QueryRef<MultipleKeysData, TagVariables> =
          dataConnect.query(
            getAllByTagAndDefaultValueQueryName,
            variables,
            serializer(),
            serializer(),
          )

        fun getInsertedWithDefaultsByKey(
          variables: SingleKeyVariables
        ): QueryRef<GetInsertedWithDefaultsByKeyQueryData, SingleKeyVariables> =
          dataConnect.query(
            getInsertedWithDefaultsByKeyQueryName,
            variables,
            serializer(),
            serializer(),
          )
      }

    private val mutations =
      object {
        fun insert(variables: InsertVariables): MutationRef<SingleKeyData, InsertVariables> =
          dataConnect.mutation(
            insertMutationName,
            variables,
            serializer(),
            serializer(),
          )

        fun insert3(variables: Insert3Variables): MutationRef<ThreeKeysData, Insert3Variables> =
          dataConnect.mutation(
            insert3MutationName,
            variables,
            serializer(),
            serializer(),
          )

        fun insertWithDefaults(): MutationRef<SingleKeyData, Unit> =
          dataConnect.mutation(
            insertWithDefaultsMutationName,
            Unit,
            serializer(),
            serializer(),
          )
      }
  }

  private class ThreeLocalDatesArb(private val localDate: Arb<NullableReference<LocalDate>>) :
    Arb<ThreeLocalDates>() {
    override fun sample(rs: RandomSource): Sample<ThreeLocalDates> =
      nextThreeLocalDates(rs) { localDate.next(rs) }.asSample()

    override fun edgecase(rs: RandomSource): ThreeLocalDates {
      val result: ThreeLocalDates =
        nextThreeLocalDates(rs) {
          if (rs.random.nextBoolean()) {
            localDate.edgecase(rs)!!
          } else {
            localDate.next(rs)
          }
        }

      return when (val case = rs.random.nextInt(0..4)) {
        0 -> result
        1 -> result.copy(localDate2 = result.localDate1)
        2 -> result.copy(localDate3 = result.localDate1)
        3 -> result.copy(localDate3 = result.localDate2)
        4 -> result.copy(localDate2 = result.localDate1, localDate3 = result.localDate1)
        else -> throw Exception("should never get here: case=$case (error code yzqq7kw3eh)")
      }
    }

    private fun nextThreeLocalDates(
      rs: RandomSource,
      nextLocalDate: () -> NullableReference<LocalDate>
    ): ThreeLocalDates {
      val dates = List(3) { nextLocalDate().ref }
      val index = rs.random.nextInt(dates.indices)
      return ThreeLocalDates(dates[0], dates[1], dates[2], index)
    }
  }

  private companion object {
    val propTestConfig =
      PropTestConfig(
        iterations = 20,
        edgeConfig = EdgeConfig(edgecasesGenerationProbability = 0.5),
      )

    fun DataConnectArb.threeNonNullLocalDates(
      localDate: Arb<LocalDate> = localDate()
    ): Arb<ThreeLocalDates> = ThreeLocalDatesArb(localDate.map { NullableReference(it) })

    fun DataConnectArb.threePossiblyNullLocalDates(
      localDate: Arb<NullableReference<LocalDate>> =
        localDate().orNullableReference(nullProbability = 0.333)
    ): Arb<ThreeLocalDates> = ThreeLocalDatesArb(localDate)
  }
}
