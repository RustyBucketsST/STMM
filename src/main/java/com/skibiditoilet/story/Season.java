package com.skibiditoilet.story;

import java.util.Arrays;
import java.util.List;

/** A season of the series: a group of missions, shown together in the story screen. */
public record Season(String id, List<Mission> missions) {
    public static Season of(String id, Mission.Builder... missions) {
        return new Season(id, Arrays.stream(missions).map(Mission.Builder::build).toList());
    }

    public String translationKey() {
        return "story.skibidi.season." + this.id;
    }
}
