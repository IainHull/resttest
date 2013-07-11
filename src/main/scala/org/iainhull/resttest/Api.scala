package org.iainhull.resttest

import java.net.URI
import java.net.URLEncoder

object Api {
  /** The Method is the entry point start the base url */
  sealed abstract class Method(name: String)

  case object GET extends Method("GET")
  case object POST extends Method("POST")
  case object PUT extends Method("PUT")
  case object DELETE extends Method("DELETE")

  case class Request(method: Method, url: URI, headers: Map[String, List[String]] = Map(), body: Option[String] = None)
  case class Response(statusCode: Int, headers: Map[String, List[String]], body: Option[String])

  def toHeaders(hs: (String, String)*): Map[String, List[String]] = {
    hs.foldRight(Map[String, List[String]]()) {
      case ((name, value), hm) =>
        val listValue = if (value == "") List() else List(value)
        val list = hm.get(name).map(listValue ++ _) getOrElse (listValue)
        hm + (name -> list)
    }
  }

  def toQueryString(qs: (String, String)*): String = {
    def urlEncodeUTF8(s: String): String = URLEncoder.encode(s, "UTF-8")

    if (!qs.isEmpty) {
      qs map {
        case (name, value) =>
          urlEncodeUTF8(name) + "=" + urlEncodeUTF8(value)
      } mkString ("?", "&", "")
    } else {
      ""
    }
  }

  /**
   * Abstract interface for submitting REST `Requests` and receiving `Responses`
   */
  trait Driver {
    def execute(request: Request): Response
  }

  /**
   * Constants for HTTP Status Codes
   */
  object Status {
    val OK = 200
    val Created = 201
    val Accepted = 202
    val BadRequest = 400
    val Unauthorized = 401
    val PaymentRequired = 402
    val Forbidden = 403
    val NotFound = 404
  }

  case class RequestBuilder(
    method: Option[Method],
    url: Option[URI],
    query: Seq[(String, String)],
    headers: Seq[(String, String)],
    queryParams: Seq[(String, String)],
    body: Option[String]) {

    def withMethod(method: Method): RequestBuilder = copy(method = Some(method))
    def withUrl(url: String): RequestBuilder = copy(url = Some(new URI(url)))
    def withBody(body: String): RequestBuilder = copy(body = Some(body))
    def addPath(path: String): RequestBuilder = {
      val s = url.get.toString
      val slash = if (s.endsWith("/")) "" else "/"
      copy(url = Some(new URI(s + slash + path)))
    }
    def addHeaders(hs: (String, String)*) = copy(headers = headers ++ hs)
    def addQuery(qs: (String, String)*) = copy(queryParams = queryParams ++ qs)

    def toRequest: Request = {
      val fullUrl = URI.create(url.get + toQueryString(queryParams: _*))
      Request(method.get, fullUrl, toHeaders(headers: _*), body)
    }

  }

  object RequestBuilder {
    implicit val emptyBuilder = RequestBuilder(None, None, Seq(), Seq(), Seq(), None)

    def apply()(implicit builder: RequestBuilder): RequestBuilder = {
      builder
    }
  }

}