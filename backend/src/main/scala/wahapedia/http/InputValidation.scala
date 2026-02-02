package wahapedia.http

object InputValidation {

  sealed trait ValidationError {
    def message: String
  }
  case object EmptyUsername extends ValidationError {
    def message = "Username cannot be empty"
  }
  case object UsernameTooLong extends ValidationError {
    def message = "Username cannot exceed 50 characters"
  }
  case object UsernameTooShort extends ValidationError {
    def message = "Username must be at least 3 characters"
  }
  case object InvalidUsernameChars extends ValidationError {
    def message = "Username can only contain letters, numbers, underscores, and hyphens"
  }
  case object EmptyArmyName extends ValidationError {
    def message = "Army name cannot be empty"
  }
  case object ArmyNameTooLong extends ValidationError {
    def message = "Army name cannot exceed 100 characters"
  }
  case object EmptyPassword extends ValidationError {
    def message = "Password cannot be empty"
  }
  case object PasswordTooShort extends ValidationError {
    def message = "Password must be at least 6 characters"
  }

  private val usernamePattern = "^[a-zA-Z0-9_-]+$".r

  def validateUsername(username: String): Either[ValidationError, String] = {
    val trimmed = username.trim
    if (trimmed.isEmpty) Left(EmptyUsername)
    else if (trimmed.length < 3) Left(UsernameTooShort)
    else if (trimmed.length > 50) Left(UsernameTooLong)
    else if (!usernamePattern.matches(trimmed)) Left(InvalidUsernameChars)
    else Right(trimmed)
  }

  def validatePassword(password: String): Either[ValidationError, String] =
    if (password.isEmpty) Left(EmptyPassword)
    else if (password.length < 6) Left(PasswordTooShort)
    else Right(password)

  def validateArmyName(name: String): Either[ValidationError, String] = {
    val trimmed = name.trim
    if (trimmed.isEmpty) Left(EmptyArmyName)
    else if (trimmed.length > 100) Left(ArmyNameTooLong)
    else Right(trimmed)
  }
}
