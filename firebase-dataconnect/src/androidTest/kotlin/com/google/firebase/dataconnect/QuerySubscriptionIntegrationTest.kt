// Copyright 2023 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

@file:OptIn(FlowPreview::class)

package com.google.firebase.dataconnect

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Subject
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.google.firebase.dataconnect.testutil.DataConnectLogLevelRule
import com.google.firebase.dataconnect.testutil.TestDataConnectFactory
import com.google.firebase.dataconnect.testutil.schemas.PersonSchema
import com.google.firebase.dataconnect.testutil.schemas.PersonSchema.Companion.newPersonSchema
import com.google.firebase.dataconnect.testutil.schemas.PersonSchema.CreatePersonMutation.execute
import com.google.firebase.dataconnect.testutil.schemas.PersonSchema.GetPersonQuery.subscribe
import com.google.firebase.dataconnect.testutil.schemas.PersonSchema.UpdatePersonMutation.execute
import java.util.concurrent.Executors
import kotlin.math.max
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class QuerySubscriptionIntegrationTest {

  @get:Rule val dataConnectFactory = TestDataConnectFactory()
  @get:Rule val dataConnectLogLevelRule = DataConnectLogLevelRule()

  private lateinit var schema: PersonSchema

  @Before
  fun initializePersonSchema() {
    schema = dataConnectFactory.newPersonSchema()
    runBlocking { schema.installEmulatorSchema() }
  }

  @Test
  fun lastResult_should_be_null_on_new_instance() {
    val querySubscription = schema.getPerson.subscribe(id = "42")
    assertThat(querySubscription.lastResult).isNull()
  }

  @Test
  fun lastResult_should_be_equal_to_the_last_collected_result(): Unit = runBlocking {
    schema.createPerson.execute(id = "TestId", name = "TestPerson", age = 42)
    val querySubscription = schema.getPerson.subscribe(id = "42")
    val result = querySubscription.flow.timeout(2000.seconds).first()
    assertThat(querySubscription.lastResult).isEqualTo(result)
  }

  @Test
  fun reload_should_notify_collecting_flows(): Unit = runBlocking {
    schema.createPerson.execute(id = "TestId12345", name = "Name0", age = 10000)
    val querySubscription = schema.getPerson.subscribe(id = "TestId12345")

    withTimeout(2.seconds) {
      val resultsChannel =
        Channel<
          DataConnectResult<PersonSchema.GetPersonQuery.Variables, PersonSchema.GetPersonQuery.Data>
        >(
          capacity = Channel.UNLIMITED
        )
      val collectJob = launch { querySubscription.flow.collect(resultsChannel::send) }

      val result1 = resultsChannel.receive()
      assertThat(result1.data.person).isEqualToGetPersonQueryResult(name = "Name0", age = 10000)

      schema.updatePerson.execute(id = "TestId12345", name = "Name1", age = 10001)

      querySubscription.reload()
      val result2 = resultsChannel.receive()
      assertThat(result2.data.person).isEqualToGetPersonQueryResult(name = "Name1", age = 10001)

      collectJob.cancel()
    }
  }

  @Test
  fun flow_collect_should_get_immediately_invoked_with_last_result(): Unit = runBlocking {
    schema.createPerson.execute(id = "TestId12345", name = "TestName", age = 10000)
    val querySubscription = schema.getPerson.subscribe(id = "TestId12345")

    withTimeout(2.seconds) {
      val result1 = querySubscription.flow.first().data.person
      assertThat(result1).isEqualToGetPersonQueryResult(name = "TestName", age = 10000)

      schema.updatePerson.execute(id = "TestId12345", name = "TestName2", age = 10002)

      val result2 = querySubscription.flow.first().data.person
      assertThat(result2).isEqualToGetPersonQueryResult(name = "TestName", age = 10000)
    }
  }

  @Test
  fun slow_flows_do_not_block_fast_flows(): Unit = runBlocking {
    schema.createPerson.execute(id = "TestId12345", name = "TestName", age = 10000)
    val querySubscription = schema.getPerson.subscribe(id = "TestId12345")

    withTimeout(2.seconds) {
      val slowFlowJob = launch {
        querySubscription.flow.collect { delay(Integer.MAX_VALUE.seconds) }
      }

      repeat(5) {
        assertWithMessage("fast flow retrieval iteration $it")
          .that(querySubscription.flow.first().data.person)
          .isEqualToGetPersonQueryResult(name = "TestName", age = 10000)
      }

      slowFlowJob.cancel()
    }
  }

  @Test
  fun reload_delivers_result_to_all_registered_flows(): Unit = runBlocking {
    schema.createPerson.execute(id = "TestId12345", name = "TestName0", age = 10000)
    val querySubscription = schema.getPerson.subscribe(id = "TestId12345")

    withTimeout(2.seconds) {
      val resultsChannel1 = Channel<PersonSchema.GetPersonQuery.Data>(capacity = Channel.UNLIMITED)
      val flowJob1 = launch {
        querySubscription.flow.map { it.data }.collect(resultsChannel1::send)
      }
      val resultsChannel2 = Channel<PersonSchema.GetPersonQuery.Data>(capacity = Channel.UNLIMITED)
      val flowJob2 = launch {
        querySubscription.flow.map { it.data }.collect(resultsChannel2::send)
      }
      resultsChannel1.purge(0.25.seconds).forEach {
        assertThat(it.person).isEqualToGetPersonQueryResult(name = "TestName0", age = 10000)
      }
      resultsChannel2.purge(0.25.seconds).forEach {
        assertThat(it.person).isEqualToGetPersonQueryResult(name = "TestName0", age = 10000)
      }

      schema.updatePerson.execute(id = "TestId12345", name = "TestName1", age = 10001)
      querySubscription.reload()

      assertThat(resultsChannel1.receive().person)
        .isEqualToGetPersonQueryResult(name = "TestName1", age = 10001)
      assertThat(resultsChannel2.receive().person)
        .isEqualToGetPersonQueryResult(name = "TestName1", age = 10001)

      flowJob1.cancel()
      flowJob2.cancel()
    }
  }

  @Test
  fun reload_concurrent_invocations_get_conflated() = runBlocking {
    schema.createPerson.execute(id = "TestId12345", name = "Name", age = 10000)
    val querySubscription = schema.getPerson.subscribe(id = "TestId12345")

    withTimeout(5.seconds) {
      val resultsChannel = Channel<PersonSchema.GetPersonQuery.Data>(capacity = Channel.UNLIMITED)
      val collectJob = launch {
        querySubscription.flow.map { it.data }.collect(resultsChannel::send)
      }

      val maxHardwareConcurrency = max(2, Runtime.getRuntime().availableProcessors())
      val multiThreadExecutor = Executors.newFixedThreadPool(maxHardwareConcurrency)
      try {
        repeat(100000) { multiThreadExecutor.execute(querySubscription::reload) }
      } finally {
        multiThreadExecutor.shutdown()
      }

      var resultCount = 0
      while (true) {
        resultCount++
        val result = withTimeoutOrNull(1.seconds) { resultsChannel.receive() } ?: break
        assertThat(result.person).isEqualToGetPersonQueryResult(name = "Name", age = 10000)
      }
      assertThat(resultCount).isGreaterThan(0)

      collectJob.cancel()
    }
  }
}

private fun Subject.isEqualToGetPersonQueryResult(name: String, age: Int?) =
  isEqualTo(PersonSchema.GetPersonQuery.Data.Person(name = name, age = age))

private suspend fun <T> ReceiveChannel<T>.purge(timeout: Duration): List<T> = coroutineScope {
  buildList {
    while (true) {
      try {
        withTimeout(timeout) { add(receive()) }
      } catch (e: TimeoutCancellationException) {
        break
      }
    }
  }
}