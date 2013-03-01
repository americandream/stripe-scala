package com.stripe.model

import com.stripe.api
import com.stripe.Utils._
import scala.concurrent.Future

case class Discount(
  id: String,
  coupon: String,
  start: Long,
  customer: String,
  end: Option[Long])

case class DeletedDiscount(id: String, deleted: Boolean)
