package org.iainhull.resttest

import org.scalatest.junit.ShouldMatchersForJUnit
import org.scalatest.Suite
import org.junit.Test
import java.net.URI

class DslTest extends Suite with ShouldMatchersForJUnit {
  import api._
  import dsl._

  val driver = new Driver {
    def execute(request: Request): Response = {
      Response(200, Map("X-Person-Id" -> List("1234")), None)
    }
  }

  @Test
  def testRequestBuilder {

    val request1: Request = RequestBuilder().withMethod(GET).withUrl("http://localhost/").withBody("body")
    request1 should have('method(GET), 'uri(new URI("http://localhost/")), 'body(Some("body")))

    val request2: Request = RequestBuilder().withMethod(GET).withUrl("http://localhost/").addPath("foo").addPath("bar")
    request2 should have('method(GET), 'uri(new URI("http://localhost/foo/bar")))
  }

  @Test
  def testExamples {
    val personJson = """{ "name": "Jason" }"""
    val rb = RequestBuilder().withUrl("http://api.rest.org/person/")
    val r1 = driver.execute(rb.withMethod(GET))
    val r2 = driver.execute(rb.withMethod(POST).withBody(personJson))
    val id = r2.headers.get("X-Person-Id").get.head
    val r3 = driver.execute(rb.withMethod(GET).addPath(id))
    val r4 = driver.execute(rb.withMethod(GET))
    val r5 = driver.execute(rb.withMethod(DELETE).addPath(id))
    val r6 = driver.execute(rb.withMethod(GET))
  }
}