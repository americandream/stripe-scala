package com.stripe

import java.net.URLEncoder
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.Properties
import scala.util.matching.Regex
import org.json4s.DefaultFormats
import org.json4s.JField
import org.json4s.JsonAST.JValue
import org.json4s.JsonDSL.map2jvalue
import org.json4s.JsonDSL.string2jvalue
import org.json4s.MappingException
import org.json4s.jvalue2extractable
import org.json4s.jvalue2monadic
import org.json4s.native.JsonMethods.compact
import org.json4s.native.JsonMethods.parse
import org.json4s.native.JsonMethods.render
import org.json4s.string2JsonInput
import java.net.URLDecoder

sealed abstract class StripeException(msg: String, cause: Throwable = null) extends Exception(msg, cause)
case class APIException(msg: String, cause: Throwable = null) extends StripeException(msg, cause)
case class APIConnectionException(msg: String, cause: Throwable = null) extends StripeException(msg, cause)
case class CardException(msg: String, code: Option[String] = None, param: Option[String] = None) extends StripeException(msg)
case class InvalidRequestException(msg: String, param: Option[String] = None) extends StripeException(msg)
case class AuthenticationException(msg: String) extends StripeException(msg)

case class ErrorContainer(error: Error)
case class Error(`type`: String, message: String, code: Option[String], param: Option[String])

object Utils {
  def urlEncodePair(k: String, v: String) = (URLEncoder.encode(k, CharSet), URLEncoder.encode(v, CharSet))
  def className(x: Any) = x.getClass.getSimpleName().toLowerCase.replace("$", "")
  def classURL(x: Any) = s"${ApiBase}/${className(x)}s"
  def instanceURL(x: Any, id: String) = s"${classURL(x)}/${id}"
  def singleInstanceURL(x: Any) = s"${ApiBase}/${className(x)}"
  def optionMap(name: String, value: Option[Any]): Iterable[(String, Any)] = {
    value.map(x => Map(name -> x)).getOrElse(Nil)
  }
}

trait StripeApi {
  self: HttpClient =>

  import Utils._
  implicit val formats = DefaultFormats
  //debug headers
  val javaPropNames = List("os.name", "os.version", "os.arch", "java.version", "java.vendor", "java.vm.version", "java.vm.vendor")
  val javaPropMap = javaPropNames.map(n => (n.toString, Properties.propOrEmpty(n).toString)).toMap
  val fullPropMap = javaPropMap + (
    "scala.version" -> Properties.scalaPropOrEmpty("version.number"),
    "bindings.version" -> BindingsVersion,
    "lang" -> "scala",
    "publisher" -> "stripe")

  val defaultHeaders = Seq(("X-Stripe-Client-User-Agent", compact(render(fullPropMap))),
    ("User-Agent", "Stripe/v1 ScalaBindings/%s".format(BindingsVersion)),
    ("Authorization", s"Bearer ${apiKey}"))

  /*
      We want POST vars of form:
      {'foo': 'bar', 'nested': {'a': 'b', 'c': 'd'}}
      to become:
      foo=bar&nested[a]=b&nested[c]=d
  */
  private def flattenParam(k: String, v: Any): List[(String, String)] = {
    v match {
      case None => Nil
      case m: Map[_, _] => m.flatMap(kv => flattenParam("%s[%s]".format(k, kv._1), kv._2)).toList
      case _ => List((k, v.toString))
    }
  }

  private def queryStrings(params: Map[String, Any]): Seq[(String, String)] = {
    params.map { (kv) =>
      flattenParam(kv._1, kv._2)
    }.flatten.toSeq
  }

  private def rawRequest(method: String, path: String, paramList: Map[String, Any])(implicit ctx: ExecutionContext): Future[JValue] = {
    val params = paramList.flatMap(kv => flattenParam(kv._1, kv._2)).map(kv => urlEncodePair(kv._1, kv._2))
    // This error should be in a future
    if (apiKey == null || apiKey.isEmpty) {
      throw AuthenticationException("No API key provided. (HINT: set your API key using 'stripe.apiKey = <API-KEY>'. You can generate API keys from the Stripe web interface. See https://stripe.com/api for details or email support@stripe.com if you have questions.")
    }
    request(method, path, defaultHeaders, params).map {
      case (statusCode, responseBody) =>
        val jsonAST = parse(responseBody /*URLDecoder.decode(responseBody, CharSet) */ ).transformField {
          // convert json _ separated names into camelCase
          case JField(fieldName, x) => JField(new Regex("(_.)").replaceAllIn(
            fieldName, (m: Regex.Match) => m.matched.substring(1).toUpperCase), x)
        }
        if (statusCode < 200 || statusCode >= 300) { handleAPIError(responseBody, statusCode, jsonAST) }

        jsonAST
    }
  }

  private def handleAPIError(rBody: String, rCode: Int, jsonAST: JValue) {
    val error = try {
      jsonAST.extract[ErrorContainer].error
    } catch {
      case e: MappingException => throw new APIException(
        "Unable to parse response body from API: %s (HTTP response code was %s)".format(rBody, rCode), e)
    }
    rCode match {
      case (400 | 404) => throw new InvalidRequestException(error.message, param = error.param)
      case 401 => throw new AuthenticationException(error.message)
      case 402 => throw new CardException(error.message, code = error.code, param = error.param)
      case _ => throw new APIException(error.message, null)
    }
  }

  def get(path: String, paramList: Map[String, Any] = Map.empty)(implicit ctx: ExecutionContext): Future[JValue] = {
    rawRequest("get", path, paramList)
  }

  def post(path: String, paramList: Map[String, Any] = Map.empty)(implicit ctx: ExecutionContext): Future[JValue] = {
    rawRequest("post", path, paramList)
  }

  def delete(path: String, paramList: Map[String, Any] = Map.empty)(implicit ctx: ExecutionContext): Future[JValue] = {
    rawRequest("delete", path, paramList)
  }
}

