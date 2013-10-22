package org.iainhull.resttest

import com.sun.jersey.api.client.Client
import com.sun.jersey.api.client.ClientResponse
import scala.collection.JavaConverters
import com.sun.jersey.api.client.WebResource

object Jersey {
  import Api._
  
  implicit object Driver extends Driver {
    val jersey = Client.create()

    def execute(request: Request): Response = {
      val response = createClientResponse(request)
      Response(response.getStatus, headers(response), Some(response.getEntity(classOf[String])))
    }

    def createClientResponse(request: Request): ClientResponse = {
      val builder = addRequestHeaders(request.headers, jersey.resource(request.url).getRequestBuilder)
      
      for (b <- request.body) {
    	builder.entity(b)
      }
      
      request.method match {
        case GET => builder.get(classOf[ClientResponse])
        case POST => builder.post(classOf[ClientResponse])
        case PUT => builder.put(classOf[ClientResponse])
        case DELETE => builder.delete(classOf[ClientResponse])
      }
    }
    
    def addRequestHeaders(headers: Map[String, List[String]], builder: WebResource#Builder): WebResource#Builder = {
      def forAllNames(names: List[String], b: WebResource#Builder): WebResource#Builder = {
        names match {
          case h :: t => forAllNames(t, forAllValues(h, headers(h), b))
          case Nil => b
        }
      } 
      def forAllValues(name: String, values: List[String], b: WebResource#Builder): WebResource#Builder = {
        values match {
          case h :: t => forAllValues(name, t, b.header(name, h))
          case Nil => b
        }
      } 
      forAllNames(headers.keys.toList, builder)
    }

    def headers(response: ClientResponse): Map[String, List[String]] = {
      import JavaConverters._

      response.getHeaders.asScala.toMap.map {
        case (k, v) =>
          (k, v.asScala.toList)
      }
    }
  }
}