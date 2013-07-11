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
  
  def returning[T](func: Extractor[T]): T = func(response)
  
  "statusCode" should "return the responses statusCode" in {
    returning(statusCode) should be(Status.OK)
  }

  "body" should "return the responses body as a String" in {
    returning(body) should be("body")
  }

  "bodyOption" should "return the responses body as an Option" in {
    returning(bodyOption) should be(Option("body"))
  }

  "header" should "return the responses header value as a String" in {
    returning(header("SimpleHeader")) should be("SimpleValue")
    returning(header("MultiHeader")) should be("Value1,Value2")
    
    evaluating { returning(header("NotAHeader")) } should produce [NoSuchElementException]
  }

  "headerList" should "return the responses header as a list" in {
    returning(headerList("SimpleHeader")) should be(List("SimpleValue"))
    returning(headerList("MultiHeader")) should be(List("Value1","Value2"))
    
    evaluating { returning(headerList("NotAHeader")) } should produce [NoSuchElementException]
  }
}