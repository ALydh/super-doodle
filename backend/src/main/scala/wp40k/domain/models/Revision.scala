package wp40k.domain.models

case class Revision(
  id: String,
  wahapediaTimestamp: String,
  dbPath: String,
  fetchedAt: String,
  isActive: Boolean
)
