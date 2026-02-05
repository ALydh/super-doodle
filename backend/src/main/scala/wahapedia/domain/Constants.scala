package wahapedia.domain

object Constants {
  object Validation {
    val MaxBattlelineDuplicates = 6
    val MaxDefaultDuplicates = 3
    val MaxEnhancements = 3
  }

  object Keywords {
    val EpicHero = "Epic Hero"
  }

  object Defaults {
    val UnknownDatasheet = "Unknown"
  }

  object Chapters {
    val FactionId = "SM"

    val Keywords: Map[String, String] = Map(
      "ultramarines" -> "Ultramarines",
      "blood-angels" -> "Blood Angels",
      "dark-angels" -> "Dark Angels",
      "space-wolves" -> "Space Wolves",
      "black-templars" -> "Black Templars",
      "deathwatch" -> "Deathwatch",
      "imperial-fists" -> "Imperial Fists",
      "raven-guard" -> "Raven Guard",
      "iron-hands" -> "Iron Hands",
      "salamanders" -> "Salamanders",
      "white-scars" -> "White Scars"
    )

    val AllKeywords: Set[String] = Keywords.values.toSet
  }
}
