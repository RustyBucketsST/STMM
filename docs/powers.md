# Alliance powers

Each Alliance type has its own powers, based on what they do in the series. It works like DragonMine Z:
you have an **energy bar**, each power goes on a key, and you unlock powers with **points** in the
**Powers menu**.

## Keys

| Key | Does |
|---|---|
| **R** | Power 1 (always unlocked) |
| **G** | Power 2 |
| **V** | Power 3 |
| **B** | Power 4 |
| **Y** | Transform into your next form; **Shift+Y** turns back to normal |
| **K** | Opens the Powers menu |

You can change all of them in Options > Controls > Skibidi Toilet.

## Energy and points

- The energy bar sits bottom-left, with a slot for each power that shows its key and its cooldown. Energy
  refills by 4 a second. Max energy is 60, plus 10 for each point of your class's **Energy** stat. Powers
  hit harder and last longer by 10% for each point of **Power**.
- In creative, powers don't use energy.
- **Points** come from defeating toilets (1 point for every 10 health the toilet had: 2 for a normal toilet,
  30 for G-Man Toilet) and from mission rewards (Claim Reward in the story screen). A "+N points" popup shows
  when you earn some. Your points also show in the story screen.
- Points you had from missions before this update move over to powers automatically.

## Cameraman

The Cameramen invent their own tech, so besides their powers they can **build** gear in the Powers menu.

| Power | Key | Unlock | Energy | Cooldown | What it does |
|---|---|---|---|---|---|
| Holoshield | R | free | 30 | 12s | A wall of hard light in front of you for 6 seconds. Blocks hits from the front (lasers, bites, arrows). Sprint into toilets to ram them. |
| Record & Scan | G | 60 | 25 | 20s | Every toilet within 32 blocks glows through walls for about 10 seconds. Tells you how many there are. |

| Tech | Cost | What it does |
|---|---|---|
| Stun Rifle | 80 | Right-click: an electric bolt that stuns what it hits for 3 seconds (it freezes in place). |
| Gravity Plunger | 100 | A plunger (double damage to toilets) that slows what it hits. Right-click pulls a mob up to 14 blocks away over to you. |

## Speakerman

| Power | Key | Unlock | Energy | Cooldown | What it does |
|---|---|---|---|---|---|
| Sonic Blast | R | free | 25 | 4s | A blast of sound in front of you. Hurts toilets and throws them back. |
| Sonic Boots | G | 50 | 15 | 3s | Dash forward on a bass kick, or hold sneak to leap straight up. No fall damage for 3 seconds. |
| Anthem Dance | V | 120 | 40 | 30s | Dance to the anthem for 8 seconds. You and allies (players and Alliance) within 10 blocks get Regeneration, Speed and Strength. |

| Tech | Cost | What it does |
|---|---|---|
| Energy Knife | 60 | A fast blade of energy. |

## TV Man

| Power | Key | Unlock | Energy | Cooldown | What it does |
|---|---|---|---|---|---|
| Hypnosis | R | free | 30 | 10s | Toilets in front of you are hypnotized for about 6 seconds and attack other toilets instead of you. G-Man Toilet and other very tough toilets are immune. |
| Teleport | G | 50 | 20 | 3s | Vanish in static and reappear up to 16 blocks where you're looking. No fall damage when you land. |
| Burning Light | V | 100 | 25 | 6s | Your screen blazes: toilets in front of you are burned and set on fire. |
| Stun Flash | B | 160 | 35 | 15s | A blinding flash that stuns every toilet within 7 blocks. |

## Forms

Like the Alliance's own Large members and Titans, you can grow into bigger forms. Unlock them in order in the
**Forms** tab of the Powers menu, then press **Y** to step up one form at a time (Normal → Large → Titan →
Titan 2.0). Each step costs energy, and a form keeps draining energy (instead of it refilling) until you turn
back with **Shift+Y** or run out, which turns you back to normal. In creative, forms are free and don't drain.

You become as big as the real thing and look like it: Large forms are 1.6 times normal size, Titans are as tall
as the Titan mobs (TV Man about 12 blocks, Cameraman about 10, Speakerman about 8.5). A bigger you has a bigger
hitbox, reaches and hits that much further, steps up to 2.5 blocks, jumps higher and takes less fall damage. You
need room to grow: if you wouldn't fit where you stand, you can't transform.

| Form | Unlock | Energy to transform | Drain | Health | Attack | Armor | Speed | Powers |
|---|---|---|---|---|---|---|---|---|
| Large | 150 | 10 | 0.2/s | +20 | +3 | +4 | +15% | +20% |
| Titan | 400 (needs Large) | 15 | 0.4/s | +80 | +10 | +10 | +60% | +60% |
| Titan 2.0 | 800 (needs Titan) | 20 | 0.6/s | +140 | +16 | +14 | +75% | +100% |

- Large: the Large Cameraman (long black coat, camcorder head), Large Speakerman (white shirt, tall speaker
  head) or Large TV Man (big static-filled TV on a pipe, four little TVs on tendrils).
- Titan: Titan Cameraman, Titan Speakerman or Titan TV Man in their 1.0 forms. Titan 2.0 is a little bigger
  than Titan; for now it uses the 1.0 Titan models.
- Your health keeps the same share when you transform or turn back. Titans and Titan 2.0 shrug off knockback.
- You turn back to normal when you die, log out or go into spectator mode.

## Notes

- Powers only hit toilets and other monsters, never players or the Alliance.
- Tech you lose can be built again for half price.
- Unlocks and points are kept per player, through death and logging out. If you change type, your points
  carry over, and powers you unlocked as another type are still unlocked if you switch back.
- Admin commands: `/skibidi powers points <players> <amount>` gives points, and
  `/skibidi powers reset <players>` clears points, unlocks and built tech.
