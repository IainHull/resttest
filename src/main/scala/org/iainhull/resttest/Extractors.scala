package org.iainhull.resttest

import Api._

/**
 * An [[Extractor]] is simply a function that takes a [[Api.Response]] and returns a 
 * value.  They can be used with the [[Dsl.RichResponse]]`.returning` methods to access the 
 * values of a response by name.  They can be use with [[Dsl.RichExtractor]] to 
 * create [[Dsl.Assertion]]s.
 * 
 * The easiest way to implement an `Extractor` is to define a variable of type `Extractor[T]`
 * where `T` is the type of the value returned. For example to extract the `statusCode` as an
 * `Int`:
 * 
 * {{{
 * val statusCode: Extractor[Int] = _.statusCode
 * }}}
 * 
 * It is also possible to create methods that create extractors.  For example to extract a 
 * specific header as a `List` of values:
 * 
 * {{{
 * def headerList(name: String): Extractor[List[String]] = _.headers(name)
 * }}}
 */
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