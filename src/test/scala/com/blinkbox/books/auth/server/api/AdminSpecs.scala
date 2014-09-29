package com.blinkbox.books.auth.server.api

import com.blinkbox.books.auth.server.AdminUserInfo
import com.blinkbox.books.auth.server.data.UserId
import com.blinkbox.books.auth.server.sso.{SsoTokenElevation, SsoTokenStatus}
import spray.http._

class AdminSpecs extends ApiSpecBase with AuthorisationTestHelpers {

  val credentials = addCredentials(OAuth2BearerToken(env.tokenInfoC.access_token))

  def search(params: (String, String)*): RouteResult = {
    val uri = Uri("/admin/users") withQuery(params : _*)

    Get(uri) ~> addCredentials(OAuth2BearerToken(env.tokenInfoC.access_token)) ~> route
  }

  def details(id: UserId): RouteResult =
    Get(s"/admin/users/${id.value.toString}") ~> addCredentials(OAuth2BearerToken(env.tokenInfoC.access_token)) ~> route

  override def beforeEach(): Unit = {
    super.beforeEach()
    env.ssoSessionInfo(SsoTokenStatus.Valid, SsoTokenElevation.Critical, tokenType = "access")
  }

  "The admin search service" should "retrieve users by username" in {
    search("username" -> "user.a@test.tst") ~> check {
      status should equal(StatusCodes.OK)
      responseAs[List[AdminUserInfo]] should equal(env.adminInfoUserA :: Nil)
    }
  }

  it should "return an empty list if a username is not found" in {
    search("username" -> "not.an.user@test.tst") ~> check {
      status should equal(StatusCodes.OK)
      responseAs[List[AdminUserInfo]] should equal(Nil)
    }
  }

  it should "retrieve users by first name and last name being case-insensitive" in {
    search("first_name" -> "a first", "last_name" -> "a Last") ~> check {
      status should equal(StatusCodes.OK)
      responseAs[List[AdminUserInfo]] should equal(env.adminInfoUserA :: Nil)
    }
  }

  it should "return an empty list if the given first name and last name do not have any exact match" in {
    search("first_name" -> "foo", "last_name" -> "bar") ~> check {
      status should equal(StatusCodes.OK)
      responseAs[List[AdminUserInfo]] should equal(Nil)
    }
  }

  it should "retrieve users by id" in {
    search("user_id" -> env.userIdA.value.toString) ~> check {
      status should equal(StatusCodes.OK)
      responseAs[List[AdminUserInfo]] should equal(env.adminInfoUserA :: Nil)
    }
  }

  it should "return an empty list if an user with the given id is not found" in {
    search("user_id" -> "100") ~> check {
      status should equal(StatusCodes.OK)
      responseAs[List[AdminUserInfo]] should equal(Nil)
    }
  }

  it should "not be reachable without proper privileges" in {
    Get("/admin/users", FormData(Map("username" -> "foo@bar.baz"))) ~> addCredentials(OAuth2BearerToken(env.tokenInfoA1.access_token)) ~> route ~> check {
      status should equal(StatusCodes.Forbidden)
    }
  }

  "The admin details endpoint" should "return user details for a given id" in {
    details(env.userIdA) ~> check {
      status should equal(StatusCodes.OK)
      responseAs[AdminUserInfo] should equal(env.adminInfoUserA)
    }
  }

  it should "return a 404 if the user does not exist" in {
    details(UserId(100)) ~> check {
      status should equal(StatusCodes.NotFound)
    }
  }

  it should "not be accessible without proper roles" in {
    Get(s"/admin/users/${env.userIdA.value.toString}") ~> addCredentials(OAuth2BearerToken(env.tokenInfoA1.access_token)) ~> route ~> check {
      status should equal(StatusCodes.Forbidden)
    }
  }

  "The administrative user information" should "contain information about previous usernames in most-recent-first order" in {
    details(env.userIdB) ~> check {
      status should equal(StatusCodes.OK)
      responseAs[AdminUserInfo] should equal(env.adminInfoUserB)
    }
  }
}
