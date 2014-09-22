package com.blinkbox.books.auth.server.service

import com.blinkbox.books.auth.server.{PreviousUsernameInfo, AdminUserInfo}
import com.blinkbox.books.auth.server.data.UserId
import com.blinkbox.books.auth.server.services.{IdSearch, NameSearch, UsernameSearch}

class DefaultAdminUserServiceSpecs extends SpecBase {
  import env._

  val adminInfoUserA = AdminUserInfo(userIdA.external, userIdA.uri, userA.username, userA.firstName, userA.lastName, userA.allowMarketing, Nil)
  val adminInfoUserB = AdminUserInfo(
    userIdB.external, userIdB.uri, userB.username, userB.firstName, userB.lastName, userB.allowMarketing,
    PreviousUsernameInfo(userBPreviousUsername2.username, userBPreviousUsername2.createdAt) ::
    PreviousUsernameInfo(userBPreviousUsername1.username, userBPreviousUsername1.createdAt) :: Nil)

  "The admin search service" should "retrieve users by username" in {
    whenReady(adminUserService.userSearch(UsernameSearch("user.a@test.tst"))) { res =>
      res should equal(adminInfoUserA :: Nil)
    }
  }

  it should "return an empty list if a username is not found" in {
    whenReady(adminUserService.userSearch(UsernameSearch("not-an-user@test.tst"))) { res =>
      res should equal(Nil)
    }
  }

  it should "retrieve users by first name and last name being case-insensitive" in {
    whenReady(adminUserService.userSearch(NameSearch("a first", "a last"))) { res =>
      res should equal(adminInfoUserA :: Nil)
    }
  }

  it should "return an empty list if the given first name and last name do not have any exact match" in {
    whenReady(adminUserService.userSearch(NameSearch("foo", "bar"))) { res =>
      res should equal(Nil)
    }
  }

  it should "retrieve users by id" in {
    whenReady(adminUserService.userSearch(IdSearch(userIdA))) { res =>
      res should equal(adminInfoUserA :: Nil)
    }
  }

  it should "return an empty list if an user with the given id is not found" in {
    whenReady(adminUserService.userSearch(IdSearch(UserId(-1)))) { res =>
      res should equal(Nil)
    }
  }

  "The admin details service" should "return user details for a given id" in {
    whenReady(adminUserService.userDetails(userIdA)) { res =>
      res should equal(Some(adminInfoUserA))
    }
  }

  it should "return an empty option if the id does not exist" in {
    whenReady(adminUserService.userDetails(UserId(-1))) { res =>
      res should equal(None)
    }
  }

  "The administrative user information" should "contain information about previous usernames in most-recent-first order" in {
    whenReady(adminUserService.userDetails(userIdB)) { res =>
      res should equal(Some(adminInfoUserB))
    }

    whenReady(adminUserService.userSearch(NameSearch("b first", "b last"))) { res =>
      res should equal(adminInfoUserB :: Nil)
    }
  }
}
