package com.stripe.model

import com.stripe.api
import com.stripe.Utils._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import com.stripe.formats

case class Customer(
    created: Long = 0L,
    id: String,
    livemode: Boolean = false,
    description: Option[String] = None,
    activeCard: Option[Card] = None,
    email: Option[String] = None,
    delinquent: Option[Boolean] = None,
    subscription: Option[Subscription] = None,
    discount: Option[Discount] = None,
    accountBalance: Option[Int] = None) {

  def update(chargeCard: Option[ChargeCard] = None,
    cardToken: Option[String] = None,
    couponCode: Option[String] = None,
    description: Option[String] = None,
    accountBalance: Option[Int] = None,
    email: Option[String] = None)(implicit ctx: ExecutionContext): Future[Customer] = {

    require(!(chargeCard.isDefined && cardToken.isDefined))
    val params = Map[String, Any]() ++
      optionMap("card", chargeCard) ++
      optionMap("card", cardToken) ++
      optionMap("coupon", couponCode) ++
      optionMap("description", description) ++
      optionMap("account_balance", accountBalance) ++
      optionMap("email", email)
    api.post(instanceURL(this, id), params).map(_.extract[Customer])
  }

  def delete()(implicit ctx: ExecutionContext): Future[DeletedCustomer] = {
    api.delete(instanceURL(this, id)).map(_.extract[DeletedCustomer])
  }

  def updateSubscription(plan: String,
    couponCode: Option[String] = None,
    prorate: Option[Boolean] = None /* default: true */ ,
    trialEnd: Option[Int] = None,
    chargeCard: Option[ChargeCard] = None,
    cardToken: Option[String] = None,
    quantity: Option[Int] = None /* default: 1 */ )(implicit ctx: ExecutionContext): Future[Subscription] = {
    require(!(chargeCard.isDefined && cardToken.isDefined))
    val params = Map("plan" -> plan) ++
      optionMap("coupon", couponCode) ++
      optionMap("prorate", prorate) ++
      optionMap("trial_end", trialEnd) ++
      optionMap("card", chargeCard) ++
      optionMap("card", cardToken) ++
      optionMap("quantity", quantity)

    api.post(s"${instanceURL(this, id)}/subscription", params).map(_.extract[Subscription])
  }

  def cancelSubscription(atPeriodEnd: Option[Boolean] = None)(implicit ctx: ExecutionContext): Future[Subscription] = {
    val params = optionMap("at_period_end", atPeriodEnd).toMap
    api.delete(s"${instanceURL(this, id)}/subscription", params).map(_.extract[Subscription])
  }
}

case class CustomerCollection(count: Int, data: List[Customer])
case class DeletedCustomer(id: String, deleted: Boolean)

object Customer {
  def get(id: String)(implicit ctx: ExecutionContext): Future[Customer] = {
    api.get(instanceURL(this, id)).map(_.extract[Customer])
  }

  def create(
    card: Option[ChargeCard] = None,
    cardToken: Option[Token] = None,
    couponCode: Option[String] = None,
    email: Option[String] = None,
    description: Option[String] = None,
    accountBalance: Option[Int] = None,
    plan: Option[String] = None,
    trialEnd: Option[Int] = None,
    quantity: Option[Int] = None)(implicit ctx: ExecutionContext): Future[Customer] = {

    require(!(card.isDefined && cardToken.isDefined))

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

    val params = Map[String, Any]() ++
      optionMap("card", cardParams) ++
      optionMap("card", cardToken.map(_.id)) ++
      optionMap("coupon_code", couponCode) ++
      optionMap("email", email) ++
      optionMap("description", description) ++
      optionMap("account_balance", accountBalance) ++
      optionMap("plan", plan) ++
      optionMap("quantity", quantity) ++
      optionMap("trialEnd", trialEnd)

    api.post(classURL(this), params).map(_.extract[Customer])
  }

  def all(
    count: Int = 10,
    offset: Int = 0)(implicit ctx: ExecutionContext): Future[List[Customer]] = {

    val params = Map("count" -> count, "offset" -> offset)

    api.get(classURL(this), params).map(_.extract[CustomerCollection].data)
  }
}