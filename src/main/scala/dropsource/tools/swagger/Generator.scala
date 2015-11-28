package dropsource.tools.swagger

import dropsource.tools.swagger.parse.ParseApi
import io.swagger.util.Json


object Generator extends App {

  val parse = ParseApi(args(2), args(0), args(1))

  val swagger = parse.buildSwagger()

  val output = Json.pretty(swagger).replaceAllLiterally("securityRequirement", "security")
  print(output)
}
