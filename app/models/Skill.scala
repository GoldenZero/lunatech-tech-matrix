package models

import play.api.libs.functional.syntax.{unlift, _}
import play.api.libs.json.{JsPath, Reads, Writes}
import slick.driver.PostgresDriver.api._
import slick.lifted.{ForeignKeyQuery, ProvenShape, TableQuery}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._


case class Skill(id: Option[Int] = None, userId: Int, techId: Int, skillLevel: SkillLevel)

object Skill {
  implicit val userSkillReads: Reads[Skill] = (
    (JsPath \ "id").readNullable[Int] and
      (JsPath \ "userId").read[Int] and
      (JsPath \ "techId").read[Int] and
      (JsPath \ "skillLevel").read[SkillLevel]
    ) (Skill.apply _)

  implicit val userSkillWrites: Writes[Skill] = (
    (JsPath \ "id").writeNullable[Int] and
      (JsPath \ "userId").write[Int] and
      (JsPath \ "techId").write[Int] and
      (JsPath \ "skillLevel").write[SkillLevel]
    ) (unlift(Skill.unapply))
}

class Skills(tag: Tag) extends Table[models.Skill](tag, "user_skills") {
  def id: Rep[Int] = column[Int]("id", O.PrimaryKey, O.AutoInc)

  def userId: Rep[Int] = column[Int]("user_id")

  def techId: Rep[Int] = column[Int]("tech_id")

  def skillLevel: Rep[SkillLevel] = column[SkillLevel]("skill_level")

  def * : ProvenShape[Skill] = (id.?, userId, techId, skillLevel) <> ((models.Skill.apply _).tupled, models.Skill.unapply _)

  def user: ForeignKeyQuery[Users, User] = foreignKey("USER_FK", userId, TableQuery[Users])(_.id)

  def tech: ForeignKeyQuery[Techs, Tech] = foreignKey("TECH_FK", techId, TableQuery[Techs])(_.id)
}


object Skills {
  val skillTable: TableQuery[Skills] = TableQuery[Skills]

  def add(userId: Int, techId: Int, skillLevel: SkillLevel): Future[Skill] = {
    skillExistsByTechAndUserId(techId, userId).flatMap {
      case true =>
        // in the end we should return a message that the skill already exists, not sure what to pass to the controller
        // in order to make this method work.
        for {
          skillId <- getSkillId(userId, techId)
        } yield Skill(skillId, userId = userId, techId = techId, skillLevel = skillLevel)
      case false =>
        val userSkillObject = Skill(
          userId = userId,
          techId = techId,
          skillLevel = skillLevel)
        val addSkillToSkillMatrixQuery = skillTable returning skillTable += userSkillObject
        Connection.db.run(addSkillToSkillMatrixQuery)
    }

  }

  def getSkillId(userId: Int, techId: Int): Future[Option[Int]] = {
    val query = skillTable.filter(x => x.userId === userId && x.techId === techId).map(_.id).take(1)
    Connection.db.run(query.result.headOption)
  }

  def getAllSkillMatrixByUser(user: User): Future[Seq[(Skill, Tech)]] = {
    val join = for {
      skill <- skillTable.filter(_.userId === user.id)
      tech <- Techs.techTable if skill.techId === tech.id
    } yield {
      (skill, tech)
    }
    Connection.db.run(join.result)
  }

  def update(skillId: Int, userId: Int, techId: Int, skillLevel: SkillLevel): Future[Option[Skill]] = {
    val nrOfUpdatedRows: Future[Int] = Connection.db.run(
      skillTable
        .filter(skill => skill.id === skillId && skill.userId === userId && skill.techId === techId)
        .map(skill => skill.skillLevel)
        .update(skillLevel))

    nrOfUpdatedRows.flatMap {
      case 0 => Future(None)
      case _ =>
        val selectQuery = skillTable.filter(_.id === skillId)
        Connection.db.run(selectQuery.result.headOption)
    }
  }

  def delete(userId: Int, skillId: Int): Future[Int] = {
    skillExistsForUser(skillId, userId).flatMap {
      case false => Future(0)
      case true => Connection.db.run(skillTable.filter(_.id === skillId).delete)
    }
  }

  def getAllSkills: Future[Seq[(Skill, User, Tech)]] = {
    val join = for {
      skill <- skillTable
      user <- Users.userTable if skill.userId === user.id
      tech <- Techs.techTable if skill.techId === tech.id
    } yield {
      (skill, user, tech)
    }

    Connection.db.run(join.result)
  }

  def getSkillById(skillId: Int): Future[Option[Skill]] = {
    val query = skillTable.filter(skill => skill.id === skillId)
    Connection.db.run(query.result.headOption)
  }

  def getSkillByTechId(techId: Int): Future[Seq[Skill]] = {
    val query = skillTable.filter(skill => skill.techId === techId)
    Connection.db.run(query.result)
  }

  private def skillExistsForUser(skillId: Int, userId: Int): Future[Boolean] = {
    Connection.db.run(skillTable.filter(skill => skill.id === skillId && skill.userId === userId).exists.result)
  }

  private def skillExistsByTechAndUserId(techId: Int, userId: Int): Future[Boolean] = {
    Connection.db.run(skillTable.filter(skill => skill.techId === techId && skill.userId === userId).exists.result)
  }

  private def getAll: Future[Seq[Skill]] = {
    Connection.db.run(skillTable.result)
  }
}