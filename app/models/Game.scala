package models

import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.functional.syntax._

import scala.concurrent.duration.Duration

object Geo {
  type Point = (Float,Float)
  type Box = (Point,Point)

  def distanceBetween(p1: Point, p2: Point): Double = {
    val (x1,y1) = p1
    val (x2,y2) = p2
    Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2))
  }

}

case class Gate(
  y: Float,
  width: Float
)

case class Island(
  location: (Float,Float),
  radius: Float
)

case class Course(
  upwind: Gate,
  downwind: Gate,
  laps: Int,
  markRadius: Float,
  islands: Seq[Island],
  bounds: Geo.Box
) {
  def width = Math.abs(bounds._1._1 - bounds._2._1)
  def height = Math.abs(bounds._1._2 - bounds._2._2)
  def center: Geo.Point = ((bounds._1._1 + bounds._2._1) / 2, (bounds._1._2 + bounds._2._2) / 2)
}

object Course {
  val default = Course(
    upwind = Gate(1000, 100),
    downwind = Gate(-100, 100),
    laps = 2,
    markRadius = 5,
    islands = Seq(
      Island((250, 300), 100),
      Island((150, 700), 80),
      Island((-200, 500), 60)
    ),
    bounds = ((800,1200), (-800,-400))
  )
}

case class Gust(
  position: Geo.Point,
  angle: Float, // degrees
  speed: Float,
  radius: Float
) {
  val radians = (90 - angle) * Math.PI / 180
}

object Gust {
  val default = Seq(
    Gust((-20, -20), 5, 4, 100),
    Gust((20, -10), -5, 6, 80),
    Gust((30, 40), 8, 1, 120)
  )
}

case class Spell(
  kind: String,
  duration: Int // seconds
)

case class Buoy(
  position: Geo.Point,
  radius: Float,
  spell: Spell
)

object Buoy {
  val default = Seq(
    Buoy((200, 200), 5, Spell("inversion", 20)),
    Buoy((-200, 400), 5, Spell("inversion", 20))
  )
}

case class RaceUpdate(
  now: DateTime,
  startTime: DateTime,
  course: Option[Course],
  opponents: Seq[BoatState] = Seq(),
  gusts: Seq[Gust] = Seq(),
  buoys: Seq[Buoy] = Seq(),
  playerSpell: Option[Spell] = None,
  triggeredSpells: Seq[Spell] = Seq(),
  leaderboard: Seq[String] = Seq()
)

object RaceUpdate {
  def initial(r: Race) = RaceUpdate(
    DateTime.now,
    startTime = r.startTime,
    course = Some(r.course)
  )
}

case class BoatState (
  name: String,
  position: Geo.Point,
  direction: Float,
  velocity: Float,
  passedGates: Seq[Float],
  ownSpell: Option[Spell] = None,
  spellCast: Option[Boolean]
) {

  def collisions(buoys: Seq[Buoy]): Option[Buoy] = buoys.find { buoy =>
    Geo.distanceBetween(buoy.position, position) <= buoy.radius
  }

}

case class PlayerUpdate(id: String, state: BoatState)

object JsonFormats {
  import utils.JsonFormats.dateTimeFormat

  implicit val pointFormat: Format[Geo.Point] = utils.JsonFormats.tuple2Format[Float,Float]
  implicit val boxFormat: Format[Geo.Box] = utils.JsonFormats.tuple2Format[Geo.Point,Geo.Point]

  implicit val spellFormat: Format[Spell] = Json.format[Spell]
  implicit val buoyFormat: Format[Buoy] = Json.format[Buoy]
  implicit val gustFormat: Format[Gust] = Json.format[Gust]
  implicit val gateFormat: Format[Gate] = Json.format[Gate]
  implicit val islandFormat: Format[Island] = Json.format[Island]
  implicit val courseFormat: Format[Course] = Json.format[Course]
  implicit val boatStateFormat: Format[BoatState] = Json.format[BoatState]
  implicit val playerUpdateFormat: Format[PlayerUpdate] = Json.format[PlayerUpdate]

  implicit val raceUpdateFormat: Format[RaceUpdate] = (
    (__ \ 'now).format[DateTime] and
      (__ \ 'startTime).format[DateTime] and
      (__ \ 'course).format[Option[Course]] and
      (__ \ 'opponents).format[Seq[BoatState]] and
      (__ \ 'gusts).format[Seq[Gust]] and
      (__ \ 'buoys).format[Seq[Buoy]] and
      (__ \ 'playerSpell).format[Option[Spell]] and
      (__ \ 'triggeredSpells).format[Seq[Spell]] and
      (__ \ 'leaderboard).format[Seq[String]]
    )(RaceUpdate.apply, unlift(RaceUpdate.unapply))

}