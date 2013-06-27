package org.iainhull.resttest

import java.net.URI
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.ShouldMatchersForJUnit
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ApiTest extends FlatSpec with ShouldMatchers {
  import Api._

  val driver = new Driver {
    def execute(request: Request): Response = {
      Response(200, Map("X-Person-Id" -> List("1234")), None)
    }
  }

  "A Simple Driver" should "take a request and return a static response" in {
    val response = driver.execute(Request(method = GET, url = new URI("http://api.rest.org/person")))
    response.statusCode should be(Status.OK)
  }

  "The Api" should "support a simple rest use case, if a little long winded" in {
    val personJson = """{ "name": "Jason" }"""
    val r1 = driver.execute(Request(GET, new URI("http://api.rest.org/person/"), Map(), None))
    val r2 = driver.execute(Request(POST, new URI("http://api.rest.org/person/"), Map(), Some(personJson)))
    val id = r2.headers.get("X-Person-Id").get.head
    val r3 = driver.execute(Request(GET, new URI("http://api.rest.org/person/" + id), Map(), None))
    val r4 = driver.execute(Request(GET, new URI("http://api.rest.org/person/"), Map(), None))
    val r5 = driver.execute(Request(DELETE, new URI("http://api.rest.org/person/" + id), Map(), None))
    val r6 = driver.execute(Request(GET, new URI("http://api.rest.org/person/"), Map(), None))
  }

  "A RequestBuilder" should "simplify the creation of request objects" in {
    val request1: Request = RequestBuilder().withMethod(GET).withUrl("http://localhost/").withBody("body").toRequest
    request1 should have('method(GET), 'uri(new URI("http://localhost/")), 'body(Some("body")))

    val request2: Request = RequestBuilder().withMethod(GET).withUrl("http://localhost/").addPath("foo").addPath("bar").toRequest
    request2 should have('method(GET), 'uri(new URI("http://localhost/foo/bar")))
  }
  
  it should "support reuse of partialy constructed builders (ensure builder is immutable)" in {
    val base = RequestBuilder().withMethod(GET).withUrl("http://localhost/").withBody("body")
    val rb1 = base.withMethod(POST).addPath("foo")
    val rb2 = base.withBody("everybody")
    
    base.toRequest should have('method(GET), 'uri(new URI("http://localhost/")), 'body(Some("body")))
    rb1.toRequest should have('method(POST), 'uri(new URI("http://localhost/foo")), 'body(Some("body")))
    rb2.toRequest should have('method(GET), 'uri(new URI("http://localhost/")), 'body(Some("everybody")))
  }
}