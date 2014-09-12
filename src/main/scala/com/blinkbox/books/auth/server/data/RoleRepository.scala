package com.blinkbox.books.auth.server.data

import com.blinkbox.books.slick.{TablesSupport, SlickTypes}

import scala.slick.driver.JdbcProfile
import scala.slick.profile.BasicProfile

trait RoleRepository[Profile <: BasicProfile] extends SlickTypes[Profile] {
  def fetchRoles(userId: UserId)(implicit session: Session): Set[Role]
}

trait JdbcRoleRepository[Profile <: JdbcProfile] extends RoleRepository[Profile] with TablesSupport[Profile, ZuulTables[Profile]] {
  import tables._
  import driver.simple._

  override def fetchRoles(userId: UserId)(implicit session: Session): Set[Role] =
    (for {
      p <- privileges if p.userId === userId
      r <- roles if r.id === p.roleId
    } yield r).list.toSet
}

class DefaultRoleRepository[Profile <: JdbcProfile](val tables: ZuulTables[Profile]) extends JdbcRoleRepository[Profile]
