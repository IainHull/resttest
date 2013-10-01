package org.iainhull.resttest

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers

import Api._

object ~ {

  def unapply(res: Response): Option[(Response, Response)] = {
    Some((res, res))
  }
}

import Dsl._


@RunWith(classOf[JUnitRunner])
class UnapplySpec extends FlatSpec with ShouldMatchers {
  import TestData._
  import JsonExtractors._
  
  def expecting[T](res: Response)(func: Response => T) = func(res)
  
  "unapply" should "work" in {
        
    val res = Response(Status.OK, toHeaders("X-Person-Id" -> "1234", Header.ContentType -> "application/json"), Some(personJson))
    
    val HeaderId = headerText("X-Person-Id")
    val BodyAsPerson = jsonBodyAs[Person]
    
//    GET / "foo" expecting {
//      case r => 
//    }
    
    
    expecting(res) {
      case StatusCode(Status.OK) ~ HeaderId(h) ~ Header.ContentType.list(l) ~ BodyAsPerson(p) =>
        l should be(List("application/json"))
        h should be ("1234")
        p.name should be ("Jason")
    }
  }
}