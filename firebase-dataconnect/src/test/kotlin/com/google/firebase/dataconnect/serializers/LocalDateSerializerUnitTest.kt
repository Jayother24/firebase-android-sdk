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

package com.google.firebase.dataconnect.serializers

import com.google.firebase.dataconnect.LocalDate
import com.google.firebase.dataconnect.testutil.property.arbitrary.intWithEvenNumDigitsDistribution
import com.google.firebase.dataconnect.testutil.property.arbitrary.nonNegativeIntWithEvenNumDigitsDistribution
import com.google.firebase.dataconnect.util.ProtoUtil.decodeFromValue
import com.google.firebase.dataconnect.util.ProtoUtil.encodeToValue
import com.google.firebase.dataconnect.util.ProtoUtil.toValueProto
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.common.ExperimentalKotest
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.EdgeConfig
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.Codepoint
import io.kotest.property.arbitrary.arabic
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.ascii
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.booleanArray
import io.kotest.property.arbitrary.constant
import io.kotest.property.arbitrary.cyrillic
import io.kotest.property.arbitrary.egyptianHieroglyphs
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.filterNot
import io.kotest.property.arbitrary.greekCoptic
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.katakana
import io.kotest.property.arbitrary.long
import io.kotest.property.arbitrary.merge
import io.kotest.property.arbitrary.next
import io.kotest.property.arbitrary.string
import io.kotest.property.arbitrary.withEdgecases
import io.kotest.property.assume
import io.kotest.property.checkAll
import io.mockk.every
import io.mockk.mockk
import kotlin.random.nextInt
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encoding.Decoder
import org.junit.Test

class LocalDateSerializerUnitTest {

  @Test
  fun `serialize() should produce the expected serialized string`() = runTest {
    checkAll(propTestConfig, Arb.localDate()) { localDate ->
      val value = encodeToValue(localDate, LocalDateSerializer, serializersModule = null)
      value.stringValue shouldBe localDate.toYYYYMMDDWithZeroPadding()
    }
  }

  @Test
  fun `serialize() should throw IllegalArgumentException if year, month, or day is negative`() =
    runTest {
      val int = Arb.intWithEvenNumDigitsDistribution(numDigits = Arb.int(1..6))
      val localDates = Arb.localDate(year = int, month = int, day = int)
      checkAll(propTestConfig, localDates) { localDate ->
        assume(localDate.year < 0 || localDate.month < 0 || localDate.day < 0)
        shouldThrow<IllegalArgumentException> { LocalDateSerializer.serialize(mockk(), localDate) }
      }
    }

  @Test
  fun `deserialize() should produce the expected LocalDate object`() = runTest {
    checkAll(propTestConfig, Arb.localDate(), Arb.int(0..10), Arb.int(0..10), Arb.int(0..10)) {
      localDate,
      yearPadding,
      monthPadding,
      dayPadding ->
      val value =
        localDate
          .toYYYYMMDDWithZeroPadding(
            yearPadding = yearPadding,
            monthPadding = monthPadding,
            dayPadding = dayPadding
          )
          .toValueProto()
      val decodedLocalDate = decodeFromValue(value, LocalDateSerializer, serializersModule = null)
      decodedLocalDate shouldBe localDate
    }
  }

  @Test
  fun `deserialize() should throw IllegalArgumentException when given unparseable strings`() =
    runTest {
      checkAll(propTestConfig, Arb.unparseableDate()) { encodedDate ->
        val decoder: Decoder = mockk { every { decodeString() } returns encodedDate }
        shouldThrow<IllegalArgumentException> { LocalDateSerializer.deserialize(decoder) }
      }
    }

  private companion object {
    val propTestConfig =
      PropTestConfig(
        iterations = 400,
        edgeConfig = EdgeConfig(edgecasesGenerationProbability = 0.2)
      )

    fun LocalDate.toYYYYMMDDWithZeroPadding(
      yearPadding: Int = 4,
      monthPadding: Int = 2,
      dayPadding: Int = 2,
    ): String {
      val yearString = year.toString().padStart(yearPadding, '0')
      val monthString = month.toString().padStart(monthPadding, '0')
      val dayString = day.toString().padStart(dayPadding, '0')
      return "$yearString-$monthString-$dayString"
    }

    fun Arb.Companion.localDate(
      year: Arb<Int> = nonNegativeIntWithEvenNumDigitsDistribution(numDigits = Arb.int(1..6)),
      month: Arb<Int> = nonNegativeIntWithEvenNumDigitsDistribution(numDigits = Arb.int(1..2)),
      day: Arb<Int> = nonNegativeIntWithEvenNumDigitsDistribution(numDigits = Arb.int(1..2)),
    ): Arb<LocalDate> {
      return arbitrary(
        edgecaseFn = { rs ->
          val yearInt = if (rs.random.nextBoolean()) year.next(rs) else year.edgecase(rs)!!
          val monthInt = if (rs.random.nextBoolean()) month.next(rs) else month.edgecase(rs)!!
          val dayInt = if (rs.random.nextBoolean()) day.next(rs) else day.edgecase(rs)!!
          LocalDate(year = yearInt, month = monthInt, day = dayInt)
        },
        sampleFn = { LocalDate(year = year.bind(), month = month.bind(), day = day.bind()) }
      )
    }

    private enum class UnparseableNumberReason {
      EmptyString,
      InvalidChars,
      Negative,
      ExceedsIntMax,
    }

    private val codepoints =
      Codepoint.ascii()
        .merge(Codepoint.egyptianHieroglyphs())
        .merge(Codepoint.arabic())
        .merge(Codepoint.cyrillic())
        .merge(Codepoint.greekCoptic())
        .merge(Codepoint.katakana())

    fun Arb.Companion.unparseableNumber(): Arb<String> {
      val reasonArb = enum<UnparseableNumberReason>()
      val validIntArb = nonNegativeIntWithEvenNumDigitsDistribution(numDigits = Arb.int(1..6))
      val validChars = listOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '-').map { it.code }
      val invalidString =
        string(1..5, codepoints.filterNot { validChars.contains(it.value) }).withEdgecases("-")
      val tooLargeValues = long(Int.MAX_VALUE.toLong()..Long.MAX_VALUE)
      return arbitrary { rs ->
        when (reasonArb.bind()) {
          UnparseableNumberReason.EmptyString -> ""
          UnparseableNumberReason.Negative -> "-${validIntArb.bind()}"
          UnparseableNumberReason.ExceedsIntMax -> "${tooLargeValues.bind()}"
          UnparseableNumberReason.InvalidChars -> {
            val flags = Array(3) { rs.random.nextBoolean() }
            if (!flags[0]) {
              flags[2] = true
            }
            val prefix = if (flags[0]) invalidString.bind() else ""
            val mid = if (flags[1]) validIntArb.bind() else ""
            val suffix = if (flags[2]) invalidString.bind() else ""
            "$prefix$mid$suffix"
          }
        }
      }
    }

    fun Arb.Companion.unparseableDash(): Arb<String> {
      val invalidString = string(1..5, codepoints.filterNot { it.value == '-'.code })
      return arbitrary { rs ->
        val flags = Array(3) { rs.random.nextBoolean() }
        if (!flags[0]) {
          flags[2] = true
        }

        val prefix = if (flags[0]) invalidString.bind() else ""
        val mid = if (flags[1]) "-" else ""
        val suffix = if (flags[2]) invalidString.bind() else ""

        "$prefix$mid$suffix"
      }
    }

    fun Arb.Companion.unparseableDate(): Arb<String> {
      val validNumber = nonNegativeIntWithEvenNumDigitsDistribution(numDigits = Arb.int(1..6))
      val unparseableNumber = unparseableNumber()
      val unparseableDash = unparseableDash()
      val booleanArray = booleanArray(Arb.constant(5), Arb.boolean())
      return arbitrary(edgecases = listOf("", "-", "--", "---")) { rs ->
        val invalidCharFlags = booleanArray.bind()
        if (invalidCharFlags.count { it } == 0) {
          invalidCharFlags[rs.random.nextInt(invalidCharFlags.indices)] = true
        }

        val year = if (invalidCharFlags[0]) unparseableNumber.bind() else validNumber.bind()
        val dash1 = if (invalidCharFlags[1]) unparseableDash.bind() else "-"
        val month = if (invalidCharFlags[2]) unparseableNumber.bind() else validNumber.bind()
        val dash2 = if (invalidCharFlags[3]) unparseableDash.bind() else "-"
        val day = if (invalidCharFlags[4]) unparseableNumber.bind() else validNumber.bind()

        "$year$dash1$month$dash2$day"
      }
    }
  }
}
