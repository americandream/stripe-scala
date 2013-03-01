package com.stripe.model

import com.stripe.Utils._
import com.stripe.formats
import com.stripe.api
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

case class Invoice(
    date: Long,
    // id is optional since UpcomingInvoices don't have an ID.
    id: Option[String],
    periodStart: Long,
    periodEnd: Long,
    lines: InvoiceLines,
    subtotal: Int,
    total: Int,
    customer: String,
    attempted: Boolean,
    closed: Boolean,
    paid: Boolean,
    livemode: Boolean,
    attemptCount: Int,
    amountDue: Int,
    startingBalance: Int,
    endingBalance: Option[Int],
    nextPaymentAttempt: Option[Long],
    charge: Option[String],
    discount: Option[Discount]) {
}

case class InvoiceCollection(count: Int, data: List[Invoice])

object Invoice {
  def upcoming(customer: Customer)(implicit ctx: ExecutionContext): Future[Invoice] = {
    val params = Map("customer" -> customer.id)

    api.get(s"${classURL(this)}/upcoming", params).map(_.extract[Invoice])
  }

  def get(id: String)(implicit ctx: ExecutionContext): Future[Invoice] = {
    api.get(instanceURL(this, id)).map(_.extract[Invoice])
  }

  def all(customer: Option[Customer] = None,
    count: Int = 10,
    offset: Int = 0)(implicit ctx: ExecutionContext): Future[List[Invoice]] = {

    val params = Map("count" -> count, "offset" -> offset) ++
      optionMap("customer", customer.map(_.id))

    api.get(classURL(this), params).map(_.extract[InvoiceCollection].data)
  }
}