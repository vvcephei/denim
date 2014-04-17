package org.vvcephei.rest

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
import com.sun.jersey.api.client.ClientResponse.Status


class RestClientTest extends FunSuite {

  def ~=[T](a: T) = Matchers.eq(a)


  test("testing one url test") {
    val mockJersey = mock(classOf[Client])
    val mockResource = mock(classOf[WebResource])
    val mockBuilder = mock(classOf[WebResource#Builder])

    val jsonClient = client(mockJersey).setType(MediaType.APPLICATION_JSON_TYPE).header("X-Stream" -> true)
    val j2 = jsonClient.segment("http://api.tumblr.com", "v2", "blog").param("api_key" -> "fuiKNFp9vQFvjLNvx4sUwti4Yb5yGutBN4Xh10LXZhhRKjWlV4")

    val expectedResultMap: Map[String, String] = Map("stat" -> "ok")

    def cr = {
      val mapper = new ObjectMapper() with ScalaObjectMapper
      mapper.registerModule(DefaultScalaModule)
      new ClientResponse(200, new InBoundHeaders(), new ByteArrayInputStream(mapper.writeValueAsBytes(expectedResultMap)), null)
    }

    when(mockJersey.resource(~=("http://api.tumblr.com/v2/blog/scipsy.tumblr.com/info?api_key=fuiKNFp9vQFvjLNvx4sUwti4Yb5yGutBN4Xh10LXZhhRKjWlV4")))
       .thenReturn(mockResource)
    when(mockJersey.resource(~=("http://api.tumblr.com/v2/blog/good.tumblr.com/info?api_key=fuiKNFp9vQFvjLNvx4sUwti4Yb5yGutBN4Xh10LXZhhRKjWlV4")))
       .thenReturn(mockResource)
    when(mockResource.`type`(any(classOf[MediaType]))).thenReturn(mockBuilder, mockBuilder)
    when(mockBuilder.header(~=("X-Stream"), ~=(true))).thenReturn(mockBuilder, mockBuilder)
    when(mockBuilder.get(~=(classOf[ClientResponse]))).thenReturn(cr, cr)

    assert(j2.segment("scipsy.tumblr.com", "info").get().entityAs[Map[String, Object]] === expectedResultMap)
    assert(j2.segment("good.tumblr.com", "info").get().entityAs[Map[String, Object]] === expectedResultMap)

  }

  test("toString works as expected") {
    val resp = RestClientResponse(Status.OK, Map(), new ByteArrayInputStream("The response goes here.".getBytes("UTF-8")))

    assert(resp.toString === """RestClientResponse(status="OK", headers=Map(), entity="The response goes here.")""")

    assert(resp.copy(entity = new ByteArrayInputStream(Array())).toString === """RestClientResponse(status="OK", headers=Map(), entity="")""")
  }

}
