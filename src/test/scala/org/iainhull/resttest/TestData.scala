package org.iainhull.resttest

import play.api.libs.json._
import play.api.libs.functional.syntax._

object TestData {
  import Api._

  val personJson = """{ "name": "Jason", "age": 27, "email": "jason@json.org" }"""
    
  val jsonDoc = Json parse """
	{ 
	  "user": {
	    "name" : "toto",
	    "age" : 25,
	    "email" : "toto@jmail.com",
	    "isAlive" : true,
        "favorite" : {
           "colors": [ "red", "green" ]
        },
	    "friend" : {
	  	  "name" : "tata",
	  	  "age" : 20,
	  	  "email" : "tata@coldmail.com"
	    }
	  }, 
      "users": [
        {
	      "name" : "toto",
	      "age" : 25,
	      "email" : "toto@jmail.com"
	    }
      ]
	}
	"""

  val jsonEmptyList = "[]"

  val jsonPersonList = "[" + personJson + "]"

  val jsonList = Json parse """
	[
	  {
	    "name" : "toto",
	    "age" : 25,
	    "email" : "toto@jmail.com"
      }, 
      {
	    "name" : "tata",
	    "age" : 20,
	    "email" : "tata@coldmail.com"
	  }
	]
	"""
    
  case class Person(name: String, age: Int, email: String)
  
  val Jason = Person("Jason", 27, "jason@json.org")
  val Toto = Person("toto", 25, "toto@jmail.com")
  val Tata = Person("tata", 20, "tata@coldmail.com")

  implicit val personReads: Reads[Person] = (
    (__ \ "name").read[String] and
    (__ \ "age").read[Int] and
    (__ \ "email").read[String])(Person)
    
   def newTestDriver =  new Driver {
    val defaultResponse = Response(200, Map("X-Person-Id" -> List("1234")), Some("body"))
    var responses = List[Response]()
    var requests = List[Request]()
    
    def lastRequest: Request = requests.head
    def nextResponse = responses.headOption.getOrElse(defaultResponse)
    def nextResponse_=(response: Response) = responses = List(response)

    def execute(request: Request): Response = {
      requests = request :: requests
      if (!responses.isEmpty) {
        val response = responses.head
        responses = responses.tail
        response
      } else {
        defaultResponse
      }
    }
  }
}