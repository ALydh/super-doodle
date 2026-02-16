package wahapedia.http.endpoints

import io.circe.Json
import io.circe.generic.auto.*
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.json.circe.*
import sttp.tapir.generic.auto.*
import wahapedia.domain.auth.AuthenticatedUser
import wahapedia.http.TapirSecurity
import wahapedia.http.dto.*

object AuthEndpoints {

  val register: PublicEndpoint[RegisterRequest, (StatusCode, Json), AuthResponse, Any] =
    endpoint.post
      .in("api" / "auth" / "register")
      .in(jsonBody[RegisterRequest])
      .errorOut(statusCode.and(jsonBody[Json]))
      .out(statusCode(StatusCode.Created).and(jsonBody[AuthResponse]))

  val login: PublicEndpoint[LoginRequest, (StatusCode, Json), AuthResponse, Any] =
    endpoint.post
      .in("api" / "auth" / "login")
      .in(jsonBody[LoginRequest])
      .errorOut(statusCode.and(jsonBody[Json]))
      .out(jsonBody[AuthResponse])

  val logout: Endpoint[(Option[String], Option[String]), Unit, Unit, Json, Any] =
    endpoint.post
      .securityIn(TapirSecurity.tokenInput)
      .in("api" / "auth" / "logout")
      .out(jsonBody[Json])

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
