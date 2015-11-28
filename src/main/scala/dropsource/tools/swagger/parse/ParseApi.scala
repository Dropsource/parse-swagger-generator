package dropsource.tools.swagger.parse

import java.net.URL

import dropsource.tools.swagger.generators.{DefinitionsGenerator, PathsGenerator}
import dropsource.tools.swagger.{SwaggerApi, config}
import io.swagger.models._
import io.swagger.models.parameters.{HeaderParameter, PathParameter, QueryParameter}
import io.swagger.models.properties._
import play.api.libs.json.{JsArray, JsObject, JsString, Json}

import scala.collection.JavaConverters._
import scalaj.http.{Http, HttpResponse}

case class ParseApi(private val appId: String,
                    private val email: String,
                    private val password: String) extends SwaggerApi(new URL(config.getString("parse.api.url"))) {

  val app = Json
    .parse(getApp.body)
    .as[ParseApplication](Json.format[ParseApplication])

  println(appId)
  println(app.restKey)

  val schema = Json
    .parse(getSchema.body)

  val cloudFunctions = Json
    .parse(getCloudFunctions.body)

  val contact = new Contact()
    .email(email)
    .url(app.dashboardURL)

  val license = new License()
    .url("https://parse.com/about/terms")
    .name("Parse " + "Terms")

  val info = new Info()
    .title(app.appName)
    .version("1.0")
    .description(app.appName + " Swagger Document")
    .contact(contact)


  /**
    * User Defined Object Definitions that will be used
    * to generate default routes & models
    */
  val objectDefinitions = (schema \ "results")
    .as[JsArray].value
    .filter(d => (d \ "className").as[String].head != '_') // Get rid of default objects
    .map { d =>
    val model = new ModelImpl()
      .name((d \ "className").as[String])
      .`type`("object")

    (d \ "fields").as[JsObject].value.foreach { case (name, typeObj) =>
      model.addProperty(name, ParseType((typeObj \ "type").as[String]).toSwaggerProperty(name))
    }

    model
  }

  /**
    * Parse Defined Default Definitions
    * These Definitions require special routes to be
    * written for them
    */
  val defaultDefinitions = (schema \ "results")
    .as[JsArray].value
    .filter(d => (d \ "className").as[String].head == '_') // Get rid of default objects
    .map { d =>
    val model = new ModelImpl()
      .name((d \ "className").as[String].drop(1))
      .`type`("object")

    (d \ "fields").as[JsObject].value.foreach { case (name, typeObj) =>
      model.addProperty(name, ParseType((typeObj \ "type").as[String]).toSwaggerProperty(name))
    }

    model
  }

  /**
    * The Default Query Parameters used by every [[io.swagger.models.Path]]
    */
  val parameters = Seq(
    new QueryParameter().name("where").`type`(SwaggerType.STRING.`type`).required(false).description("Where query filter ({':field': ':value'})"),
    new QueryParameter().name("order").`type`(SwaggerType.STRING.`type`).required(false).description("Specify a field to sort by (+/- :field)"),
    new QueryParameter().name("limit").`type`(SwaggerType.INTEGER.`type`).format(SwaggerType.INTEGER.format).required(false).description("Limit the number of objects returned by the query"),
    new QueryParameter().name("skip").`type`(SwaggerType.INTEGER.`type`).format(SwaggerType.INTEGER.format).required(false).description("Use with limit to paginate through results"),
    new QueryParameter().name("keys").`type`(SwaggerType.ARRAY.`type`).items(new StringProperty()).collectionFormat("csv").required(false).description("Restrict the fields returned by the query (comma separated)"),
    new QueryParameter().name("include").`type`(SwaggerType.ARRAY.`type`).items(new StringProperty()).collectionFormat("csv").required(false).description("Use on Pointer columns to return the full object (comma separated)"),
    new HeaderParameter().name("X-Parse-Revocable-Session").required(false).`type`(SwaggerType.INTEGER.`type`).format(SwaggerType.INTEGER.format).description("Whether or not to return a revocable session."),
    new HeaderParameter().name("X-Parse-Session-Token").required(true).`type`(SwaggerType.STRING.`type`).description("The Session token of the Logged In User."),
    new PathParameter().name("objectId").`type`(SwaggerType.STRING.`type`).description("Id of the object.")
  )

  val responses = Map(
    "BadRequest" -> new Response().schema(new RefProperty().asDefault(DefinitionsGenerator.error.getName)).description("Bad Request"),
    "Updated" -> new Response().schema(new RefProperty(DefinitionsGenerator.updated.getName)).description("Successful Update"),
    "Deleted" -> new Response().schema(new RefProperty(DefinitionsGenerator.deleted.getName)).description("Successful Deletion")
  )

  val definitions: Seq[ModelImpl] = DefinitionsGenerator.defaults(objectDefinitions ++ defaultDefinitions) ++ objectDefinitions

  val paths: Map[String, Path] = objectDefinitions
    .flatMap(PathsGenerator.defaultPaths)
    .toMap

  val tags = objectDefinitions.map(d => new Tag().name(d.getName).description(d.getName + " operations"))

  def getSchema: HttpResponse[String] =
    Http(url.toString + "/schemas")
      .headers(Map("X-Parse-Application-Id" -> appId, "X-Parse-Master-Key" -> app.masterKey))
      .method("GET")
      .asString

  def getCloudFunctions: HttpResponse[String] =
    Http(url.toString + "/hooks/functions")
      .headers(Map("X-Parse-Application-Id" -> appId, "X-Parse-Master-Key" -> app.masterKey))
      .method("GET")
      .asString

  private def getApp: HttpResponse[String] =
    Http(url.toString + "/apps/" + appId)
      .headers(Map("X-Parse-Email" -> email, "X-Parse-Password" -> password))
      .method("GET")
      .asString

  override def buildSwagger(): Swagger = {
    val swagger = super.buildSwagger()

    swagger.paths((swagger.getPaths.asScala ++ PathsGenerator.userPaths(defaultDefinitions.find(_.getName == "User").get)).asJava)
    defaultDefinitions.foreach(m => swagger.model(m.getName, m))
    swagger
  }
}

case class ParseApplication(appName: String,
                            dashboardURL: String,
                            applicationId: String,
                            clientKey: String,
                            javascriptKey: String,
                            windowsKey: String,
                            webhookKey: String,
                            restKey: String,
                            masterKey: String,
                            clientPushEnabled: Boolean,
                            clientClassCreationEnabled: Boolean,
                            requireRevocableSessions: Boolean,
                            revokeSessionOnPasswordChange: Boolean)

case class ParseType(`type`: String) {

  def toSwaggerProperty(name: String): Property = {
    val prop = `type` match {
      case ParseType.STRING => new StringProperty()
      case ParseType.NUMBER => new IntegerProperty()
      case ParseType.BOOLEAN => new BooleanProperty()
      case ParseType.ARRAY => new ArrayProperty()
      case ParseType.OBJECT => new ObjectProperty()
      case ParseType.DATE => new DateProperty
      case ParseType.FILE => new FileProperty()
      case ParseType.RELATION => new RefProperty("Relation")
      case ParseType.ACL => new StringProperty()
      case ParseType.POINTER => new RefProperty("Pointer")
    }

    prop.setName(name)
    prop
  }
}

object ParseType {
  val STRING = "String"
  val NUMBER = "Number"
  val BOOLEAN = "Boolean"
  val ARRAY = "Array"
  val OBJECT = "Object"
  val DATE = "Date"
  val RELATION = "Relation"
  val POINTER = "Pointer"
  val GEO_POINT = "GeoPoint"
  val FILE = "File"
  val ACL = "ACL"
}

case class SwaggerType(`type`: String, format: String) {
  def toJson = JsObject(Map("type" -> `type`, "format" -> format)
    .filter(_._2.nonEmpty)
    .map(s => s._1 -> JsString(s._2))
    .toSeq)
}

object SwaggerType {
  val STRING = SwaggerType("string", "")
  val INTEGER = SwaggerType("integer", "int32")
  val LONG = SwaggerType("integer", "int64")
  val FLOAT = SwaggerType("number", "float")
  val DOUBLE = SwaggerType("number", "double")
  val BYTE = SwaggerType("string", "byte")
  val BINARY = SwaggerType("string", "binary")
  val BOOLEAN = SwaggerType("boolean", "")
  val DATE = SwaggerType("string", "date")
  val DATE_TIME = SwaggerType("string", "date-time")
  val PASSWORD = SwaggerType("string", "password")
  val FILE = SwaggerType("file", "")
  val ARRAY = SwaggerType("array", "")
}