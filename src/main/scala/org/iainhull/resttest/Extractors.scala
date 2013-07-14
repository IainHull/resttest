package org.iainhull.resttest

trait Extractors {
  import Api._
  
  type Extractor[T] = Response => T
  
  def statusCode: Extractor[Int] = _.statusCode

  def body: Extractor[String] = _.body.get

  def bodyOption: Extractor[Option[String]] = _.body
  
  def header(name: String): Extractor[String] = _.headers(name).mkString(",")

  def headerList(name: String): Extractor[List[String]] = _.headers(name)
}