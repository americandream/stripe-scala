package com.stripe

import org.apache.http.conn.ClientConnectionManager

package object apachehttp {
  var connectionManager: ClientConnectionManager = null
}