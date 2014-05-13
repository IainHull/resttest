package org.iainhull.resttest

import java.net.URI
import java.net.URLEncoder

/**
 * Provides the main api for creating and sending REST Web service requests
 * (as an object for importing).
 *
 * {{{
 * val request = Request(GET, new URI("http://api.rest.org/person", Map(), None))
 * val response = driver.execute(request)
 * response.statusCode should be(Status.OK)
 * response.body match {
 *   Some(body) => objectMapper.readValue[List[Person]](body) should have length(0)
 *   None => fail("Expected a body"))
 * }
 * }}}
 *
 * or using the [[RequestBuilder]]
 * {{{
 * val request = driver.execute(RequestBuilder().withUrl("http://api.rest.org/person/").withMethod(GET))
 * }}}
 *
 * This provides the basic interface used to implement the [[Dsl]], users
 * are expected to use the Dsl.
 */
object Api {
  
  /** HTTP Method */
  sealed abstract class Method(name: String)
  /** HTTP delete method */
  case object DELETE extends Method("DELETE")
  /** HTTP get method */
  case object GET extends Method("GET")
  /** HTTP head method */
  case object HEAD extends Method("HEAD")
  /** HTTP patch method */
  case object PATCH extends Method("PATCH")
  /** HTTP post method */
  case object POST extends Method("POST")
  /** HTTP put method */
  case object PUT extends Method("PUT")

  /** The HTTP Request */
  case class Request(method: Method, url: URI, headers: Map[String, List[String]] = Map(), body: Option[String] = None)

  /** The HTTP Response */
  case class Response(statusCode: Int, headers: Map[String, List[String]], body: Option[String])

  /** 
   * Builder to make creating [[Request]] objects nicer - normally this should
   * be driven through the [[Dsl]]. 
   */
  case class RequestBuilder(
    method: Option[Method],
    url: Option[URI],
    query: Seq[(String, String)],
    headers: Seq[(String, String)],
    queryParams: Seq[(String, String)],
    body: Option[String]) {

    /** specify the method of the {{Request}} */
    def withMethod(method: Method): RequestBuilder = copy(method = Some(method))

    /** specify the url of the {{Request}} */
    def withUrl(url: String): RequestBuilder = copy(url = Some(new URI(url)))

    /** specify the body of the {{Request}} */
    def withBody(body: String): RequestBuilder = copy(body = Some(body))

    /** Append the specified path to the url of the {{Request}}, fails if url not set yet */
    def addPath(path: String): RequestBuilder = {
      val s = url.get.toString
      val slash = if (s.endsWith("/")) "" else "/"
      copy(url = Some(new URI(s + slash + path)))
    }
    
    /** Add headers to the {{Request}} */
    def addHeaders(hs: (String, String)*) = copy(headers = headers ++ hs)
    
    /** Add query parameters to the {{Request}} */
    def addQuery(qs: (String, String)*) = copy(queryParams = queryParams ++ qs)

    /** Build the {{Request}} */
    def toRequest: Request = {
      val fullUrl = URI.create(url.get + toQueryString(queryParams: _*))
      Request(method.get, fullUrl, toHeaders(headers: _*), body)
    }
  }

  /**
   * Helper object for RequestBuilder class, supplies the emptyBuilder 
   * instance and access to the current implicit builder. 
   */
  object RequestBuilder {
    /**
     * The initial empty RequestBuilder instance.  This is the default implicit 
     * instance.
     */
    implicit val emptyBuilder = RequestBuilder(None, None, Seq(), Seq(), Seq(), None)

    /**
     * Returns the current RequestBuilder from the implicit context.
     */
    def apply()(implicit builder: RequestBuilder): RequestBuilder = {
      builder
    }
  }

  /**
   * Abstract interface for submitting REST `Requests` and receiving `Responses`
   */
  type HttpClient = Request => Response
  
  /**
   * Constants for HTTP Status Codes
   */
  object Status {
    val Continue = 100
    val SwitchingProtocols = 101
    val OK = 200
    val Created = 201
    val Accepted = 202
    val NonAuthoritativeInformation = 203
    val NoContent = 204
    val ResetContent = 205
    val PartialContent = 206
    val MultipleChoices = 300
    val MovedPermanently = 301
    val Found = 302
    val SeeOther = 303
    val NotModified = 304
    val UseProxy = 305
    val SwitchProxy = 306
    val TemporaryRedirect = 307
    val PermanentRedirect = 308
    val BadRequest = 400
    val Unauthorized = 401
    val PaymentRequired = 402
    val Forbidden = 403
    val NotFound = 404
    val MethodNotAllowed = 405
    val NotAcceptable = 406
    val ProxyAuthenticationRequired = 407
    val RequestTimeout = 408
    val Conflict = 409
    val Gone = 410
    val LengthRequired = 411
    val PreconditionFailed = 412
    val RequestEntityTooLarge = 413
    val RequestUriTooLong = 414
    val UnsupportedMediaType = 415
    val RequestedRangeNotSatisfiable = 416
    val ExpectationFailed = 417
    val InternalServerError = 500
    val NotImplemented = 501
    val BadGateway = 502
    val ServiceUnavailable = 503
    val GatewayTimeout = 504
    val HttpVersionNotSupported = 505
  }

  /**
   * Convert a sequence of `(name, value)` tuples into a map of headers.
   * Each tuple creates an entry in the map, duplicate `name`s add the
   * `value` to the list.
   */
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
}

/**
 * Provides the main api for creating and sending REST Web service requests 
 * (as a trait for mixing in).
 */
trait Api {
  /** The HTTP Methods used to make a request */
  type Method = Api.Method
  val GET = Api.GET
  val POST = Api.POST
  val PUT = Api.PUT
  val DELETE = Api.DELETE
  val HEAD = Api.HEAD
  val PATCH = Api.PATCH

  /** The HTTP Request */
  type Request = Api.Request
  val Request = Api.Request

  /** The HTTP Response */
  type Response = Api.Response
  val Response = Api.Response

  /** The HTTP RequestBuilder */
  type RequestBuilder = Api.RequestBuilder
  val RequestBuilder = Api.RequestBuilder

  /**
   * HttpClient
   */
  type HttpClient = Api.HttpClient

  val Status = Api.Status

  def toHeaders(hs: (String, String)*): Map[String, List[String]] = Api.toHeaders(hs: _*)
  def toQueryString(qs: (String, String)*): String = toQueryString(qs: _*)
}
