package com.stripe.play

import com.stripe.apiKey
import com.stripe.ApiBase
import com.stripe.BindingsVersion
import com.stripe.Utils._
import com.stripe.StripeApi
import scala.concurrent.Future
import play.api.libs.ws.WS
import scala.util.Properties
import play.api.libs.ws.WS
import play.api.libs.ws.WS.WSRequestHolder
import scala.concurrent.ExecutionContext
import java.net.HttpURLConnection
import play.api.libs.json.Reads
import play.api.libs.json.Json
import com.stripe.model.Account
import com.stripe.HttpClient
import com.stripe.APIConnectionException

trait PlayHttpClient extends HttpClient {

  def request(method: String, url: String, headers: Seq[(String, String)], paramList: Map[String, String])(implicit ctx: ExecutionContext): Future[(Int, String)] = {
    val httpClient = client(url, headers)
    method.toLowerCase() match {
      case "get" =>
        httpClient.withQueryString(paramList.toSeq: _*).get().map { response =>
          (response.status, response.body)
        }
      case "post" =>
        httpClient.post(paramList.map(x => (x._1, Seq(x._2)))).map { response =>
          (response.status, response.body)
        }
      case "delete" =>
        httpClient.delete().map { response =>
          (response.status, response.body)
        }
      case _ =>
        throw new APIConnectionException("Unrecognized HTTP method %r. This may indicate a bug in the Stripe bindings.".format(method))
    }
  }

  private def client(path: String, headers: Seq[(String, String)]): WSRequestHolder = {
    WS.url(path).withHeaders(headers: _*)
  }
}

