package com.stripe

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import java.util.UUID
import com.stripe.model._
import org.scalatest.WordSpec
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.Future
import scala.concurrent.Await
import com.stripe.play.PlayHttpClient
import com.stripe.apachehttp.ApacheHttpClient
import org.scalatest.FunSpec

class StripeSuite extends WordSpec with StripeApiBehaviors {
  "Stripe APIs" when {
    "driven by Play! WS" should {
      com.stripe.api = new StripeApi with PlayHttpClient
      behave like allSuites
    }
    "driven by apache http" should {
      com.stripe.api = new StripeApi with ApacheHttpClient
      behave like allSuites
    }
  }
}

trait StripeApiBehaviors extends ShouldMatchers {
  this: WordSpec =>
  //set the stripe API key
  apiKey = "tGN0bIwXnHdwOa85VABjPdSn8nWY7G7I"

  val defaultTimeout = 5.minutes

  val defaultCard = ChargeCard(name = Some("Scala User"),
    cvc = Some(100),
    addressLine1 = Some("12 Main Street"),
    addressLine2 = Some("Palo Alto"),
    addressZip = Some("94105"),
    addressCountry = Some("USA"),
    number = "4242424242424242",
    expMonth = 3,
    expYear = 2015)

  def resultOf[T](f: Future[T]): T = {
    Await.result(f, defaultTimeout)
  }

  def uniquePlan() = Plan(id = s"PLAN-${UUID.randomUUID}", amount = 100, currency = "usd", interval = "month", name = "Scala Plan")

  def uniqueCoupon = Coupon(id = Some(s"COUPON-${UUID.randomUUID}"),
    duration = "once",
    percentOff = Some(10))

  val defaultInvoiceItem = InvoiceItem(amount = 100, currency = "usd")

  def allSuites {
    chargeSuite
    customerSuite
    planSuite
    invoiceItemSuite
    invoiceSuite
    tokenSuite
    accountSuite
    couponSuite
  }

  def chargeSuite {
    "have Charges that" should {
      "be createable" in {
        val charge = resultOf(Charge.create(amount = 100, currency = "usd", card = Some(defaultCard)))
        charge.refunded should be(false)
      }
      "be retrievable individually" in {
        val createdCharge = resultOf(Charge.create(amount = 100, currency = "usd", card = Some(defaultCard)))
        val retrievedCharge = resultOf(Charge.get(createdCharge.id))
        createdCharge should equal(retrievedCharge)
      }
      "be refundable" in {
        val charge = resultOf(Charge.create(amount = 100, currency = "usd", card = Some(defaultCard)))
        val refundedCharge = resultOf(charge.refund())
        refundedCharge.refunded should equal(true)
      }
      "be listable" in {
        val charge = resultOf(Charge.create(amount = 100, currency = "usd", card = Some(defaultCard)))
        val charges = resultOf(Charge.all())
        charges.headOption should be('defined)
      }
      "raise CardException for an invalid card" in {
        val e = intercept[CardException] {
          resultOf(Charge.create(amount = 100,
            currency = "usd", card = Some(defaultCard.copy(number = "4242424242424241"))))
        }
        e.param.get should equal("number")
      }
      "pass CVC, address, and zip checks in testmode" in {
        val charge = resultOf(Charge.create(amount = 100, currency = "usd", card = Some(defaultCard)))
        charge.card.cvcCheck.get should equal("pass")
        charge.card.addressLine1Check.get should equal("pass")
        charge.card.addressZipCheck.get should equal("pass")
      }
    }
  }

  def customerSuite {
    "have Customers that" should {
      "be creatable" in {
        val customer = resultOf(Customer.create(card = Some(defaultCard),
          description = Some("Test Description")))
        customer.description.get should be("Test Description")
        customer.activeCard should be('defined)
      }
      "be retrievable individually" in {
        val customer = resultOf(Customer.create(card = Some(defaultCard),
          description = Some("Test Description")))
        val retrievedCustomer = resultOf(Customer.get(customer.id))
        retrievedCustomer should equal(customer)
      }
      "be updateable" in {
        val customer = resultOf(Customer.create(card = Some(defaultCard),
          description = Some("Test Description")))
        val updatedCustomer = resultOf(customer.update(description = Some("Updated Scala Customer")))
        updatedCustomer.description.get should equal("Updated Scala Customer")
      }
      "be deleteable" in {
        val customer = resultOf(Customer.create(card = Some(defaultCard),
          description = Some("Test Description")))
        val deletedCustomer = resultOf(customer.delete())
        deletedCustomer.deleted should be(true)
        deletedCustomer.id should equal(customer.id)
      }
      "be listable" in {
        val customer = resultOf(Customer.create(card = Some(defaultCard),
          description = Some("Test Description")))
        val customers = resultOf(Customer.all())
        customers.headOption should be('defined)
      }
    }
  }

  def planSuite {
    "have Plans that" should {
      "be createable" in {
        val plan = resultOf(Plan.create(uniquePlan.copy(interval = "year")))
        plan.interval should equal("year")
      }
      "be retrieveable individually" in {
        val plan = resultOf(Plan.create(uniquePlan))
        val retrievedPlan = resultOf(Plan.get(plan.id))
        retrievedPlan should equal(plan)
      }
      "be deleteable" in {
        val plan = resultOf(Plan.create(uniquePlan))
        val deletedPlan = resultOf(plan.delete)
        deletedPlan.deleted should be(true)
        deletedPlan.id should equal(plan.id)
      }
      "be listable" in {
        val plan = resultOf(Plan.create(uniquePlan))
        val plans = resultOf(Plan.all())
        plans.headOption should be('defined)
      }
    }
    "have Customers that" should {
      "be creatable with a plan" in {
        val plan = resultOf(Plan.create(uniquePlan))
        val customer = resultOf(Customer.create(card = Some(defaultCard), plan = Some(plan.id)))
        customer.subscription.get.plan.id should equal(plan.id)
      }
      "be addable to a customer without a plan" in {
        val customer = resultOf(Customer.create(card = Some(defaultCard)))
        val plan = resultOf(Plan.create(uniquePlan))
        val subscription = resultOf(customer.updateSubscription(plan = plan.id))
        subscription.customer should equal(customer.id)
        subscription.plan.id should equal(plan.id)
      }
      "override an existing plan" in {
        val origPlan = resultOf(Plan.create(uniquePlan))
        val customer = resultOf(Customer.create(card = Some(defaultCard), plan = Some(origPlan.id)))
        customer.subscription.get.plan.id should equal(origPlan.id)
        val newPlan = resultOf(Plan.create(uniquePlan))
        val subscription = resultOf(customer.updateSubscription(newPlan.id))
        val updatedCustomer = resultOf(Customer.get(customer.id))
        updatedCustomer.subscription.get.plan.id should equal(newPlan.id)
      }
      "be able to cancel subscriptions" in {
        val plan = resultOf(Plan.create(uniquePlan))
        val customer = resultOf(Customer.create(card = Some(defaultCard), plan = Some(plan.id)))
        customer.subscription.get.status should equal("active")
        val cancelledSubscription = resultOf(customer.cancelSubscription())
        cancelledSubscription.status should be("canceled")
      }
    }
  }

  def invoiceItemSuite {
    def createDefaultInvoiceItem(): InvoiceItem = {
      val customer = resultOf(Customer.create(card = Some(defaultCard)))
      return resultOf(InvoiceItem.create(customer, defaultInvoiceItem))
    }
    "have Invoice Items that" should {
      "be creatable" in {
        val createdItem = createDefaultInvoiceItem()
        createdItem.id should be('defined)
        val retrievedInvoiceItem = resultOf(InvoiceItem.get(createdItem.id.get))
        createdItem should equal(retrievedInvoiceItem)
      }
      "be updateable" in {
        val item = createDefaultInvoiceItem()
        val updated = resultOf(item.update(amount = Some(200), description = Some("Updated Invoice")))
        updated.amount should equal(200)
        updated.description should equal(Some("Updated Invoice"))
      }
      "be deleteable" in {
        val item = createDefaultInvoiceItem()
        val deleted = resultOf(item.delete())
        deleted.deleted should be(true)
        deleted.id should equal(item.id.get)
      }
      "be listable" in {
        val invoiceItem = createDefaultInvoiceItem()
        val invoiceItems = resultOf(InvoiceItem.all())
        invoiceItems.headOption should be('defined)
      }
    }
  }

  def invoiceSuite {
    "have Invoices that" should {
      "be createable and retrievable" in {
        val customer = resultOf(Plan.create(uniquePlan).flatMap { plan =>
          Customer.create(card = Some(defaultCard), plan = Some(plan.id))
        })
        val invoices = resultOf(Invoice.all())
        val createdInvoice = invoices.head
        val retrievedInvoice = resultOf(Invoice.get(createdInvoice.id.get))
        retrievedInvoice.id should equal(createdInvoice.id)
        createdInvoice.id should be('defined)
      }
      "be listable" in {
        val customer = resultOf(Plan.create(uniquePlan).flatMap { plan =>
          Customer.create(card = Some(defaultCard), plan = Some(plan.id))
        })
        val invoices = resultOf(Invoice.all())
        invoices.headOption should be('defined)
      }
      "be looked up for a customer" in {
        val plan = resultOf(Plan.create(uniquePlan.copy(intervalCount = 2)))
        val customer = resultOf(Customer.create(card = Some(defaultCard)))
        resultOf(customer.updateSubscription(plan.id))
        val invoices = resultOf(Invoice.all(customer = Some(customer)))
        val invoice = invoices.head
        invoice.customer should equal(customer.id)
      }
      "be able to find upcoming invoices" in {
        val customer = resultOf(Customer.create(card = Some(defaultCard)))
        val invoiceItem = resultOf(InvoiceItem.create(customer, defaultInvoiceItem))
        val upcomingInvoice = resultOf(Invoice.upcoming(customer))
        upcomingInvoice.amountDue should equal(defaultInvoiceItem.amount)
      }
    }
  }

  def tokenSuite {
    "has Tokens that" should {
      "be createable" in {
        val token = resultOf(Token.create(defaultCard))
        token.used should be(false)
      }
      "be retrievable" in {
        val token = resultOf(Token.create(defaultCard))
        val retrievedToken = resultOf(Token.get(token.id))
        retrievedToken should equal(token)
      }
      "be usable" in {
        val token = resultOf(Token.create(defaultCard))
        token.used should be(false)
        val charge = resultOf(Charge.create(amount = 100, currency = "usd", cardToken = Some(token)))
        val retrievedToken = resultOf(Token.get(token.id))
        retrievedToken.used should equal(true)
      }
    }
  }

  def accountSuite {
    "have Account that" should {
      "be retrieved" in {
        val account = resultOf(Account.get())
        account.email should equal(Some("test+bindings@stripe.com"))
        account.chargeEnabled should equal(false)
        account.detailsSubmitted should equal(false)
        account.statementDescriptor should be(None)
        account.currenciesSupported.length should be(1)
        account.currenciesSupported.head should be("USD")
      }
    }
  }

  def couponSuite {
    "have Coupons that" should {
      "be able to be created and retrieved" in {
        val createdCoupon = resultOf(Coupon.create(uniqueCoupon))
        val retrievedCoupon = resultOf(Coupon.get(createdCoupon.id.get))
        createdCoupon should equal(retrievedCoupon)
      }
      "be deletable" in {
        val coupon = resultOf(Coupon.create(uniqueCoupon))
        val deletedCoupon = resultOf(coupon.delete())
        deletedCoupon.deleted should be(true)
        deletedCoupon.id should equal(coupon.id.get)
      }
      "be listed" in {
        val coupon = resultOf(Coupon.create(uniqueCoupon))
        val coupons = resultOf(Coupon.all())
        coupons.headOption should be('defined)
      }
    }
  }
}
