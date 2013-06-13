package org.iainhull.resttest

import java.net.URI

object dsl {
  import api._

  case class RequestBuilder(
    method: Option[Method],
    url: Option[URI],
    query: Seq[(String, String)],
    headers: Seq[(String, String)],
    body: Option[String]) {

    def withMethod(method: Method): RequestBuilder = copy(method = Some(method))
    def withUrl(url: String): RequestBuilder = copy(url = Some(new URI(url)))
    def withBody(body: String): RequestBuilder = copy(body = Some(body))
    def addPath(path: String): RequestBuilder = {
      val s = url.get.toString
      val slash = if (s.endsWith("/")) "" else "/"
      copy(url = Some(new URI(s + slash + path)))
    }
    def addHeaders(hs: Seq[(String, String)]) = copy(headers = headers ++ hs)

    def toRequest: Request = {
      Request(method.get, url.get, toHeaders(headers: _*), body)
    }
  }

  object RequestBuilder {
    val emptyBuilder = RequestBuilder(None, None, Seq(), Seq(), None)
    
    def apply(): RequestBuilder = {
      emptyBuilder
    }
  }

  implicit def toRequest(builder: RequestBuilder): Request = builder.toRequest
}