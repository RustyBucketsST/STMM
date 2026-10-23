# Using your own sounds

Every sound in the mod is made by `tools/gen_sounds.py` (a small synthesizer, so no recordings and nothing
copied from the show). You can swap any of them for your own with a resource pack, without touching the mod.

## Making the resource pack

1. Make a folder, for example `My Skibidi Sounds`, with this inside:

   ```
   My Skibidi Sounds/
     pack.mcmeta
     assets/skibidi/sounds/toilet/sing1.ogg     <- your files, named like the ones below
   ```

2. Put this in `pack.mcmeta` (15 is the pack format for Minecraft 1.20.1):

   ```json
   {"pack": {"pack_format": 15, "description": "My Skibidi Toilet sounds"}}
   ```

3. Your sounds must be **.ogg** files. Use **mono** (one channel) for everything except the story and menu
   sounds, or they won't get quieter with distance. Audacity can export mono .ogg files.
4. Put the folder in `.minecraft/resourcepacks`, then turn it on in Options > Resource Packs.

Any file you don't replace keeps the mod's own sound. To use more or fewer variations of a sound (say, six
songs for the toilets instead of four), copy `assets/skibidi/sounds.json` from the mod jar into your pack
and edit its list for that sound.

## The sound files

| File (in `assets/skibidi/sounds/`) | Used for |
|---|---|
| `toilet/sing1.ogg` | `entity.skibidi_toilet.ambient` |
| `toilet/sing2.ogg` | `entity.skibidi_toilet.ambient` |
| `toilet/sing3.ogg` | `entity.skibidi_toilet.ambient` |
| `toilet/sing4.ogg` | `entity.skibidi_toilet.ambient` |
| `toilet/hurt1.ogg` | `entity.skibidi_toilet.hurt` |
| `toilet/hurt2.ogg` | `entity.skibidi_toilet.hurt` |
| `toilet/death1.ogg` | `entity.skibidi_toilet.death` |
| `toilet/death2.ogg` | `entity.skibidi_toilet.death` |
| `toilet/flush1.ogg` | `entity.skibidi_toilet.flush` |
| `toilet/flush2.ogg` | `entity.skibidi_toilet.flush` |
| `toilet/bite.ogg` | `entity.skibidi_toilet.bite` |
| `weapon/laser1.ogg` | `entity.jetpack_toilet.shoot` |
| `weapon/laser2.ogg` | `entity.jetpack_toilet.shoot` |
| `weapon/blaster.ogg` | `item.alliance_blaster.shoot` |
| `weapon/strider_bolt.ogg` | `entity.camera_strider.shoot` |
| `weapon/giant_laser.ogg` | `entity.giant_flying_toilet.laser` |
| `move/swoop.ogg` | `entity.giant_flying_toilet.swoop` |
| `weapon/spit.ogg` | `entity.skibidi_urinal.spit` |
| `move/charge_rev.ogg` | `entity.fast_armored_toilet.rev` |
| `move/crash.ogg` | `entity.fast_armored_toilet.crash` |
| `move/slam.ogg` | `entity.hat_toilet.slam` |
| `move/titan_step.ogg` | `entity.titan.step` |
| `move/choir.ogg` | `entity.saint_toilet.hymn` |
| `move/demonic_roar.ogg` | `entity.saint_toilet.roar` |
| `dj/kick.ogg` | `entity.dj_toilet.kick` |
| `dj/snare.ogg` | `entity.dj_toilet.snare` |
| `dj/hat.ogg` | `entity.dj_toilet.hat` |
| `dj/clap.ogg` | `entity.dj_toilet.clap` |
| `alliance/camera_flash.ogg` | `entity.cameraman.flash` |
| `alliance/speaker_blast.ogg` | `entity.speakerman.blast` |
| `alliance/tv_static.ogg` | `entity.tv_man.static` |
| `story/mission_start.ogg` | `story.mission_start` |
| `story/objective.ogg` | `story.objective` |
| `story/mission_complete.ogg` | `story.mission_complete` |
| `story/mission_failed.ogg` | `story.mission_failed` |
| `story/reward.ogg` | `story.reward` |
| `ui/click.ogg` | `ui.click` |
| `power/holoshield.ogg` | `power.holoshield` |
| `power/shield_hit.ogg` | `power.shield_hit` |
| `power/record_scan.ogg` | `power.record_scan` |
| `power/sonic_boots.ogg` | `power.sonic_boots` |
| `power/anthem.ogg` | `power.anthem_dance` (the dance music: 8 seconds, as long as the dance) |
| `power/hypnosis.ogg` | `power.hypnosis` |
| `power/teleport.ogg` | `power.teleport` |
| `power/burning_light.ogg` | `power.burning_light` |
| `power/stun_flash.ogg` | `power.stun_flash` |
| `power/no_energy.ogg` | `power.no_energy` |
| `weapon/stun_rifle.ogg` | `item.stun_rifle.shoot` |
| `weapon/gravity_pull.ogg` | `item.gravity_plunger.pull` |
| `weapon/energy_knife.ogg` | `item.energy_knife.swing` |
