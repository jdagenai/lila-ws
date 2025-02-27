package lila.ws
package ipc

import com.github.blemale.scaffeine.{ AsyncLoadingCache, Scaffeine }
import play.api.libs.json.*
import scala.concurrent.duration.*
import scala.concurrent.{ ExecutionContext, Future }

final class CrowdJson(
    mongo: Mongo,
    lightUserApi: LightUserApi
)(using ec: ExecutionContext):

  def room(crowd: RoomCrowd.Output): Future[ClientIn.Crowd] = {
    if (crowd.users.sizeIs > 20) keepOnlyStudyMembers(crowd) map { users =>
      crowd.copy(users = users, anons = 0)
    }
    else Future successful crowd
  } flatMap spectatorsOf map ClientIn.Crowd.apply

  def round(crowd: RoundCrowd.Output): Future[ClientIn.Crowd] =
    spectatorsOf(
      crowd.room.copy(
        users = if (crowd.room.users.sizeIs > 20) Nil else crowd.room.users
      )
    ) map { spectators =>
      ClientIn.Crowd(
        Json
          .obj(
            "white"    -> (crowd.players.white > 0),
            "black"    -> (crowd.players.black > 0),
            "watchers" -> spectators
          )
      )
    }

  private def spectatorsOf(crowd: RoomCrowd.Output): Future[JsObject] =
    if (crowd.users.isEmpty) Future successful Json.obj("nb" -> crowd.members)
    else
      inquirersCache.get {} flatMap { inquirers =>
        Future.traverse(crowd.users.filterNot(inquirers.contains))(lightUserApi.get) map { names =>
          Json.obj(
            "nb"    -> crowd.members,
            "users" -> names.filterNot(isBotName),
            "anons" -> crowd.anons
          )
        }
      }

  private def isBotName(str: String) = str startsWith "BOT "

  private val inquirersCache: AsyncLoadingCache[Unit, Set[User.ID]] =
    Scaffeine()
      .expireAfterWrite(1.second)
      .buildAsyncFuture(_ => mongo.inquirers)

  private val isStudyCache: AsyncLoadingCache[String, Boolean] =
    Scaffeine()
      .expireAfterWrite(20.minutes)
      .buildAsyncFuture(mongo.studyExists)

  private def keepOnlyStudyMembers(crowd: RoomCrowd.Output): Future[Iterable[User.ID]] =
    isStudyCache.get(crowd.roomId.value) flatMap {
      case false => Future successful Nil
      case true  => mongo.studyMembers(crowd.roomId.value) map crowd.users.toSet.intersect
    }
