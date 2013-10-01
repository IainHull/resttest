package org.iainhull.resttest

import org.scalatest.matchers.ShouldMatchers.AnyRefShouldWrapper
import org.scalatest.matchers.HavePropertyMatcher
import org.scalatest.matchers.HavePropertyMatchResult
import org.scalatest.matchers.ShouldMatchers.AnyShouldWrapper

/**
 * Adds [[http://www.scalatest.org/ ScalaTest]] support to the RestTest [[Dsl]].
 *
 * The `should` keyword is added to [[RequestBuilder]] and [[Response]] expressions, the
 * `RequestBuilder` is executed first and `should` applied to the `Response`.
 *
 * The `have` keyword supports [[Extractor]]s.  See [[ExtractorToHavePropertyMatcher]] for more details.
 *
 * == Example ==
 *
 * {{{
 * using (_ url "http://api.rest.org/person") { implicit rb =>
 *   GET should have (statusCode(Status.OK), jsonBodyAsList[Person] === EmptyList)
 *
 *   val (status, id) = POST body personJson returning (statusCode, headerText("X-Person-Id"))
 *   status should be(Status.Created)
 *
 *   val foo = GET / id should have (statusCode(Status.OK), jsonBodyAs[Person] === Jason)
 *
 *   GET should have (statusCode === Status.OK, jsonBodyAsList[Person] === Seq(Jason))
 *
 *   DELETE / id should have (statusCode === Status.OK)
 *
 *   GET / id should have (statusCode === Status.NotFound)
 *
 *   GET should have (statusCode(Status.OK), jsonBodyAsList[Person] === EmptyList)
 * }
 * }}}
 */
trait RestMatchers {
  import language.implicitConversions
  import Api._
  import Dsl._

  /**
   * Implicitly execute a [[RequestBuilder]] and convert the [[Response]] into a `AnyRefShouldWrapper`
   *
   * This adds support for ScalaTest's `ShouldMatchers` to `RequestBuilder`
   */
  implicit def requestBuilderToShouldWrapper(builder: RequestBuilder)(implicit driver: Driver): AnyRefShouldWrapper[Response] = {
    responseToShouldWrapper(builder execute ())
  }

  /**
   * Implicitly convert a [[Response]] into a `AnyRefShouldWrapper`
   *
   * This adds support for ScalaTest's `ShouldMatchers` to `Response`
   */
  implicit def responseToShouldWrapper(response: Response): AnyRefShouldWrapper[Response] = {
    new AnyRefShouldWrapper(response)
  }

  implicit def methodToShouldWrapper(method: Method)(implicit builder: RequestBuilder, driver: Driver): AnyRefShouldWrapper[Response] = {
    requestBuilderToShouldWrapper(builder.withMethod(method))
  }


  implicit def extractorToShouldWrapper[T](extractor: Extractor[T])(implicit response: Response): AnyShouldWrapper[T] = {
    val v: T = extractor.op(response)
    new AnyShouldWrapper[T](v)
  }  
  
  /**
   * Implicitly add operations to [[Extractor]] that create `HavePropertyMatcher`s.
   *
   * This adds support for reusing `Extractor`s in `should have(...)` expressions, for example
   *
   * {{{
   * response should have(statusCode(Status.OK))
   * response should have(headerText("header2") === "value")
   * response should have(statusCode !== 1)
   * response should have(statusCode > 1)
   * response should have(statusCode in (Status.OK, Status.Created))
   * response should have(statusCode between (400, 499))
   * }}}
   *
   * == Operations ==
   *
   * The following operations are added to all `Extractors`
   *
   * $ - `extractor(expected)` - the extracted value is equal to the `expected` value.
   * $ - `extractor === expected` - the extracted value is equal to the `expected` value.
   * $ - `extractor !== expected` - the extracted value is not equal to the `expected` value.
   * $ - `extractor in (expected1, expected2, ...)` - the extracted value is in the list of expected values.
   *
   * The following operations are added to `Extractor`s that support `scala.math.Ordering`.
   * More precisely these operations are added to `Extractor[T]` if there exists an implicit
   * `Ordering[T]` for any type `T`.
   *
   * $ - `extractor < expected` - the extracted value is less than the `expected` value.
   * $ - `extractor <= expected` - the extracted value is less than or equal to the `expected` value.
   * $ - `extractor > expected` - the extracted value is greater than the `expected` value.
   * $ - `extractor <= expected` - the extracted value is greater than or equal to the `expected` value.
   * $ - `extractor between (lowExpected, highExpected)` - the extracted value is greater than or equal to `lowExpected` and less than or equal to `highExpected`.
   */
  implicit class ExtractorToHavePropertyMatcher[T](extractor: Extractor[T]) {
    def apply(expected: T) = makeMatcher(_ == _, expected)

    def ===(expected: T) = makeMatcher(_ == _, expected)
    def !==(expected: T) = makeMatcher(_ != _, expected, "!= ")

    def <(expected: T)(implicit ord: math.Ordering[T]) = makeMatcher(ord.lt, expected, "< ")
    def <=(expected: T)(implicit ord: math.Ordering[T]) = makeMatcher(ord.lteq, expected, "<= ")
    def >(expected: T)(implicit ord: math.Ordering[T]) = makeMatcher(ord.gt, expected, "> ")
    def >=(expected: T)(implicit ord: math.Ordering[T]) = makeMatcher(ord.gteq, expected, ">= ")

    private def makeMatcher(pred: (T, T) => Boolean, expected: T, expectedHint: String = ""): HavePropertyMatcher[Response, String] = {
      new HavePropertyMatcher[Response, String] {
        def apply(response: Response) = {
          val actual = extractor.op(response)
          new HavePropertyMatchResult(
            pred(actual, expected),
            extractor.name,
            expectedHint + expected.toString,
            actual.toString)
        }
      }
    }

    def in(firstExpected: T, moreExpected: T*): HavePropertyMatcher[Response, String] = {
      val allExpected = firstExpected +: moreExpected
      new HavePropertyMatcher[Response, String] {
        def apply(response: Response) = {
          val actual = extractor.op(response)
          new HavePropertyMatchResult(
            allExpected.contains(actual),
            extractor.name,
            "in " + allExpected.mkString("(", ",", ")"),
            actual.toString)
        }
      }
    }

    def between(lowExpected: T, highExpected: T)(implicit ord: math.Ordering[T]): HavePropertyMatcher[Response, String] = {
      new HavePropertyMatcher[Response, String] {
        def apply(response: Response) = {
          val actual = extractor.op(response)
          new HavePropertyMatchResult(
            ord.lteq(lowExpected, actual) && ord.lteq(actual, highExpected),
            extractor.name,
            "between (" + lowExpected + "," + highExpected + ")",
            actual.toString)
        }
      }
    }
  }

  /**
   * Implicitly convert an [[Extractor]] that returns any type of `Option` into a `HavePropertyMatcher`.
   *
   * This adds support for reusing `Extractor[Option[_]]`s in `should have(...)` expressions, for example
   *
   * {{{
   * response should have(header("header2"))
   * response should have(body)
   * }}}
   */
  implicit class OptionExtractorToHavePropertyMatcher(extractor: Extractor[Option[_]]) extends HavePropertyMatcher[Response, String] {
    def apply(response: Response) = {
      val actual = extractor.op(response)
      new HavePropertyMatchResult(
        actual.isDefined,
        extractor.name,
        "defined",
        (if (actual.isDefined) "" else "not") + " defined")
    }
  }
}

object RestMatchers extends RestMatchers