package org.iainhull.resttest

import org.scalatest.junit.ShouldMatchersForJUnit
import org.scalatest.Suite
import org.junit.Test
import java.net.URI

class DslTest extends Suite with ShouldMatchersForJUnit {
  import Api._
  import Dsl._

  implicit val driver = new Driver {
    def execute(request: Request): Response = {
      Response(200, Map("X-Person-Id" -> List("1234")), None)
    }
  }

  @Test
  def testExample1 {
    val personJson = """{ "name": "Jason" }"""
    val r1 = driver.execute(RequestBuilder().withMethod(GET).withUrl("http://api.rest.org/person/"))
    val r2 = driver.execute(RequestBuilder().withMethod(POST).withUrl("http://api.rest.org/person/").withBody(personJson))
    val id = r2.headers.get("X-Person-Id").get.head
    val r3 = driver.execute(RequestBuilder().withMethod(GET).withUrl("http://api.rest.org/person/").addPath(id))
    val r4 = driver.execute(RequestBuilder().withMethod(GET).withUrl("http://api.rest.org/person/"))
    val r5 = driver.execute(RequestBuilder().withMethod(DELETE).withUrl("http://api.rest.org/person/").addPath(id))
    val r6 = driver.execute(RequestBuilder().withMethod(GET).withUrl("http://api.rest.org/person/"))
  }

  @Test
  def testExample2 {
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

  @Test
  def example3 {
    val personJson = """{ "name": "Jason" }"""
    val r1 = driver.execute(GET withUrl "http://api.rest.org/person/")
    val r2 = driver.execute(POST withUrl "http://api.rest.org/person/" withBody personJson)
    val id = r2.headers.get("X-Person-Id").get.head
    val r3 = driver.execute(GET withUrl "http://api.rest.org/person/" addPath id)
    val r4 = driver.execute(GET withUrl "http://api.rest.org/person/")
    val r5 = driver.execute(DELETE withUrl "http://api.rest.org/person/" addPath id)
    val r6 = driver.execute(GET withUrl "http://api.rest.org/person/")
  }

  @Test
  def example4 {
    val personJson = """{ "name": "Jason" }"""
    val r1 = GET withUrl "http://api.rest.org/person/" execute ()
    val r2 = POST withUrl "http://api.rest.org/person/" withBody personJson execute ()
    val id = r2.headers.get("X-Person-Id").get.head
    val r3 = GET withUrl "http://api.rest.org/person/" addPath id execute ()
    val r4 = GET withUrl "http://api.rest.org/person/" execute ()
    val r5 = DELETE withUrl "http://api.rest.org/person/" addPath id execute ()
    val r6 = GET withUrl "http://api.rest.org/person/" execute ()
  }
}