package org.iainhull.resttest

import Api._
import scala.util.Try

trait ExtractorLike[+A] {
  def name: String
  def unapply(res: Response): Option[A] = Try { value(res) }.toOption
  def value(implicit res: Response): A
}

/**
 * Primary implementation of ExtractorLike.  The name and extraction 
 * 
 * @param name
 *        The name of the extractor
 * @param op
 *        The operation to extract the value of type `A` from the `Response`
 */ 
case class Extractor[+A](name: String, op: Response => A) extends ExtractorLike[A] {
  def value(implicit res: Response): A = op(res)
  
  /**
   * Create a new `Extractor` by executing a new function to modify the result.
   * Normally followed by `as`.
   * 
   * {{{
   * val JsonBody = BodyText andThen Json.parse as "JsonBody"
   * }}}
   */
  def andThen[B](nextOp: A => B): Extractor[B] = copy(name = name + ".andThen ?", op = op andThen nextOp)
  
  /**
   * Rename the extractor
   */
  def as(newName: String) = copy(name = newName)
}

case class Header(header: String) extends ExtractorLike[String] {
  val asText = Extractor[String]("Header(" + header + ")", _.headers(header).mkString(","))
  val asList = Extractor[List[String]]("Header(" + header + ").asList", _.headers(header))
  val asOption = Extractor[Option[List[String]]]("Header(" + header + ").asOption", _.headers.get(header))
  val isDefined = new Object {
    def unapply(res: Response): Boolean = {
      res.headers.contains(header)
    }
  }

  def ->(value: String): (String, String) = {
    (header, value)
  }

  override def name: String = asText.name
  override def unapply(res: Response): Option[String] = asText.unapply(res)
  override def value(implicit res: Response): String = asText.value

}

object Header {
  val AccessControlAllowOrigin = Header("Access-Control-Allow-Origin")
  val AcceptRanges = Header("Accept-Ranges")
  val Age = Header("Age")
  val Allow = Header("Allow")
  val CacheControl = Header("Cache-Control")
  val Connection = Header("Connection")
  val ContentEncoding = Header("Content-Encoding")
  val ContentLanguage = Header("Content-Language")
  val ContentLength = Header("Content-Length")
  val ContentLocation = Header("Content-Location")
  val ContentMd5 = Header("Content-MD5")
  val ContentDisposition = Header("Content-Disposition")
  val ContentRange = Header("Content-Range")
  val ContentType = Header("Content-Type")
  val Date = Header("Date")
  val ETag = Header("ETag")
  val Expires = Header("Expires")
  val LastModified = Header("Last-Modified")
  val Link = Header("Link")
  val Location = Header("Location")
  val P3P = Header("P3P")
  val Pragma = Header("Pragma")
  val ProxyAuthenticate = Header("Proxy-Authenticate")
  val Refresh = Header("Refresh")
  val RetryAfter = Header("Retry-After")
  val Server = Header("Server")
  val SetCookie = Header("Set-Cookie")
  val StrictTransportSecurity = Header("Strict-Transport-Security")
  val Trailer = Header("Trailer")
  val TransferEncoding = Header("Transfer-Encoding")
  val Vary = Header("Vary")
  val Via = Header("Via")
  val Warning = Header("Warning")
  val WwwAuthenticate = Header("WWW-Authenticate")
}

trait Extractors {
  import language.implicitConversions
  import Extractors._

  val StatusCode = Extractor[Int]("statusCode", _.statusCode)

  val Body = Extractor[Option[String]]("bodyOption", _.body)

  val BodyText = Extractor[String]("body", _.body.get)

  /**
   * Enable Extractors to be chained together in case clauses.
   *
   * For example:
   * {{{
   * GET / id expecting {
   *   case StatusCode(Status.OK) ~ Header.ContentType(ct) ~ BodyAsPerson(person) =>
   *     ct should be("application/json")
   *     person should be(Jason)
   * }
   * }}}
   */
  object ~ {
    def unapply(res: Response): Option[(Response, Response)] = {
      Some((res, res))
    }
  }
}

object Extractors extends Extractors {
}