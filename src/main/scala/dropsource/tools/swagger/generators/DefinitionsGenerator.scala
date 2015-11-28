package dropsource.tools.swagger.generators

import dropsource.tools.swagger.parse.ParseType
import io.swagger.models.ModelImpl
import io.swagger.models.properties._

object DefinitionsGenerator {
  def defaults(objectDefinitions: Seq[ModelImpl]): Seq[ModelImpl] = {
    val user = objectDefinitions.find(_.getName == "User").get
    Seq(
      created, updated, error, deleted, pointer, relation, userSignedUp, associatedFile, geoPoint
    ) ++ objectDefinitions.map(query) ++ Seq(userLoggedIn(user))
  }

  def created = new ModelImpl().name("Created").`type`("object")
    .property("createdAt", new DateProperty().description("Created At Date Time"))
    .property("objectId", new StringProperty().description("Id of the created object"))

  def userSignedUp = new ModelImpl().name("UserCreated").`type`("object")
    .property("createdAt", new DateProperty().description("Created At Date Time"))
    .property("objectId", new StringProperty().description("Id of the created object"))
    .property("sessionToken", new StringProperty().description("Session Id of the created object"))

  def userLoggedIn(user: ModelImpl) = {
    val model = new ModelImpl().name(user.getName + "LoggedIn").`type`("object")
      .property("sessionToken", new StringProperty().description("Session Id of the created object"))

    model.setProperties(user.getProperties)
    model
  }

  def updated = new ModelImpl()
    .name("Updated")
    .`type`("object")
    .property("updatedAt", new DateProperty().description("Updated At Date"))

  def fileUploaded = new ModelImpl()
    .name("FileUploaded")
    .`type`("object")
    .property("name", new StringProperty().description("Name of the Image"))
    .property("url", new StringProperty().description("URL of the Image"))

  def deleted = new ModelImpl().name("Deleted").`type`("object")

  def query(childDefinition: ModelImpl) = new ModelImpl()
    .name(childDefinition.getName + "Query")
    .`type`("object")
    .property("results", new ArrayProperty()
      .items(new RefProperty(childDefinition.getName))
      .description(childDefinition.getName + " Results")
    )

  def error = new ModelImpl()
    .name("Error")
    .`type`("object")
    .property("code", new IntegerProperty())
    .property("error", new StringProperty())

  def pointer = new ModelImpl()
    .name("Pointer")
    .`type`("object")
    .property("__type", new StringProperty())
    .property("className", new StringProperty())
    .property("objectId", nonRequiredStringProp)

  def relation = new ModelImpl()
    .name("Relation")
    .`type`("object")
    .property("__type", new StringProperty())
    .property("className", new StringProperty())
    .property("objectId", nonRequiredStringProp)

  def associatedFile = new ModelImpl()
    .name(ParseType.FILE)
    .`type`("object")
    .property("__type", new StringProperty()) // File
    .property("name", new StringProperty())


  def geoPoint = new ModelImpl()
    .name(ParseType.GEO_POINT)
    .`type`("object")
    .property("__type", new StringProperty()) // GeoPoint
    .property("latitude", new FloatProperty())
    .property("longitude", new FloatProperty())

  private def nonRequiredStringProp = {
    val s = new StringProperty()
    s.setRequired(false)
    s
  }
}