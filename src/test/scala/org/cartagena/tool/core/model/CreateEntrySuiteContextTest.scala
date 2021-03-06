package org.cartagena.tool.core.model

import org.cartagena.tool.core.model.SuiteContext.KeyAlreadyPresentException
import org.cartagena.tool.core.model.SuiteContextTestUtil._
import org.scalatest.{FlatSpec, Matchers}

import scala.util.Success

class CreateEntrySuiteContextTest extends FlatSpec with Matchers {

  "create" should "succeed to create an entry with given value and key" in {
    // given
    val context = MySuiteContextTest()

    val key = "key3"
    val value = "value3"

    // when
    val actual = context create[String](key, value)

    // then
    actual should be(Success(value))
    context.get[String](key) should be(Success(value))
  }

  it should "fail to create an entry with given value and key where key is already associated with some value" in {
    // given
    val context = MySuiteContextTest()

    val key = "key1"
    val value = "value1"

    // when
    val actual = context create[String](key, value)

    // then
    actual.isFailure should be(true)
    actual.failed.get shouldBe a[KeyAlreadyPresentException]
  }

  it should "succeed to create an entry with given collection value and key" in {
    // given
    val context = MySuiteContextTest()

    val key = "key4"
    val value = "value4" :: Nil

    // when
    val actual = context create[List[String]](key, value)

    // then
    actual should be(Success(value))
    context.get[List[String]](key) should be(Success(value))
  }

  it should "fail to create an entry with given collection value and key where key is already associated with some " +
    "value" in {
    // given
    val context = MySuiteContextTest()

    val key = "key1"
    val value = "value1" :: Nil

    // when
    val actual = context create[List[String]](key, value)

    // then
    actual.isFailure should be(true)
    actual.failed.get shouldBe a[KeyAlreadyPresentException]
  }

}
