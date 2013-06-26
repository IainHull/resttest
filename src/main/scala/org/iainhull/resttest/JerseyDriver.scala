package org.iainhull.resttest

import com.sun.jersey.api.client.Client
import com.sun.jersey.api.client.ClientResponse
import scala.collection.JavaConverters

object Jersey {
  import Api._
  
  implicit object Driver extends Driver {
    val jersey = Client.create()

    def execute(request: Request): Response = {
      val response = createClientResponse(request)
      Response(response.getStatus, headers(response), Some(response.getEntity(classOf[String])))
    }

    def createClientResponse(request: Request): ClientResponse = {
      val resource = jersey.resource(request.uri)
      request.method match {
        case GET => resource.get(classOf[ClientResponse])
        case POST => resource.post(classOf[ClientResponse])
        case PUT => resource.put(classOf[ClientResponse])
        case DELETE => resource.delete(classOf[ClientResponse])
      }
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