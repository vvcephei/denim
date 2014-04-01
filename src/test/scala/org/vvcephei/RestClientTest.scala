package org.vvcephei

import org.scalatest.FunSuite
import javax.ws.rs.core.MediaType
import RestClient._
import org.mockito.Mockito._
import com.sun.jersey.api.client.{ClientResponse, WebResource, Client}
import org.mockito.Matchers._
import org.mockito.Matchers
import com.sun.jersey.core.header.InBoundHeaders
import java.io.ByteArrayInputStream
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule


class RestClientTest extends FunSuite {

  def ~=[T](a: T) = Matchers.eq(a)


  test("testing one url test") {
    val mockJersey = mock(classOf[Client])
    val jsonClient = client(mockJersey).`type`(MediaType.APPLICATION_JSON_TYPE)
    val j2 = jsonClient.segment("http://api.tumblr.com", "v2", "blog").param("api_key" -> "fuiKNFp9vQFvjLNvx4sUwti4Yb5yGutBN4Xh10LXZhhRKjWlV4")

    val mockResource = mock(classOf[WebResource])
    val mockBuilder = mock(classOf[WebResource#Builder])
    val mapper = new ObjectMapper() with ScalaObjectMapper
    mapper.registerModule(DefaultScalaModule)
    val map: Map[String, String] = Map("stat" -> "ok")
    def cr = new ClientResponse(200, new InBoundHeaders(), new ByteArrayInputStream(mapper.writeValueAsBytes(map)), null)

    when(mockJersey.resource(~=("http://api.tumblr.com/v2/blog/scipsy.tumblr.com/info?api_key=fuiKNFp9vQFvjLNvx4sUwti4Yb5yGutBN4Xh10LXZhhRKjWlV4")))
      .thenReturn(mockResource)
    when(mockJersey.resource(~=("http://api.tumblr.com/v2/blog/good.tumblr.com/info?api_key=fuiKNFp9vQFvjLNvx4sUwti4Yb5yGutBN4Xh10LXZhhRKjWlV4")))
      .thenReturn(mockResource)
    when(mockResource.`type`(any(classOf[MediaType]))).thenReturn(mockBuilder, mockBuilder)
    when(mockBuilder.get(~=(classOf[ClientResponse]))).thenReturn(cr, cr)

    assert(j2.segment("scipsy.tumblr.com", "info").get[Map[String, Object]]() === map)
    assert(j2.segment("good.tumblr.com", "info").get[Map[String, Object]]() === map)
  }

}
