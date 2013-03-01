package com.stripe.model

import com.stripe.api
import com.stripe.formats
import com.stripe.Utils._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext

case class Charge(
    created: Long,
    id: String,
    livemode: Boolean,
    paid: Boolean,
    amount: Int,
    currency: String,
    refunded: Boolean,
    disputed: Option[Boolean],
    fee: Int,
    card: Card,
    failureMessage: Option[String],
    amountRefunded: Option[Int],
    customer: Option[String],
    invoice: Option[String],
    description: Option[String]) {

  def refund()(implicit ctx: ExecutionContext): Future[Charge] = {
    api.post(s"${instanceURL(this, id)}/refund", Map[String, Any]()).map(_.extract[Charge])
  }
}

case class ChargeCollection(count: Int, data: List[Charge])

/** TODO: Add application fee versions of the create calls above */
object Charge {
  def create(
    amount: Int,
    currency: String,
    customer: Option[Customer] = None,
    cardToken: Option[Token] = None,
    card: Option[ChargeCard] = None,
    description: Option[String] = None)(implicit ctx: ExecutionContext): Future[Charge] = {

    require(!(customer.isDefined && card.isDefined && cardToken.isDefined))

    val customerId = customer.map(_.id)
    val cardParams = card.map(x => Map(
      "number" -> x.number,
      "exp_month" -> x.expMonth,
      "exp_year" -> x.expYear) ++
      optionMap("cvc", x.cvc) ++
      optionMap("name", x.name) ++
      optionMap("address_line1", x.addressLine1) ++
      optionMap("address_line2", x.addressLine2) ++
      optionMap("address_zip", x.addressZip) ++
      optionMap("address_state", x.addressState) ++
      optionMap("address_country", x.addressCountry))

    val params = Map("amount" -> amount,
      "currency" -> currency) ++
      optionMap("customer", customerId) ++
      optionMap("card", cardToken.map(_.id)) ++
      optionMap("card", cardParams) ++
      optionMap("description", description)

    api.post(classURL(this), params).map(_.extract[Charge])
  }

  def get(id: String)(implicit ctx: ExecutionContext): Future[Charge] = {
    api.get(instanceURL(this, id)).map(_.extract[Charge])
  }

  def all(
    count: Int = 10,
    offset: Int = 0,
    customer: Option[Customer] = None)(implicit ctx: ExecutionContext): Future[List[Charge]] = {

    val customerParam = customer.map(x => Map("customer" -> x.id)).getOrElse(Nil)
    val params = Map("count" -> count, "offset" -> offset) ++ customerParam

    api.get(classURL(this), params).map(_.extract[ChargeCollection].data)
  }
}

