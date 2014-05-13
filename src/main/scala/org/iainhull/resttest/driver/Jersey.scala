package org.iainhull.resttest.driver

import com.sun.jersey.api.client.Client
import com.sun.jersey.api.client.ClientResponse
import com.sun.jersey.api.client.WebResource
import org.iainhull.resttest.Api
import org.iainhull.resttest.TestDriver
import scala.collection.JavaConverters

/**
 * Provides the Jersey httpClient implementation (as a trait to support
 * mixing in).
 */
trait Jersey extends Api {
  import Jersey.Impl

  implicit val httpClient: HttpClient = { request =>
    val response = Impl.createClientResponse(request)
    Response(response.getStatus, Impl.headers(response), Some(response.getEntity(classOf[String])))
  }
}

/**
 * Provides the Jersey httpClient implementation (as an object to support
 * straight import).
 */
object Jersey extends Jersey {
  private object Impl {

    val jersey = Client.create()

    def createClientResponse(request: Request): ClientResponse = {
      val builder = addRequestHeaders(request.headers, jersey.resource(request.url).getRequestBuilder)

      for (b <- request.body) {
        builder.entity(b)
      }

      request.method match {
        case GET => builder.get(classOf[ClientResponse])
        case POST => builder.post(classOf[ClientResponse])
        case PUT => builder.put(classOf[ClientResponse])
        case DELETE => builder.delete(classOf[ClientResponse])
        case HEAD => builder.method("HEAD", classOf[ClientResponse])
        case PATCH => builder.method("PATCH", classOf[ClientResponse])
      }
    }

    def addRequestHeaders(headers: Map[String, List[String]], builder: WebResource#Builder): WebResource#Builder = {
      def forAllNames(names: List[String], b: WebResource#Builder): WebResource#Builder = {
        names match {
          case h :: t => forAllNames(t, forAllValues(h, headers(h), b))
          case Nil => b
        }
      }
      def forAllValues(name: String, values: List[String], b: WebResource#Builder): WebResource#Builder = {
        values match {
          case h :: t => forAllValues(name, t, b.header(name, h))
          case Nil => b
        }
      }
      forAllNames(headers.keys.toList, builder)
    }

    def headers(response: ClientResponse): Map[String, List[String]] = {
      import JavaConverters._

      response.getHeaders.asScala.toMap.map {
        case (k, v) =>
          (k, v.asScala.toList)
      }
    }
  }
}

/**
 * The JerseySystemTestDriver can be mixed into Rest Test Suites to execute
 * then with Jersey.  Suites must provide the baseUrl from which all test
 * paths are relative.
 */
trait JerseySystemTestDriver extends TestDriver with Jersey {
  override implicit def defBuilder = RequestBuilder.emptyBuilder withUrl baseUrl

  /**
   * Implements must specify the baseUrl from which all test paths are relative.
   */
  def baseUrl: String
}