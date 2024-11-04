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

package com.google.firebase.dataconnect

import com.google.firebase.dataconnect.testutil.property.arbitrary.DateEdgeCases.maxDayForMonth
import com.google.firebase.dataconnect.testutil.shouldContainWithNonAbuttingText
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.withClue
import io.kotest.common.ExperimentalKotest
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldEndWith
import io.kotest.matchers.string.shouldStartWith
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.of
import io.kotest.property.arbitrary.year
import io.kotest.property.assume
import io.kotest.property.checkAll
import java.time.Year
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.number
import org.junit.Test

@Suppress("ReplaceCallWithBinaryOperator")
class LocalDateUnitTest {

  @Test
  fun `constructor() should set properties to corresponding arguments`() = runTest {
    checkAll(propTestConfig, Arb.int(), Arb.int(), Arb.int()) { year, month, day ->
      val localDate = LocalDate(year = year, month = month, day = day)
      assertSoftly {
        withClue("year") { localDate.year shouldBe year }
        withClue("month") { localDate.month shouldBe month }
        withClue("day") { localDate.day shouldBe day }
      }
    }
  }

  @Test
  fun `equals() should return true when invoked with itself`() = runTest {
    checkAll(propTestConfig, Arb.int(), Arb.int(), Arb.int()) { year, month, day ->
      val localDate = LocalDate(year = year, month = month, day = day)
      localDate.equals(localDate) shouldBe true
    }
  }

  @Test
  fun `equals() should return true when invoked with a distinct, but equal, instance`() = runTest {
    checkAll(propTestConfig, Arb.int(), Arb.int(), Arb.int()) { year, month, day ->
      val localDate1 = LocalDate(year = year, month = month, day = day)
      val localDate2 = LocalDate(year = year, month = month, day = day)
      localDate1.equals(localDate2) shouldBe true
    }
  }

  @Test
  fun `equals() should return false when invoked with null`() = runTest {
    checkAll(propTestConfig, Arb.int(), Arb.int(), Arb.int()) { year, month, day ->
      val localDate = LocalDate(year = year, month = month, day = day)
      localDate.equals(null) shouldBe false
    }
  }

  @Test
  fun `equals() should return false when invoked with a different type`() = runTest {
    val others = Arb.of("foo", 42, java.time.LocalDate.now())
    checkAll(propTestConfig, Arb.int(), Arb.int(), Arb.int(), others) { year, month, day, other ->
      val localDate = LocalDate(year = year, month = month, day = day)
      localDate.equals(other) shouldBe false
    }
  }

  @Test
  fun `equals() should return false when only the year differs`() = runTest {
    checkAll(propTestConfig, Arb.int(), Arb.int(), Arb.int(), Arb.int()) { year1, month, day, year2
      ->
      assume(year1 != year2)
      val localDate1 = LocalDate(year = year1, month = month, day = day)
      val localDate2 = LocalDate(year = year2, month = month, day = day)
      localDate1.equals(localDate2) shouldBe false
    }
  }

  @Test
  fun `equals() should return false when only the month differs`() = runTest {
    checkAll(propTestConfig, Arb.int(), Arb.int(), Arb.int(), Arb.int()) { year, month1, day, month2
      ->
      assume(month1 != month2)
      val localDate1 = LocalDate(year = year, month = month1, day = day)
      val localDate2 = LocalDate(year = year, month = month2, day = day)
      localDate1.equals(localDate2) shouldBe false
    }
  }

  @Test
  fun `equals() should return false when only the day differs`() = runTest {
    checkAll(propTestConfig, Arb.int(), Arb.int(), Arb.int(), Arb.int()) { year, month, day1, day2
      ->
      assume(day1 != day2)
      val localDate1 = LocalDate(year = year, month = month, day = day1)
      val localDate2 = LocalDate(year = year, month = month, day = day2)
      localDate1.equals(localDate2) shouldBe false
    }
  }

  @Test
  fun `hashCode() should return the same value when invoked repeatedly`() = runTest {
    checkAll(propTestConfig, Arb.int(), Arb.int(), Arb.int()) { year, month, day ->
      val localDate = LocalDate(year = year, month = month, day = day)
      val hashCode = localDate.hashCode()
      repeat(5) { withClue("iteration=$it") { localDate.hashCode() shouldBe hashCode } }
    }
  }

  @Test
  fun `hashCode() should return the same value when invoked on equal, but distinct, objects`() =
    runTest {
      checkAll(propTestConfig, Arb.int(), Arb.int(), Arb.int()) { year, month, day ->
        val localDate1 = LocalDate(year = year, month = month, day = day)
        val localDate2 = LocalDate(year = year, month = month, day = day)
        localDate1.hashCode() shouldBe localDate2.hashCode()
      }
    }

  @Test
  fun `hashCode() should return different values for different years`() = runTest {
    checkAll(propTestConfig, Arb.int(), Arb.int(), Arb.int(), Arb.int()) { year1, month, day, year2
      ->
      assume(year1.hashCode() != year2.hashCode())
      val localDate1 = LocalDate(year = year1, month = month, day = day)
      val localDate2 = LocalDate(year = year2, month = month, day = day)
      localDate1.hashCode() shouldNotBe localDate2.hashCode()
    }
  }

  @Test
  fun `hashCode() should return different values for different months`() = runTest {
    checkAll(propTestConfig, Arb.int(), Arb.int(), Arb.int(), Arb.int()) { year, month1, day, month2
      ->
      assume(month1.hashCode() != month2.hashCode())
      val localDate1 = LocalDate(year = year, month = month1, day = day)
      val localDate2 = LocalDate(year = year, month = month2, day = day)
      localDate1.hashCode() shouldNotBe localDate2.hashCode()
    }
  }

  @Test
  fun `hashCode() should return different values for different days`() = runTest {
    checkAll(propTestConfig, Arb.int(), Arb.int(), Arb.int(), Arb.int()) { year, month, day1, day2
      ->
      assume(day1.hashCode() != day2.hashCode())
      val localDate1 = LocalDate(year = year, month = month, day = day1)
      val localDate2 = LocalDate(year = year, month = month, day = day2)
      localDate1.hashCode() shouldNotBe localDate2.hashCode()
    }
  }

  @Test
  fun `toString() should return a string conforming to what is expected`() = runTest {
    checkAll(propTestConfig, Arb.int(), Arb.int(), Arb.int()) { year, month, day ->
      val localDate = LocalDate(year = year, month = month, day = day)
      val toStringResult = localDate.toString()
      assertSoftly {
        toStringResult shouldStartWith "LocalDate("
        toStringResult shouldEndWith ")"
        toStringResult shouldContainWithNonAbuttingText "year=$year"
        toStringResult shouldContainWithNonAbuttingText "month=$month"
        toStringResult shouldContainWithNonAbuttingText "day=$day"
      }
    }
  }

  @Test
  fun `copy() with no arguments should return an equal, but distinct, instance`() = runTest {
    checkAll(propTestConfig, Arb.int(), Arb.int(), Arb.int()) { year, month, day ->
      val localDate1 = LocalDate(year = year, month = month, day = day)
      val localDate2 = localDate1.copy()
      localDate1 shouldBe localDate2
    }
  }

  @Test
  fun `copy() with all arguments should return a new instance with the given arguments`() =
    runTest {
      checkAll(propTestConfig, Arb.int(), Arb.int(), Arb.int(), Arb.int(), Arb.int(), Arb.int()) {
        year1,
        month1,
        day1,
        year2,
        month2,
        day2 ->
        val localDate1 = LocalDate(year = year1, month = month1, day = day1)
        val localDate2 = localDate1.copy(year = year2, month = month2, day = day2)
        localDate2 shouldBe LocalDate(year = year2, month = month2, day = day2)
      }
    }

  @Test
  fun `toJavaLocalDate() should return an equivalent java time LocalDate object`() = runTest {
    checkAll(propTestConfig, Arb.javaLocalDateYear(), Arb.month(), Arb.day()) { year, month, day ->
      assume(day <= maxDayForMonth(month))
      val fdcLocalDate = LocalDate(year = year, month = month, day = day)
      val jLocalDate = fdcLocalDate.toJavaLocalDate()
      assertSoftly {
        withClue("year") { jLocalDate.year shouldBe year }
        withClue("month") { jLocalDate.month.number shouldBe month }
        withClue("dayOfMonth") { jLocalDate.dayOfMonth shouldBe day }
      }
    }
  }

  @Test
  fun `toKotlinxLocalDate() should return an equivalent java time LocalDate object`() = runTest {
    checkAll(propTestConfig, Arb.javaLocalDateYear(), Arb.month(), Arb.day()) { year, month, day ->
      assume(day <= maxDayForMonth(month))
      val fdcLocalDate = LocalDate(year = year, month = month, day = day)
      val kLocalDate = fdcLocalDate.toKotlinxLocalDate()
      assertSoftly {
        withClue("year") { kLocalDate.year shouldBe year }
        withClue("month") { kLocalDate.month.number shouldBe month }
        withClue("dayOfMonth") { kLocalDate.dayOfMonth shouldBe day }
      }
    }
  }

  @Test
  fun `toDataConnectLocalDate() on java time LocalDate should return an equivalent LocalDate object`() =
    runTest {
      checkAll(propTestConfig, Arb.javaLocalDateYear(), Arb.month(), Arb.day()) { year, month, day
        ->
        assume(day <= maxDayForMonth(month))
        val jLocalDate = java.time.LocalDate.of(year, month, day)
        val fdcLocalDate = jLocalDate.toDataConnectLocalDate()
        assertSoftly {
          withClue("year") { fdcLocalDate.year shouldBe year }
          withClue("month") { fdcLocalDate.month shouldBe month }
          withClue("day") { fdcLocalDate.day shouldBe day }
        }
      }
    }

  @Test
  fun `toDataConnectLocalDate() on kotlinx datetime LocalDate should return an equivalent LocalDate object`() =
    runTest {
      checkAll(propTestConfig, Arb.javaLocalDateYear(), Arb.month(), Arb.day()) { year, month, day
        ->
        assume(day <= maxDayForMonth(month))
        val kLocalDate =
          kotlinx.datetime.LocalDate(year = year, monthNumber = month, dayOfMonth = day)
        val fdcLocalDate = kLocalDate.toDataConnectLocalDate()
        assertSoftly {
          withClue("year") { fdcLocalDate.year shouldBe year }
          withClue("month") { fdcLocalDate.month shouldBe month }
          withClue("day") { fdcLocalDate.day shouldBe day }
        }
      }
    }

  private companion object {
    val propTestConfig = PropTestConfig(iterations = 50)

    fun Arb.Companion.javaLocalDateYear(): Arb<Int> = int(Year.MIN_VALUE..Year.MAX_VALUE)

    fun Arb.Companion.month(): Arb<Int> = int(1..12)

    fun Arb.Companion.day(): Arb<Int> = int(1..31)
  }
}
