package com.stripe.apachehttp

import com.stripe.apiKey
import com.stripe.BindingsVersion
import com.stripe.ApiBase
import com.stripe.CharSet
import com.stripe.Utils._
import org.apache.http.client.methods.HttpDelete
import org.apache.http.message.BasicNameValuePair
import org.apache.http.util.EntityUtils
import org.apache.http.client.ClientProtocolException
import org.apache.http.client.methods.HttpRequestBase
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.methods.HttpGet
import com.stripe.APIConnectionException
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.params.SyncBasicHttpParams
import com.stripe.HttpClient
import org.apache.http.client.params.ClientPNames
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.params.CoreProtocolPNames
import org.apache.http.params.CoreConnectionPNames
import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import scala.collection.JavaConversions._
import org.json4s.JsonDSL._
import org.json4s.native.JsonMethods._
import org.apache.http.message.BasicHeader
import org.apache.http.entity.StringEntity

trait ApacheHttpClient extends HttpClient {

  private def client(headers: Seq[(String, String)]): DefaultHttpClient = {
    val defaultHeaders = asJavaCollection(List(
      new BasicHeader("X-Stripe-Client-User-Agent", compact(render(headers))),
      new BasicHeader("User-Agent", "Stripe/v1 ScalaBindings/%s".format(BindingsVersion)),
      new BasicHeader("Authorization", "Bearer %s".format(apiKey))))

    val httpParams = new SyncBasicHttpParams().
      setParameter(ClientPNames.DEFAULT_HEADERS, defaultHeaders).
      setParameter(CoreProtocolPNames.HTTP_CONTENT_CHARSET, CharSet).
      setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 30000). //30 seconds
      setParameter(CoreConnectionPNames.SO_TIMEOUT, 80000) //80 seconds

    new DefaultHttpClient(connectionManager, httpParams)
  }

  def getRequest(url: String, paramList: Map[String, String]): HttpRequestBase = {
    val params = paramList.toSeq.map(kv => s"${kv._1}=${kv._2}").mkString("&")
    new HttpGet(s"${url}?${params}")
  }

  def deleteRequest(url: String): HttpRequestBase = new HttpDelete(url)

  def postRequest(url: String, paramList: Map[String, String]): HttpRequestBase = {
    val request = new HttpPost(url)
    val postParams = paramList.map(kv => new BasicNameValuePair(kv._1, kv._2)).toSeq.mkString("&")
    // the post params are already URL encoded.
    request.setEntity(new StringEntity(postParams, CharSet))
    request.setHeader("Content-Type", "application/x-www-form-urlencoded")
    request
  }

  def request(
    method: String,
    url: String,
    headers: Seq[(String, String)],
    params: Map[String, String])(implicit ctx: ExecutionContext): Future[(Int, String)] = {
    Future {
      val httpClient = client(headers)
      try {
        val request = method.toLowerCase match {
          case "get" => getRequest(url, params)
          case "delete" => deleteRequest(url)
          case "post" => postRequest(url, params)
          case _ => throw new APIConnectionException("Unrecognized HTTP method %r. This may indicate a bug in the Stripe bindings. Please contact support@stripe.com for assistance.".format(method))
        }
        val response = httpClient.execute(request)
        val entity = response.getEntity
        val body = EntityUtils.toString(entity)
        EntityUtils.consume(entity)
        (response.getStatusLine.getStatusCode, body)
      } catch {
        case e @ (_: java.io.IOException | _: ClientProtocolException) => throw APIConnectionException("Could not connect to Stripe (%s). Please check your internet connection and try again. If this problem persists, you should check Stripe's service status at https://twitter.com/stripe, or let us know at support@stripe.com.".format(ApiBase), e)
      } finally {
        httpClient.getConnectionManager.shutdown()
      }
    }
  }
}