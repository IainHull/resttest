package org.iainhull.resttest

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import play.api.libs.json._
import play.api.libs.functional.syntax._

@RunWith(classOf[JUnitRunner])
class JsonExtractorsSpec extends FlatSpec with ShouldMatchers {
  import JsonExtractors._
  import TestData._
  import Api._

  "jsonToList" should "deserialise to scala types" in {
    jsonToList[String](jsonList, __ \\ "name") should be(List("toto", "tata"))
    jsonToList[String](jsonDoc, __ \ "user" \ "favorite" \ "colors") should be(List("red", "green"))

  }

  it should "deserialise to custom types" in {
    jsonToList[Person](jsonList, __) should be(List(Toto, Tata))
    jsonToList[Person](jsonDoc, __ \ "users") should be(List(Toto))
  }

  "jsonToValue" should "deserialise to scala types" in {
    jsonToValue[String](jsonDoc, __ \ "user" \ "name") should be("toto")
    jsonToValue[String](jsonDoc, __ \ "user" \ "favorite" \ "colors" apply (0)) should be("red")

  }

  it should "deserialise to custom types" in {
    jsonToValue[Person](jsonList, __ apply (0)) should be(Toto)
    jsonToValue[Person](jsonDoc, __ \ "user") should be(Toto)
  }

  def evaluate[T](func: Extractor[T], json: JsValue): T = func(Response(Status.OK, Map(), Some(Json.stringify(json))))

  
  "jsonBodyAsList" should "deserialise to scala types" in {
    evaluate(jsonBodyAsList[Person], jsonList) should be(List(Toto, Tata))
    evaluate(jsonBodyAsList[Int](__ \\ "age"), jsonList) should be(List(25, 20))
  }

  "jsonBodyAs" should "deserialise to scala types" in {
    evaluate(jsonBodyAs[Person], Json parse personJson) should be(Jason)
    evaluate(jsonBodyAs[String](__ \ "user" \ "name"), jsonDoc) should be("toto")
  }
}