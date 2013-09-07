package org.iainhull.resttest

import play.api.libs.json.JsValue
import play.api.libs.json.JsPath
import play.api.libs.json.Reads
import play.api.libs.json.JsArray
import play.api.libs.json.Json

trait JsonExtractors extends Extractors { 
  import Api._
  import Dsl._
  
  /**
   * Extract a path from a json document and deserialise it to a List
   */
  def jsonToList[T: Reads](json: JsValue, path: JsPath = JsPath): Seq[T] = {
    path(json) match {
      case Seq(array: JsArray) => array.as[List[T]](Reads.list[T])
      case seq: Seq[JsValue] => seq map (_.as[T])
    }
  }

  /**
   * Extract a path from a json document and deserialise it to a value
   */
  def jsonToValue[T: Reads](json: JsValue, path: JsPath = JsPath): T = {
    path.asSingleJson(json).as[T]
  }

  /**
   * Extract the response body as a json document
   */
  val jsonBody = Extractor[JsValue]("jsonBody", bodyText.op andThen Json.parse)

  /**
   * Extract the response body as an object.
   */
  def jsonBodyAs[T: Reads]: Extractor[T] = jsonBodyAs(JsPath)

  /**
   * Extract a portion of the response body as an object.
   *
   * @param path the path for the portion of the response to use
   */
  def jsonBodyAs[T: Reads](path: JsPath = JsPath) = Extractor[T]("jsonBodyAs", jsonBody.op andThen (jsonToValue(_, path)))

  /**
   * Extract the response body as a List of objects.
   */
  def jsonBodyAsList[T: Reads]: Extractor[Seq[T]] = jsonBodyAsList(JsPath)

  /**
   * Extract a portion of the response body as a List of objects.
   *
   * @param path the path for the portion of the response to use
   */
  def jsonBodyAsList[T: Reads](path: JsPath = JsPath) = Extractor[Seq[T]]("jsonBodyAsList", jsonBody.op andThen (jsonToList(_, path)))
}

object JsonExtractors extends JsonExtractors with Dsl
