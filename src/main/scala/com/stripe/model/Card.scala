package com.stripe.model

case class ChargeCard(
  number: String,
  expMonth: Int,
  expYear: Int,
  cvc: Option[Int] = None,
  name: Option[String] = None,
  addressLine1: Option[String] = None,
  addressLine2: Option[String] = None,
  addressZip: Option[String] = None,
  addressState: Option[String] = None,
  addressCountry: Option[String] = None)

case class Card(
  last4: String,
  `type`: String,
  expMonth: Int,
  expYear: Int,
  fingerprint: String,
  country: Option[String] = None,
  name: Option[String] = None,
  addressLine1: Option[String] = None,
  addressLine2: Option[String] = None,
  addressZip: Option[String] = None,
  addressState: Option[String] = None,
  addressCountry: Option[String] = None,
  cvcCheck: Option[String] = None,
  addressLine1Check: Option[String] = None,
  addressZipCheck: Option[String] = None)