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

  def withUrl(url: String)(implicit builder: RequestBuilder): RequestBuilder = builder.withUrl(url)
  def withBody(body: String)(implicit builder: RequestBuilder): RequestBuilder = builder.withBody(body)
  def addPath(path: String)(implicit builder: RequestBuilder): RequestBuilder = builder.addPath(path)
  def addHeaders(hs: Seq[(String, String)])(implicit builder: RequestBuilder) = builder.addHeaders(hs)
}