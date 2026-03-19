# Stack Research

**Domain:** Pure functional tabletop wargame rules engine (Scala)
**Researched:** 2026-03-19
**Confidence:** HIGH (existing codebase verified; library versions confirmed against Maven/GitHub)

---

## Context: This Is a Continuation, Not Greenfield

The engine already exists in `wahapedia.engine`. This research is about what to use to **complete** it — not what to build it with. The core stack is fixed by the existing codebase. The question is what supporting patterns and libraries to add for the remaining work.

**Fixed constraints (must not change):**
- Scala 3.7.4 (current in build.sbt)
- Pure domain: no IO in engine package — `Either[GameError, (GameState, List[GameEvent])]` at every boundary
- ScalaTest 3.2.19 `AnyFlatSpec` + `Matchers` — existing test pattern
- `FixedDiceRoller` for deterministic test dice — existing pattern, extend it

---

## Recommended Stack

### Core Technologies

| Technology | Version | Purpose | Why Recommended |
|------------|---------|---------|-----------------|
| Scala 3 | 3.7.4 (current) | Language | Already in use. Opaque types for zero-cost domain IDs (`UnitId`, `ModelId`, `PlayerId`) already established. Enums for `Phase`, `PipelineStep`, `RerollTarget` — idiomatic and exhaustive. |
| ScalaTest | 3.2.19 (current) | Unit and integration tests | Already in use. `AnyFlatSpec` + `Matchers` is the established pattern across the engine. Do not switch. |
| ScalaCheck | 1.19.0 | Property-based testing for rule invariants | Rules have mathematical properties (e.g., "save with AP-0 never worse than base save", "FNP never increases damage", "reroll All always >= reroll Ones"). PBT catches edge-case interactions that scenario tests miss. Latest version (Sep 2024) supports Scala 3 natively via `scalacheck_3`. Use via `ScalaTestPlusScalaCheck` integration. |

### Supporting Libraries

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| scalatest-plus-scalacheck | 3.2.19.0 | Bridge between ScalaTest and ScalaCheck | Add when writing property specs for rule logic (stratagem CP constraints, coherency math, scoring invariants). Version must match ScalaTest minor version. |
| cats-core | already transitive via cats-effect | `NonEmptyList`, `Validated`, `Chain` | Use `NonEmptyList` when representing units that must have at least one model (avoids partial functions on `.head`). Use `Validated` for accumulating multiple validation errors in rule checks where you want all failures, not just the first. |
| scala-parser-combinators | 2.4.0 (current in build.sbt) | Ability string parsing | Already used. Extend `CoreEffects.fromWeaponAbilityString` for remaining unimplemented abilities (Melta, Blast, Hazardous, Indirect Fire, Pistol, Precision). |

### Development Tools

| Tool | Purpose | Notes |
|------|---------|-------|
| sbt | Build tool | Already configured. No changes needed. |
| FixedDiceRoller | Deterministic test dice | Already exists in `wahapedia.engine.combat`. The established pattern: pass exact die rolls as `Int*` varargs, engine consumes them in order. Extend for multi-round game simulations by constructing with larger sequences. |
| `scala.util.Random` seeded | Statistical simulation (non-test) | For the Bot/AI simulation driver that runs full games. Seed at construction, pass as `DiceRoller`. The pure `DiceRoller` trait already decouples this from the domain. |

---

## Installation

```scala
// Add to build.sbt libraryDependencies
"org.scalacheck" %% "scalacheck" % "1.19.0" % Test,
"org.scalatestplus" %% "scalacheck-1-18" % "3.2.19.0" % Test
```

The scalatest-plus-scalacheck artifact naming follows `scalacheck-{major}-{minor}` of the ScalaCheck version used. `scalacheck-1-18` is the correct artifact name for ScalaCheck 1.18.x/1.19.x compatibility with ScalaTest 3.2.19.

---

## Alternatives Considered

| Recommended | Alternative | When to Use Alternative |
|-------------|-------------|-------------------------|
| ScalaTest `AnyFlatSpec` (continue) | MUnit | Greenfield Scala 3 projects — lighter, better IDE output. Not here: switching would break all existing tests for no gain. |
| ScalaCheck via ScalaTest integration | standalone ScalaCheck `Properties` | If you wanted to run PBT outside the test framework. Not needed — the ScalaTest integration is simpler and fits the existing pattern. |
| `Either[GameError, T]` for rule validation | `cats.data.Validated` | When you need ALL validation failures at once (not stop-on-first). Use `Validated` only at the validation aggregation layer (e.g., validating an entire army list), not inside the engine's combat pipeline where first-error-wins is correct. |
| Plain `sealed trait Effect` ADT | Cats Free monad / DSL | If you needed to interpret effects differently for different backends (test vs. live). Overkill here: the engine has one interpretation and the `FixedDiceRoller` already solves the testability problem without Free. |
| Opaque types for domain IDs (continue) | `AnyVal` wrapper classes | Scala 2 idiom. Opaque types in Scala 3 are zero-cost and already established in this codebase. |

---

## What NOT to Use

| Avoid | Why | Use Instead |
|-------|-----|-------------|
| Akka / Pekko | No actor model needed. The engine is pure synchronous state transitions. Adding actors introduces async complexity, distributed failure modes, and JVM overhead for no benefit. | Plain `Either`-chained `execute` calls |
| scala-rules (scala-rules/rule-engine) | Abandoned — Scala 2.11 only, no Scala 3 port, archived. | `sealed trait Effect` + `EffectResolver` (existing approach) |
| `mutable.PriorityQueue` for fight order | Fights First / Fights Last ordering is a small, deterministic sort over a bounded list — not a dynamic priority queue. Using a mutable structure inside a pure function is a code smell. | `List.sortBy` over `UnitState` with a derived priority function |
| Cats Effect IO in the engine package | The engine is already pure (`wahapedia.engine` has no IO). Bringing IO into the domain layer would destroy testability and violate the established architecture. IO belongs only at the simulation runner layer (`wp40k.Main` and simulation harness). | `Either[GameError, (GameState, List[GameEvent])]` |
| `var` accumulation inside phase functions | `FightPhase.executeFight` already uses `var currentState` / `var allEvents` — this is a known compromise for the multi-weapon loop. For new phases, prefer `foldLeft` over weapon profiles to stay purely functional. | `weaponProfiles.foldLeft((state, List.empty[GameEvent])) { ... }` |

---

## Stack Patterns by Variant

**For encoding a new keyword/ability (e.g., Melta, Hazardous, Blast):**
- Add a new `Effect` case class or extend `CriticalEffect`
- Register in `CoreEffects.fromWeaponAbilityString`
- Write a `AttackPipelineSpec` scenario test with `FixedDiceRoller`
- Do NOT create a new resolver — funnel through `EffectResolver.collectAttackEffects`

**For encoding a new phase rule with conditional timing (e.g., Fights First ordering):**
- Add a `SubPhase` enum value or ordering predicate in the phase object
- Do NOT add IO or async — remain pure `(GameState, Command) => Either[GameError, (GameState, List[GameEvent])]`
- Use `List.sortBy` with a stable priority function over alive units

**For the simulation/bot layer:**
- Create a separate `wahapedia.simulation` package — never inside `wahapedia.engine`
- Bot player is a function `GameState => Command` — pure, no IO
- The `IO` wrapper goes at the outermost simulation runner only (cats-effect `IOApp`)
- Seed `scala.util.Random` once at simulation start, wrap in the existing `DiceRoller` trait

**For property-based tests on rule invariants:**
- Use `ScalaCheck` `Gen[UnitState]`, `Gen[WeaponProfile]`, `Gen[DiceRoller]`
- Express as mathematical properties: "damage after FNP <= damage before FNP"
- Integrate via `AnyFlatSpec with ScalaCheckPropertyChecks` from scalatest-plus-scalacheck

---

## Version Compatibility

| Package | Compatible With | Notes |
|---------|-----------------|-------|
| scalacheck_3 1.19.0 | Scala 3.7.4 | Confirmed published for Scala 3 |
| scalatestplus-scalacheck `scalacheck-1-18` 3.2.19.0 | ScalaTest 3.2.19 + ScalaCheck 1.18.x/1.19.x | Artifact name reflects ScalaCheck API version, not exact patch — 1.19.0 is API-compatible with the 1.18 integration |
| cats-core / cats-effect | Already in build.sbt as transitive dep | Use `cats.data.NonEmptyList`, `cats.data.Validated` from cats-core without adding a new dependency |

---

## Sources

- `/Users/alexanderlydh/Repositories/super-doodle/backend/build.sbt` — verified existing library versions (Scala 3.7.4, ScalaTest 3.2.19, cats-effect 3.6.1, fs2 3.12.2)
- `github.com/typelevel/scalacheck/releases` — confirmed ScalaCheck 1.19.0 released Sep 6, 2024, Scala 3 support verified (HIGH confidence)
- `mvnrepository.com/artifact/org.scalacheck/scalacheck_3/1.18.1` — confirmed Scala 3 artifact published (HIGH confidence)
- `scalatest.org/plus/scalacheck/versions` — confirmed scalatest-plus-scalacheck version matrix (HIGH confidence)
- Existing engine source (`wahapedia.engine.*`) — architecture patterns confirmed by reading actual code (HIGH confidence)
- `github.com/scala-rules/rule-engine` — confirmed abandoned, Scala 2.11 only, no Scala 3 (HIGH confidence, avoid)
- WebSearch for Free monad vs ADT tradeoffs — confirmed that Free monad is overkill when one interpretation exists and testability is already solved (MEDIUM confidence)

---

*Stack research for: WH40K 10th Edition game engine rules completion*
*Researched: 2026-03-19*
