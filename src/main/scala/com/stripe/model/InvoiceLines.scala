package com.stripe.model

case class InvoiceLineSubscriptionPeriod(start: Long, end: Long)
case class InvoiceLineSubscription(plan: Plan, amount: Int, period: InvoiceLineSubscriptionPeriod)
case class InvoiceLines(
    subscriptions: List[InvoiceLineSubscription],
    invoiceItems: List[InvoiceItem],
    prorations: List[InvoiceItem]) {
}