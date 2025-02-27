package lila.ws

import chess.Color
import chess.format.{ FEN, Uci }

trait StringValue extends Any:
  def value: String
  override def toString = value
trait IntValue extends Any:
  def value: Int
  override def toString = value.toString

case class User(id: User.ID) extends AnyVal

object User:
  type ID = String

object Game:
  case class Id(value: String) extends AnyVal with StringValue:
    def full(playerId: PlayerId) = FullId(s"$value$playerId")
  case class FullId(value: String) extends AnyVal with StringValue:
    def gameId   = Id(value take 8)
    def playerId = PlayerId(value drop 8)
  case class PlayerId(value: String) extends AnyVal with StringValue

  case class Player(id: PlayerId, userId: Option[User.ID])

  // must only contain invariant data (no status, turns, or termination)
  // because it's cached in Mongo.scala
  case class Round(id: Id, players: Color.Map[Player], ext: Option[RoundExt]):
    def player(id: PlayerId, userId: Option[User.ID]): Option[RoundPlayer] =
      Color.all.collectFirst {
        case c if players(c).id == id && players(c).userId == userId => RoundPlayer(id, c, ext)
      }

  case class RoundPlayer(
      id: PlayerId,
      color: Color,
      ext: Option[RoundExt]
  ):
    def tourId  = ext collect { case RoundExt.Tour(id) => id }
    def swissId = ext collect { case RoundExt.Swiss(id) => id }
    def simulId = ext collect { case RoundExt.Simul(id) => id }

  enum RoundExt(val id: String):
    case Tour(i: String)  extends RoundExt(i)
    case Swiss(i: String) extends RoundExt(i)
    case Simul(i: String) extends RoundExt(i)

object Simul:
  type ID = String

case class Simul(id: Simul.ID) extends AnyVal

object Tour:
  type ID = String

case class Tour(id: Tour.ID) extends AnyVal

object Study:
  type ID = String

case class Study(id: Study.ID) extends AnyVal

object Chat:
  type ID = String

object Team:
  type ID = String
  case class View(hasChat: Boolean)
  enum Access(val id: Int):
    case None    extends Access(0)
    case Leaders extends Access(10)
    case Members extends Access(20)

object Swiss:
  type ID = String

object Challenge:
  case class Id(value: String) extends AnyVal with StringValue
  enum Challenger:
    case Anon(secret: String)
    case User(userId: String)
    case Open

object Racer:
  type RaceId = String
  enum PlayerId:
    case User(uid: String)
    case Anon(sid: String)

case class Chat(id: Chat.ID) extends AnyVal

case class Sri(value: String) extends AnyVal with StringValue

object Sri:
  type Str = String
  def random = Sri(util.Util.secureRandom string 12)
  def from(str: String): Option[Sri] =
    if (str contains ' ') None
    else Some(Sri(str))

case class Flag private (value: String) extends AnyVal with StringValue

object Flag:
  def make(value: String) =
    value match
      case "simul" | "tournament" | "api" => Some(Flag(value))
      case _                              => None
  val api = Flag("api")

case class IpAddress(value: String) extends AnyVal with StringValue

case class Path(value: String) extends AnyVal with StringValue

case class ChapterId(value: String) extends AnyVal with StringValue

case class JsonString(value: String) extends AnyVal with StringValue

case class SocketVersion(value: Int) extends AnyVal with IntValue with Ordered[SocketVersion]:
  def compare(other: SocketVersion) = Integer.compare(value, other.value)

case class IsTroll(value: Boolean) extends AnyVal

case class RoomId(value: String) extends AnyVal with StringValue

object RoomId:
  def apply(v: StringValue): RoomId = RoomId(v.value)

case class ReqId(value: Int) extends AnyVal with IntValue

case class UptimeMillis(millis: Long) extends AnyVal:
  def toNow: Long = UptimeMillis.make.millis - millis

object UptimeMillis:
  def make = UptimeMillis(System.currentTimeMillis() - util.Util.startedAtMillis)

case class ThroughStudyDoor(user: User, through: Either[RoomId, RoomId])

case class RoundEventFlags(
    watcher: Boolean,
    owner: Boolean,
    player: Option[chess.Color],
    moveBy: Option[chess.Color],
    troll: Boolean
)

case class UserTv(value: User.ID) extends AnyVal with StringValue

case class Clock(white: Int, black: Int)
case class Position(lastUci: Uci, fen: FEN, clock: Option[Clock], turnColor: Color):
  def fenWithColor = s"$fen ${turnColor.letter}"
