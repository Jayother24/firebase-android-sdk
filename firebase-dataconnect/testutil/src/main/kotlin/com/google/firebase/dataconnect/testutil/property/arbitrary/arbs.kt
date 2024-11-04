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

@file:Suppress("UnusedReceiverParameter")

package com.google.firebase.dataconnect.testutil.property.arbitrary

import com.google.firebase.dataconnect.ConnectorConfig
import com.google.firebase.dataconnect.DataConnectSettings
import com.google.firebase.dataconnect.LocalDate
import io.kotest.property.Arb
import io.kotest.property.RandomSource
import io.kotest.property.Sample
import io.kotest.property.arbitrary.Codepoint
import io.kotest.property.arbitrary.alphanumeric
import io.kotest.property.arbitrary.arabic
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.ascii
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.cyrillic
import io.kotest.property.arbitrary.double
import io.kotest.property.arbitrary.egyptianHieroglyphs
import io.kotest.property.arbitrary.filterNot
import io.kotest.property.arbitrary.hex
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.merge
import io.kotest.property.arbitrary.next
import io.kotest.property.arbitrary.orNull
import io.kotest.property.arbitrary.string
import io.kotest.property.asSample
import io.mockk.mockk
import kotlin.random.nextInt
import kotlinx.serialization.modules.SerializersModule

@Suppress("MemberVisibilityCanBePrivate")
object DataConnectArb {
  val anyScalar: AnyScalarArb = AnyScalarArb
  val javaTime: JavaTimeArbs = JavaTimeArbs

  val codepoints: Arb<Codepoint> =
    Codepoint.ascii()
      .merge(Codepoint.egyptianHieroglyphs())
      .merge(Codepoint.arabic())
      .merge(Codepoint.cyrillic())
      // Do not produce character code 0 because it's not supported by Postgresql:
      // https://www.postgresql.org/docs/current/datatype-character.html
      .filterNot { it.value == 0 }

  fun string(length: IntRange = 0..100, codepoints: Arb<Codepoint>? = null): Arb<String> =
    Arb.string(length, codepoints ?: DataConnectArb.codepoints)

  fun float(): Arb<Double> = Arb.double().filterNot { it.isNaN() || it.isInfinite() }

  fun id(length: Int = 20): Arb<String> = Arb.string(size = length, Codepoint.alphanumeric())

  fun uuid(): Arb<String> = Arb.string(size = 32, Codepoint.hex())

  fun connectorName(
    string: Arb<String> = Arb.string(size = 8, Codepoint.alphanumeric())
  ): Arb<String> = arbitrary { "connector_${string.bind()}" }

  fun connectorLocation(
    string: Arb<String> = Arb.string(size = 8, Codepoint.alphanumeric())
  ): Arb<String> = arbitrary { "location_${string.bind()}" }

  fun connectorServiceId(
    string: Arb<String> = Arb.string(size = 8, Codepoint.alphanumeric())
  ): Arb<String> = arbitrary { "serviceId_${string.bind()}" }

  fun connectorConfig(
    prefix: String? = null,
    connector: Arb<String> = connectorName(),
    location: Arb<String> = connectorLocation(),
    serviceId: Arb<String> = connectorServiceId(),
  ): Arb<ConnectorConfig> {
    val wrappedConnector = prefix?.let { connector.withPrefix(it) } ?: connector
    val wrappedLocation = prefix?.let { location.withPrefix(it) } ?: location
    val wrappedServiceId = prefix?.let { serviceId.withPrefix(it) } ?: serviceId
    return arbitrary {
      ConnectorConfig(
        connector = wrappedConnector.bind(),
        location = wrappedLocation.bind(),
        serviceId = wrappedServiceId.bind(),
      )
    }
  }

  fun accessToken(
    string: Arb<String> = Arb.string(size = 8, Codepoint.alphanumeric())
  ): Arb<String> = arbitrary { "accessToken_${string.bind()}" }

  fun requestId(string: Arb<String> = Arb.string(size = 8, Codepoint.alphanumeric())): Arb<String> =
    arbitrary {
      "requestId_${string.bind()}"
    }

  fun operationName(
    string: Arb<String> = Arb.string(size = 8, Codepoint.alphanumeric())
  ): Arb<String> = arbitrary { "operationName_${string.bind()}" }

  fun projectId(string: Arb<String> = Arb.string(size = 8, Codepoint.alphanumeric())): Arb<String> =
    arbitrary {
      "projectId_${string.bind()}"
    }

  fun host(string: Arb<String> = Arb.string(size = 8, Codepoint.alphanumeric())): Arb<String> =
    arbitrary {
      "host_${string.bind()}"
    }

  fun dataConnectSettings(
    prefix: String? = null,
    host: Arb<String> = host(),
    sslEnabled: Arb<Boolean> = Arb.boolean(),
  ): Arb<DataConnectSettings> {
    val wrappedHost = prefix?.let { host.withPrefix(it) } ?: host
    return arbitrary {
      DataConnectSettings(host = wrappedHost.bind(), sslEnabled = sslEnabled.bind())
    }
  }

  fun tag(string: Arb<String> = Arb.string(size = 50, Codepoint.alphanumeric())): Arb<String> =
    arbitrary {
      "tag_${string.bind()}"
    }

  fun serializersModule(): Arb<SerializersModule?> =
    arbitrary<SerializersModule> { mockk() }.orNull(nullProbability = 0.333)

  fun localDate(): Arb<LocalDate> = LocalDateArb()
}

val Arb.Companion.dataConnect: DataConnectArb
  get() = DataConnectArb

inline fun <reified T : Any> Arb.Companion.mock(): Arb<T> = arbitrary { mockk<T>(relaxed = true) }

/**
 * Returns an [Arb] identical to [Arb.Companion.nonNegativeInt] except that the values it produces
 * have an equal probability of having any given number of digits in its base-10 string
 * representation. This is useful for testing int values that get zero padded when they are small.
 * @see intWithEvenNumDigitsDistribution
 */
fun Arb.Companion.nonNegativeIntWithEvenNumDigitsDistribution(
  numDigits: Arb<Int>? = null
): Arb<Int> =
  if (numDigits === null) {
    NonNegativeIntWithEvenNumDigitsDistributionArb()
  } else {
    NonNegativeIntWithEvenNumDigitsDistributionArb(numDigits)
  }

/**
 * Returns an [Arb] identical to [Arb.Companion.int] except that the values it produces have an
 * equal probability of having any given number of digits in its base-10 string representation. This
 * is useful for testing int values that get zero padded when they are small.
 * @see nonNegativeIntWithEvenNumDigitsDistribution
 */
fun Arb.Companion.intWithEvenNumDigitsDistribution(numDigits: Arb<Int>? = null): Arb<Int> {
  val arb = nonNegativeIntWithEvenNumDigitsDistribution(numDigits)
  return arbitrary(
    edgecaseFn = { rs -> arb.edgecase(rs)?.let { if (rs.random.nextBoolean()) it else -it } },
    sampleFn = { rs -> arb.next(rs).let { if (rs.random.nextBoolean()) it else -it } }
  )
}

/** @see Arb.Companion.nonNegativeIntWithEvenNumDigitsDistribution */
private class NonNegativeIntWithEvenNumDigitsDistributionArb(
  private val numDigits: Arb<Int> = Arb.int(1..10)
) : Arb<Int>() {
  override fun edgecase(rs: RandomSource): Int {
    val curNumDigits = if (rs.random.nextBoolean()) numDigits.next(rs) else numDigits.edgecase(rs)!!
    val range = rangeByNumDigits[curNumDigits - 1]
    return if (rs.random.nextBoolean()) range.first else range.last
  }

  override fun sample(rs: RandomSource): Sample<Int> {
    val curNumDigits = numDigits.next(rs)
    val range = rangeByNumDigits[curNumDigits - 1]
    return rs.random.nextInt(range).asSample()
  }

  private companion object {
    val rangeByNumDigits =
      listOf(
        0..9,
        10..99,
        100..999,
        1000..9999,
        10000..99999,
        100000..999999,
        1000000..9999999,
        10000000..99999999,
        100000000..999999999,
        1000000000..Int.MAX_VALUE,
      )
  }
}

/** An [Arb] that produces [LocalDate] objects that are accepted by Firebase Data Connect. */
private class LocalDateArb : Arb<LocalDate>() {

  override fun sample(rs: RandomSource): Sample<LocalDate> {
    val year = rs.random.nextInt(yearRange)
    val month = rs.random.nextInt(monthRange)
    val day = rs.random.nextInt(dayRangeInMonth[month - 1])
    return LocalDate(year = year, month = month, day = day).asSample()
  }

  override fun edgecase(rs: RandomSource): LocalDate {
    val year = rs.edgeCaseFromRange(yearRange)
    val month = rs.edgeCaseFromRange(monthRange)
    val day = rs.edgeCaseFromRange(dayRangeInMonth[month - 1])
    return LocalDate(year = year, month = month, day = day)
  }

  private companion object {
    val yearRange: IntRange = DateEdgeCases.MIN_YEAR..DateEdgeCases.MAX_YEAR
    val monthRange: IntRange = 1..12
    val dayRangeInMonth: List<IntRange> =
      listOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31).map { 1..it }

    private fun RandomSource.edgeCaseFromRange(range: IntRange): Int =
      if (random.nextBoolean()) {
        random.nextInt(range)
      } else if (random.nextBoolean()) {
        range.first
      } else {
        range.last
      }
  }
}
