package com.stripe

import scala.concurrent.Future
import scala.concurrent.ExecutionContext

trait HttpClient {
  def request(
    method: String,
    url: String,
    headers: Seq[(String, String)],
    paramList: Map[String, String])(implicit ctx: ExecutionContext): Future[(Int, String)]
}