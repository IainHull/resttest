package org.iainhull.resttest

import Api._
import scala.util.Try

case class Extractor[+T](name: String, op: Response => T) {
  def unapply(res: Response): Option[T] = Try { op(res) }.toOption
  def value(implicit res: Response): T = op(res)
}

trait Extractors {
  import language.implicitConversions
  import Extractors._

  val StatusCode = Extractor[Int]("statusCode", _.statusCode)

  val Body = Extractor[Option[String]]("bodyOption", _.body)

  val BodyText = Extractor[String]("body", _.body.get)

  def headerText(name: String) = Extractor[String]("header(" + name + ")", _.headers(name).mkString(","))

  def headerList(name: String) = Extractor[List[String]]("headerList(" + name + ")", _.headers(name))

  def header(name: String) = Extractor[Option[List[String]]]("headerOption(" + name + ")", _.headers.get(name))

  class Header(name: String) {
    lazy val list = headerList(name)
    lazy val text = headerText(name)
    lazy val present = new Object {
      def unapply(res: Response): Boolean = {
        res.headers.contains(name)
      }
    }
    def -> (value: String): (String, String) = {
      (name, value)
    }
  }

  object Header {
    val AccessControlAllowOrigin = new Header("Access-Control-Allow-Origin")
    val AcceptRanges = new Header("Accept-Ranges")
    val Age = new Header("Age")
    val Allow = new Header("Allow")
    val CacheControl = new Header("Cache-Control")
    val Connection = new Header("Connection")
    val ContentEncoding = new Header("Content-Encoding")
    val ContentLanguage = new Header("Content-Language")
    val ContentLength = new Header("Content-Length")
    val ContentLocation = new Header("Content-Location")
    val ContentMd5 = new Header("Content-MD5")
    val ContentDisposition = new Header("Content-Disposition")
    val ContentRange = new Header("Content-Range")
    val ContentType = new Header("Content-Type")
    val Date = new Header("Date")
    val ETag = new Header("ETag")
    val Expires = new Header("Expires")
    val LastModified = new Header("Last-Modified")
    val Link = new Header("Link")
    val Location = new Header("Location")
    val P3P = new Header("P3P")
    val Pragma = new Header("Pragma")
    val ProxyAuthenticate = new Header("Proxy-Authenticate")
    val Refresh = new Header("Refresh")
    val RetryAfter = new Header("Retry-After")
    val Server = new Header("Server")
    val SetCookie = new Header("Set-Cookie")
    val StrictTransportSecurity = new Header("Strict-Transport-Security")
    val Trailer = new Header("Trailer")
    val TransferEncoding = new Header("Transfer-Encoding")
    val Vary = new Header("Vary")
    val Via = new Header("Via")
    val Warning = new Header("Warning")
    val WwwAuthenticate = new Header("WWW-Authenticate")
  }
}

object Extractors extends Extractors {
}