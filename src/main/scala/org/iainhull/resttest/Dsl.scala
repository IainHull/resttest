package org.iainhull.resttest

import java.net.URI

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
 * creates a `RequestBuilder` with the method and url properties set.
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
 * [[RichRequestBuilder]]`.asserting`.
 * 
 * The `execute` method executes the request with the implicit [[Api.Driver]] and returns the `Response`.
 * {{{
 * val response: Response = GET url "http://api.rest.org/person" execute ()
 * }}}
 * 
 * The `returning` method executes the request like the `execute` method, except it applies one or more
 * [[Extractor]]s to the `Response` to return only the extracted information. 
 * {{{
 * val code1 = GET url "http://api.rest.org/person" returning (statusCode)
 * val (code2, people) = GET url "http://api.rest.org/person" returning (statusCode, jsonBodyAsList[Person])
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
trait Dsl extends Extractors {
  import language.implicitConversions
  import Api._

  implicit def toRequest(builder: RequestBuilder): Request = builder.toRequest
  implicit def methodToRequestBuilder(method: Method)(implicit builder: RequestBuilder): RequestBuilder = builder.withMethod(method)
  implicit def methodToRichRequestBuilder(method: Method)(implicit builder: RequestBuilder): RichRequestBuilder = new RichRequestBuilder(methodToRequestBuilder(method)(builder))

  trait Assertion {
    def verify(res: Response): Unit
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
      assertions foreach (_.verify(res))
      res
    }
  }

  def using(config: RequestBuilder => RequestBuilder)(process: RequestBuilder => Unit)(implicit builder: RequestBuilder): Unit = {
    process(config(builder))
  }
  
  implicit class RichResponse(response: Response) {
    def returning[T1](ext1: Extractor[T1])(implicit driver: Driver): T1 = {
      ext1.op(response)
    }

    def returning[T1, T2](ext1: Extractor[T1], ext2: Extractor[T2]): (T1, T2) = {
      (ext1.op(response), ext2.op(response))
    }

    def returning[T1, T2, T3](ext1: Extractor[T1], ext2: Extractor[T2], ext3: Extractor[T3]): (T1, T2, T3) = {
      (ext1.op(response), ext2.op(response), ext3.op(response))
    }

    def returning[T1, T2, T3, T4](ext1: Extractor[T1], ext2: Extractor[T2], ext3: Extractor[T3], ext4: Extractor[T4]): (T1, T2, T3, T4) = {
      (ext1.op(response), ext2.op(response), ext3.op(response), ext4.op(response))
    }
  }
  
  implicit def requestBuilderToRichResponse(builder: RequestBuilder)(implicit driver: Driver): RichResponse = new RichResponse(builder.execute())
  implicit def methodToRichResponse(method: Method)(implicit builder: RequestBuilder, driver: Driver): RichResponse = new RichResponse(builder.withMethod(method).execute())

  implicit class RichExtractor[T](ext: Extractor[T]) {
    def is(expected: T): Assertion = new Assertion {
      override def verify(res: Response): Unit = {
        val actual = ext.op(res)
        if (actual != expected) throw new AssertionError(actual + " != " + expected)
      }
    }

    def isNot(expected: T): Assertion = new Assertion {
      override def verify(res: Response): Unit = {
        val actual = ext.op(res)
        if (actual == expected) throw new AssertionError(actual + " == " + expected)
      }
    }
    def isIn(expectedVals: T*): Assertion = new Assertion {
      override def verify(res: Response): Unit = {
        val actual = ext.op(res)
        if (!expectedVals.contains(actual)) throw new AssertionError(actual + " not in " + expectedVals)
      }
    }
  }
}

object Dsl extends Dsl
