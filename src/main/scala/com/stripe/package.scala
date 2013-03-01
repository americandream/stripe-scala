package com

package object stripe {
  var apiKey: String = ""
  var api: StripeApi with HttpClient = null
  val ApiBase = "https://api.stripe.com/v1"
  val BindingsVersion = "1.2-2.10"
  val CharSet = "UTF-8"

  implicit val formats = org.json4s.DefaultFormats
}
