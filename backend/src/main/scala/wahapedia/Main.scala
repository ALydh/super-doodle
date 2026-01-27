package wahapedia

import cats.effect.{IO, IOApp}
import cats.implicits.*
import fs2.Stream
import wahapedia.domain.models.*
import wahapedia.csv.{CsvProcessor, StreamingCsvParser}
import wahapedia.errors.ParseException

object Main extends IOApp.Simple {

  case class ParsedData(
    factions: List[Faction],
    sources: List[Source],
    abilities: List[Ability],
    datasheets: List[Datasheet],
    modelProfiles: List[ModelProfile],
    wargear: List[Wargear],
    unitComposition: List[UnitComposition],
    unitCost: List[UnitCost],
    keywords: List[DatasheetKeyword],
    datasheetAbilities: List[DatasheetAbility],
    stratagems: List[Stratagem],
    datasheetStratagems: List[DatasheetStratagem],
    enhancements: List[Enhancement],
    datasheetEnhancements: List[DatasheetEnhancement],
    detachmentAbilities: List[DetachmentAbility],
    datasheetDetachmentAbilities: List[DatasheetDetachmentAbility],
    options: List[DatasheetOption],
    leaders: List[DatasheetLeader],
    lastUpdate: List[LastUpdate]
  )

  def run: IO[Unit] = {
    val program = for {
      _ <- IO.println("Starting Warhammer 40k CSV Parser...")

      factions <- parseWithProgress("Factions.csv", FactionParser)
      sources <- parseWithProgress("Source.csv", SourceParser)
      abilities <- parseWithProgress("Abilities.csv", AbilityParser)
      datasheets <- parseWithProgress("Datasheets.csv", DatasheetParser)
      modelProfiles <- parseWithProgress("Datasheets_models.csv", ModelProfileParser)
      wargear <- parseWithProgress("Datasheets_wargear.csv", WargearParser)
      unitComposition <- parseWithProgress("Datasheets_unit_composition.csv", UnitCompositionParser)
      unitCost <- parseWithProgress("Datasheets_models_cost.csv", UnitCostParser)
      keywords <- parseWithProgress("Datasheets_keywords.csv", DatasheetKeywordParser)
      datasheetAbilities <- parseWithProgress("Datasheets_abilities.csv", DatasheetAbilityParser)
      options <- parseWithProgress("Datasheets_options.csv", DatasheetOptionParser)
      leaders <- parseWithProgress("Datasheets_leader.csv", DatasheetLeaderParser)
      stratagems <- parseWithProgress("Stratagems.csv", StratagemParser)
      datasheetStratagems <- parseWithProgress("Datasheets_stratagems.csv", DatasheetStratagemParser)
      enhancements <- parseWithProgress("Enhancements.csv", EnhancementParser)
      datasheetEnhancements <- parseWithProgress("Datasheets_enhancements.csv", DatasheetEnhancementParser)
      detachmentAbilities <- parseWithProgress("Detachment_abilities.csv", DetachmentAbilityParser)
      datasheetDetachmentAbilities <- parseWithProgress("Datasheets_detachment_abilities.csv", DatasheetDetachmentAbilityParser)
      lastUpdate <- parseWithProgress("Last_update.csv", LastUpdateParser)

      data = ParsedData(
        factions, sources, abilities, datasheets, modelProfiles, wargear,
        unitComposition, unitCost, keywords, datasheetAbilities, stratagems,
        datasheetStratagems, enhancements, datasheetEnhancements,
        detachmentAbilities, datasheetDetachmentAbilities, options, leaders, lastUpdate
      )
      
      _ <- printSummary(data)
      
    } yield ()
    
    program.handleErrorWith { error =>
      error match {
        case pe: ParseException => 
          IO.println(s"Parse error: ${pe.getMessage}")
        case e => 
          IO.println(s"Unexpected error: ${e.getMessage}")
          IO.raiseError(e)
      }
    }
  }

  private def parseWithProgress[A](
    filename: String, 
    parser: StreamingCsvParser[A]
  ): IO[List[A]] = {
    for {
      _ <- IO.println(s"Parsing $filename...")
      result <- CsvProcessor.failFastParse(s"../data/wahapedia/$filename", parser)
      _ <- IO.println(s"Parsed ${result.length} records from $filename")
    } yield result
  }

  private def printSummary(data: ParsedData): IO[Unit] = {
    for {
      _ <- IO.println("\n" + "="*60)
      _ <- IO.println("PARSING COMPLETE")
      _ <- IO.println("="*60)
      
      _ <- IO.println(s"\nCORE ENTITIES:")
      _ <- IO.println(s"  Factions: ${data.factions.length}")
      _ <- IO.println(s"  Sources: ${data.sources.length}")
      _ <- IO.println(s"  Abilities: ${data.abilities.length}")
      _ <- IO.println(s"  Datasheets: ${data.datasheets.length}")
      
      _ <- IO.println(s"\nUNIT DATA:")
      _ <- IO.println(s"  Model Profiles: ${data.modelProfiles.length}")
      _ <- IO.println(s"  Wargear: ${data.wargear.length}")
      _ <- IO.println(s"  Unit Composition: ${data.unitComposition.length}")
      _ <- IO.println(s"  Unit Costs: ${data.unitCost.length}")
      
      _ <- IO.println(s"\nASSOCIATIONS:")
      _ <- IO.println(s"  Keywords: ${data.keywords.length}")
      _ <- IO.println(s"  Datasheet Abilities: ${data.datasheetAbilities.length}")
      _ <- IO.println(s"  Options: ${data.options.length}")
      _ <- IO.println(s"  Leaders: ${data.leaders.length}")
      
      _ <- IO.println(s"\nSTRATEGEMS:")
      _ <- IO.println(s"  Stratagems: ${data.stratagems.length}")
      _ <- IO.println(s"  Datasheet Stratagems: ${data.datasheetStratagems.length}")
      
      _ <- IO.println(s"\nENHANCEMENTS:")
      _ <- IO.println(s"  Enhancements: ${data.enhancements.length}")
      _ <- IO.println(s"  Datasheet Enhancements: ${data.datasheetEnhancements.length}")
      
      _ <- IO.println(s"\nDETACHMENT ABILITIES:")
      _ <- IO.println(s"  Detachment Abilities: ${data.detachmentAbilities.length}")
      _ <- IO.println(s"  Datasheet Detachment Abilities: ${data.datasheetDetachmentAbilities.length}")
      
      _ <- IO.println(s"\nMETADATA:")
      _ <- IO.println(s"  Last Update: ${data.lastUpdate.headOption.map(_.timestamp).getOrElse("Unknown")}")
      
      totalRecords = data.factions.length + data.sources.length + data.abilities.length +
        data.datasheets.length + data.modelProfiles.length + data.wargear.length +
        data.unitComposition.length + data.unitCost.length + data.keywords.length +
        data.datasheetAbilities.length + data.stratagems.length + data.datasheetStratagems.length +
        data.enhancements.length + data.datasheetEnhancements.length + data.detachmentAbilities.length +
        data.datasheetDetachmentAbilities.length + data.options.length + data.leaders.length
      
      _ <- IO.println(s"\nTOTAL RECORDS PARSED: $totalRecords")
      _ <- IO.println("="*60)
      
    } yield ()
  }
}