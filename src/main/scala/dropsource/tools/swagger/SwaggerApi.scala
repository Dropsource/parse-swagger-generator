package dropsource.tools.swagger

import java.net.URL

import io.swagger.models._
import io.swagger.models.auth.{ApiKeyAuthDefinition, In, SecuritySchemeDefinition}
import io.swagger.models.parameters.Parameter

import scala.collection.JavaConverters._

abstract class SwaggerApi(val url: URL) {

  private val swagger = new Swagger()

  val info: Info
  val version: String = "2.0"
  val host: String = url.getHost
  val basePath: String = url.getPath
  val schemes: Seq[Scheme] = Seq(Scheme.forValue(url.getProtocol))
  val produces: Seq[String] = Seq("application/json")
  val securityDefinitions: Map[String, SecuritySchemeDefinition] = Map(
    "parseApplicationId" -> new ApiKeyAuthDefinition().in(In.forValue("header")).name("X-Parse-Application-Id"),
    "parseRestApiKey" -> new ApiKeyAuthDefinition().in(In.forValue("header")).name("X-Parse-REST-API-Key")
  )
  val security: Seq[SecurityRequirement] = Seq(
    new SecurityRequirement().requirement("parseApplicationId"),
    new SecurityRequirement().requirement("parseRestApiKey")
  )

  val objectDefinitions: Seq[ModelImpl]
  val definitions: Seq[ModelImpl]
  val paths: Map[String, Path]
  val responses: Map[String, Response]
  val parameters: Seq[Parameter]
  val tags: Seq[Tag]

  def buildSwagger(): Swagger = {
    swagger.setSwagger(version)

    swagger.info(info)
      .host(host)
      .schemes(schemes.asJava)
      .basePath(basePath)
      .produces(produces.asJava)
      .tags(tags.asJava)
      .responses(responses.asJava)
      .paths(paths.asJava)

    swagger.setSecurityDefinitions(securityDefinitions.asJava)
    security.foreach(s => swagger.security(s))
    definitions.foreach(d => swagger.model(d.getName, d))
    parameters.foreach(p => swagger.parameter(p.getName, p))

    swagger
  }
}