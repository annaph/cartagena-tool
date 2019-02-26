package org.cartagena.tool.core.model

import org.cartagena.tool.core.model.Process._
import org.scalatest.{FlatSpec, Matchers}

class AppendWithExistsProcessTest extends FlatSpec with Matchers {

  "liftOne ++ exists" should "create process to convert only first integer to 'true' and then append to find odd " +
    "even integer " in {
    // given
    val process = liftOne[Int, Boolean](_ => true) ++ exists(_ % 2 != 0)
    val expected = Stream(true, false, true)

    // when
    val actual = process(Stream(1, 2, 3, 4)).map(_.get)

    // then
    actual should contain theSameElementsInOrderAs expected
  }

  "lift ++ exists" should "create process to convert each integer to 'true' and then append to do nothing" in {
    // given
    val process = lift[Int, Boolean](_ => true) ++ exists(_ => true)
    val expected = Stream(true, true, true)

    // when
    val actual = process(Stream(1, 2, 3)).map(_.get)

    // then
    actual should contain theSameElementsInOrderAs expected
  }

  "filter ++ exists" should "create process to filter 'true' values and then append to do nothing" in {
    // given
    val process = filter[Boolean](i => i) ++ exists(_ => false)
    val expected = Stream(true, true, true)

    // when
    val actual = process(Stream(false, true, false, true, false, true)).map(_.get)

    // then
    actual should contain theSameElementsInOrderAs expected
  }

  "take ++ exists" should "create process to take first three boolean values and then append to find 'true' value" in {
    // given
    val process = take[Boolean](3) ++ exists(i => i)
    val expected = Stream(true, false, true, false, true)

    // when
    val actual = process(Stream(true, false, true, false, true, true)).map(_.get)

    // then
    actual should contain theSameElementsInOrderAs expected
  }

  "drop ++ exists" should "create process to drop first three boolean values and then append to do nothing" in {
    // given
    val process = drop[Boolean](3) ++ exists(i => i)
    val expected = Stream(false, true, true)

    // when
    val actual = process(Stream(true, false, true, false, true, true)).map(_.get)

    // then
    actual should contain theSameElementsInOrderAs expected
  }

  "takeWhile ++ exists" should "create process to take boolean values while value is 'true' and then append to find " +
    "'true' value" in {
    // given
    val process = takeWhile[Boolean](i => i) ++ exists(i => i)
    val expected = Stream(true, true, true, false, true)

    // when
    val actual = process(Stream(true, true, true, false, true, true)).map(_.get)

    // then
    actual should contain theSameElementsInOrderAs expected
  }

  "dropWhile ++ exists" should "create process to drop boolean values while value is 'true' and then append to do " +
    "nothing" in {
    // given
    val process = dropWhile[Boolean](i => i) ++ exists(i => i)
    val expected = Stream(false, true, false)

    // when
    val actual = process(Stream(true, true, true, false, true, false)).map(_.get)

    // then
    actual should contain theSameElementsInOrderAs expected
  }

  "exists ++ exists" should "create process to find even integer and then append to find next even integer" in {
    // given
    val process = exists[Int](_ % 2 == 0) ++ exists(_ % 2 == 0)
    val expected = Stream(false, false, true, false, true)

    // when
    val actual = process(Stream(1, 3, 4, 5, 6, 7)).map(_.get)

    // then
    actual should contain theSameElementsInOrderAs expected
  }

}
