package org.iainhull.resttest.driver

import spray.testkit.ScalatestRouteTest
import org.iainhull.resttest.TestDriver
import org.scalatest.Suite
import scala.collection.JavaConversions
import spray.http.HttpHeaders.RawHeader
import spray.http.MediaType
import spray.http.HttpEntity
import spray.http.ContentType
import spray.routing.Route
import org.iainhull.resttest.Api

trait SprayUnitTestDriver extends TestDriver with ScalatestRouteTest {
  this: Suite =>
   
  import Api._

  override implicit val httpClient: HttpClient = { req =>
      import JavaConversions._
      
	  val rb1 = req.method match {
	    case GET => Get(req.url.getPath())
	    case POST => Post(req.url.getPath())
	    case PUT => Put(req.url.getPath())
	    case DELETE => Delete(req.url.getPath())
	    case HEAD => Head(req.url.getPath())
	    case PATCH => Patch(req.url.getPath())
	  }
	  
	  val headers = req.headers.map { case (name, list) => RawHeader(name, list.mkString(", ")) }.toList
	  val contentType = headers.find(_.lowercaseName == "content-type").map(_.value).getOrElse("text/plain")
	  val mediaType = MediaType.custom(contentType)
	  val rb2 = rb1.withHeadersAndEntity(headers, req.body.map(HttpEntity(ContentType(mediaType),_)).getOrElse(HttpEntity.Empty))
	  
	  rb2 ~> myRoute ~> check {
	    val responseHeaders = response.headers.map( h => h.name -> h.value.split(", ").toList).toSeq
	    Response(status.intValue, Map() ++ responseHeaders, Some(responseAs[String]))
	  }
	}

  override implicit def defBuilder: Api.RequestBuilder = Api.RequestBuilder.emptyBuilder withUrl ("")
  
  def actorRefFactory = system
  def myRoute: Route
}