package com.virtuslab.atlas

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers._
import akka.stream.ActorMaterializer
import Model._
import akka.util.ByteString
import com.virtuslab.atlas.API._
import de.heikoseeberger.akkahttpjson4s.Json4sSupport
import org.json4s.ext.EnumNameSerializer
import org.json4s.{DefaultFormats, Extraction, native}

import scala.concurrent.Future

class AtlasClient(implicit system: ActorSystem) extends Json4sSupport {

  private implicit val materializer = ActorMaterializer()
  private implicit val formats = DefaultFormats + new EnumNameSerializer(Status) + new EnumNameSerializer(Cardinality)
  private implicit val serialization = native.Serialization

  import system.dispatcher

  private val http = Http(system)

  private val domain = "localhost"//"sandbox.hortonworks.com"
  private val atlasApi = s"http://$domain:21000/api/atlas/v2"
  private val loginApi = s"http://$domain:21000/j_spring_security_check"
  private val username = "admin"
  private val password = "admin"
  private val  atlasSessionIdKeys = Seq("ATLASSESSIONID", "JSESSIONID")

  type AtlasSession = HttpHeader

  private def getCookies(authorized: HttpResponse) = {
    val seq = authorized.headers.filter(_.is(`Set-Cookie`.lowercaseName))
    println(seq)
    seq
  }

  def createSparkApplication(sparkApp: SparkApplication, create: Boolean): Future[HttpResponse] = {

    authorize().flatMap { authorized =>
      mapOrReturn(authorized) {
        val session = getAtlasSession(getCookies(authorized))
        println("SESSION | " + session)
        session.map { atlasSession =>
          if (create) {
            println("create")
            createOrUpdateSparkApplicationMetatype(atlasSession).flatMap { createMetatypeResult =>
              mapOrReturn(createMetatypeResult)(insertOrUpdateSparkApplication(sparkApp, atlasSession))
            }
          } else {
            println("not create")
            insertOrUpdateSparkApplication(sparkApp, atlasSession)
          }
        }.getOrElse(Future.successful(authorized))
      }
    }
  }

  private def insertOrUpdateSparkApplication(sparkApp: SparkApplication, atlasSession: AtlasSession) = {

    val inputs = sparkApp.inputs.map(input => AtlasEntity(typeName = "fs_path", Extraction.decompose(input)))
    val outputs = sparkApp.outputs.map(output => AtlasEntity(typeName = "fs_path", Extraction.decompose(output)))
    val atlasEntitiesWithExtInfo = AtlasEntitiesWithExtInfo(inputs ++ outputs, Map.empty)

    insertOrUpdateIOEntities(atlasEntitiesWithExtInfo, atlasSession).flatMap(
      mapOrReturn(_)(
        insertOrUpdateSparkApplicationEntity(
          prepareSparkApplicationEntity(sparkApp, inputs, outputs),
          atlasSession
        )
      )
    )

  }
  private def mapOrReturn(response: HttpResponse)(futureResult: => Future[HttpResponse]): Future[HttpResponse] = {
    if (response.status.isSuccess()) {
      println("is success ? " + response.status.isSuccess())
      futureResult
    } else {
      println("is success ? " + response.status.isSuccess())
      Future.successful(response)
    }
  }

  private def authorize(): Future[HttpResponse] = {
    val body: FormData = FormData("j_username" -> username, "j_password" -> password)
    http.singleRequest(HttpRequest(
      uri = loginApi,
      method = HttpMethods.POST,
      headers = Nil,
      entity = body.toEntity)
    ).map{ response =>
      println("AUTHORIZE |" + response)
      response
    }
  }

  private def getAtlasSession(cookies: Seq[HttpHeader]): Option[AtlasSession] = {
    cookies.find(cookie => atlasSessionIdKeys.contains(cookie.asInstanceOf[`Set-Cookie`].cookie.name))
      .map{ cookie =>
        val setCookie = cookie.asInstanceOf[`Set-Cookie`].cookie
        Cookie(setCookie.name, setCookie.value)
      }
  }

  private def postData(resource: String, atlasSession: AtlasSession)(body: RequestEntity) = {
    println("Request to " + atlasApi + resource)
    HttpRequest(
      uri = atlasApi + resource,
      method = HttpMethods.POST,
      headers = atlasSession :: Nil,
      entity = body.withContentType(ContentTypes.`application/json`))
  }


  private def prepareSparkApplicationEntity(entity: SparkApplication, inputs: Seq[API.AtlasEntity], outputs: Seq[API.AtlasEntity]) = {
    val applicationEntity = Extraction.decompose(entity)
      .replace("inputs" :: Nil, Extraction.decompose(inputs.map(AtlasObjectId.apply)))
      .replace("outputs" :: Nil, Extraction.decompose(outputs.map(AtlasObjectId.apply)))
    val application = AtlasEntity(typeName = "spark_application", applicationEntity)
    AtlasEntityWithExtInfo(application, Map.empty)
  }

  private def insertOrUpdateIOEntities(atlasEntitiesWithExtInfo: AtlasEntitiesWithExtInfo, atlasSession: AtlasSession) =
    Marshal(atlasEntitiesWithExtInfo).to[MessageEntity]
      .map(postData("/entity/bulk", atlasSession))
      .flatMap(http.singleRequest(_))
      .map{ response =>
        response.entity.dataBytes.runFold(ByteString(""))(_ ++ _).foreach { body =>
          println("Got response, body: " + body.utf8String)
        }
        response
      }

  private def insertOrUpdateSparkApplicationEntity(atlasEntityWithExtInfo: AtlasEntityWithExtInfo, atlasSession: AtlasSession) =
    Marshal(atlasEntityWithExtInfo).to[MessageEntity]
      .map(postData("/entity", atlasSession))
      .flatMap(http.singleRequest(_))
      .map{ response =>
        response.entity.dataBytes.runFold(ByteString(""))(_ ++ _).foreach { body =>
          println("Got response, body: " + body.utf8String)
        }
        response
      }

  private def createOrUpdateSparkApplicationMetatype(atlasSession: AtlasSession) =
    Marshal(Model.sparkApplication).to[MessageEntity]
      .map(postData("/types/typedefs", atlasSession))
      .flatMap(http.singleRequest(_))
      .map{ response =>
        response.entity.dataBytes.runFold(ByteString(""))(_ ++ _).foreach { body =>
          println("Got response, body: " + body.utf8String)
        }
        response
      }

}
