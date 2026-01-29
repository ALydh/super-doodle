package wahapedia.auth

import cats.effect.IO
import org.mindrot.jbcrypt.BCrypt

object PasswordHasher {

  def hash(password: String): IO[String] =
    IO.blocking(BCrypt.hashpw(password, BCrypt.gensalt()))

  def verify(password: String, hash: String): IO[Boolean] =
    IO.blocking(BCrypt.checkpw(password, hash))
}
