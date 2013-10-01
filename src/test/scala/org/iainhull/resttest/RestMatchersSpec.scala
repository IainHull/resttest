package org.iainhull.resttest

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.matchers.HavePropertyMatcher
import org.scalatest.matchers.HavePropertyMatchResult

@RunWith(classOf[JUnitRunner])
class RestMatcherSpec extends FlatSpec with ShouldMatchers {
  import language.implicitConversions

  import TestData._
  import Api._
  import Dsl._
  import RestMatchers._

  implicit val driver = newTestDriver

  val response = Response(Status.OK, toHeaders("header1" -> "", "header2" -> "value", "header3" -> "value1", "header3" -> "value2"), None)

  "RestMatchers" should "support 'have property' equals check" in {
    response should have('statusCode(Status.OK))
    response should have(StatusCode(Status.OK))
    response should have(StatusCode === Status.OK)

    response should have(headerText("header2") === "value")
  }

  it should "support 'have property' not-equals check" in {
    response should have(StatusCode !== 1)
    response should have(headerText("header2") !== "not value")
    response should have(headerList("header2") !== List("not value"))
  }

  it should "support 'have property' in check" in {
    response should have(StatusCode in (Status.OK, Status.Created))
  }

  it should "support 'have property' check for Extractor[Option[_]]" in {
    response should have(header("header1"))
    response should not(have(Body))
  }

  it should "support 'have property' Ordered comparison operator checks" in {
    response should have(StatusCode > 1)
    response should have(StatusCode >= 1)
    response should have(StatusCode < 299)
    response should have(StatusCode <= 299)
  }

  it should "support 'have property' Ordered between operator checks" in {
    response should have(StatusCode between (200, 299))
  }

  "Sample use-case" should "support asserting on values from the response with have matchers" in {
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

    using(_ url "http://api.rest.org/person") { implicit rb =>
      GET should have(StatusCode(Status.OK), jsonBodyAsList[Person] === EmptyList)

      val (status, id) = POST body personJson returning (StatusCode, headerText("X-Person-Id"))
      status should be(Status.Created)

      val foo = GET / id should have(StatusCode(Status.OK), jsonBodyAs[Person] === Jason)

      GET should have(StatusCode === Status.OK, jsonBodyAsList[Person] === Seq(Jason))

      DELETE / id should have(StatusCode === Status.OK)

      GET / id should have(StatusCode === Status.NotFound)

      GET should have(StatusCode(Status.OK), jsonBodyAsList[Person] === EmptyList)
    }
  }

  it should "support asserting on extractor as values" in {
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

    using(_ url "http://api.rest.org/person") { implicit rb =>
      GET expecting { implicit res =>
        StatusCode should be(Status.OK)
        jsonBodyAsList[Person] should be(EmptyList)
      }

      val id = POST body personJson expecting { implicit res =>
        StatusCode should be(Status.Created)
        headerText("X-Person-Id").value
      }

      GET / id expecting { implicit res =>
        StatusCode should be(Status.OK)
        jsonBodyAs[Person] should be(Jason)
      }

      GET expecting { implicit res =>
        StatusCode should be(Status.OK)
        jsonBodyAsList[Person] should be(Seq(Jason))
      }

      DELETE / id expecting { implicit res =>
        StatusCode should be(Status.OK)
      }

      GET / id expecting { implicit res =>
        StatusCode should be(Status.NotFound)
      }

      GET expecting { implicit res =>
        StatusCode should be(Status.OK)
        jsonBodyAsList[Person] should be(EmptyList)
      }
    }
  }
}
