package org.iainhull.resttest

import Api._
  
case class Extractor[+T](name: String, op: Response => T)
  
trait Extractors {
  import language.implicitConversions
  
  val statusCode = Extractor[Int]("statusCode", _.statusCode)

  val body = Extractor[Option[String]]("bodyOption", _.body)

  val bodyText = Extractor[String]("body", _.body.get)
  
  def headerText(name: String) = Extractor[String]("header("+name+")", _.headers(name).mkString(","))

  def headerList(name: String) = Extractor[List[String]]("headerList("+name+")", _.headers(name))

  def header(name: String) = Extractor[Option[List[String]]]("headerOption("+name+")", _.headers.get(name))
}