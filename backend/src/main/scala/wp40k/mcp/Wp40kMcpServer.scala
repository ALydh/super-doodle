package wp40k.mcp

import cats.effect.IO
import cats.effect.kernel.Resource
import cats.implicits.*
import ch.linkyard.mcp.protocol.Initialize.PartyInfo
import ch.linkyard.mcp.server.{McpServer, ToolFunction}
import doobie.Transactor

class Wp40kMcpServer(refXa: Transactor[IO], userXa: Transactor[IO], refPrefix: String) extends McpServer[IO]:

  override def initialize(client: McpServer.Client[IO], info: McpServer.ConnectionInfo[IO]): Resource[IO, McpServer.Session[IO]] =
    Resource.pure(Wp40kSession())

  private class Wp40kSession extends McpServer.Session[IO] with McpServer.ToolProvider[IO]:
    override val serverInfo: PartyInfo = PartyInfo("wp40k-mcp", "1.0.0")

    override def instructions: IO[Option[String]] = Some(
      """Wp40k MCP Server — Warhammer 40,000 reference data and army management.
        |
        |Reference tools (no auth needed): list_factions, get_faction_datasheets, get_datasheet_detail,
        |get_faction_stratagems, get_faction_enhancements, get_faction_detachments, search_datasheets,
        |get_core_abilities, get_weapon_abilities, validate_army.
        |
        |get_faction_datasheets returns enriched summaries with point costs, keywords, transport, and leader
        |relationships. Optional filters: role (e.g. "Characters"), keyword (e.g. "Beast Snagga"),
        |search (name substring). Use this to browse a faction's units without needing get_datasheet_detail.
        |
        |get_faction_detachments returns full detachment rule text (abilities, descriptions) — not just names.
        |
        |search_datasheets returns enriched results (costs, keywords, leaders) when a factionId is provided.
        |
        |get_faction_stratagems accepts optional detachmentId to filter by detachment, and excludeCore (boolean)
        |to omit core stratagems. Without filters, returns all stratagems (faction + core).
        |
        |get_faction_enhancements returns enhancements with an eligibleDatasheets field listing which units
        |can take each enhancement, based on the datasheet_enhancements table.
        |
        |validate_army accepts an optional units list (datasheetId, sizeOptionLine, enhancementId, attachedLeaderId,
        |isAllied) to validate full army composition including points, duplicates, leaders, and enhancements.
        |All fields except factionId, battleSize, and detachmentId are optional — warlordId defaults to the first
        |unit if omitted, units defaults to empty, sizeOptionLine defaults to 1, isAllied defaults to false.
        |
        |Army management tools (require auth token): list_armies, get_army, create_army, update_army, delete_army.
        |Pass your bearer token as the 'token' parameter for authenticated tools.""".stripMargin
    ).pure

    override val tools: IO[List[ToolFunction[IO]]] =
      McpTools.allTools(refXa, userXa, refPrefix).pure
