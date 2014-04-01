package org.vvcephei

import com.sun.jersey.api.client.{UniformInterfaceException, ClientResponse, Client}
import javax.ws.rs.core.{UriBuilder, MediaType}
import java.util.Locale
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import javax.ws.rs.core.Response.Status.Family

trait RestClient {
  // builders
  def segment(segment: String*): RestClient

  def param(kv: (String, String)*): RestClient

  def `type`(mediaType: MediaType): RestClient

  def accept(mediaType: MediaType*): RestClient

  def addAccept(mediaType: MediaType*): RestClient

  def acceptLanguage(locale: Locale*): RestClient

  // queries
  def head(): ClientResponse

  def get[T: Manifest](): T

  def put(): Unit

  def put(requestEntity: Any): Unit

  def put[T: Manifest](): T

  def put[T: Manifest](requestEntity: Any): T

  def post(): Unit

  def post(requestEntity: Any): Unit

  def post[T: Manifest](): T

  def post[T: Manifest](requestEntity: Any): T

  def delete(): Unit

  def delete(requestEntity: Any): Unit

  def delete[T: Manifest](): T

  def delete[T: Manifest](requestEntity: Any): T

  def options[T: Manifest](): T
}

object RestClient {
  def client(): RestClient = client(Client.create())

  def client(client: Client): RestClient = ScalaClient(client)

  private case class ScalaClient(private val _client: Client,
                                 private val _urlSegments: List[String] = Nil,
                                 private val _urlParams: List[(String, String)] = Nil,
                                 private val _type: Option[MediaType] = None,
                                 private val _accept: Seq[MediaType] = Seq(),
                                 private val _acceptLanguage: Seq[Locale] = Seq()
                                  ) extends RestClient {
    def segment(segment: String*) = copy(_urlSegments = _urlSegments ++ segment)

    def param(kv: (String, String)*) = copy(_urlParams = _urlParams ++ kv)

    def `type`(mediaType: MediaType) = copy(_type = Some(mediaType))

    def accept(mediaType: MediaType*) = copy(_accept = mediaType)

    def addAccept(mediaType: MediaType*) = copy(_accept = _accept ++ mediaType)

    def acceptLanguage(locale: Locale*) = copy(_acceptLanguage = locale)

    private lazy val b = {
      require(!_urlSegments.isEmpty, "must provide a path to query")
      _urlSegments.foreach(seg => require(!seg.contains("?"), s"URL path segments can't contain query params: $seg. Use param() instead."))

      var uriBuilder = UriBuilder.fromPath(_urlSegments.head).segment(_urlSegments.tail: _*)
      for ((k, v) <- _urlParams) {
        uriBuilder = uriBuilder.queryParam(k, v)
      }
      val url = uriBuilder.build().toString
      val res = _client.resource(url)

      val res2 = _type match {
        case Some(t) => res.`type`(t)
        case None => res
      }

      val res3 = if (!_accept.isEmpty) res2.accept(_accept: _*) else res2
      val res4 = if (!_acceptLanguage.isEmpty) res3.acceptLanguage(_acceptLanguage: _*) else res3

      res4
    }

    def head() = b.head()

    def get[T: Manifest]() = transformResponse[T](b.get(classOf[ClientResponse]))

    def put() = b.put()

    def put(requestEntity: Any) = b.put(transformRequest(requestEntity))

    def put[T: Manifest]() = transformResponse[T](b.put(classOf[ClientResponse]))

    def put[T: Manifest](requestEntity: Any) = transformResponse[T](b.put(classOf[ClientResponse], transformRequest(requestEntity)))

    def post() = b.post()

    def post(requestEntity: Any) = b.post(transformRequest(requestEntity))

    def post[T: Manifest]() = transformResponse[T](b.post(classOf[ClientResponse]))

    def post[T: Manifest](requestEntity: Any) = transformResponse[T](b.post(classOf[ClientResponse], transformRequest(requestEntity)))

    def delete() = b.delete()

    def delete(requestEntity: Any) = b.delete(transformRequest(requestEntity))

    def delete[T: Manifest]() = transformResponse[T](b.delete(classOf[ClientResponse]))

    def delete[T: Manifest](requestEntity: Any) = transformResponse[T](b.delete(classOf[ClientResponse], transformRequest(requestEntity)))

    def options[T: Manifest]() = transformResponse[T](b.delete(classOf[ClientResponse]))

    private val mapper = new ObjectMapper() with ScalaObjectMapper
    mapper.registerModule(DefaultScalaModule)
    private val jmapper = new ObjectMapper()

    private def transformResponse[T: Manifest](cr: ClientResponse) =
      if (cr.getClientResponseStatus.getFamily == Family.SUCCESSFUL) {
        mapper.readValue[T](cr.getEntityInputStream)
      } else {
        throw new UniformInterfaceException(cr)
      }

    private def transformRequest(requestEntity: Any) = jmapper.readValue(mapper.writeValueAsBytes(requestEntity), classOf[Object])
  }
}


