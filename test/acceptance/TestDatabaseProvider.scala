package acceptance

import acceptance.TestData._
import models._
import play.api.Application
import play.api.db.slick.DatabaseConfigProvider
import slick.driver.JdbcProfile
import slick.driver.PostgresDriver.api._
import slick.jdbc.JdbcBackend
import slick.lifted.TableQuery

import scala.concurrent._
import scala.concurrent.duration.Duration


object TestDatabaseProvider {

  implicit val app: Application = play.api.Play.current


  val db: JdbcBackend#DatabaseDef = DatabaseConfigProvider.get[JdbcProfile].db

  val skillTable: TableQuery[Skills] = TableQuery[Skills]
  val techTable: TableQuery[Techs] = TableQuery[Techs]
  val userTable: TableQuery[Users] = TableQuery[Users]

  def setupDatabase(): Unit = {
    val setup = DBIO.seq((skillTable.schema ++ techTable.schema ++ userTable.schema).create)
    Await.result(db.run(setup), Duration.Inf)
  }


  def insertUserData(): Map[String, Int] = {
    val userOdersky = User(None, "Martin", "Odersky", "martin.odersky@scala.com")
    val userSeverus = User(None, "Severus", "Snape", "severus.snape@hogwarts.com")

    val idUserOdersky: Int = Await.result(db.run(userTable returning userTable.map(_.id) += userOdersky), Duration.Inf)
    val idUserSeverus: Int = Await.result(db.run(userTable returning userTable.map(_.id) += userSeverus), Duration.Inf)
    Map(ID_USER_ODERSKY -> idUserOdersky, ID_USER_SNAPE -> idUserSeverus)

  }

  def insertTechData(): Map[String, Int] = {
    val techScala = Tech(None, "scala", TechType.LANGUAGE)
    val techFunctional = Tech(None, "functional programming", TechType.CONCEPTUAL)
    val techDefense = Tech(None, "defense against the dark arts", TechType.CONCEPTUAL)
    val techDarkArts= Tech(None, "dark arts", TechType.CONCEPTUAL)

    val idTechScala: Int = Await.result(db.run(techTable returning techTable.map(_.id) += techScala), Duration.Inf)
    val idTechFunctional: Int = Await.result(db.run(techTable returning techTable.map(_.id) += techFunctional), Duration.Inf)
    val idTechDefense: Int = Await.result(db.run(techTable returning techTable.map(_.id) += techDefense), Duration.Inf)
    val idTechDarkArts: Int = Await.result(db.run(techTable returning techTable.map(_.id) += techDarkArts), Duration.Inf)

    Map(ID_TECH_SCALA -> idTechScala, ID_TECH_FUNCTIONAL -> idTechFunctional, ID_TECH_DEFENSE -> idTechDefense, ID_TECH_DARK_ARTS -> idTechDarkArts)
  }

  def insertSkillData(): Map[String, Int] = {
    val userMap = insertUserData()
    val techMap = insertTechData()

    val skillOderskyScala = Skill(None, userMap(ID_USER_ODERSKY), techMap(ID_TECH_SCALA), SkillLevel.CAN_TEACH)
    val skillOderskyFunctional = Skill(None, userMap(ID_USER_ODERSKY), techMap(ID_TECH_FUNCTIONAL), SkillLevel.CAN_TEACH)
    val skillSeverusDefense = Skill(None, userMap(ID_USER_SNAPE), techMap(ID_TECH_DEFENSE), SkillLevel.CAN_TEACH)
    val skillSeverusDarkArts = Skill(None, userMap(ID_USER_SNAPE), techMap(ID_TECH_DARK_ARTS), SkillLevel.CAN_TEACH)

    val idSkillOderskyScala: Int = Await.result(db.run(skillTable returning skillTable.map(_.id) += skillOderskyScala), Duration.Inf)
    Await.result(db.run(skillTable += skillOderskyFunctional), Duration.Inf)
    val idSkillSeverusDefense: Int = Await.result(db.run(skillTable returning skillTable.map(_.id) += skillSeverusDefense), Duration.Inf)
    Await.result(db.run(skillTable += skillSeverusDarkArts), Duration.Inf)

    userMap ++ techMap ++ Map(
      SKILL_ODERSKY_SCALA -> idSkillOderskyScala,
      SKILL_SEVERUS_DEFENSE ->  idSkillSeverusDefense
    )
  }

  def cleanUserData(): Int = {
    Await.result(db.run(userTable.delete), Duration.Inf)
  }

  def cleanTechData(): Int = {
    Await.result(db.run(techTable.delete), Duration.Inf)
  }

  def cleanSkillData(): Int = {
    Await.result(db.run(skillTable.delete), Duration.Inf)
    cleanUserData()
    cleanTechData()
  }

  def dropDatabase(): Unit = {
    val setup = DBIO.seq((skillTable.schema ++ techTable.schema ++ userTable.schema).drop)
    Await.result(db.run(setup), Duration.Inf)
  }

}
