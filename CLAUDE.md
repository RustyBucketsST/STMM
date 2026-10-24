# Skibidi Toilet mod (Minecraft Fabric 1.20.1)

A Fabric mod adding Skibidi Toilet content: toilets, the Alliance (Cameramen, Speakermen, TV Men, Titans),
a story mode, player characters with Alliance powers. Modeled on how DragonMine Z (DMZ) works.

## Rules from the owner (always follow)

- **"DONT add anything unless you ask me what to add."** Ask before adding any content, character, item,
  mechanic or feature. When the owner gives latitude for a specific area (e.g. "you can add in other items
  for them"), stay inside that area.
- **No copied assets or code.** DragonMine Z is GPL-3.0: don't copy its code, art or sounds. We build our own
  versions and may base the *approach* on its techniques. Don't use the real Skibidi song or clips/audio
  from the show. All sounds are synthesized by `tools/gen_sounds.py`; all textures are drawn by
  `tools/gen_texture.py`.

## Decisions already made

- Friendly fire is on. Toilets spawn naturally in the Overworld only; Alliance mobs spawn naturally.
- Titan heights follow the series: TV Man tallest (~12 blocks, `TitanEntity.TV_MAN_SCALE` 6), Cameraman ~10
  (`CAMERAMAN_SCALE` 5), Speakerman shortest (~8.5, `SPEAKERMAN_SCALE` 4.25). G-Man Toilet is as tall as Titan
  Cameraman (`GManToiletEntity.SCALE` 7.5, so about 6 blocks wide). All step up 2.5 blocks. The wiki gives no
  real heights.
- Titans and G-Man Toilet come only from the story or `/summon` (no spawn eggs). Story-only characters do
  have spawn eggs (for testing).
- Character creator: 3 types (Cameraman, Speakerman, TV Man), 4 classes (Fighter, Shooter, Tank, Support),
  5 stats (Strength, Power, Defense, Vitality, Energy). Stat upgrades/training NPCs are **not built yet**.
- Story mode: Chapter 1, 38 missions in seasons, opened with **J** or the Alliance Commander NPC. Parties
  (`/party ...`) up to 4, enemies +50% health per extra player.
- Powers ("Like DMZ"): energy bar + powers on keys R/G/V/B, Powers menu on K, unlocked with points. Points
  come from killing toilets (maxHealth/10) and mission rewards. Cameramen and Speakermen also *build tech*
  items with points. Details: `docs/powers.md`.
- Player forms (owner's choices): Normal → Large → Titan → Titan 2.0, bought in order in the Powers menu's Forms
  tab, Y steps up (energy cost, then drains; Shift+Y back), **real size** (Large 1.6x, Titans the Titan mob scales,
  Titan 2.0 x1.1) with the matching mob model. Titan 2.0 still uses the 1.0 models (owner chose: build the 2.0
  models from the wiki next). In a Titan form the keys carry **Titan powers** (`Power.onKeys`, shared code in
  `entity/ability/TitanMoves` with the Titan mobs); the type's own powers fill the remaining keys.
  Planned next (owner chose): buyable **gear that shows on the player's model** in every form and gives a bonus:
  jetpack, shoulder gear (CCTV cameras / speakers / blade claws by type), armor plates.

## Open questions / backlog (ask the owner before doing any of these)

- **Blockbench**: every mob except the Spider Toilet's legs was remodelled from the wiki's pictures to look like
  the series (1.0 forms) and lives in `blockbench/` (see "Models" below). Still hand-written Java: spider legs,
  player heads (`PlayerHeadModels`, which no longer match the new mob heads), suits. Possible next: those, 3D
  tech items. Ask first.
- Never answered: Alliance vs toilet balance.
- Ideas mentioned but not started: Titan branch of the story; Chapter 1 "batch 3" characters (Strider /
  Hydra Strider, Camera Toilet, Police/Camo, Scientist Cameraman, Helicopter, Strider Speaker, Mafia, Glitch,
  infection); G-Toilet 2.0 form; stat upgrades and training NPCs (also a points source).

## Build

- Toolchain: Fabric Loom, Minecraft 1.20.1, Yarn build.10, Fabric API 0.92.2, Gradle 8.14.3, Java 17 target.
- `./gradlew build --no-parallel --max-workers=1 -q` → `build/libs/skibidi-toilet-0.1.0.jar`.
  (Add `--offline` once dependencies are cached.)

## Code layout (`src/main/java/com/skibiditoilet`)

- `registry/` – entities, items, sounds (`ModSounds`, mirrors `assets/skibidi/sounds.json`), particles,
  effects (STUNNED, HYPNOTIZED), damage types (data in `data/skibidi/damage_type`), attachments
  (CHARACTER, STORY, POWERS: persistent, copied on death).
- `entity/` – `SkibidiToiletEntity` is the base of every toilet (plunger double damage, flush death, stun via
  `isAiDisabled`, hypnosis targeting, kill points). `AllianceEntity` is the base of the Alliance mobs.
  `LaserBeam` is the instant-hit shot used by lasers, the Urinal's spray and tech items (`fireFrom` shoots from
  any point, e.g. a Titan's core).
- `entity/ability/` – mob abilities from the series: `MobAbility` (cooldown, condition, optional windup),
  `MobAbilities` (one at a time, ticked from mobTick), `AllianceAbilities` (every Alliance mob's, by entity type),
  `TitanMoves` (the Titans' moves, used by Titan mobs and players in Titan form), `Targets` (area targeting
  shared with the player powers). G-Toilet's eye lasers live in `GManToiletEntity`, the
  Titan jetpack flames and riot shield in `TitanEntity`. Listed for players in `docs/story-chapter1.md`.
- `character/` – creator data and sync. `story/` – missions (`StoryMissions` defines all 38), runner,
  parties. `power/` – `Power` and `Tech` enums, `PowerData` (points/unlocks), `PowerManager` (energy,
  cooldowns, runtime, forms: transform/drain/stats), `PowerEffects` (what each power does), `PowerSync`
  (packets), `Form` (the forms and their numbers), `FormHolder` (form + scale kept on every player by
  `mixin/PlayerEntityMixin`, which scales hitbox/eyes/fall).
- Common mixins in `mixin/` (player size, jump, server reach checks); MixinExtras is available.
- `fx/ModFx` – server-side effect requests (one small S2C packet each); drawn by `client/fx/ClientFx`.
- `client/` – renderers/models, `character/` creator screens and player appearance, `story/` story
  screen/HUD, `power/` HUD, Powers menu, Holoshield rendering, `PlayerFormRenderer`/`PlayerFormModel` (a player
  in a form drawn as the mob). Client mixins in `mixin/client/` (camera shake and form camera distance, hurt
  overlay, player model parts, form rendering, Anthem Dance pose, form reach).
- Docs for players: `docs/story-chapter1.md`, `docs/powers.md`, `docs/custom-sounds.md` (resource-pack guide;
  keep its table in sync when adding sounds).

## Models (Blockbench)

- `blockbench/*.bbmodel` (Modded Entity format, box UV) are the source of the classes in `client/bbmodel/`.
  After editing one in Blockbench run `python3 tools/bbmodel_to_java.py`; never edit the generated Java.
- `blockbench/models.json` maps each file to its class. Groups listed under "separate" (variant extras:
  headphones, halo, hat, jetpack; and the pedestal, which the spider toilet leaves out) get their own
  `add...` method instead of being added with their parent; they are hidden in Blockbench by default.
  Groups/cubes with Export switched off are reference only (the suit in the Alliance files).
- Java relies on the part name "head" (toilet/urinal animation and accessories attach to it; Alliance heads
  replace the biped head). The toilet model is a little wider/taller than its 0.8x1.3 hitbox.
- Textures still come from `gen_texture.py`. For the toilet, urinal and regular Alliance heads a UV moved in
  Blockbench must be moved in the drawing code too; every other model's drawing code reads each cube's UV from
  its .bbmodel by cube name (`bb_boxes`).
  `python3 tools/bbmodel_to_java.py --embed-textures` refreshes the texture copies inside the .bbmodel files.
- `large_tv_man.bbmodel` (Large TV Man 1.0, from the wiki picture) is only used by players in the Large form;
  there is no Large TV Man mob.
  Toilet textures are 128x128: head and variant extras in the top half, toilet body in the bottom half.
- **Titans** are their **1.0 forms** (owner's request), built from the wiki's 1.0 pictures and descriptions:
  `titan_cameraman`, `titan_speakerman` (owner's favourite, check his picture before changing him),
  `titan_tv_man`. Each `titan_*.bbmodel` is built at twice the detail (2 units per pixel) with biped part
  names and doubled pivots, feet on the ground: head/body (0,-24,0), arms (+-10,-20,0), legs (+-3.8,0,0.2).
  `TitanModel` converts the biped animation to those units and `AllianceRenderer.titanFactory` draws it at
  half the Titan scale, plus `textures/entity/titan_*_glow.png` full-bright (cores, screens, red drivers).
- **G-Toilet** (`gman_toilet.bbmodel`, from the wiki picture of his 1.0 form): a tall seatless bowl, a tall
  cistern with a ledge and flush knob, and G-Man's head with its own brow and nose cubes. Also built at
  twice the detail; `GManToiletModel` doubles the head's bob/flush offsets and `SkibidiToiletRenderer.detailed`
  draws it at half the G-Man scale. The Giant Flying Toilet uses the same tall toilet with its own face
  (`infra_toilet` in gen_texture.py). The wiki API (`skibidi-toilet.fandom.com/api.php?action=parse&page=...`)
  works with curl when the normal pages are blocked.

## Generated assets

- `python3 tools/gen_texture.py src/main/resources/assets/skibidi/textures` redraws every texture. Each
  image reseeds its RNG, so adding new ones must leave existing files byte-identical; check with md5sums
  before/after.
- `python3 tools/gen_sounds.py [outdir] [names...]` makes mono .ogg sounds (numpy + ffmpeg/libvorbis).
  Pass names to regenerate only new sounds. Register new events in `ModSounds`, `sounds.json`
  (with a subtitle), and lang.
- Lang is `src/main/resources/assets/skibidi/lang/en_us.json`.

## Testing in-game (cloud container)

- Server: `./gradlew runServer` (dir `run/`, gitignored). Needs `run/eula.txt` (`eula=true`) and in
  `run/server.properties`: `online-mode=false`, `enable-rcon=true`, `rcon.port=25575`,
  `rcon.password=skibiditest`. `tools/dev/rcon.py "cmd" "sleep 1" "cmd"` sends commands.
- Client: start `Xvfb :99 -screen 0 1280x720x24`, then
  `DISPLAY=:99 LIBGL_ALWAYS_SOFTWARE=1 ./gradlew runClient --args="--quickPlayMultiplayer localhost:25565 --username Player653"`.
  `tools/dev/xctl.py` (python-xlib + Pillow) takes screenshots and sends keys/clicks; window is 854x480 at
  (213,120), GUI scale 2. The container has no audio device, so sounds can't be heard in-game.
- Test platform used so far: smooth stone at y=150, x/z 470–530. `doImmediateRespawn` is on.
- Ops commands: `/skibidi character reset <p>`, `/skibidi story reset|complete <p> [mission]`,
  `/skibidi powers points <p> <n>`, `/skibidi powers reset <p>`.

## Gotchas learned the hard way

- A record component can't share its name with a builder method (accessor clash).
- `setNoGravity` is reset from NBT on `/summon`; flyers override `hasNoGravity()` instead.
- `tryRemoveModifier` only removes *persistent* attribute modifiers; use `removeModifier(UUID)` for others.
- NoAI / AI-disabled mobs ignore velocity, so knockback or pulls on a stunned mob happen after the stun.
- RCON `/tp` is relative to world spawn: use `execute as <p> at @s run tp ...`. `tp ... facing` aims from
  the feet, not the eyes; give an explicit pitch when aiming at small mobs.
- When killing the dev client by process name, never put "KnotClient" in the same shell command line that
  runs the kill (it matches itself).
- If Xvfb won't start: remove `/tmp/.X99-lock` and `/tmp/.X11-unix/X99`.
- Fabric's `HudRenderCallback` draws after chat; `WorldRenderEvents.AFTER_TRANSLUCENT` +
  `RenderLayer.getLightning()` is used for additive beams and the Holoshield.
