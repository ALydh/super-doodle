package wahapedia.domain.types

import wahapedia.errors.ParseError
import wahapedia.errors.InvalidId

opaque type DatasheetId = String
opaque type FactionId = String
opaque type AbilityId = String
opaque type SourceId = String

object DatasheetId {
  def apply(id: String): DatasheetId = id
  def value(id: DatasheetId): String = id
  
  def parse(id: String): Either[ParseError, DatasheetId] = {
    if (id.matches("\\d{9}")) Right(DatasheetId(id))
    else Left(InvalidId(id))
  }
}

object FactionId {
  def apply(id: String): FactionId = id
  def value(id: FactionId): String = id

  def parse(id: String): Either[ParseError, FactionId] = {
    if (id.nonEmpty && id.matches("[A-Za-z]{2,3}")) Right(FactionId(id))
    else Left(InvalidId(id))
  }
}

object AbilityId {
  def apply(id: String): AbilityId = id
  def value(id: AbilityId): String = id
  
  def parse(id: String): Either[ParseError, AbilityId] = {
    if (id.matches("[a-z]{3}\\d{3}")) Right(AbilityId(id))
    else Left(InvalidId(id))
  }
}

object SourceId {
  def apply(id: String): SourceId = id
  def value(id: SourceId): String = id
  
  def parse(id: String): Either[ParseError, SourceId] = {
    if (id.nonEmpty) Right(SourceId(id))
    else Left(InvalidId(id))
  }
}