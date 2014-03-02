package org.iainhull.resttest

import java.net.URI
import org.scalatest.FlatSpec
import org.scalatest.Matchers
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ApiSpec extends FlatSpec with Matchers {
  import Api._
  import TestData._

  "A Simple Driver" should "take a request and return a static response" in {
    val response = TestClient(Request(method = GET, url = new URI("http://api.rest.org/person")))
    response.statusCode should be(Status.OK)
  }

  "The Api" should "support a simple rest use case, if a little long winded" in {
    val personJson = """{ "name": "Jason" }"""
    val r1 = TestClient(Request(GET, new URI("http://api.rest.org/person/"), Map(), None))
    val r2 = TestClient(Request(POST, new URI("http://api.rest.org/person/"), Map(), Some(personJson)))
    val id = r2.headers("X-Person-Id").head
    val r3 = TestClient(Request(GET, new URI("http://api.rest.org/person/" + id), Map(), None))
    val r4 = TestClient(Request(GET, new URI("http://api.rest.org/person/"), Map(), None))
    val r5 = TestClient(Request(DELETE, new URI("http://api.rest.org/person/" + id), Map(), None))
    val r6 = TestClient(Request(GET, new URI("http://api.rest.org/person/"), Map(), None))
  }

  "A RequestBuilder" should "simplify the creation of request objects" in {
    val request1: Request = RequestBuilder().withMethod(GET).withUrl("http://localhost/").withBody("body").toRequest
    request1 should have('method(GET), 'url(new URI("http://localhost/")), 'body(Some("body")))

    val request2: Request = RequestBuilder().withMethod(GET).withUrl("http://localhost/").addPath("foo").addPath("bar").toRequest
    request2 should have('method(GET), 'url(new URI("http://localhost/foo/bar")))
  }
  
  it should "support reuse of partialy constructed builders (ensure builder is immutable)" in {
    val base = RequestBuilder().withMethod(GET).withUrl("http://localhost/").withBody("body")
    val rb1 = base.withMethod(POST).addPath("foo")
    val rb2 = base.withBody("everybody")
    
    base.toRequest should have('method(GET), 'url(new URI("http://localhost/")), 'body(Some("body")))
    rb1.toRequest should have('method(POST), 'url(new URI("http://localhost/foo")), 'body(Some("body")))
    rb2.toRequest should have('method(GET), 'url(new URI("http://localhost/")), 'body(Some("everybody")))
  }
}