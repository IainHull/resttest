package org.iainhull.resttest

import org.scalatest.junit.ShouldMatchersForJUnit
import org.scalatest.Suite
import org.junit.Test
import java.net.URI
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import play.api.libs.json._

@RunWith(classOf[JUnitRunner])
class DslSpec extends FlatSpec with ShouldMatchers {
  import Api._
  import Dsl._
  import TestData._

  implicit val driver = newTestDriver

  "The DSL" should "support a basic rest use case with a RequestBuilder" in {
    RequestBuilder().withMethod(GET).withUrl("http://api.rest.org/person/").toRequest should
      have('method(GET), 'url(new URI("http://api.rest.org/person/")))

    RequestBuilder().withMethod(POST).withUrl("http://api.rest.org/person/").withBody(personJson).toRequest should
      have('method(POST), 'url(new URI("http://api.rest.org/person/")), 'body(Some(personJson)))

    val id = "myid"
    RequestBuilder().withMethod(GET).withUrl("http://api.rest.org/person/").addPath(id).toRequest should
      have('method(GET), 'url(new URI("http://api.rest.org/person/myid")))

    RequestBuilder().withMethod(DELETE).withUrl("http://api.rest.org/person/").addPath(id).toRequest should
      have('method(DELETE), 'url(new URI("http://api.rest.org/person/myid")))
  }

  it should "support a basic rest use case, reusing a RequestBuilder" in {
    val rb = RequestBuilder().withUrl("http://api.rest.org/person/")

    rb.withMethod(GET).toRequest should
      have('method(GET), 'url(new URI("http://api.rest.org/person/")))

    rb.withMethod(POST).withBody(personJson).toRequest should
      have('method(POST), 'url(new URI("http://api.rest.org/person/")), 'body(Some(personJson)))

    val id = "myid"
    rb.withMethod(GET).addPath(id).toRequest should
      have('method(GET), 'url(new URI("http://api.rest.org/person/myid")))

    rb.withMethod(DELETE).addPath(id).toRequest should
      have('method(DELETE), 'url(new URI("http://api.rest.org/person/myid")))
  }

  it should "support a basic rest use case, with Method boostrapping the DSL and infix notation" in {
    (GET withUrl "http://api.rest.org/person/").toRequest should
      have('method(GET), 'url(new URI("http://api.rest.org/person/")))

    (POST withUrl "http://api.rest.org/person/" withBody personJson).toRequest should
      have('method(POST), 'url(new URI("http://api.rest.org/person/")), 'body(Some(personJson)))

    val id = "myid"
    (GET withUrl "http://api.rest.org/person/" addPath id).toRequest should
      have('method(GET), 'url(new URI("http://api.rest.org/person/myid")))

    (DELETE withUrl "http://api.rest.org/person/" addPath id).toRequest should
      have('method(DELETE), 'url(new URI("http://api.rest.org/person/myid")))
  }

  it should "support a basic rest use case, with Method boostrapping the DSL and execute method" in {
    GET withUrl "http://api.rest.org/person/" execute () should be(driver.nextResponse)
    driver.lastRequest should have('method(GET), 'url(new URI("http://api.rest.org/person/")))

    POST withUrl "http://api.rest.org/person/" withBody personJson execute ()
    driver.lastRequest should have('method(POST), 'url(new URI("http://api.rest.org/person/")), 'body(Some(personJson)))
  }

  it should "support abstracting common values with codeblocks" in {
    RequestBuilder() withUrl "http://api.rest.org/person/" apply { implicit rb =>
      GET execute ()
      driver.lastRequest should have('method(GET), 'url(new URI("http://api.rest.org/person/")))

      POST withBody personJson execute ()
      driver.lastRequest should have('method(POST), 'url(new URI("http://api.rest.org/person/")), 'body(Some(personJson)))

      val id = "myid"
      GET addPath id execute ()
      driver.lastRequest should have('method(GET), 'url(new URI("http://api.rest.org/person/myid")))
    }
  }

  it should "support abstracting common values with nested codeblocks" in {
    RequestBuilder() withUrl "http://api.rest.org/person/" apply { implicit rb =>
      RequestBuilder() addHeaders ("X-Custom-Header" -> "foo") apply { implicit rb =>
        GET execute ()
        driver.lastRequest should have('method(GET), 'url(new URI("http://api.rest.org/person/")), 'headers(toHeaders("X-Custom-Header" -> "foo")))
      }
    }
  }

  it should "support abstracting common values with codeblocks and method aliases" in {
    RequestBuilder() url "http://api.rest.org/" apply { implicit rb =>
      GET / 'person :? ('page -> 2, 'per_page -> 100) execute ()
      driver.lastRequest should have('method(GET), 'url(new URI("http://api.rest.org/person?page=2&per_page=100")))
    }
  }
  
  it should "support abstracting common values with using method" in {
    using(_ url "http://api.rest.org/") { implicit rb =>
      GET / 'person :? ('page -> 2, 'per_page -> 100) execute ()
      driver.lastRequest should have('method(GET), 'url(new URI("http://api.rest.org/person?page=2&per_page=100")))
    }
  }
  
  it should "support returning values from the response" in {
    using(_ url "http://api.rest.org/person/") { implicit rb =>
      val (c1, b1) = GET returning (StatusCode, BodyText)
      driver.lastRequest should have('method(GET), 'url(new URI("http://api.rest.org/person/")))
      c1 should be(Status.OK)
      b1 should be("body")
    }
  }

  it should "support asserting values equals check" in {
    using(_ url "http://api.rest.org/person/") { implicit rb =>
      GET asserting (StatusCode === Status.OK)
      driver.lastRequest should have('method(GET), 'url(new URI("http://api.rest.org/person/")))
      
      GET asserting (Header("X-Person-Id") === "1234")

      val e = evaluating { GET asserting (StatusCode === Status.Created) } should produce[AssertionError]
      driver.lastRequest should have('method(GET), 'url(new URI("http://api.rest.org/person/")))
      e should have('message("statusCode: 200 did not equal 201"))
    }
  }
  
  it should "support asserting values not-equals check" in {
    using(_ url "http://api.rest.org/person/") { implicit rb =>
      GET asserting (StatusCode !== Status.Created)
      driver.lastRequest should have('method(GET), 'url(new URI("http://api.rest.org/person/")))
      
      GET asserting (Header("X-Person-Id") !== "999")

      val e = evaluating { GET asserting (StatusCode !== Status.OK) } should produce[AssertionError]
      e should have('message("statusCode: 200 did equal 200"))
    }
  }
  
  it should "support asserting values in check" in {
    using(_ url "http://api.rest.org/person/") { implicit rb =>
      GET asserting (StatusCode in (Status.OK, Status.Created))
      driver.lastRequest should have('method(GET), 'url(new URI("http://api.rest.org/person/")))
      
      val e = evaluating { GET asserting (StatusCode in (Status.Created, Status.Accepted)) } should produce[AssertionError]
      e should have('message("statusCode: 200 was not in (201, 202)"))
    }
  }
  
  it should "support asserting values Ordered comparison operator checks" in {
    using(_ url "http://api.rest.org/person/") { implicit rb =>
    GET asserting (StatusCode > 1)
    GET asserting (StatusCode >= 1)
    GET asserting (StatusCode < 299)
    GET asserting (StatusCode <= 299)
    
      val e = evaluating { GET asserting (StatusCode > 999) } should produce[AssertionError]
      driver.lastRequest should have('method(GET), 'url(new URI("http://api.rest.org/person/")))
      e should have('message("statusCode: 200 was not greater than 999"))
    }
  }

  /**
   * These use-cases do not contain any asserts they are simply use to show
   * the DSL supports various forms of syntax.  If they compile they work.
   * The workings of the DSL are checked above, those tests verify that the
   * functionality of the DSL works as expected, but are not as easy to read
   * Each test starts with a use-case to verify the syntax which is then ported
   * to a test above to verify the functionality.
   */
  "Sample use-case" should "support a basic rest use case with a RequestBuilder" in {
    val r1 = driver.execute(RequestBuilder().withMethod(GET).withUrl("http://api.rest.org/person/"))
    val r2 = driver.execute(RequestBuilder().withMethod(POST).withUrl("http://api.rest.org/person/").withBody(personJson))
    val id = r2.headers.get("X-Person-Id").get.head
    val r3 = driver.execute(RequestBuilder().withMethod(GET).withUrl("http://api.rest.org/person/").addPath(id))
    val r4 = driver.execute(RequestBuilder().withMethod(GET).withUrl("http://api.rest.org/person/"))
    val r5 = driver.execute(RequestBuilder().withMethod(DELETE).withUrl("http://api.rest.org/person/").addPath(id))
    val r6 = driver.execute(RequestBuilder().withMethod(GET).withUrl("http://api.rest.org/person/"))
  }

  it should "support a basic rest use case, reusing a RequestBuilder" in {
    val rb = RequestBuilder().withUrl("http://api.rest.org/person/")
    val r1 = driver.execute(rb.withMethod(GET))
    val r2 = driver.execute(rb.withMethod(POST).withBody(personJson))
    val id = r2.headers.get("X-Person-Id").get.head
    val r3 = driver.execute(rb.withMethod(GET).addPath(id))
    val r4 = driver.execute(rb.withMethod(GET))
    val r5 = driver.execute(rb.withMethod(DELETE).addPath(id))
    val r6 = driver.execute(rb.withMethod(GET))
  }

  it should "support a basic rest use case, with Method boostrapping the DSL and infix notation" in {
    val r1 = driver.execute(GET withUrl "http://api.rest.org/person/")
    val r2 = driver.execute(POST withUrl "http://api.rest.org/person/" withBody personJson)
    val id = r2.headers.get("X-Person-Id").get.head
    val r3 = driver.execute(GET withUrl "http://api.rest.org/person/" addPath id)
    val r4 = driver.execute(GET withUrl "http://api.rest.org/person/")
    val r5 = driver.execute(DELETE withUrl "http://api.rest.org/person/" addPath id)
    val r6 = driver.execute(GET withUrl "http://api.rest.org/person/")
  }

  it should "support a basic rest use case, with Method boostrapping the DSL and execute method" in {
    val r1 = GET withUrl "http://api.rest.org/person/" execute ()
    val r2 = POST withUrl "http://api.rest.org/person/" withBody personJson execute ()
    val id = r2.headers.get("X-Person-Id").get.head
    val r3 = GET withUrl "http://api.rest.org/person/" addPath id execute ()
    val r4 = GET withUrl "http://api.rest.org/person/" execute ()
    val r5 = DELETE withUrl "http://api.rest.org/person/" addPath id execute ()
    val r6 = GET withUrl "http://api.rest.org/person/" execute ()
  }

  it should "support abstracting common values with codeblocks" in {
    RequestBuilder() withUrl "http://api.rest.org/person/" addHeaders
      ("Content-Type" -> "application/json") apply { implicit rb =>
        val r1 = GET execute ()
        val r2 = POST withBody personJson execute ()
        val id = r2.headers("X-Person-Id").head
        val r3 = GET addPath id execute ()
        val r4 = GET execute ()
        val r5 = DELETE addPath id execute ()
        val r6 = GET execute ()
      }
  }

  it should "support abstracting common values with codeblocks and method aliases" in {
    RequestBuilder() url "http://api.rest.org/" apply { implicit rb =>
      val r1 = GET / 'person execute ()
      val r2 = POST / 'person body personJson execute ()
      val id = r2.headers("X-Person-Id").head
      val r3 = GET / 'person / id execute ()
      val r4 = GET / 'person execute ()
      val r5 = DELETE / 'person / id execute ()
      val r6 = GET / 'person :? ('page -> 2, 'per_page -> 100) execute ()
    }
  }

  it should "support shorter names for common builder methods" in {
    RequestBuilder() url "http://api.rest.org/" apply { implicit rb =>
      val r1 = GET / 'person execute ()
      val r2 = POST / 'person body personJson execute ()
      val id = r2.headers("X-Person-Id").head
      val r3 = GET / 'person / id execute ()
      val r4 = GET / 'person execute ()
      val r5 = DELETE / 'person / id execute ()
      val r6 = GET / 'person :? ('page -> 2, 'per_page -> 100) execute ()
    }
  }

  it should "support returning values from the response" in {
    RequestBuilder() url "http://api.rest.org/person/" apply { implicit rb =>
      val (c1, b1) = GET returning (StatusCode, Body)
      val (c2, id) = POST body personJson returning (StatusCode, Header("X-Person-Id"))
      val (c3, b3) = GET / id returning (StatusCode, Body)
      val (c4, b4) = GET returning (StatusCode, Body)
      val c5 = DELETE / id returning StatusCode
      val (c6, b6) = GET returning (StatusCode, Body)
    }
  }

  it should "support asserting on values from the response" in {
    import JsonExtractors._
    val EmptyList = Seq()

    driver.responses = Response(Status.OK, Map(), Some("[]")) ::
      Response(Status.Created, toHeaders("X-Person-Id" -> "99"), None) ::
      Response(Status.OK, Map(), Some(personJson)) ::
      Response(Status.OK, Map(), Some("[" + personJson + "]")) ::
      Response(Status.OK, Map(), None) ::
      Response(Status.NotFound, Map(), None) ::
      Response(Status.OK, Map(), Some("[]")) ::
      Nil

    RequestBuilder() url "http://api.rest.org/person" apply { implicit rb =>
      GET asserting (StatusCode === Status.OK, jsonBodyAsList[Person] === EmptyList)
      val id = POST body personJson asserting (StatusCode === Status.Created) returning (Header("X-Person-Id"))
      GET / id asserting (StatusCode === Status.OK, jsonBodyAs[Person] === Jason)
      GET asserting (StatusCode === Status.OK, jsonBodyAsList[Person] === Seq(Jason))
      DELETE / id asserting (StatusCode === Status.OK)
      GET / id asserting (StatusCode === Status.NotFound)
      GET asserting (StatusCode === Status.OK, jsonBodyAsList[Person] === EmptyList)
    }
  }
  
  it should "support expecting on values from the response" in {
    import JsonExtractors._
    val EmptyList = Seq()

    driver.responses = Response(Status.OK, Map(), Some("[]")) ::
      Response(Status.Created, toHeaders("X-Person-Id" -> "99"), None) ::
      Response(Status.OK, Map(), Some(personJson)) ::
      Response(Status.OK, Map(), Some("[" + personJson + "]")) ::
      Response(Status.OK, Map(), None) ::
      Response(Status.NotFound, Map(), None) ::
      Response(Status.OK, Map(), Some("[]")) ::
      Nil
      
    val BodyAsPersonList = jsonBodyAsList[Person]
    val BodyAsPerson= jsonBodyAs[Person]
    val PersonIdHeader = Header("X-Person-Id")

    using(_ url "http://api.rest.org/person") { implicit rb =>
      GET expecting {
        case StatusCode(Status.OK) & BodyAsPersonList(EmptyList) =>
      }
      val id = POST body personJson expecting {
        case StatusCode(Status.Created) ~ PersonIdHeader(id) => id
      }
      GET / id expecting {
        case StatusCode(Status.OK) & BodyAsPerson(p) => p should be(Jason)
      }
      GET expecting {
        case StatusCode(Status.OK) & BodyAsPersonList(xp) => xp should be (Seq(Jason))
      }
      DELETE / id expecting {
        case StatusCode(Status.OK) =>
      }
      GET / id expecting {
        case StatusCode(Status.NotFound) =>
      }
      GET expecting {
        case StatusCode(Status.OK) & BodyAsPersonList(EmptyList) =>
      }
    }
  }
}