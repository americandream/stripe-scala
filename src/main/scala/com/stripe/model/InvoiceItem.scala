package com.stripe.model

import com.stripe.api
import com.stripe.formats
import com.stripe.Utils._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext

case class InvoiceItem(
    id: Option[String] = None,
    amount: Int,
    currency: String,
    date: Long = 0L,
    livemode: Boolean = false,
    description: Option[String] = None,
    invoice: Option[Invoice] = None) {

  def update(amount: Option[Int] = None, description: Option[String] = None)(implicit ctx: ExecutionContext): Future[InvoiceItem] = {
    val params = Map[String, Any]() ++ optionMap("amount", amount) ++ optionMap("description", description)
    require(id.isDefined)
    api.post(instanceURL(this, id.get), params).map(_.extract[InvoiceItem])
  }

  def delete()(implicit ctx: ExecutionContext): Future[DeletedInvoiceItem] = {
    require(id.isDefined)
    api.delete(instanceURL(this, id.get)).map(_.extract[DeletedInvoiceItem])
  }
}

case class DeletedInvoiceItem(id: String, deleted: Boolean)

case class InvoiceItemCollection(count: Int, data: List[InvoiceItem])

object InvoiceItem {
  def create(
    customer: Customer,
    invoiceItem: InvoiceItem)(implicit ctx: ExecutionContext): Future[InvoiceItem] = {
    // if the invoice is set, it must have an id
    require(invoiceItem.invoice.map(_.id.isDefined).getOrElse(true))

    val params = Map("customer" -> customer.id,
      "amount" -> invoiceItem.amount,
      "currency" -> invoiceItem.currency) ++
      optionMap("invoice", invoiceItem.invoice.map(_.id))
    optionMap("description", invoiceItem.description)

    api.post(classURL(this), params).map(_.extract[InvoiceItem])
  }

  def get(id: String)(implicit ctx: ExecutionContext): Future[InvoiceItem] = {
    api.get(instanceURL(this, id)).map(_.extract[InvoiceItem])
  }

  def all(
    customer: Option[Customer] = None,
    count: Int = 10,
    offset: Int = 0)(implicit ctx: ExecutionContext): Future[List[InvoiceItem]] = {
    val customerParam = customer.map(x => Map("customer" -> x.id)).getOrElse(Nil)
    val params = Map("count" -> count, "offset" -> offset) ++ customerParam

    api.get(classURL(this), params).map(_.extract[InvoiceItemCollection].data)
  }
}