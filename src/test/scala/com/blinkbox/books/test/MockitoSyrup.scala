package com.blinkbox.books.test

import org.hamcrest.{BaseMatcher, Description, Matcher}
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.scalatest.mock.MockitoSugar

trait MockitoSyrup extends MockitoSugar {
  import scala.language.implicitConversions

  implicit def func2answer[T](f: => T): Answer[T] = new Answer[T] {
    override def answer(invocation: InvocationOnMock): T = f
  }

  implicit def func2matcher[T](f: T => Boolean): Matcher[T] = new BaseMatcher[T] {
    override def matches(item: Any): Boolean = f(item.asInstanceOf[T])
    override def describeTo(description: Description): Unit = description.appendText("custom matcher")
  }
}
