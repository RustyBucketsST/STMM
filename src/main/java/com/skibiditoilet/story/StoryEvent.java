package com.skibiditoilet.story;

/** Something that happened to a mission, shown to its players as a toast. */
public enum StoryEvent {
    STARTED("started"),
    STAGE_COMPLETE("stage_complete"),
    COMPLETED("completed"),
    /** Finished with a party, but this player had already done it or hadn't reached it yet. */
    HELPED("helped"),
    FAILED_DIED("failed_died"),
    FAILED_LEFT_AREA("failed_left_area"),
    FAILED_PEACEFUL("failed_peaceful"),
    ABANDONED("abandoned");

    private final String id;

    StoryEvent(String id) {
        this.id = id;
    }

    public String translationKey() {
        return "story.skibidi.event." + this.id;
    }

    public boolean isGood() {
        return this == STARTED || this == STAGE_COMPLETE || this == COMPLETED || this == HELPED;
    }
}
