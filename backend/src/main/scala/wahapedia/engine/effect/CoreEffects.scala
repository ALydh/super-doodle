package wahapedia.engine.effect

object CoreEffects:

  def fromWeaponAbilityString(abilityStr: String): List[Effect] =
    val normalized = abilityStr.trim.toLowerCase
    val parts = normalized.split(",").map(_.trim).filter(_.nonEmpty)
    parts.flatMap(parseAbility).toList

  private def parseAbility(s: String): Option[Effect] = s match
    case "lethal hits" =>
      Some(OnCritical(PipelineStep.HitRoll, CriticalEffect.AutoWound))

    case sustainedPattern(nStr) =>
      val n = if nStr == null || nStr.isEmpty then 1 else nStr.toInt
      Some(OnCritical(PipelineStep.HitRoll, CriticalEffect.ExtraHits(n)))

    case "devastating wounds" =>
      Some(OnCritical(PipelineStep.WoundRoll, CriticalEffect.MortalWounds(1)))

    case "torrent" =>
      Some(AutoPass(PipelineStep.HitRoll))

    case "twin-linked" =>
      Some(RerollDice(PipelineStep.WoundRoll, RerollTarget.Failed))

    case "lance" =>
      Some(ModifyRoll(PipelineStep.WoundRoll, 1, DidNotMove()))

    case heavyPattern() =>
      Some(ModifyRoll(PipelineStep.HitRoll, 1, DidNotMove()))

    case "assault" =>
      None

    case rapidFirePattern(nStr) =>
      val n = if nStr == null || nStr.isEmpty then 1 else nStr.toInt
      Some(ExtraAttacks(n, DidNotMove()))

    case blastPattern() =>
      None

    case meltaPattern(nStr) =>
      val n = if nStr == null || nStr.isEmpty then 2 else nStr.toInt
      None

    case "hazardous" =>
      None

    case "indirect fire" =>
      None

    case "pistol" =>
      None

    case "precision" =>
      None

    case "ignores cover" =>
      None

    case antiPattern(keyword, target) =>
      Some(SetMinimumRoll(PipelineStep.WoundRoll, target.dropRight(1).toInt))

    case _ => None

  private val sustainedPattern = """sustained hits (\d+)?""".r
  private val rapidFirePattern = """rapid fire (\d+)?""".r
  private val heavyPattern = """heavy""".r
  private val blastPattern = """blast""".r
  private val meltaPattern = """melta (\d+)?""".r
  private val antiPattern = """anti-(.+?) (\d+\+)""".r

  def fromCoreAbility(name: String): Option[Effect] = name.toLowerCase match
    case "stealth" =>
      Some(ModifyRoll(PipelineStep.HitRoll, -1))

    case fnpPattern(target) =>
      Some(FeelNoPainEffect(target.dropRight(1).toInt))

    case _ => None

  private val fnpPattern = """feel no pain (\d+\+)""".r

  val DeepStrike = "deep strike"
  val LoneOperative = "lone operative"
  val Infiltrators = "infiltrators"
  val FightsFirst = "fights first"
  val DeadlyDemise = "deadly demise"
  val Stealth = "stealth"
