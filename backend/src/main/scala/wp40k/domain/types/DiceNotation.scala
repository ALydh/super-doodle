package wp40k.domain.types

import wp40k.errors.ParseError
import wp40k.errors.InvalidFormat

opaque type DiceNotation = String

object DiceNotation {
  def apply(notation: String): DiceNotation = notation
  def value(dice: DiceNotation): String = dice
  
  def parse(s: String): Either[ParseError, DiceNotation] = {
    if (s.matches("\\d*D\\d+(\\+\\d+)?")) Right(DiceNotation(s))
    else Left(InvalidFormat("dice", s))
  }
  
  extension (dice: DiceNotation) {
    def asString: String = dice
  }
}