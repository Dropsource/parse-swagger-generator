package dropsource.tools

import com.typesafe.config.ConfigFactory

package object swagger {
  val config = ConfigFactory.load()
}
