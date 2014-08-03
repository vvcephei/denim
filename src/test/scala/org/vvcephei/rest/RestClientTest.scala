package org.vvcephei.rest

import java.io.ByteArrayInputStream
import java.net.{URI, URL}
import javax.ws.rs.core.MediaType

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import com.google.common.io.ByteStreams
import com.sun.jersey.api.client.ClientResponse.Status
import com.sun.jersey.api.client.{WebResource, Client => JerseyClient, ClientResponse => JerseyClientResponse}
import com.sun.jersey.core.header.InBoundHeaders
import org.mockito.Matchers
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.FunSuite
import org.vvcephei.rest.Client.Executor
import org.vvcephei.rest.RestClient._

import scala.collection.JavaConversions._
import scala.reflect.classTag


sealed trait Accessor

case object TEXT extends Accessor

case object LONG extends Accessor

case object INT extends Accessor

//...

sealed trait Verb

case object GET extends Verb

case object PUT extends Verb

case object POST extends Verb

case object OPTIONS extends Verb

case object HEAD extends Verb

case object DELETE extends Verb

//...

trait Response {
  def get(key: String): Response
  def /(key: String): Response = get(key)

  def as[T: Manifest]: T

  def /(t: TEXT.type): String = as[String]
  def /(i: INT.type): Int = as[Int]
  def /(i: LONG.type): Long = as[Long]
}

case class UntypedResponse(response: JerseyClientResponse) extends Response {
  override def get(key: String) = ???
  override def as[T: Manifest] = response.getEntity(classTag[T].runtimeClass.asInstanceOf[Class[T]])
}

case class JsonResponse(json: JsonNode)(implicit mapper: ObjectMapper with ScalaObjectMapper) extends Response {
  override def get(key: String) = JsonResponse(if (json == null) null else json.path(key))
  override def as[T: Manifest] = mapper.convertValue[T](json)
}

case class JsonClientResponse(response: JerseyClientResponse)(implicit mapper: ObjectMapper with ScalaObjectMapper) extends Response {
  private lazy val cachedInputStream = ByteStreams.toByteArray(response.getEntityInputStream)

  def get(key: String) = JsonResponse(mapper.readTree(cachedInputStream).path(key))
  def as[T: Manifest] = mapper.readValue[T](cachedInputStream)
}

case class Client(headers: Map[String, String] = Map(),
                  protocol: Option[String] = None,
                  host: Option[String] = None,
                  port: Option[Int] = None,
                  path: List[String] = Nil,
                  params: List[(String, String)] = Nil,
                  body: Option[Any] = None)(implicit val executor: Executor) {
  def isJsonClient: Boolean = headers.getOrElse("content-type", "").contains("json") || path.lastOption.getOrElse("").endsWith(".json")

  def header(h: (String, String)): Client = copy(headers = headers ++ Seq(h))

  def protocol(p: String): Client = copy(protocol = Some(p))
  def host(h: String): Client = copy(host = Some(h))
  def port(p: Int): Client = copy(port = Some(p))

  def /(segment: String*): Client = copy(path = path ++ segment)
  def segment(segment: String*): Client = this / (segment: _*)

  def &(param: (String, String)*): Client = copy(params = params ++ param)
  def param(param: (String, String)*): Client = copy(params = params ++ param)

  def body(body: Any): Client = copy(body = Some(body))

  def go(v: GET.type) = executor(this, GET)
  def go(v: PUT.type) = executor(this, PUT)
  def go(v: POST.type) = executor(this, POST)
  def go(v: OPTIONS.type) = executor(this, OPTIONS)
  def go(v: HEAD.type) = executor(this, HEAD)
}

object Client {
  type Executor = (Client, Verb) => Response

  def apply(s: String)(implicit executor: Executor): Client = apply(new URL(s))

  def apply(uri: URL)(implicit executor: Executor): Client = new Client(
    protocol = Option(uri.getProtocol),
    host = Option(uri.getHost),
    port = if (uri.getPort == -1) None else Some(uri.getPort),
    path = if (uri.getPath.isEmpty) Nil else List(uri.getPath),
    params = extractParams(Option(uri.getQuery) getOrElse "").toList)

  private def split(s: String, on: String) = {
    val list: List[String] = s.split(on).toList
    list match {
      case Nil => None
      case List("") => None
      case k :: v :: Nil => Some((k, v))
      case other => throw new IllegalArgumentException(other.toString())
    }
  }

  private def extractParams(s: String) = for {
    pairStr <- s.split("&")
    pair <- split(pairStr, "=")
  } yield {
    pair
  }
}

class RestClientTest extends FunSuite {

  def ~=[T](a: T) = Matchers.eq(a)


  test("testing one url test") {
    val mockJersey = mock(classOf[JerseyClient])
    val mockResource = mock(classOf[WebResource])
    val mockBuilder = mock(classOf[WebResource#Builder])

    val jsonClient = client(mockJersey).setType(MediaType.APPLICATION_JSON_TYPE).header("X-Stream" -> true)
    val j2 = jsonClient.segment("http://api.tumblr.com", "v2", "blog").param("api_key" -> "fuiKNFp9vQFvjLNvx4sUwti4Yb5yGutBN4Xh10LXZhhRKjWlV4")

    val expectedResultMap: Map[String, String] = Map("stat" -> "ok")

    def cr = {
      val mapper = new ObjectMapper() with ScalaObjectMapper
      mapper.registerModule(DefaultScalaModule)
      new JerseyClientResponse(200, new InBoundHeaders(), new ByteArrayInputStream(mapper.writeValueAsBytes(expectedResultMap)), null)
    }

    when(mockJersey.resource(~=("http://api.tumblr.com/v2/blog/scipsy.tumblr.com/info?api_key=fuiKNFp9vQFvjLNvx4sUwti4Yb5yGutBN4Xh10LXZhhRKjWlV4")))
       .thenReturn(mockResource)
    when(mockJersey.resource(~=("http://api.tumblr.com/v2/blog/good.tumblr.com/info?api_key=fuiKNFp9vQFvjLNvx4sUwti4Yb5yGutBN4Xh10LXZhhRKjWlV4")))
       .thenReturn(mockResource)
    when(mockResource.`type`(any(classOf[MediaType]))).thenReturn(mockBuilder, mockBuilder)
    when(mockBuilder.header(~=("X-Stream"), ~=(true))).thenReturn(mockBuilder, mockBuilder)
    when(mockBuilder.get(~=(classOf[JerseyClientResponse]))).thenReturn(cr, cr)

    assert(j2.segment("scipsy.tumblr.com", "info").get().entityAs[Map[String, Object]] === expectedResultMap)
    assert(j2.segment("good.tumblr.com", "info").get().entityAs[Map[String, Object]] === expectedResultMap)

  }

  test("toString works as expected") {
    val resp = RestClientResponse(Status.OK, Map(), new ByteArrayInputStream("The response goes here.".getBytes("UTF-8")))

    assert(resp.toString === """RestClientResponse(status="OK", headers=Map(), entity="The response goes here.")""")

    assert(resp.copy(entity = new ByteArrayInputStream(Array())).toString === """RestClientResponse(status="OK", headers=Map(), entity="")""")
  }

  test("new syntax") {

    implicit def jacksonObjectMapper(): ObjectMapper with ScalaObjectMapper = {
      val mapper = new ObjectMapper with ScalaObjectMapper
      mapper.registerModule(DefaultScalaModule)
      mapper
    }

    implicit def cachingJerseyExecutor(c: Client, verb: Verb): Response = {
      import c._


      val uri: URI = new URI(
        protocol.orNull,
        null,
        host.orNull,
        port getOrElse -1,
        if (path.isEmpty) null else path mkString "/",
        if (params.isEmpty) null else params map {p => p._1 + "=" + p._2} mkString "&",
        null)
      def addHeaders(resource: WebResource#Builder, headers: List[(String, String)]): WebResource#Builder = headers match {
        case Nil => resource
        case (k, v) :: rest => addHeaders(resource.header(k, v), rest)
      }
      val resource: WebResource#Builder = addHeaders(JerseyClient.create().resource(uri).getRequestBuilder, headers.toList)

      val response: JerseyClientResponse = verb match {
        case GET => resource.get(classOf[JerseyClientResponse])
        case PUT => resource.put(classOf[JerseyClientResponse], body)
        case POST => resource.post(classOf[JerseyClientResponse], body)
        case OPTIONS => resource.options(classOf[JerseyClientResponse])
        case HEAD => resource.head()
        case DELETE => resource.delete(classOf[JerseyClientResponse], body)
      }

      if (c.isJsonClient || response.getHeaders.get("content-type").mkString("").contains("json")) {
        JsonClientResponse(response)(jacksonObjectMapper())
      } else {
        UntypedResponse(response)
      }
    }



    /*
        val client: Client = Client(headers = Map("content-type", "application/json")).host("example.com").port(8080)
        val response: Response = client / "this" /("is", "a", "path") & ("an" -> "arg") & ("another" -> "arg") go GET

        val s: String = response / "json" / "keys" / "here" / TEXT
        val d: Double = (response / "json" / "keys" / "also" / "here").as[Double]

        val x = Client("http://www.example.com:8080").header("content-type" -> "application/json") / "path" / "to" body List("a","b") go PUT
      */
    val ip = Client("http://ip.jsontest.com/")
    val response: Response = ip go GET
    val s1: String = response / "ip" / TEXT
    val map: Map[String, Any] = response.as[Map[String,Any]]

    val echo = (Client("http://echo.jsontest.com") / "/key/value" / "one/two" go GET).as[Map[String,Any]]

    val valid = (Client("http://validate.jsontest.com/") & ("json" -> """{"key":"value"}""") go GET).as[Map[String,Any]]
    map
  }

}
