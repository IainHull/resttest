package org.iainhull.resttest

import java.net.URI
import scala.util.Success
import scala.util.Failure

/**
 * Provides a DSL for simplifying REST system tests.  This is meant to be used with ScalaTest or similar testing framework.
 *
 * For example to post a json document to a REST endpoint and check the statusCode:
 * {{{
 * val personJson = """{ "name": "fred" }"""
 * POST url "http://api.rest.org/person" body personJson asserting (statusCode is Status.Created)
 * }}}
 *
 * Or to get a json document from a REST endpoint and convert the json array to a List of Person objects:
 * {{{
 * val people = GET url "http://api.rest.org/person" returning (jsonBodyAsList[Person])
 * }}}
 *
 * Finally a more complete example that using a ScalaTest Spec to verify a simple REST API.
 * {{{
 * class DslSpec extends FlatSpec with Dsl {
 *   "An empty api" should "support adding and deleting a single object" {
 *     using (_ url "http://api.rest.org/person") { implicit rb =>
 *       GET asserting (statusCode is Status.OK, jsonBodyAsList[Person] is EmptyList)
 *       val id = POST body personJson asserting (statusCode is Status.Created) returning (header("X-Person-Id"))
 *       GET / id asserting (statusCode is Status.OK, jsonBodyAs[Person] is Jason)
 *       GET asserting (statusCode is Status.OK, jsonBodyAsList[Person] is Seq(Jason))
 *       DELETE / id asserting (statusCode is Status.OK)
 *       GET / id asserting (statusCode is Status.NotFound)
 *       GET asserting (statusCode is Status.OK, jsonBodyAsList[Person] is EmptyList)
 *     }
 *   }
 * }
 * }}}
 *
 * == Configuring a Request ==
 *
 * The DSL centers around the [[Api.RequestBuilder]], which specifies the properties
 * of the request.  Most expressions begin with the HTTP [[Api.Method]] followed by a
 * call to [[RichRequestBuilder]], this converts the `Method` to a [[Api.RequestBuilder]].
 * The resulting `RequestBuilder` contains both the `Method` and secondary property.
 * For example:
 * {{{
 * GET url "http://api.rest.org/person"
 * }}}
 * is the same as
 * {{{
 * RequestBuilder().withMethod(GET).withUrl("http://api.rest.org/person")
 * }}}
 *
 * The `RequestBuilder` DSL also supports default values passed implicitly into expressions,
 * for example:
 * {{{
 * implicit val defaults = RequestBuilder() addHeader ("Accept", "application/json")
 * GET url "http://api.rest.org/person"
 * }}}
 * creates a `RequestBuilder` with a method, url and accept header set.  The default values
 * are normal expressed the with the [[using]] expression.
 *
 * == Executing a Request ==
 *
 * There are three ways to execute a request: [[RichRequestBuilder]]`.execute`, [[RichResponse]]`.returning`,
 * [[RichRequestBuilder]]`.asserting`, these can all be applied to `RequestBuilder` instances.
 *
 * The `execute` method executes the request with the implicit [[Api.HttpClient]] and returns the `Response`.
 * {{{
 * val response: Response = GET url "http://api.rest.org/person" execute ()
 * }}}
 *
 * The `returning` method executes the request like the `execute` method, except it applies one or more
 * [[Extractor]]s to the `Response` to return only the extracted information.
 * {{{
 * val code1 = GET url "http://api.rest.org/person" returning (StatusCode)
 * val (code2, people) = GET url "http://api.rest.org/person" returning (StatusCode, jsonBodyAsList[Person])
 * }}}
 *
 * The `asserting` method executes the request like the `execute` method, except it verifies the specified
 * value of one or more `Response` values.  `asserting` is normally used with extractors, see [RichExtractor]
 * for more information. `asserting` and `returning` methods can be used in the same expression.
 * {{{
 * GET url "http://api.rest.org/person" asserting (statusCode is Status.OK)
 * val people = GET url "http://api.rest.org/person" asserting (statusCode is Status.OK) returning (jsonBodyAsList[Person])
 * }}}
 *
 *
 * == Working with Extractors ==
 *
 * Extractors are simply functions that take a [[Api.Response]] are extract or convert part of its contents.
 * Extracts are written to assume that the data they require is in the response, if it is not they throw an
 * Exception (failing the test).  See [[Extractors]] for more information on the available default `Extractor`s
 * And how to implement your own.
 */
trait Dsl extends Api with Extractors {
  import language.implicitConversions

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

    def execute()(implicit client: HttpClient): Response = {
      client(builder)
    }

    def apply[T](proc: RequestBuilder => T): T = {
      proc(builder)
    }

    def asserting(assertions: Assertion*)(implicit client: HttpClient): Response = {
      val res = execute()
      val assertionResults = for {
        a <- assertions
        r <- a.result(res)
      } yield r
      if (assertionResults.nonEmpty) {
        throw assertionFailed(assertionResults)
      }
      res
    }

    def expecting[T](func: Response => T)(implicit client: HttpClient): T = {
      val res = execute()
      func(res)
    }
  }

  /**
   * Extend the default request's configuration so that partially configured requests to be reused.  Foe example:
   *
   * {{{
   * using(_ url "http://api.rest.org/person") { implicit rb =>
   *   GET asserting (StatusCode === Status.OK, jsonBodyAsList[Person] === EmptyList)
   *   val id = POST body personJson asserting (StatusCode === Status.Created) returning (Header("X-Person-Id"))
   *   GET / id asserting (StatusCode === Status.OK, jsonBodyAs[Person] === Jason)
   * }
   * }}}
   *
   * @param config
   * 		a function to configure the default request
   * @param block
   *        the block of code where the the newly configured request is applied
   * @param builder
   *        the current default request, implicitly resolved, defaults to the empty request
   */
  def using(config: RequestBuilder => RequestBuilder)(process: RequestBuilder => Unit)(implicit builder: RequestBuilder): Unit = {
    process(config(builder))
  }

  implicit class RichResponse(response: Response) {
    def returning[T1](ext1: ExtractorLike[T1])(implicit client: HttpClient): T1 = {
      ext1.value(response).get
    }

    def returning[T1, T2](ext1: ExtractorLike[T1], ext2: ExtractorLike[T2]): (T1, T2) = {
      val tryValue = for {
        r1 <- ext1.value(response)
        r2 <- ext2.value(response)
      } yield (r1, r2)
      tryValue.get
    }

    def returning[T1, T2, T3](ext1: ExtractorLike[T1], ext2: ExtractorLike[T2], ext3: ExtractorLike[T3]): (T1, T2, T3) = {
      val tryValue = for {
        r1 <- ext1.value(response)
        r2 <- ext2.value(response)
        r3 <- ext3.value(response)
      } yield (r1, r2, r3)
      tryValue.get
    }

    def returning[T1, T2, T3, T4](ext1: ExtractorLike[T1], ext2: ExtractorLike[T2], ext3: ExtractorLike[T3], ext4: ExtractorLike[T4]): (T1, T2, T3, T4) = {
      val tryValue = for {
        r1 <- ext1.value(response)
        r2 <- ext2.value(response)
        r3 <- ext3.value(response)
        r4 <- ext4.value(response)
      } yield (r1, r2, r3, r4)
      tryValue.get
    }
  }

  implicit def requestBuilderToRichResponse(builder: RequestBuilder)(implicit client: HttpClient): RichResponse = new RichResponse(builder.execute())
  implicit def methodToRichResponse(method: Method)(implicit builder: RequestBuilder, client: HttpClient): RichResponse = new RichResponse(builder.withMethod(method).execute())

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

    def <[B >: A](expected: B)(implicit ord: math.Ordering[B]): Assertion = makeAssertion(ord.lt(_, expected), expected, "was not less than")
    def <=[B >: A](expected: B)(implicit ord: math.Ordering[B]): Assertion = makeAssertion(ord.lteq(_, expected), expected, "was not less than or equal")
    def >[B >: A](expected: B)(implicit ord: math.Ordering[B]): Assertion = makeAssertion(ord.gt(_, expected), expected, "was not greater than")
    def >=[B >: A](expected: B)(implicit ord: math.Ordering[B]): Assertion = makeAssertion(ord.gteq(_, expected), expected, "was not greater than or equal")

    def in[B >: A](expectedVals: B*): Assertion = makeAssertion(expectedVals.contains(_), expectedVals.mkString("(", ", ", ")"), "was not in")
    def notIn[B >: A](expectedVals: B*): Assertion = makeAssertion(!expectedVals.contains(_), expectedVals.mkString("(", ", ", ")"), "was in")

    private def makeAssertion[B](pred: A => Boolean, expected: Any, text: String) = new Assertion {
      override def result(res: Response): Option[String] = {
        val actual = ext.value(res)
        actual match {
          case Success(a) =>
            if (!pred(a)) Some(s"${ext.name}: a $text $expected") else None
          case Failure(e) =>
            Some(e.getMessage)
        }
      }
    }

  }
}

object Dsl extends Dsl
