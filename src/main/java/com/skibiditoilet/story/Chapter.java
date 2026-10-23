package com.skibiditoilet.story;

import java.util.List;

/** A chapter of the series, one tab in the story screen. */
public record Chapter(String id, List<Season> seasons) {
    public String translationKey() {
        return "story.skibidi.chapter." + this.id;
    }
}
