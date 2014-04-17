package org.vvcephei.rest

import com.sun.jersey.api.client.ClientResponse.Status
import java.io.InputStream
import javax.ws.rs.core.{MultivaluedMap, UriBuilder, MediaType}
import com.sun.jersey.api.client._
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import java.util.Locale
import scala.Some
import scala.collection.JavaConversions._
import scala.io.Source

/**
 * A REST response.
 * @param status The status code of the response
 * @param headers The headers that came back
 * @param entity The body of the response
 */
case class RestClientResponse(status: Status, headers: Map[String, List[String]], entity: InputStream) {
  /**
   * coerce the response body to the parameterized type T
   */
  def entityAs[T: Manifest]: T = RestClient.mapper.readValue[T](entity)

  def entityAsString: String = Source.fromInputStream(entity).mkString

  override def toString = s"""RestClientResponse(status="${status}", headers=$headers, entity="${entityAsString}")"""
}

/**
 * A reusable client builder for making REST queries.
 * <br/>
 * You specify the rest resource, params, and headers using the builder methods. You query the resource by using the verb methods.
 * <br/>
 * The verbs return a [[org.vvcephei.rest.RestClientResponse]].
 */
trait RestClient {
  // BUILDERS *****************************************************************

  /**
   * Add the arguments as url segments to the request
   * @param segment
   * @return a new RestClient
   */
  def segment(segment: String*): RestClient

  /**
   * Add the kv pairs to the query params for the request
   * @param kv
   * @return a new RestClient
   */
  def param(kv: (String, String)*): RestClient

  /**
   * Add the arguments to the accept header for the request
   * @param mediaType
   * @return a new RestClient
   */
  def accept(mediaType: MediaType*): RestClient

  /**
   * Add the kv pairs to the header of the request
   * @param kv
   * @return a new RestClient
   */
  def header(kv: (String, Any)*): RestClient

  /**
   * Override the request type with the argument.
   * @param mediaType
   * @return a new RestClient
   */
  def setType(mediaType: MediaType): RestClient

  // QUERIES *****************************************************************

  def head(): RestClientResponse

  def options(): RestClientResponse

  def get(): RestClientResponse

  def put(): RestClientResponse

  def put(requestEntity: Any): RestClientResponse

  def post(): RestClientResponse

  def post(requestEntity: Any): RestClientResponse

  def delete(): RestClientResponse

  def delete(requestEntity: Any): RestClientResponse
}

object RestClient {
  def client(): RestClient = client(Client.create())

  def client(client: Client): RestClient = ScalaClient(client)

  private[rest] val mapper = new ObjectMapper() with ScalaObjectMapper
  mapper.registerModule(DefaultScalaModule)
  private[this] val jmapper = new ObjectMapper()

  private case class ScalaClient(private val _client: Client,
                                 private val _urlSegments: List[String] = Nil,
                                 private val _urlParams: List[(String, String)] = Nil,
                                 private val _headers: List[(String, Any)] = Nil,
                                 private val _type: Option[MediaType] = None,
                                 private val _accept: Seq[MediaType] = Seq(),
                                 private val _acceptLanguage: Seq[Locale] = Seq()
                                   ) extends RestClient {
    def segment(segment: String*) = copy(_urlSegments = _urlSegments ++ segment)

    def param(kv: (String, String)*) = copy(_urlParams = _urlParams ++ kv)

    def setType(mediaType: MediaType) = copy(_type = Some(mediaType))

    def accept(mediaType: MediaType*) = copy(_accept = _accept ++ mediaType)

    def header(kv: (String, Any)*) = copy(_headers = _headers ++ kv)

    private lazy val b = {
      require(!_urlSegments.isEmpty, "must provide a path to query")
      _urlSegments.foreach(seg => require(!seg.contains("?"), s"URL path segments can't contain query params: $seg. Use param() instead."))

      val url = {
        val f = for ((k, v) <- _urlParams) yield { (ub: UriBuilder) => ub.queryParam(k, v) }
        val g = f.fold((ub: UriBuilder) => ub)(_ compose _)
        g(UriBuilder.fromPath(_urlSegments.head).segment(_urlSegments.tail: _*)).build().toString
      }

      type ReqBuild = RequestBuilder[WebResource#Builder] with UniformInterface

      val res = _client.resource(url)
      val res2 = _type match {
        case Some(t) => res.`type`(t)
        case None    => res
      }
      val res3 = {
        val f = for ((k, v) <- _headers) yield { (w: ReqBuild) => w.header(k, v) }
        val g = f.fold((b: ReqBuild) => b)(_ compose _)
        g(res2)
      }
      val res4 = if (!_accept.isEmpty) res3.accept(_accept: _*) else res3
      val res5 = if (!_acceptLanguage.isEmpty) res4.acceptLanguage(_acceptLanguage: _*) else res4
      res5
    }

    def head() = clientResponseToRCR(b.head())

    def get() = clientResponseToRCR(b.get(classOf[ClientResponse]))

    def put() = clientResponseToRCR(b.put(classOf[ClientResponse]))

    def put(requestEntity: Any) = clientResponseToRCR(b.put(classOf[ClientResponse], transformRequest(requestEntity)))

    def post() = clientResponseToRCR(b.post(classOf[ClientResponse]))

    def post(requestEntity: Any) = clientResponseToRCR(b.post(classOf[ClientResponse], transformRequest(requestEntity)))

    def delete() = clientResponseToRCR(b.delete(classOf[ClientResponse]))

    def delete(requestEntity: Any) = clientResponseToRCR(b.delete(classOf[ClientResponse], transformRequest(requestEntity)))

    def options() = clientResponseToRCR(b.options(classOf[ClientResponse]))

    private[this] def clientResponseToRCR(cr: ClientResponse): RestClientResponse = {
      RestClientResponse(cr.getClientResponseStatus, multivaluedMapToScala(cr.getHeaders), cr.getEntityInputStream)
    }
    private[this] def multivaluedMapToScala[K, V](mvm: MultivaluedMap[K, V]) = {
      val b = scala.collection.immutable.Map.newBuilder[K, List[V]]
      for (e <- mvm.entrySet()) {
        b += (e.getKey -> e.getValue.toList)
      }
      b.result()
    }

    private[this] def transformRequest(requestEntity: Any) = jmapper.readValue(mapper.writeValueAsBytes(requestEntity), classOf[Object])
  }

}