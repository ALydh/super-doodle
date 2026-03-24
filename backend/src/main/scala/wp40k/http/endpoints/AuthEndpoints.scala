package wp40k.http.endpoints

import io.circe.Json
import io.circe.generic.auto.*
import sttp.model.StatusCode
import sttp.model.headers.CookieValueWithMeta
import sttp.tapir.*
import sttp.tapir.json.circe.*
import sttp.tapir.generic.auto.*
import wp40k.http.TapirSecurity
import wp40k.http.dto.*

object AuthEndpoints {

  val register: PublicEndpoint[RegisterRequest, (StatusCode, Json), (AuthResponse, CookieValueWithMeta), Any] =
    endpoint.post
      .in("api" / "auth" / "register")
      .in(jsonBody[RegisterRequest])
      .errorOut(statusCode.and(jsonBody[Json]))
      .out(statusCode(StatusCode.Created).and(jsonBody[AuthResponse]).and(setCookie("session")))

  val login: PublicEndpoint[LoginRequest, (StatusCode, Json), (AuthResponse, CookieValueWithMeta), Any] =
    endpoint.post
      .in("api" / "auth" / "login")
      .in(jsonBody[LoginRequest])
      .errorOut(statusCode.and(jsonBody[Json]))
      .out(jsonBody[AuthResponse].and(setCookie("session")))

  val logout: Endpoint[(Option[String], Option[String]), Unit, Unit, (Json, CookieValueWithMeta), Any] =
    endpoint.post
      .securityIn(TapirSecurity.tokenInput)
      .in("api" / "auth" / "logout")
      .out(jsonBody[Json].and(setCookie("session")))

  val me: Endpoint[(Option[String], Option[String]), Unit, (StatusCode, Json), UserResponse, Any] =
    endpoint.get
      .securityIn(TapirSecurity.tokenInput)
      .in("api" / "auth" / "me")
      .errorOut(statusCode.and(jsonBody[Json]))
      .out(jsonBody[UserResponse])

  val createInvite: Endpoint[(Option[String], Option[String]), Unit, (StatusCode, Json), InviteResponse, Any] =
    endpoint.post
      .securityIn(TapirSecurity.tokenInput)
      .in("api" / "invites")
      .errorOut(statusCode.and(jsonBody[Json]))
      .out(statusCode(StatusCode.Created).and(jsonBody[InviteResponse]))

  val listInvites: Endpoint[(Option[String], Option[String]), Unit, (StatusCode, Json), List[InviteResponse], Any] =
    endpoint.get
      .securityIn(TapirSecurity.tokenInput)
      .in("api" / "invites")
      .errorOut(statusCode.and(jsonBody[Json]))
      .out(jsonBody[List[InviteResponse]])

  val all = List(register, login, logout, me, createInvite, listInvites)
}
