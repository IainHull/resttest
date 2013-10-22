package org.iainhull.resttest

import java.net.URI

object Dsl extends Extractors {
  import language.implicitConversions
  import Api._

  implicit def toRequest(builder: RequestBuilder): Request = builder.toRequest
  implicit def methodToRequestBuilder(method: Method)(implicit builder: RequestBuilder): RequestBuilder = builder.withMethod(method)
  implicit def methodToRichRequestBuilder(method: Method)(implicit builder: RequestBuilder): RichRequestBuilder = new RichRequestBuilder(methodToRequestBuilder(method)(builder))

  trait Assertion {
    def result(res: Response): Option[String]
  }
  
  def assertionFailed(assertionResults: Seq[String]): Throwable = {
    new AssertionError(assertionResults.mkString(","))
  }

  implicit class RichRequestBuilder(builder: RequestBuilder) {
    def url(u: String) = builder.withUrl(u)
    def body(b: String) = builder.withBody(b)
    def /(p: Symbol) = builder.addPath(p.name)
    def /(p: Any) = builder.addPath(p.toString)
    def :?(params: (Symbol, Any)*) = builder.addQuery(params map (p => (p._1.name, p._2.toString)): _*)

    def execute()(implicit driver: Driver): Response = {
      driver.execute(builder)
    }

    def apply[T](proc: RequestBuilder => T): T = {
      proc(builder)
    }

    def asserting(assertions: Assertion*)(implicit driver: Driver): Response = {
      val res = execute()
      val assertionResults = for {
        a <- assertions
        r <- a.result(res)
      } yield r
      if(assertionResults.nonEmpty) {
        throw assertionFailed(assertionResults)
      }
      res
    }

    def expecting[T](func: Response => T)(implicit driver: Driver): T = {
      val res = execute()
      func(res)
    }
  }

  def using(config: RequestBuilder => RequestBuilder)(process: RequestBuilder => Unit)(implicit builder: RequestBuilder): Unit = {
    process(config(builder))
  }

  implicit class RichResponse(response: Response) {
    def returning[T1](ext1: ExtractorLike[T1])(implicit driver: Driver): T1 = {
      ext1.value(response)
    }

    def returning[T1, T2](ext1: ExtractorLike[T1], ext2: ExtractorLike[T2]): (T1, T2) = {
      (ext1.value(response), ext2.value(response))
    }

    def returning[T1, T2, T3](ext1: ExtractorLike[T1], ext2: ExtractorLike[T2], ext3: ExtractorLike[T3]): (T1, T2, T3) = {
      (ext1.value(response), ext2.value(response), ext3.value(response))
    }

    def returning[T1, T2, T3, T4](ext1: ExtractorLike[T1], ext2: ExtractorLike[T2], ext3: ExtractorLike[T3], ext4: ExtractorLike[T4]): (T1, T2, T3, T4) = {
      (ext1.value(response), ext2.value(response), ext3.value(response), ext4.value(response))
    }
  }

  implicit def requestBuilderToRichResponse(builder: RequestBuilder)(implicit driver: Driver): RichResponse = new RichResponse(builder.execute())
  implicit def methodToRichResponse(method: Method)(implicit builder: RequestBuilder, driver: Driver): RichResponse = new RichResponse(builder.withMethod(method).execute())

  /**
   * Add operator support to `Extractor`s these are used to generate an `Assertion` using the extracted value.
   * 
   * {{{
   * GET url "http://api.rest.org/person" assert (StatusCode === Status.Ok)
   * }}}
   *   
   * == Operations ==
   *
   * The following operations are added to all `Extractors`
   *
   * $ - `extractor === expected` - the extracted value is equal to the `expected` value.
   * $ - `extractor !== expected` - the extracted value is not equal to the `expected` value.
   * $ - `extractor in (expected1, expected2, ...)` - the extracted value is in the list of expected values.
   * $ - `extractor notIn (expected1, expected2, ...)` - the extracted value is in the list of expected values.
   *
   * The following operations are added to `Extractor`s that support `scala.math.Ordering`.
   * More precisely these operations are added to `Extractor[T]` if there exists an implicit
   * `Ordering[T]` for any type `T`.
   *
   * $ - `extractor < expected` - the extracted value is less than the `expected` value.
   * $ - `extractor <= expected` - the extracted value is less than or equal to the `expected` value.
   * $ - `extractor > expected` - the extracted value is greater than the `expected` value.
   * $ - `extractor <= expected` - the extracted value is greater than or equal to the `expected` value.
   */
  implicit class RichExtractor[A](ext: ExtractorLike[A]) {
    def ===[B >: A](expected: B): Assertion = makeAssertion(_ == expected, expected, "did not equal")
    def !==[B >: A](expected: B): Assertion = makeAssertion(_ != expected, expected, "did equal")

    def < [B >: A](expected: B)(implicit ord: math.Ordering[B]): Assertion = makeAssertion(ord.lt  (_, expected), expected, "was not less than")
    def <=[B >: A](expected: B)(implicit ord: math.Ordering[B]): Assertion = makeAssertion(ord.lteq(_, expected), expected, "was not less than or equal")
    def > [B >: A](expected: B)(implicit ord: math.Ordering[B]): Assertion = makeAssertion(ord.gt(  _, expected), expected, "was not greater than")
    def >=[B >: A](expected: B)(implicit ord: math.Ordering[B]): Assertion = makeAssertion(ord.gteq(_, expected), expected, "was not greater than or equal")    
    
    def in[B >: A](expectedVals: B*): Assertion = makeAssertion(expectedVals.contains(_), expectedVals.mkString("(", ", ", ")"), "was not in")
    def notIn[B >: A](expectedVals: B*): Assertion = makeAssertion(!expectedVals.contains(_), expectedVals.mkString("(", ", ", ")"), "was in")
    
    private def makeAssertion[B](pred: A => Boolean, expected: Any, text: String) = new Assertion {
      override def result(res: Response): Option[String] = {
        val actual = ext.value(res)
        if (!pred(actual)) Some(s"${ext.name}: $actual $text $expected") else None
      }
    }

  }
}