package org.iainhull.resttest

import java.net.URI

object Dsl extends Extractors {
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

    def expecting[T](func: Response => T)(implicit driver: Driver): T = {
      val res = execute()
      func(res)
    }
  }

  object ~ {
    def unapply(res: Response): Option[(Response, Response)] = {
      Some((res, res))
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