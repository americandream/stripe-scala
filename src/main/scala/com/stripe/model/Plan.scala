package com.stripe.model

import com.stripe.api
import com.stripe.formats
import com.stripe.Utils._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext

case class Plan(
    id: String,
    name: String,
    interval: String,
    amount: Int,
    currency: String,
    livemode: Boolean = false,
    trialPeriodDays: Option[Int] = None,
    intervalCount: Int = 1) {

  def delete(implicit ctx: ExecutionContext): Future[DeletedPlan] = {
    api.delete(instanceURL(this, id)).map(_.extract[DeletedPlan])
  }

  def updateName(newName: String)(implicit ctx: ExecutionContext): Future[Plan] = {
    api.post(instanceURL(this, id), Map("name" -> newName)).map(_.extract[Plan])
  }
}

case class PlanCollection(count: Int, data: List[Plan])

case class DeletedPlan(id: String, deleted: Boolean)

object Plan {
  def create(plan: Plan)(implicit ctx: ExecutionContext): Future[Plan] = {
    val params = Map(
      "id" -> plan.id,
      "amount" -> plan.amount,
      "currency" -> plan.currency,
      "interval" -> plan.interval,
      "interval_count" -> plan.intervalCount,
      "name" -> plan.name) ++ optionMap("trial_period_days", plan.trialPeriodDays)

    api.post(classURL(this), params).map(_.extract[Plan])
  }

  def get(id: String)(implicit ctx: ExecutionContext): Future[Plan] = {
    api.get(instanceURL(this, id)).map(_.extract[Plan])
  }

  def all(count: Int = 10, offset: Int = 0)(implicit ctx: ExecutionContext): Future[List[Plan]] = {
    api.get(classURL(this), Map("count" -> count, "offset" -> offset)).map(_.extract[PlanCollection].data)
  }
}