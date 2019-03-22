package org.cartagena.tool.core

import java.net.URL

import org.cartagena.tool.core.model.Context

object CartagenaConverters {

  implicit def stringToUrl(str: String): URL =
    new URL(str)

  implicit class ContextOperations(context: Context) {

    private var _key: Option[String] = None
    private var _isCreateNew = false

    def </[T: Manifest](key: String): T =
      context get[T] key

    def ~=>(key: String): ContextOperations =
      createKey(key)

    def ~==>(key: String): ContextOperations =
      updateKey(key)

    def <=~[T: Manifest](key: String): Unit =
      context remove[T] key

    def createKey(key: String): ContextOperations = {
      updateInternalState(key, isCreateNew = true)
      this
    }

    def updateKey(key: String): ContextOperations = {
      updateInternalState(key, isCreateNew = false)
      this
    }

    def />[T](value: T): Unit =
      withValue(value)

    def withValue[T](value: T): Unit = _key match {
      case Some(key) if _isCreateNew =>
        context create(key, value)
      case Some(key) if !_isCreateNew =>
        context update(key, value)
      case None =>
        throw KeyNotSpecifiedException
    }

    private def updateInternalState(key: String, isCreateNew: Boolean): Unit = {
      _key = Some(key)
      _isCreateNew = isCreateNew
    }

    object KeyNotSpecifiedException extends Exception("No key specified!")

  }

}