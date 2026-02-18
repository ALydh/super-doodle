export interface GlossaryEntry {
  name: string;
  description: string;
}

export interface GlossarySection {
  title: string;
  entries: GlossaryEntry[];
}

export const glossarySections: GlossarySection[] = [
  {
    title: "Core Game Concepts",
    entries: [
      {
        name: "Movement (M)",
        description:
          "The Movement characteristic determines the maximum distance in inches a model can move during the Movement phase. Models with a Movement of 0\" cannot move at all.",
      },
      {
        name: "Toughness (T)",
        description:
          "The Toughness characteristic represents how durable a model is. When an attack wounds, it compares the attacking weapon's Strength against the target's Toughness to determine the wound roll required.",
      },
      {
        name: "Save (Sv)",
        description:
          "The Save characteristic represents the model's armour. After a successful wound roll, the defending player makes a saving throw by rolling a D6 and adding the weapon's AP modifier. If the result equals or exceeds the Save characteristic, the damage is prevented.",
      },
      {
        name: "Wounds (W)",
        description:
          "The Wounds characteristic shows how much damage a model can sustain before being destroyed. Each unsaved wound inflicts damage equal to the weapon's Damage characteristic, and the model is destroyed when reduced to 0 wounds.",
      },
      {
        name: "Leadership (Ld)",
        description:
          "The Leadership characteristic is used for Battle-shock tests. A unit must take a Battle-shock test at the start of the Command phase if it is below half strength. Roll 2D6 — if the result is equal to or greater than the unit's Leadership, the test is passed.",
      },
      {
        name: "Objective Control (OC)",
        description:
          "The Objective Control characteristic determines how much a model contributes to controlling an objective marker. A player controls an objective if the total OC of their models within range of it exceeds the opponent's total OC. Battle-shocked units have an OC of 0.",
      },
      {
        name: "Hit Roll",
        description:
          "When a model makes an attack, roll a D6 for the hit roll. If the result equals or exceeds the attacking model's Ballistic Skill (for ranged) or Weapon Skill (for melee), the attack hits. An unmodified 6 always hits, and an unmodified 1 always misses.",
      },
      {
        name: "Wound Roll",
        description:
          "After a successful hit, compare the weapon's Strength (S) to the target's Toughness (T): S is double T or more = 2+, S is greater than T = 3+, S equals T = 4+, S is less than T = 5+, S is half T or less = 6+. An unmodified 6 always wounds, and an unmodified 1 always fails.",
      },
      {
        name: "Saving Throw",
        description:
          "After a successful wound, the defending player rolls a D6 for a saving throw. The AP of the weapon modifies the required roll (e.g., AP -2 worsens the save by 2). If the modified roll equals or exceeds the model's Save characteristic, the wound is prevented. A model with an invulnerable save can use that instead.",
      },
      {
        name: "Invulnerable Save",
        description:
          "Some models have an invulnerable save, which is never modified by the attacking weapon's AP. The controlling player can choose to use either their normal save or their invulnerable save for each saving throw, but not both.",
      },
      {
        name: "Armour Penetration (AP)",
        description:
          "AP represents a weapon's ability to punch through armour. It is expressed as a negative modifier (e.g., AP -1, AP -2). This value worsens the target's saving throw by the specified amount. AP 0 means the weapon does not modify the save at all.",
      },
      {
        name: "Damage (D)",
        description:
          "The Damage characteristic determines how many wounds are inflicted on a model for each unsaved wound. For example, a weapon with Damage 3 inflicts 3 wounds per failed save. If a weapon has variable damage (e.g., D3), roll each time damage is inflicted. Excess damage from a single attack does not carry over to other models.",
      },
      {
        name: "Battle-shock",
        description:
          "A unit must take a Battle-shock test at the start of the Command phase if it is below half strength (fewer than half its starting models, or below half wounds for single-model units). Roll 2D6 — if the result is below the unit's Leadership, the unit is Battle-shocked until the start of your next Command phase. Battle-shocked units have OC 0 and cannot use Stratagems.",
      },
      {
        name: "Command Points (CP)",
        description:
          "Command Points are a resource used to activate Stratagems. Both players start the battle with a number of CP determined by the mission, and gain 1 CP at the start of each Command phase. Each Stratagem can only be used once per phase unless stated otherwise.",
      },
      {
        name: "Strength (S)",
        description:
          "The Strength characteristic of a weapon determines how likely it is to wound the target. It is compared against the target's Toughness to determine the wound roll needed.",
      },
      {
        name: "Attacks (A)",
        description:
          "The Attacks characteristic determines how many hit rolls a model makes when using a weapon. Each attack generates one hit roll.",
      },
      {
        name: "Ballistic Skill (BS)",
        description:
          "The Ballistic Skill represents a model's proficiency with ranged weapons. When making a ranged attack, the hit roll must equal or exceed this value to score a hit.",
      },
      {
        name: "Weapon Skill (WS)",
        description:
          "The Weapon Skill represents a model's proficiency in close combat. When making a melee attack, the hit roll must equal or exceed this value to score a hit.",
      },
    ],
  },
  {
    title: "Phases",
    entries: [
      {
        name: "Command Phase",
        description:
          "The first phase of each player's turn. In this phase: 1) Gain 1 CP. 2) Take Battle-shock tests for any units below half strength. 3) Resolve any abilities that trigger in the Command phase.",
      },
      {
        name: "Movement Phase",
        description:
          "In the Movement phase, you can move each of your units up to their Movement characteristic in inches. Units can also choose to Remain Stationary (0\" movement, unlocks Heavy weapons bonus), make a Normal Move, Advance (add D6\" but cannot shoot or charge unless the unit has Assault weapons), or Fall Back from engagement range.",
      },
      {
        name: "Shooting Phase",
        description:
          "In the Shooting phase, each of your eligible units can select targets and make ranged attacks. Units that Advanced can only fire Assault weapons. Units that Remained Stationary gain the benefit of Heavy weapons. Units within Engagement Range of enemy models can only shoot with Pistol weapons.",
      },
      {
        name: "Charge Phase",
        description:
          "In the Charge phase, you can declare charges with any of your units that are not within Engagement Range of an enemy. Select one or more enemy units within 12\" as targets, then roll 2D6 for the charge distance. The charging unit must end within Engagement Range (1\") of all selected targets, or the charge fails.",
      },
      {
        name: "Fight Phase",
        description:
          "In the Fight phase, units within Engagement Range (1\") of enemy models can make melee attacks. The active player selects an eligible unit to fight with first (units that charged this turn must be selected before other units). Each model makes attacks with its melee weapons. Fights First abilities allow a unit to fight before other units.",
      },
      {
        name: "Engagement Range",
        description:
          "A model is within Engagement Range of an enemy model if it is within 1\" horizontally and 5\" vertically. Models within Engagement Range of enemy models can fight in the Fight phase, and cannot make Normal Moves, Advance, or Fall Back without specific rules.",
      },
      {
        name: "Normal Move",
        description:
          "A Normal Move allows a unit to move each model up to a distance equal to its Movement characteristic. Models cannot move within Engagement Range of any enemy models during a Normal Move.",
      },
      {
        name: "Advance",
        description:
          "When a unit Advances, each model can move up to its Movement characteristic plus a D6\" roll. An Advancing unit cannot shoot (unless it has Assault weapons), declare a charge, or fight in that turn.",
      },
      {
        name: "Fall Back",
        description:
          "A unit within Engagement Range of an enemy can Fall Back during the Movement phase. Each model moves up to its full Movement characteristic and must end outside Engagement Range of all enemy models. A unit that Falls Back cannot shoot or charge that turn unless it has specific abilities allowing it.",
      },
      {
        name: "Remain Stationary",
        description:
          "A unit that Remains Stationary does not move in the Movement phase. Models with Heavy weapons gain +1 to hit when the unit Remains Stationary.",
      },
    ],
  },
  {
    title: "Keywords & Unit Types",
    entries: [
      {
        name: "Infantry",
        description:
          "A keyword for foot soldiers and similar models. Infantry models can benefit from cover when receiving attacks and can move through terrain features such as ruins and walls.",
      },
      {
        name: "Monster",
        description:
          "Large creature models such as Carnifexes or Daemon Princes. Monsters can shoot ranged weapons while within Engagement Range (like Vehicles) and do not suffer penalties for moving and shooting Heavy weapons.",
      },
      {
        name: "Vehicle",
        description:
          "Tanks, transports, and similar models. Vehicles can shoot ranged weapons while within Engagement Range of enemy units (targeting other units not in engagement). They do not suffer penalties for moving and shooting Heavy weapons.",
      },
      {
        name: "Cavalry",
        description:
          "Mounted models such as Space Marine Outriders or Thunderwolf Cavalry. Cavalry models typically have high Movement characteristics.",
      },
      {
        name: "Fly",
        description:
          "Models with the Fly keyword can move over other models and terrain features as if they were not there during a Normal Move, Advance, or Fall Back. They can also move over enemy models when Falling Back without needing to end outside Engagement Range of models they moved over.",
      },
      {
        name: "Character",
        description:
          "Heroes, leaders, and commanders. Characters can be attached to Bodyguard units using the Leader ability, gaining protection from being targeted directly (opponent must target the Bodyguard unit instead). Lone Characters not leading a unit gain the Lone Operative ability.",
      },
      {
        name: "Battleline",
        description:
          "Core troops of an army. Battleline units can be taken without restrictions and typically form the backbone of an army list. They often have Objective Control of 2 per model.",
      },
      {
        name: "Epic Hero",
        description:
          "A named, unique character that can only be included once per army. Epic Heroes cannot have Enhancements attached to them.",
      },
      {
        name: "Dedicated Transport",
        description:
          "A transport vehicle that does not use a Battleline or other role slot. Dedicated Transports carry Infantry models. You cannot include more Dedicated Transports than there are other non-Transport units in the army.",
      },
      {
        name: "Swarm",
        description:
          "Units of small, numerous creatures. Swarm models typically cannot hold objectives (OC 0) and are often hard to remove due to special abilities.",
      },
      {
        name: "Walker",
        description:
          "A keyword for bipedal or multi-legged vehicles such as Dreadnoughts. Walkers typically share Vehicle rules but may also have access to some Infantry benefits like cover.",
      },
      {
        name: "Titanic",
        description:
          "The largest models in the game such as Knights and super-heavy vehicles. Titanic models are not slowed by terrain and can shoot and charge even if they Fall Back. They also cannot be targeted by certain abilities.",
      },
      {
        name: "Towering",
        description:
          "A rule associated with very tall models (typically Titanic). Towering models are always visible regardless of terrain and cannot benefit from cover. Other models can always draw line of sight to them.",
      },
    ],
  },
  {
    title: "Terrain & Cover",
    entries: [
      {
        name: "Cover (Benefit of Cover)",
        description:
          "When a unit has the Benefit of Cover, each time a ranged attack is made against it, add 1 to the saving throw. This does not apply to invulnerable saves. Units gain cover when they are within or behind terrain features, or through abilities.",
      },
      {
        name: "Light Cover",
        description:
          "Provided by terrain like craters, barricades, or dense foliage. When a unit is behind or within Light Cover terrain, it gains the Benefit of Cover (add 1 to saving throws against ranged attacks).",
      },
      {
        name: "Heavy Cover",
        description:
          "Provided by solid terrain like ruins or fortified walls. Heavy Cover provides the Benefit of Cover and additionally subtracts 1 from the hit roll of ranged attacks targeting units behind it.",
      },
      {
        name: "Ruins",
        description:
          "A common terrain type representing destroyed buildings. Infantry, Beasts, and Swarm models can move through the walls and floors of ruins. Models on or behind ruins gain the Benefit of Cover. Line of sight can be drawn through windows and openings.",
      },
      {
        name: "Area Terrain",
        description:
          "Terrain features like woods or craters that cover an area. Models within Area Terrain gain the Benefit of Cover. Area Terrain may also provide concealment, meaning units entirely within it that are more than a certain distance from the attacker cannot be seen.",
      },
      {
        name: "Obstacle",
        description:
          "Linear terrain features like walls and barricades. Infantry models behind an Obstacle gain the Benefit of Cover from attacks crossing the obstacle. Some obstacles block movement for non-Infantry models.",
      },
      {
        name: "Line of Sight",
        description:
          "A model can see a target if a straight line can be drawn from any part of the attacking model to any part of the target model without being fully blocked by terrain. If any part of the target is visible, it can be targeted. Some terrain features and rules modify line of sight.",
      },
    ],
  },
  {
    title: "Army Building",
    entries: [
      {
        name: "Points",
        description:
          "Every unit has a points cost that reflects its power level. When building an army, the total points of all units must not exceed the agreed points limit for the battle (e.g., 2000 points for a Strike Force game).",
      },
      {
        name: "Detachment",
        description:
          "A Detachment is a set of special rules, Enhancements, and Stratagems that an army can use. Each faction has multiple Detachments to choose from, and you select one when building your army. The Detachment grants a Detachment rule, Enhancements, and Stratagems that define your army's playstyle.",
      },
      {
        name: "Enhancements",
        description:
          "Upgrades available through your chosen Detachment that can be given to Character models. Each Enhancement can only be taken once per army, and each Character can only have one Enhancement. They cost additional points and grant powerful abilities.",
      },
      {
        name: "Warlord",
        description:
          "One Character model in your army must be designated as the Warlord. The Warlord has no inherent special rules in 10th edition, but some missions and abilities reference it.",
      },
      {
        name: "Incursion (1000 pts)",
        description:
          "A smaller battle size played at 1000 points. Incursion games use a smaller battlefield (44\" x 30\") and are ideal for quicker games or learning the rules. Armies have fewer CP to start and a more limited selection of units.",
      },
      {
        name: "Strike Force (2000 pts)",
        description:
          "The standard battle size played at 2000 points. Strike Force games use a full-size battlefield (44\" x 60\") and are the default format for most matched play events and tournaments.",
      },
      {
        name: "Onslaught (3000 pts)",
        description:
          "The largest standard battle size at 3000 points. Onslaught games use a full-size battlefield and allow for massive armies with more units and larger models.",
      },
      {
        name: "Faction Rules",
        description:
          "Every faction has a set of army-wide rules that apply regardless of which Detachment is selected. These represent the fundamental nature of the faction (e.g., Oath of Moment for Space Marines, Waaagh! for Orks).",
      },
      {
        name: "Detachment Rules",
        description:
          "Rules provided by the chosen Detachment that grant specific bonuses and define the army's playstyle. Each Detachment also provides unique Stratagems and Enhancements tied to its theme.",
      },
      {
        name: "Allies",
        description:
          "Some factions can include units from allied factions in their army. Allied units follow specific restrictions — they typically cannot benefit from your army's Detachment rules, faction rules, or Stratagems unless stated otherwise.",
      },
    ],
  },
  {
    title: "General Stratagems",
    entries: [
      {
        name: "Fire Overwatch",
        description:
          "Cost: 1 CP. Used in the opponent's Movement or Charge phase. Select one of your units that is being charged or that an enemy is moving near. That unit can shoot at the charging/moving unit, but hits only on unmodified 6s regardless of BS.",
      },
      {
        name: "Counter-Offensive",
        description:
          "Cost: 2 CP. Used in the Fight phase after an enemy unit has fought. Select one of your units within Engagement Range that has not yet fought this phase. That unit fights next, interrupting the normal fight sequence.",
      },
      {
        name: "Epic Challenge",
        description:
          "Cost: 1 CP. Used in the Fight phase when a Character in your unit is selected to fight. Select one enemy Character within Engagement Range — your Character must direct all of its attacks against that enemy Character, and in return gains +1 to wound against it.",
      },
      {
        name: "Insane Bravery",
        description:
          "Cost: 1 CP. Used in the Command phase when one of your units fails a Battle-shock test. That unit is no longer Battle-shocked.",
      },
      {
        name: "Rapid Ingress",
        description:
          "Cost: 1 CP. Used at the end of your opponent's Movement phase. Select one of your units in Reserves — that unit can arrive on the battlefield as if it were the Reinforcements step of your Movement phase.",
      },
      {
        name: "Go to Ground",
        description:
          "Cost: 1 CP. Used in your opponent's Shooting phase when one of your Infantry units is targeted. Until the end of the phase, that unit gains the Benefit of Cover and has a 6+ invulnerable save (or improves its existing invulnerable save by 1 if it already has one).",
      },
      {
        name: "Smokescreen",
        description:
          "Cost: 1 CP. Used in your opponent's Shooting phase when one of your units with the Smoke keyword is targeted. Until the end of the phase, your opponent must subtract 1 from the hit roll for attacks against that unit.",
      },
      {
        name: "Grenade",
        description:
          "Cost: 1 CP. Used in the Shooting phase. Select one unit with the Grenades keyword, then select one enemy unit within 8\" and visible to it. Roll six D6 — for each 4+, that enemy unit suffers 1 mortal wound.",
      },
      {
        name: "Heroic Intervention",
        description:
          "Cost: 2 CP. Used in your opponent's Charge phase after they have made all their charges. Select one of your units within 6\" of an enemy unit that charged this phase. Your unit can make a 6\" move, but must end closer to the nearest enemy model and within Engagement Range of at least one enemy unit that charged this phase.",
      },
      {
        name: "Tankshock",
        description:
          "Cost: 1 CP. Used in the Charge phase when one of your Vehicle or Monster units ends a charge move. Select one enemy unit within Engagement Range — roll a number of D6 equal to the charging model's Strength characteristic. For each 5+, the target suffers 1 mortal wound.",
      },
    ],
  },
  {
    title: "Mission & Deployment",
    entries: [
      {
        name: "Deployment Zone",
        description:
          "The area of the battlefield where each player sets up their army before the game begins. Each mission specifies deployment zone layouts (e.g., Dawn of War, Hammer and Anvil). Models must be wholly within their deployment zone during setup.",
      },
      {
        name: "Primary Objectives",
        description:
          "Objectives scored by controlling objective markers on the battlefield. At the end of each player's Command phase (starting from the second battle round), they score VP for each objective marker they control. Primary objectives are the main source of Victory Points.",
      },
      {
        name: "Secondary Objectives",
        description:
          "Additional objectives chosen before the battle that provide extra Victory Points. Players can choose between Fixed secondaries (known before the game) or Tactical secondaries (drawn randomly each turn from a deck). Completing secondaries is key to winning matches.",
      },
      {
        name: "Victory Points (VP)",
        description:
          "Points scored during the game by completing Primary Objectives, Secondary Objectives, and Gambits. The player with the most VP at the end of the game wins. The maximum VP is typically capped at 100.",
      },
      {
        name: "Gambits",
        description:
          "High-risk, high-reward objectives that can be declared during the game (from the third battle round onwards). A Gambit replaces your normal primary scoring for a round, offering more VP if achieved but nothing if failed. Examples include 'Take and Hold' (control specific objectives).",
      },
      {
        name: "Fixed Secondaries",
        description:
          "A set of secondary objectives chosen before the game begins and kept for the entire battle. Players who choose Fixed secondaries know exactly what they need to achieve, allowing for deliberate strategy building.",
      },
      {
        name: "Tactical Secondaries",
        description:
          "Secondary objectives drawn randomly from a deck each turn. Tactical secondaries can be discarded once per turn if they are unsuitable. They offer more flexibility but less predictability compared to Fixed secondaries.",
      },
      {
        name: "Reserves",
        description:
          "Units not deployed on the battlefield at the start of the game are placed in Reserves. They arrive during the Reinforcements step of the Movement phase (typically from the second battle round onwards). Units with Deep Strike or similar abilities can arrive from Reserves in specific ways.",
      },
      {
        name: "Reinforcements",
        description:
          "The step during the Movement phase when units in Reserves can be placed on the battlefield. Reinforcement units are typically set up more than 9\" from enemy models. Units not arrived by the end of the third battle round are destroyed.",
      },
      {
        name: "Objective Markers",
        description:
          "Physical markers placed on the battlefield that denote key locations worth Victory Points. A player controls an objective marker if the total OC of their models within 3\" of the centre of the marker exceeds the opponent's total OC. Once controlled, a marker stays controlled until the opponent takes it.",
      },
    ],
  },
];
