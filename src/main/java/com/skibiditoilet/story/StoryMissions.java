package com.skibiditoilet.story;

import com.skibiditoilet.registry.ModItems;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.skibiditoilet.registry.ModEntities.BIG_SKIBIDI_TOILET;
import static com.skibiditoilet.registry.ModEntities.CAMERAMAN;
import static com.skibiditoilet.registry.ModEntities.CAMERA_STRIDER;
import static com.skibiditoilet.registry.ModEntities.DJ_TOILET;
import static com.skibiditoilet.registry.ModEntities.FAST_ARMORED_TOILET;
import static com.skibiditoilet.registry.ModEntities.FLYING_TOILET;
import static com.skibiditoilet.registry.ModEntities.GIANT_FLYING_TOILET;
import static com.skibiditoilet.registry.ModEntities.GMAN_TOILET;
import static com.skibiditoilet.registry.ModEntities.HAT_TOILET;
import static com.skibiditoilet.registry.ModEntities.JETPACK_TOILET;
import static com.skibiditoilet.registry.ModEntities.LARGE_CAMERAMAN;
import static com.skibiditoilet.registry.ModEntities.LARGE_SPEAKERMAN;
import static com.skibiditoilet.registry.ModEntities.MICRO_TOILET;
import static com.skibiditoilet.registry.ModEntities.SAINT_TOILET;
import static com.skibiditoilet.registry.ModEntities.SCIENTIST_TOILET;
import static com.skibiditoilet.registry.ModEntities.SKIBIDI_TOILET;
import static com.skibiditoilet.registry.ModEntities.SKIBIDI_URINAL;
import static com.skibiditoilet.registry.ModEntities.SPEAKERMAN;
import static com.skibiditoilet.registry.ModEntities.SPIDER_TOILET;
import static com.skibiditoilet.registry.ModEntities.TITAN_CAMERAMAN;
import static com.skibiditoilet.registry.ModEntities.TITAN_SPEAKERMAN;
import static com.skibiditoilet.registry.ModEntities.TRIPLE_TOILET;
import static com.skibiditoilet.story.Spawn.boss;
import static com.skibiditoilet.story.Spawn.of;
import static com.skibiditoilet.story.Stage.defeat;
import static com.skibiditoilet.story.Stage.survive;

/**
 * Every mission, one per episode of the series, in story order. Each mission unlocks the next.
 *
 * Characters the mod doesn't have yet are played by the closest existing mob; those spots are marked
 * "stand-in" and get swapped as the new characters are added.
 */
public final class StoryMissions {
    public static final List<Chapter> CHAPTERS = List.of(
            new Chapter("rise_of_the_toilets", List.of(
                    // Episodes 1-7: the player is a scout who got there early, and only has to stay alive.
                    Season.of("the_beginning",
                            Mission.episode(1, "strange_toilet_activity").points(20)
                                    .stage(survive(30, of(SKIBIDI_TOILET, 1)).reinforce(of(SKIBIDI_TOILET, 1), 10, 3)),
                            // The urinals can't move; toilets come to drive you into their spit.
                            Mission.episode(2, "going_up").points(20)
                                    .stage(survive(45, of(SKIBIDI_URINAL, 3)).reinforce(of(SKIBIDI_TOILET, 1), 15, 5)),
                            Mission.episode(3, "the_highway").points(20)
                                    .stage(survive(60, of(BIG_SKIBIDI_TOILET, 1), of(SKIBIDI_TOILET, 4))
                                            .reinforce(of(SKIBIDI_TOILET, 1), 15, 6)),
                            Mission.episode(4, "the_restaurant").points(20)
                                    .stage(survive(60, of(SKIBIDI_TOILET, 3), of(BIG_SKIBIDI_TOILET, 1), of(MICRO_TOILET, 4),
                                            of(FLYING_TOILET, 1)).reinforce(of(MICRO_TOILET, 2), 12, 10))),
                    Season.of("the_spread",
                            Mission.episode(5, "bridge_watch").points(25)
                                    .stage(survive(60, of(GIANT_FLYING_TOILET, 1), of(FLYING_TOILET, 2))
                                            .reinforce(of(FLYING_TOILET, 1), 15, 6)),
                            // Taking out the DJ ends the party early.
                            Mission.episode(6, "the_nightclub").points(25)
                                    .stage(survive(45, boss(DJ_TOILET), of(SKIBIDI_TOILET, 5)).reinforce(of(SKIBIDI_TOILET, 1), 10, 8)),
                            Mission.episode(7, "first_flush").points(30).reward(ModItems.PLUNGER, 1)
                                    .stage(defeat(of(SKIBIDI_TOILET, 3)))
                                    .stage(survive(30, boss(GMAN_TOILET).escapes(), of(BIG_SKIBIDI_TOILET, 1))),
                            Mission.episode(8, "the_war_begins").points(30)
                                    .stage(defeat(of(SKIBIDI_TOILET, 7), of(BIG_SKIBIDI_TOILET, 3))
                                            .allies(of(CAMERAMAN, 6)))),
                    Season.of("the_alliance",
                            Mission.episode(9, "the_plaza").points(30)
                                    .stage(defeat(of(SKIBIDI_TOILET, 2))),
                            Mission.episode(10, "the_cathedral").points(35)
                                    .stage(defeat(boss(SAINT_TOILET), of(SKIBIDI_TOILET, 6))
                                            .allies(of(CAMERAMAN, 4))),
                            Mission.episode(11, "triple_threat").points(35)
                                    .stage(defeat(boss(TRIPLE_TOILET), of(BIG_SKIBIDI_TOILET, 2), of(SKIBIDI_TOILET, 4))
                                            .allies(of(CAMERAMAN, 3), of(LARGE_CAMERAMAN, 2)))),
                    Season.of("skibidi_warfare",
                            Mission.episode(12, "the_bridge").points(40)
                                    .stage(defeat(of(SKIBIDI_TOILET, 10), of(BIG_SKIBIDI_TOILET, 2))
                                            .allies(of(CAMERAMAN, 4), of(LARGE_CAMERAMAN, 1)))
                                    .stage(survive(45, boss(GMAN_TOILET).escapes())),
                            Mission.episode(13, "strider_vs_sky").points(40)
                                    .stage(defeat(boss(GIANT_FLYING_TOILET), of(FLYING_TOILET, 3))
                                            .allies(of(CAMERA_STRIDER, 1), of(CAMERAMAN, 2))),
                            Mission.episode(14, "fast_armored").points(40)
                                    .stage(defeat(of(BIG_SKIBIDI_TOILET, 3), of(SKIBIDI_TOILET, 3))
                                            .allies(of(CAMERAMAN, 3), of(LARGE_CAMERAMAN, 2)))
                                    .stage(defeat(boss(FAST_ARMORED_TOILET)))),
                    Season.of("toilet_spying",
                            Mission.episode(15, "baba_booey").points(45)
                                    .stage(defeat(of(SKIBIDI_TOILET, 12), of(BIG_SKIBIDI_TOILET, 2)))
                                    .stage(defeat(boss(HAT_TOILET))),
                            // The Chief Scientist. Stand-in: the Strider Toilet that bursts out.
                            Mission.episode(16, "the_secret_lab").points(45)
                                    .stage(defeat(boss(SCIENTIST_TOILET).health(60), of(SKIBIDI_TOILET, 4),
                                            of(SPIDER_TOILET, 2)))
                                    .stage(defeat(boss(BIG_SKIBIDI_TOILET).health(80))),
                            Mission.episode(17, "the_meeting").points(45)
                                    .stage(defeat(of(SKIBIDI_TOILET, 8), of(BIG_SKIBIDI_TOILET, 2))
                                            .allies(of(CAMERAMAN, 2)))
                                    .stage(survive(30, boss(GMAN_TOILET).escapes()))),
                    Season.of("titan_cameraman",
                            // Stand-in: Police and Camo Toilets.
                            Mission.episode(18, "titan_rising").points(50)
                                    .stage(defeat(of(SKIBIDI_TOILET, 10), of(BIG_SKIBIDI_TOILET, 2))
                                            .allies(of(TITAN_CAMERAMAN, 1))),
                            Mission.episode(19, "snack_break").points(50)
                                    .stage(defeat(of(TRIPLE_TOILET, 2), of(SKIBIDI_TOILET, 4))
                                            .allies(of(TITAN_CAMERAMAN, 1))),
                            // Stand-in: the Strider Toilets.
                            Mission.episode(20, "laser_eyes").points(55)
                                    .stage(defeat(of(SPIDER_TOILET, 2)).allies(of(TITAN_CAMERAMAN, 1)))
                                    .stage(defeat(boss(GMAN_TOILET).escapes(), of(FAST_ARMORED_TOILET, 1)))),
                    Season.of("toilets_are_winning",
                            Mission.episode(21, "interrogation").points(55)
                                    .stage(defeat(of(SKIBIDI_TOILET, 1).chained()).allies(of(CAMERAMAN, 2)))
                                    .stage(survive(30, boss(GMAN_TOILET).escapes())),
                            // Stand-in: Helicopter Toilets.
                            Mission.episode(22, "the_canal").points(55)
                                    .stage(defeat(of(SKIBIDI_TOILET, 8), of(JETPACK_TOILET, 5), of(BIG_SKIBIDI_TOILET, 2))
                                            .allies(of(CAMERA_STRIDER, 2), of(CAMERAMAN, 4))),
                            Mission.episode(23, "repairs").points(55)
                                    .stage(defeat(of(SKIBIDI_TOILET, 8)).allies(of(TITAN_CAMERAMAN, 1)))),
                    Season.of("speakerman",
                            // Stand-in: the Hydra Strider Toilet.
                            Mission.episode(24, "street_fight").points(60)
                                    .stage(defeat(of(SKIBIDI_TOILET, 12), of(BIG_SKIBIDI_TOILET, 3))
                                            .allies(of(CAMERAMAN, 3), of(LARGE_CAMERAMAN, 2)))
                                    .stage(defeat(boss(BIG_SKIBIDI_TOILET).health(120))),
                            // Stand-in: the Strider Speaker.
                            Mission.episode(25, "the_ambush").points(60)
                                    .stage(defeat(of(SKIBIDI_TOILET, 16), of(BIG_SKIBIDI_TOILET, 2), of(SPIDER_TOILET, 2))
                                            .allies(of(SPEAKERMAN, 5), of(LARGE_SPEAKERMAN, 1))),
                            Mission.episode(26, "titan_speakerman").points(60)
                                    .stage(defeat(of(SKIBIDI_TOILET, 10), of(BIG_SKIBIDI_TOILET, 2), of(HAT_TOILET, 1))
                                            .allies(of(TITAN_SPEAKERMAN, 1), of(SPEAKERMAN, 3), of(CAMERAMAN, 3)))),
                    Season.of("dawn_of_disaster",
                            Mission.episode(27, "the_hideout").points(65)
                                    .stage(defeat(of(SKIBIDI_TOILET, 16), of(SPIDER_TOILET, 4))
                                            .allies(of(LARGE_SPEAKERMAN, 1), of(CAMERAMAN, 3), of(LARGE_CAMERAMAN, 1), of(SPEAKERMAN, 2))),
                            // Stand-in: the Mafia Skibidi Toilet, who fights with a jetpack.
                            Mission.episode(28, "the_mafia").points(65)
                                    .stage(defeat(of(BIG_SKIBIDI_TOILET, 3)).allies(of(TITAN_SPEAKERMAN, 1)))
                                    .stage(defeat(boss(JETPACK_TOILET).health(100))),
                            // Stand-in: the Glitch Skibidi Toilet.
                            Mission.episode(29, "glitch").points(70)
                                    .stage(defeat(boss(SPIDER_TOILET).health(80))
                                            .allies(of(CAMERAMAN, 4), of(SPEAKERMAN, 2)))),
                    // Stand-ins until the Parasite and the infected Alliance are added.
                    Season.of("the_infection",
                            Mission.episode(30, "captured").points(70)
                                    .stage(survive(60, of(SKIBIDI_TOILET, 4), of(BIG_SKIBIDI_TOILET, 1))
                                            .reinforce(of(SKIBIDI_TOILET, 1), 10, 6)),
                            Mission.episode(31, "infected").points(70)
                                    .stage(defeat(of(BIG_SKIBIDI_TOILET, 2), of(SKIBIDI_TOILET, 6))
                                            .allies(of(CAMERAMAN, 3))),
                            // Stand-in: the Giant Camo Toilet.
                            Mission.episode(32, "giant_camo").points(75)
                                    .stage(defeat(boss(BIG_SKIBIDI_TOILET).health(150), of(SKIBIDI_TOILET, 4))
                                            .allies(of(TITAN_SPEAKERMAN, 1)))),
                    Season.of("alliance_technology",
                            // Stand-in: the Prototype Mutant and the infected Titan Speakerman.
                            Mission.episode(33, "city_on_fire").points(75)
                                    .stage(survive(90, boss(GMAN_TOILET).escapes(), of(BIG_SKIBIDI_TOILET, 2),
                                            of(JETPACK_TOILET, 3)).reinforce(of(SKIBIDI_TOILET, 2), 15, 10)),
                            Mission.episode(34, "the_hangar").points(75)
                                    .stage(defeat(of(BIG_SKIBIDI_TOILET, 2), of(SKIBIDI_TOILET, 4))
                                            .allies(of(LARGE_CAMERAMAN, 3), of(LARGE_SPEAKERMAN, 1))),
                            // Gives the Blaster until the Parasite Disabler Gun is added.
                            Mission.episode(35, "disabler_gun").points(80).reward(ModItems.ALLIANCE_BLASTER, 1)
                                    .stage(defeat(of(SKIBIDI_TOILET, 10), of(BIG_SKIBIDI_TOILET, 2))
                                            .allies(of(CAMERAMAN, 3)))
                                    .stage(survive(30, boss(GMAN_TOILET).escapes()))),
                    Season.of("operation_disinfection",
                            Mission.episode(36, "cure_the_army").points(80)
                                    .stage(defeat(of(SKIBIDI_TOILET, 12), of(BIG_SKIBIDI_TOILET, 3))
                                            .allies(of(CAMERAMAN, 4))),
                            // Stand-in: the Prototype Mutant.
                            Mission.episode(37, "pdlt").points(85)
                                    .stage(defeat(boss(BIG_SKIBIDI_TOILET).health(150), of(SKIBIDI_TOILET, 6))
                                            .allies(of(CAMERAMAN, 4))),
                            // Stand-in: G-Toilet 2.0 is G-Toilet at full strength, with no escape.
                            Mission.episode(38, "g_toilet_2").points(100)
                                    .stage(defeat(of(SKIBIDI_TOILET, 4), of(BIG_SKIBIDI_TOILET, 3))
                                            .allies(of(LARGE_CAMERAMAN, 2), of(LARGE_SPEAKERMAN, 2)))
                                    .stage(defeat(boss(GMAN_TOILET)))))));

    private static final List<Mission> ORDER = new ArrayList<>();
    private static final Map<String, Mission> BY_ID = new HashMap<>();
    private static final Map<Mission, Season> SEASONS = new HashMap<>();

    static {
        for (Chapter chapter : CHAPTERS) {
            for (Season season : chapter.seasons()) {
                for (Mission mission : season.missions()) {
                    ORDER.add(mission);
                    BY_ID.put(mission.id(), mission);
                    SEASONS.put(mission, season);
                }
            }
        }
    }

    private StoryMissions() {
    }

    @Nullable
    public static Mission get(String id) {
        return BY_ID.get(id);
    }

    public static List<Mission> all() {
        return ORDER;
    }

    public static Season seasonOf(Mission mission) {
        return SEASONS.get(mission);
    }

    /** The mission that has to be finished first, or null for the very first one. */
    @Nullable
    public static Mission previous(Mission mission) {
        int index = ORDER.indexOf(mission);
        return index > 0 ? ORDER.get(index - 1) : null;
    }

    public static boolean isUnlocked(StoryData data, Mission mission) {
        Mission previous = previous(mission);
        return previous == null || data.isCompleted(previous.id());
    }
}
