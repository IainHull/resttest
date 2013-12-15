package org.iainhull.resttest

import play.api.libs.json.JsValue
import play.api.libs.json.JsPath
import play.api.libs.json.Reads
import play.api.libs.json.JsArray
import play.api.libs.json.Json

trait JsonExtractors {
  import Extractors._
  
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
  val JsonBody = BodyText andThen Json.parse as "JsonBody"

  /**
   * Extract the response body as an object.
   */
  def jsonBodyAs[T: Reads](implicit tag : reflect.ClassTag[T] ): Extractor[T] = jsonBodyAs(JsPath)

  /**
   * Extract a portion of the response body as an object.
   *
   * @param path the path for the portion of the response to use
   */
  def jsonBodyAs[T: Reads](path: JsPath = JsPath)(implicit tag : reflect.ClassTag[T] ) = {
    JsonBody andThen (jsonToValue(_, path)) as (s"JsonBodyAs[${tag.runtimeClass.getName}]")
  }

  /**
   * Extract the response body as a List of objects.
   */
  def jsonBodyAsList[T: Reads](implicit tag : reflect.ClassTag[T] ): Extractor[Seq[T]] = jsonBodyAsList(JsPath)

  /**
   * Extract a portion of the response body as a List of objects.
   *
   * @param path the path for the portion of the response to use
   */
  def jsonBodyAsList[T: Reads](path: JsPath = JsPath)(implicit tag : reflect.ClassTag[T] ) = {
    JsonBody andThen (jsonToList(_, path)) as (s"JsonBodyAsList[${tag.runtimeClass.getName}]")
  }
}

object JsonExtractors extends JsonExtractors
