package org.iainhull.resttest

import java.net.URI

object Dsl {
  import Api._

  implicit def toRequest(builder: RequestBuilder): Request = builder.toRequest
  implicit def methodToRequestBuilder(method: Method)(implicit builder: RequestBuilder): RequestBuilder = builder.withMethod(method)
  implicit def methodToRichRequestBuilder(method: Method)(implicit builder: RequestBuilder): RichRequestBuilder = new RichRequestBuilder(methodToRequestBuilder(method)(builder))

  implicit class RichRequestBuilder(builder: RequestBuilder) {
    def execute()(implicit driver: Driver): Response = {
      driver.execute(builder)
    }

    def apply[T](proc: RequestBuilder => T): T = {
      proc(builder)
    }
  }
}