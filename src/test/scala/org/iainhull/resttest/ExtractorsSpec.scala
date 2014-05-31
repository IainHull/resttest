package org.iainhull.resttest

import org.scalatest.Matchers
import org.scalatest.FlatSpec
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ExtractorsSpec extends FlatSpec with Matchers {
  import Dsl._
  
  val response = Response(Status.OK, toHeaders("SimpleHeader" -> "SimpleValue", "MultiHeader" -> "Value1", "MultiHeader" -> "Value2"), Some("body"))
  
  def returning[T](ext: ExtractorLike[T]): T = ext.value(response)
  
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

  "Header" should "return the responses header value as a String" in {
    returning(Header("SimpleHeader")) should be("SimpleValue")
    returning(Header("MultiHeader")) should be("Value1,Value2")
    
    an [NoSuchElementException] should be thrownBy { returning(Header("NotAHeader")) } 
  }

  "Header.asOption" should "return the responses header value as an Option[List[String]]" in {
    returning(Header("SimpleHeader").asOption) should be(Some(List("SimpleValue")))
    returning(Header("MultiHeader").asOption) should be(Some(List("Value1","Value2")))
    
    returning(Header("NotAHeader").asOption) should be(None)
  }


  "Header.asList" should "return the responses header as a list" in {
    returning(Header("SimpleHeader").asList) should be(List("SimpleValue"))
    returning(Header("MultiHeader").asList) should be(List("Value1","Value2"))
    
    an [NoSuchElementException] should be thrownBy  { returning(Header("NotAHeader").asList) }
  }
}