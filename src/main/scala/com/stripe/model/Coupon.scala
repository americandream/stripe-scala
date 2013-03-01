package com.stripe.model

import com.stripe.api
import com.stripe.formats
import com.stripe.Utils._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext

case class Coupon(
    id: Option[String] = None,
    amountOff: Option[Int] = None,
    percentOff: Option[Int] = None,
    currency: Option[String] = None,
    livemode: Boolean = false,
    duration: String = "month",
    redeemBy: Option[Long] = None,
    maxRedemptions: Option[Int] = None,
    timesRedeemed: Option[Int] = None,
    durationInMonths: Option[Int] = None) {

  def delete()(implicit ctx: ExecutionContext): Future[DeletedCoupon] = {
    require(id.isDefined)
    api.delete(instanceURL(this, id.get)).map(_.extract[DeletedCoupon])
  }
}

case class CouponCollection(count: Int, data: List[Coupon])

case class DeletedCoupon(id: String, deleted: Boolean)

object Coupon {
  def create(coupon: Coupon)(implicit ctx: ExecutionContext): Future[Coupon] = {
    val params: Map[String, Any] = Map("duration" -> coupon.duration) ++
      optionMap("id", coupon.id) ++
      optionMap("amount_off", coupon.amountOff) ++
      optionMap("percent_off", coupon.percentOff) ++
      optionMap("currency", coupon.currency) ++
      optionMap("duration_in_months", coupon.durationInMonths) ++
      optionMap("max_redemptions", coupon.maxRedemptions) ++
      optionMap("redeemBy", coupon.redeemBy)

    api.post(classURL(this), params).map(_.extract[Coupon])
  }

  def get(id: String)(implicit ctx: ExecutionContext): Future[Coupon] = {
    api.get(instanceURL(this, id)).map(_.extract[Coupon])
  }

  def all(count: Int = 10, offset: Int = 0)(implicit ctx: ExecutionContext): Future[List[Coupon]] = {
    api.get(classURL(this), Map("count" -> count, "offset" -> offset)).map(_.extract[CouponCollection].data)
  }
}
