package com.stripe.model

import com.stripe.api
import com.stripe.formats
import com.stripe.Utils._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext

case class Account(
  id: String,
  email: Option[String],
  statementDescriptor: Option[String],
  detailsSubmitted: Boolean,
  chargeEnabled: Boolean,
  currenciesSupported: Array[String])

object Account {
  def get()(implicit ctx: ExecutionContext): Future[Account] = {
    api.get(singleInstanceURL(this)).map(_.extract[Account])
  }
}