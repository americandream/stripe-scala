package com.stripe.model

import com.stripe.api
import com.stripe.formats
import com.stripe.Utils._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext

case class Token(
  id: String,
  created: Int,
  livemode: Boolean,
  used: Boolean,
  card: Card)

object Token {
  def create(card: ChargeCard)(implicit ctx: ExecutionContext): Future[Token] = {
    val cardParams = Map(
      "number" -> card.number,
      "exp_month" -> card.expMonth,
      "exp_year" -> card.expYear) ++
      optionMap("cvc", card.cvc) ++
      optionMap("name", card.name) ++
      optionMap("address_line_1", card.addressLine1) ++
      optionMap("address_line_2", card.addressLine2) ++
      optionMap("address_zip", card.addressZip) ++
      optionMap("address_state", card.addressState) ++
      optionMap("address_country", card.addressCountry)

    api.post(classURL(this), Map("card" -> cardParams)).map(_.extract[Token])
  }

  def get(id: String)(implicit ctx: ExecutionContext): Future[Token] = {
    api.get(instanceURL(this, id)).map(_.extract[Token])
  }
}