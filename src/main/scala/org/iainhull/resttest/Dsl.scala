package org.iainhull.resttest

import java.net.URI

object Dsl {
  import Api._

  implicit def toRequest(builder: RequestBuilder): Request = builder.toRequest
  implicit def methodToRequestBuilder(method: Method): RequestBuilder = RequestBuilder().withMethod(method)
  
  type Return[T] = Response => T
  
  implicit class RichRequestBuilder(builder: RequestBuilder)(implicit driver: Driver) {
    def execute(): Response = {
      driver.execute(builder)
    }
  }
}