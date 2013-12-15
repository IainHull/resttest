package org.iainhull.resttest

import java.net.URI
import java.net.URLEncoder

/**
 * Provides the main api for creating and sending REST Web service requests.
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
trait Api {
  /** The HTTP Methods used to make a request */
  type Method = Api.Impl.Method
  val GET = Api.Impl.GET
  val POST = Api.Impl.POST
  val PUT = Api.Impl.PUT
  val DELETE = Api.Impl.DELETE
  val HEAD = Api.Impl.HEAD
  val PATCH = Api.Impl.PATCH

  /** The HTTP Request */
  type Request = Api.Impl.Request
  val Request = Api.Impl.Request

  /** The HTTP Response */
  type Response = Api.Impl.Response
  val Response = Api.Impl.Response

  /** The HTTP RequestBuilder */
  type RequestBuilder = Api.Impl.RequestBuilder
  val RequestBuilder = Api.Impl.RequestBuilder

  /**
   * Abstract interface for submitting REST `Requests` and receiving `Responses`
   */
  type HttpClient = Request => Response

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

  val Status = Api.Impl.Status
}

object Api extends Api {

  /**
   * Implementation types used by the [[Api]], these are forward declared in the `Api` trait
   */
  object Impl {
    sealed abstract class Method(name: String)
    case object DELETE extends Method("DELETE")
    case object GET extends Method("GET")
    case object HEAD extends Method("HEAD")
    case object PATCH extends Method("PATCH")
    case object POST extends Method("POST")
    case object PUT extends Method("PUT")

    /** The HTTP Request */
    case class Request(method: Method, url: URI, headers: Map[String, List[String]] = Map(), body: Option[String] = None)

    /** The HTTP Response */
    case class Response(statusCode: Int, headers: Map[String, List[String]], body: Option[String])

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
  }
}