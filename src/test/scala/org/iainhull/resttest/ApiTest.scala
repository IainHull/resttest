package org.iainhull.resttest

import org.junit.Test
import org.scalatest.junit.ShouldMatchersForJUnit
import java.net.URI
import org.scalatest.Suite

class ApiTest extends Suite with ShouldMatchersForJUnit {
  import api._

  val driver = new Driver {
    def execute(request: Request): Response = {
      Response(200, Map("X-Person-Id" -> List("1234")), None)
    }
  }

  @Test
  def simpleDriver {

    val response = driver.execute(Request(method = GET, uri = new URI("http://api.rest.org/person")))
    response.statusCode should be(Status.OK)
  }

  @Test
  def examples {
    val personJson = """{ "name": "Jason" }"""
    val r1 = driver.execute(Request(GET, new URI("http://api.rest.org/person/"), Map(), None))
    val r2 = driver.execute(Request(POST, new URI("http://api.rest.org/person/"), Map(), Some(personJson)))
    val id = r2.headers.get("X-Person-Id").get.head
    val r3 = driver.execute(Request(GET, new URI("http://api.rest.org/person/" + id), Map(), None))
    val r4 = driver.execute(Request(GET, new URI("http://api.rest.org/person/"), Map(), None))
    val r5 = driver.execute(Request(DELETE, new URI("http://api.rest.org/person/" + id), Map(), None))
    val r6 = driver.execute(Request(GET, new URI("http://api.rest.org/person/"), Map(), None))
  }
}