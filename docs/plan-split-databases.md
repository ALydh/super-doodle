# Split Databases Plan

Separate the single SQLite database into two: a read-only reference database (built at CI time) and a read-write user database (persists across deploys).

## Current State

Single `wahapedia.db` containing:
- Reference data (~19 tables from CSVs)
- User data (6 tables for auth + armies)

## Target State

### Reference DB (`wahapedia-ref.db`) - Read-only, built at CI time

Tables populated from CSVs:
- `factions`, `sources`, `abilities`
- `datasheets`, `model_profiles`, `wargear`, `unit_composition`, `unit_cost`
- `datasheet_keywords`, `datasheet_abilities`, `datasheet_options`, `datasheet_leaders`
- `datasheet_stratagems`, `datasheet_enhancements`, `datasheet_detachment_abilities`
- `stratagems`, `enhancements`, `detachment_abilities`
- `parsed_wargear_options`, `parsed_loadouts`, `unit_wargear_defaults`, `weapon_abilities`
- `last_update`

### User DB (`wahapedia-user.db`) - Read-write, persists across deploys

Tables for user-generated data:
- `users`, `sessions`, `invites`
- `armies`, `army_units`, `army_unit_wargear_selections`

## Benefits

- No CSV files needed on server
- Reference data is immutable, can be replaced on each deploy
- User data persists independently
- Smaller deployment artifact

## Files to Create/Modify

### New Files

**`backend/src/main/scala/wahapedia/db/Database.scala`**
- Two transactors: `refXa` (read-only) and `userXa` (read-write)
- Path configuration via environment variables: `REF_DB_PATH`, `USER_DB_PATH`
- Default paths for local dev: `wahapedia-ref.db`, `wahapedia-user.db`

**`backend/src/main/scala/wahapedia/BuildRefDb.scala`**
- Standalone app to create and populate reference DB from CSVs
- Usage: `sbt "runMain wahapedia.BuildRefDb ../data/wahapedia wahapedia-ref.db"`
- Used only at build time

**`scripts/build-ref-db.sh`**
```bash
#!/bin/bash
cd backend
sbt "runMain wahapedia.BuildRefDb ../data/wahapedia wahapedia-ref.db"
```

### Modified Files

**`backend/src/main/scala/wahapedia/db/Schema.scala`**
- Split into `initializeRefSchema()` and `initializeUserSchema()`
- Reference schema: CSV-derived tables only
- User schema: auth + army tables

**`backend/src/main/scala/wahapedia/db/ReferenceDataRepository.scala`**
- Accept `refXa` transactor parameter
- All queries read from reference DB

**`backend/src/main/scala/wahapedia/db/ArmyRepository.scala`**
- Accept `userXa` transactor parameter
- All queries read/write to user DB

**`backend/src/main/scala/wahapedia/db/UserRepository.scala`**
- Accept `userXa` transactor parameter

**`backend/src/main/scala/wahapedia/db/SessionRepository.scala`**
- Accept `userXa` transactor parameter

**`backend/src/main/scala/wahapedia/db/InviteRepository.scala`**
- Accept `userXa` transactor parameter

**`backend/src/main/scala/wahapedia/Main.scala`**
- Read DB paths from environment variables
- Initialize both databases
- Pass correct transactor to each repository

## Implementation Order

1. Create `Database.scala` with dual transactor management
2. Create `BuildRefDb.scala` for CI-time DB building
3. Modify `Schema.scala` to split ref/user schemas
4. Update all repositories to accept transactor parameter
5. Update `Main.scala` to wire everything together
6. Create `scripts/build-ref-db.sh`
7. Update Makefile with `build-ref-db` target

## Verification

1. Run `scripts/build-ref-db.sh` - creates `wahapedia-ref.db`
2. Run `sbt run` - app uses both databases
3. Test: create user, create army, verify data in correct DBs
4. Restart app - user data persists, ref data loads
