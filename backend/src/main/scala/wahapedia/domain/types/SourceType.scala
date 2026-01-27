package wahapedia.domain.types

import wahapedia.errors.{ParseError, InvalidFormat}

enum SourceType:
  case Expansion
  case Rulebook
  case FactionPack
  case Codex
  case Index
  case Boxset
  case Datasheet

object SourceType {
  def parse(s: String): Either[ParseError, SourceType] = s match {
    case "Expansion" => Right(SourceType.Expansion)
    case "Rulebook" => Right(SourceType.Rulebook)
    case "Faction Pack" => Right(SourceType.FactionPack)
    case "Codex" => Right(SourceType.Codex)
    case "Index" => Right(SourceType.Index)
    case "Boxset" => Right(SourceType.Boxset)
    case "Datasheet" => Right(SourceType.Datasheet)
    case _ => Left(InvalidFormat("source_type", s))
  }

  def asString(sourceType: SourceType): String = sourceType match {
    case SourceType.Expansion => "Expansion"
    case SourceType.Rulebook => "Rulebook"
    case SourceType.FactionPack => "Faction Pack"
    case SourceType.Codex => "Codex"
    case SourceType.Index => "Index"
    case SourceType.Boxset => "Boxset"
    case SourceType.Datasheet => "Datasheet"
  }
}
