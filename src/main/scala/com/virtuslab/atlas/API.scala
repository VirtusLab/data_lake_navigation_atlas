package com.virtuslab.atlas

import com.virtuslab.atlas.API.Cardinality.Cardinality
import org.json4s.JsonAST.{JObject, JValue}

object API {
  object Status extends Enumeration {
    type Status = Value
    val ACTIVE = Value("ACTIVE")
    val DELETED = Value("DELETED")
  }
  object Cardinality extends Enumeration {
    type Cardinality = Value
    val SINGLE = Value("SINGLE")
  }

  case class AtlasMetatype(
                          name: String,
                          superTypes: Seq[String],
                          typeVersion: String,
                          attributesDefs: Seq[AtlasAttribute]
                          )
  case class AtlasAttribute(
                           name: String,
                           typeName: String,
                           cardinality: Cardinality = Cardinality.SINGLE,
                           isUnique: Boolean = false,
                           isOptional: Boolean = false,
                           isIndexable: Boolean = false
                           )
  case class AtlasClassification(typeName: String,
                                attributes: JValue)
  case class AtlasEntity(typeName: String,
                       attributes: JValue,
                       classification: List[AtlasClassification] = List.empty,
                       version: Option[Int] = None,
                       status: Status.Status = Status.ACTIVE,
                       guid: Option[String] = None,
                       createdBy: Option[String] = None,
                       updatedBy: Option[String] = None,
                       createTime: Option[Long] = None,
                       updateTime: Option[Long] = None)

  case class UniqueAttributes(qualifiedName: Option[String])
  object UniqueAttributes {
    def apply(jValue: JValue) = new UniqueAttributes(
      jValue.asInstanceOf[JObject].values.get("qualifiedName").asInstanceOf[Option[String]]
    )
  }

  case class AtlasObjectId(guid: Option[String], typeName: String, uniqueAttributes: UniqueAttributes)
  object AtlasObjectId {
    def apply(entity: AtlasEntity) =
      new AtlasObjectId(entity.guid, entity.typeName, UniqueAttributes(entity.attributes))
  }

  case class AtlasEntitiesWithExtInfo(entities: List[AtlasEntity], referredEntities: Map[String, Any])
  case class AtlasEntityWithExtInfo(entity: AtlasEntity, referredEntities: Map[String, Any])
  case class AtlasMetatypeWrapper(entityDefs: Seq[AtlasMetatype])
}
