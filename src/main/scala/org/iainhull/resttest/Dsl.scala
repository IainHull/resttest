package org.iainhull.resttest

import java.net.URI

object Dsl {
  import Api._

  implicit def toRequest(builder: RequestBuilder): Request = builder.toRequest
  implicit def methodToRequestBuilder(method: Method)(implicit builder: RequestBuilder): RequestBuilder = builder.withMethod(method)
  implicit def methodToRichRequestBuilder(method: Method)(implicit builder: RequestBuilder): RichRequestBuilder = new RichRequestBuilder(methodToRequestBuilder(method)(builder))

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
  }
}