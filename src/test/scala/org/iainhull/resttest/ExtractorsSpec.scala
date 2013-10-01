package org.iainhull.resttest

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FlatSpec
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ExtractorsSpec extends FlatSpec with ShouldMatchers {
  import Api._
  import Dsl._
  
  val response = Response(Status.OK, toHeaders("SimpleHeader" -> "SimpleValue", "MultiHeader" -> "Value1", "MultiHeader" -> "Value2"), Some("body"))
  
  def returning[T](ext: Extractor[T]): T = ext.op(response)
  
  "statusCode" should "return the responses statusCode" in {
    returning(StatusCode) should be(Status.OK)
  }

  "body" should "return the responses body as a Option[String]" in {
    returning(Body) should be(Some("body"))
  }

  "bodyText" should "return the responses body as a String" in {
    returning(BodyText) should be("body")
  }

  "bodyOption" should "return the responses body as an Option" in {
    returning(Body) should be(Option("body"))
  }

  "header" should "return the responses header value as an Option[List[String]]" in {
    returning(header("SimpleHeader")) should be(Some(List("SimpleValue")))
    returning(header("MultiHeader")) should be(Some(List("Value1","Value2")))
    
    returning(header("NotAHeader")) should be(None)
  }

  "headerText" should "return the responses header value as a String" in {
    returning(headerText("SimpleHeader")) should be("SimpleValue")
    returning(headerText("MultiHeader")) should be("Value1,Value2")
    
    evaluating { returning(headerText("NotAHeader")) } should produce [NoSuchElementException]
  }

  "headerList" should "return the responses header as a list" in {
    returning(headerList("SimpleHeader")) should be(List("SimpleValue"))
    returning(headerList("MultiHeader")) should be(List("Value1","Value2"))
    
    evaluating { returning(headerList("NotAHeader")) } should produce [NoSuchElementException]
  }
}