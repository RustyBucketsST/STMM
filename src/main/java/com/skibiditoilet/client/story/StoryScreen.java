package com.skibiditoilet.client.story;

import com.skibiditoilet.client.character.AllianceButton;
import com.skibiditoilet.client.character.AllianceUi;
import com.skibiditoilet.client.character.CharacterPreview;
import com.skibiditoilet.client.power.ClientPowerState;
import com.skibiditoilet.story.Chapter;
import com.skibiditoilet.story.Mission;
import com.skibiditoilet.story.MissionStatus;
import com.skibiditoilet.story.Season;
import com.skibiditoilet.story.StoryData;
import com.skibiditoilet.story.StoryMissions;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The story: a chapter's missions as a tree of nodes, one per episode, grouped by season. Drag to move
 * around and scroll to zoom; pick a mission to see what happens in it, start it, or claim its reward.
 */
public class StoryScreen extends Screen {
    private static final int NODE = 20;
    private static final int STEP_X = 38;
    private static final int ZIGZAG = 13;
    private static final int SEASON_GAP = 24;
    private static final int BAND_TOP = -ZIGZAG - NODE / 2 - 16;
    private static final int BAND_BOTTOM = ZIGZAG + NODE / 2 + 6;
    private static final float MIN_ZOOM = 0.6f;
    private static final float MAX_ZOOM = 1.6f;
    private static final int NAV_ROW = 14;

    private static final int LOCKED_FILL = 0xFF16202B;
    private static final int AVAILABLE_FILL = 0xFF16405F;
    private static final int ACTIVE_FILL = 0xFF7A5C0E;
    private static final int ACTIVE_BORDER = 0xFFFFD34D;
    private static final int DONE_FILL = 0xFF1C5535;
    private static final int DONE_BORDER = 0xFF4CD787;
    private static final int BAND = 0x6012283C;

    private enum Status {
        LOCKED, AVAILABLE, ACTIVE, CLAIMABLE, COMPLETED
    }

    private record Node(Mission mission, int x, int y) {
    }

    private record Band(Season season, int left, int right) {
    }

    private final Chapter chapter = StoryMissions.CHAPTERS.get(0);
    private final Map<Mission, Node> nodes = new LinkedHashMap<>();
    private final List<Band> bands = new ArrayList<>();
    private final Map<EntityType<?>, LivingEntity> previews = new HashMap<>();

    private int navX, navY, navW, navH;
    private int canvasX, canvasY, canvasW, canvasH;
    private int detailX, detailY, detailW, detailH;
    private float camX, camY;
    private float zoom = 1.0f;
    private boolean panning;
    private double panDistance;
    private float previewYaw;

    private Mission selected;
    private AllianceButton actionButton;

    public StoryScreen() {
        super(Text.translatable("screen.skibidi.story"));
        int x = 0;
        for (Season season : this.chapter.seasons()) {
            int left = x;
            for (int i = 0; i < season.missions().size(); i++) {
                Mission mission = season.missions().get(i);
                this.nodes.put(mission, new Node(mission, x, i % 2 == 0 ? -ZIGZAG : ZIGZAG));
                x += STEP_X;
            }
            this.bands.add(new Band(season, left - NODE / 2 - 8, x - STEP_X + NODE / 2 + 8));
            x += SEASON_GAP;
        }
        this.selected = this.startingMission();
        Node node = this.nodes.get(this.selected);
        this.camX = node.x;
        this.camY = (BAND_TOP + BAND_BOTTOM) / 2.0f;
    }

    /** The mission running now, else one with a reward waiting, else the next one to play. */
    private Mission startingMission() {
        MissionStatus status = ClientStoryState.status();
        Mission running = status != null ? StoryMissions.get(status.missionId()) : null;
        if (running != null && this.nodes.containsKey(running)) {
            return running;
        }
        StoryData data = ClientStoryState.data();
        Mission next = null;
        for (Mission mission : this.nodes.keySet()) {
            if (data.isClaimable(mission.id())) {
                return mission;
            }
            if (next == null && !data.isCompleted(mission.id()) && StoryMissions.isUnlocked(data, mission)) {
                next = mission;
            }
        }
        return next != null ? next : this.nodes.keySet().iterator().next();
    }

    @Override
    protected void init() {
        int top = 24;
        int bottom = this.height - 6;
        this.navX = 6;
        this.navY = top;
        this.navW = MathHelper.clamp(this.width / 4, 92, 130);
        this.navH = bottom - top;
        int rightX = this.navX + this.navW + 4;
        int rightW = this.width - rightX - 6;
        this.canvasX = rightX;
        this.canvasY = top;
        this.canvasW = rightW;
        this.canvasH = Math.max(84, (int) ((bottom - top) * 0.44f));
        this.detailX = rightX;
        this.detailY = this.canvasY + this.canvasH + 4;
        this.detailW = rightW;
        this.detailH = bottom - this.detailY;

        // The chapter tabs. Later chapters get their own tab here.
        this.addDrawableChild(new AllianceButton(this.navX, 5, Math.min(200, this.textRenderer.getWidth(
                Text.translatable(this.chapter.translationKey())) + 16), 15,
                Text.translatable(this.chapter.translationKey()), () -> true, () -> {
        }));

        int columnW = this.rewardColumnWidth();
        this.actionButton = this.addDrawableChild(new AllianceButton(this.detailX + this.detailW - columnW - 4,
                this.detailY + this.detailH - 22, columnW, 18, Text.empty(), this::onAction));
        this.updateActionButton();
    }

    private int rewardColumnWidth() {
        return MathHelper.clamp(this.detailW / 3, 90, 130);
    }

    private Status status(Mission mission) {
        MissionStatus running = ClientStoryState.status();
        StoryData data = ClientStoryState.data();
        if (running != null && running.missionId().equals(mission.id())) {
            return Status.ACTIVE;
        }
        if (data.isClaimable(mission.id())) {
            return Status.CLAIMABLE;
        }
        if (data.isCompleted(mission.id())) {
            return Status.COMPLETED;
        }
        return StoryMissions.isUnlocked(data, mission) ? Status.AVAILABLE : Status.LOCKED;
    }

    private void updateActionButton() {
        Status status = this.status(this.selected);
        boolean busy = ClientStoryState.status() != null;
        this.actionButton.setMessage(Text.translatable(switch (status) {
            case LOCKED -> "screen.skibidi.story.locked";
            case AVAILABLE -> "screen.skibidi.story.start";
            case ACTIVE -> "screen.skibidi.story.abandon";
            case CLAIMABLE -> "screen.skibidi.story.claim";
            case COMPLETED -> "screen.skibidi.story.completed";
        }));
        this.actionButton.active = status == Status.ACTIVE || status == Status.CLAIMABLE
                || status == Status.AVAILABLE && !busy;
    }

    private void onAction() {
        switch (this.status(this.selected)) {
            case AVAILABLE -> {
                ClientStoryState.start(this.selected);
                this.close();
            }
            case ACTIVE -> {
                ClientStoryState.abandon();
                this.close();
            }
            case CLAIMABLE -> ClientStoryState.claim(this.selected);
            default -> {
            }
        }
    }

    @Override
    public void tick() {
        this.updateActionButton();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        this.previewYaw += this.client.getLastFrameDuration() * 2.0f;
        Text points = Text.translatable("screen.skibidi.story.points", ClientPowerState.data().points());
        context.drawTextWithShadow(this.textRenderer, points, this.width - 6 - this.textRenderer.getWidth(points), 9,
                AllianceUi.HIGHLIGHT);

        this.renderNavigator(context, mouseX, mouseY);
        Node hovered = this.renderCanvas(context, mouseX, mouseY);
        ItemStack hoveredItem = this.renderDetails(context, mouseX, mouseY);
        super.render(context, mouseX, mouseY, delta);

        if (hovered != null) {
            context.drawTooltip(this.textRenderer, List.of(MissionText.title(hovered.mission),
                    Text.translatable("screen.skibidi.story.status." + this.status(hovered.mission).name().toLowerCase(Locale.ROOT))
                            .styled(style -> style.withColor(AllianceUi.TEXT_DIM))), mouseX, mouseY);
        } else if (hoveredItem != null) {
            context.drawItemTooltip(this.textRenderer, hoveredItem, mouseX, mouseY);
        }
    }

    // ------------------------------------------------------------------ navigator

    private void renderNavigator(DrawContext context, int mouseX, int mouseY) {
        AllianceUi.panel(context, this.textRenderer, Text.translatable("screen.skibidi.story.seasons"),
                this.navX, this.navY, this.navW, this.navH);
        StoryData data = ClientStoryState.data();
        Season current = StoryMissions.seasonOf(this.selected);
        List<Season> seasons = this.chapter.seasons();
        for (int i = 0; i < seasons.size(); i++) {
            Season season = seasons.get(i);
            int y = this.navY + 15 + i * NAV_ROW;
            if (y + NAV_ROW > this.navY + this.navH - 2) {
                break;
            }
            boolean hover = mouseX >= this.navX + 2 && mouseX < this.navX + this.navW - 2 && mouseY >= y && mouseY < y + NAV_ROW;
            if (season == current) {
                context.fill(this.navX + 2, y, this.navX + this.navW - 2, y + NAV_ROW - 1, AllianceUi.SELECTED);
            } else if (hover) {
                context.fill(this.navX + 2, y, this.navX + this.navW - 2, y + NAV_ROW - 1, AllianceUi.BUTTON_HOVER);
            }
            long done = season.missions().stream().filter(mission -> data.isCompleted(mission.id())).count();
            String count = done + "/" + season.missions().size();
            int countW = this.textRenderer.getWidth(count);
            boolean finished = done == season.missions().size();
            context.drawTextWithShadow(this.textRenderer, count, this.navX + this.navW - 5 - countW, y + 3,
                    finished ? DONE_BORDER & 0xFFFFFF : AllianceUi.TEXT_DIM);
            Text name = Text.translatable(season.translationKey());
            context.drawTextWithShadow(this.textRenderer, this.trim(name, this.navW - 14 - countW),
                    this.navX + 5, y + 3, season == current ? 0xFFFFFF : AllianceUi.TEXT);
        }
    }

    @Nullable
    private Season seasonAt(double mouseX, double mouseY) {
        if (mouseX < this.navX + 2 || mouseX >= this.navX + this.navW - 2) {
            return null;
        }
        int row = (int) Math.floor((mouseY - this.navY - 15) / NAV_ROW);
        List<Season> seasons = this.chapter.seasons();
        return row >= 0 && row < seasons.size() && this.navY + 15 + (row + 1) * NAV_ROW <= this.navY + this.navH - 2
                ? seasons.get(row) : null;
    }

    // ------------------------------------------------------------------ mission tree

    @Nullable
    private Node renderCanvas(DrawContext context, int mouseX, int mouseY) {
        AllianceUi.panel(context, this.canvasX, this.canvasY, this.canvasW, this.canvasH);
        context.enableScissor(this.canvasX + 1, this.canvasY + 1, this.canvasX + this.canvasW - 1, this.canvasY + this.canvasH - 1);
        MatrixStack matrices = context.getMatrices();
        matrices.push();
        matrices.translate(this.canvasX + this.canvasW / 2.0f, this.canvasY + this.canvasH / 2.0f, 0.0f);
        matrices.scale(this.zoom, this.zoom, 1.0f);
        matrices.translate(-this.camX, -this.camY, 0.0f);

        for (Band band : this.bands) {
            context.fill(band.left, BAND_TOP, band.right, BAND_BOTTOM, BAND);
            context.drawBorder(band.left, BAND_TOP, band.right - band.left, BAND_BOTTOM - BAND_TOP, AllianceUi.BORDER_DIM);
            Text name = Text.translatable(band.season.translationKey());
            context.drawTextWithShadow(this.textRenderer, this.trim(name, band.right - band.left - 8),
                    band.left + 4, BAND_TOP + 4, AllianceUi.TITLE);
        }

        Node previous = null;
        for (Node node : this.nodes.values()) {
            if (previous != null) {
                int color = ClientStoryState.data().isCompleted(previous.mission.id()) ? DONE_BORDER : AllianceUi.BORDER_DIM;
                connect(context, previous, node, color);
            }
            previous = node;
        }

        Node hovered = this.nodeAt(mouseX, mouseY);
        float pulse = 0.5f + 0.5f * MathHelper.sin(Util.getMeasuringTimeMs() / 250.0f);
        for (Node node : this.nodes.values()) {
            this.renderNode(context, node, node == hovered, pulse);
        }
        matrices.pop();
        context.disableScissor();
        return hovered;
    }

    /** An elbow line between two missions: across, up or down, across. */
    private static void connect(DrawContext context, Node from, Node to, int color) {
        int midX = (from.x + to.x) / 2;
        context.fill(Math.min(from.x, midX), from.y - 1, Math.max(from.x, midX) + 1, from.y + 1, color);
        context.fill(midX - 1, Math.min(from.y, to.y) - 1, midX + 1, Math.max(from.y, to.y) + 1, color);
        context.fill(Math.min(midX, to.x), to.y - 1, Math.max(midX, to.x) + 1, to.y + 1, color);
    }

    private void renderNode(DrawContext context, Node node, boolean hovered, float pulse) {
        Status status = this.status(node.mission);
        int fill;
        int border;
        int text = 0xFFFFFF;
        switch (status) {
            case LOCKED -> {
                fill = LOCKED_FILL;
                border = AllianceUi.BORDER_DIM;
                text = AllianceUi.TEXT_DIM;
            }
            case AVAILABLE -> {
                fill = AVAILABLE_FILL;
                border = lerpColor(pulse, AllianceUi.BORDER, AllianceUi.HIGHLIGHT);
            }
            case ACTIVE, CLAIMABLE -> {
                fill = ACTIVE_FILL;
                border = ACTIVE_BORDER;
            }
            default -> {
                fill = DONE_FILL;
                border = DONE_BORDER;
            }
        }
        int half = NODE / 2;
        if (node.mission == this.selected) {
            roundedRect(context, node.x - half - 2, node.y - half - 2, NODE + 4, NODE + 4, 0xFFFFFFFF, 0xFFFFFFFF);
        }
        roundedRect(context, node.x - half, node.y - half, NODE, NODE, hovered ? lerpColor(0.5f, fill, 0xFFFFFFFF) : fill, border);
        String number = String.valueOf(node.mission.episode());
        context.drawText(this.textRenderer, number, node.x - this.textRenderer.getWidth(number) / 2 + 1, node.y - 3, text, true);
        if (status == Status.CLAIMABLE) {
            // A reward is waiting: a gold "!" badge on the corner.
            roundedRect(context, node.x + half - 5, node.y - half - 4, 9, 9, ACTIVE_BORDER, ACTIVE_BORDER);
            context.drawText(this.textRenderer, "!", node.x + half - 1, node.y - half - 3, 0x3A2A00, false);
        }
    }

    /** A box with clipped corners, so nodes read as dots. */
    private static void roundedRect(DrawContext context, int x, int y, int w, int h, int fill, int border) {
        context.fill(x + 2, y, x + w - 2, y + h, border);
        context.fill(x, y + 2, x + w, y + h - 2, border);
        context.fill(x + 1, y + 1, x + w - 1, y + h - 1, border);
        context.fill(x + 2, y + 1, x + w - 2, y + h - 1, fill);
        context.fill(x + 1, y + 2, x + w - 1, y + h - 2, fill);
    }

    private static int lerpColor(float t, int a, int b) {
        int alpha = (int) MathHelper.lerp(t, a >>> 24, b >>> 24);
        int red = (int) MathHelper.lerp(t, a >> 16 & 0xFF, b >> 16 & 0xFF);
        int green = (int) MathHelper.lerp(t, a >> 8 & 0xFF, b >> 8 & 0xFF);
        int blue = (int) MathHelper.lerp(t, a & 0xFF, b & 0xFF);
        return alpha << 24 | red << 16 | green << 8 | blue;
    }

    private boolean inCanvas(double mouseX, double mouseY) {
        return mouseX >= this.canvasX && mouseX < this.canvasX + this.canvasW
                && mouseY >= this.canvasY && mouseY < this.canvasY + this.canvasH;
    }

    @Nullable
    private Node nodeAt(double mouseX, double mouseY) {
        if (!this.inCanvas(mouseX, mouseY)) {
            return null;
        }
        double treeX = (mouseX - this.canvasX - this.canvasW / 2.0) / this.zoom + this.camX;
        double treeY = (mouseY - this.canvasY - this.canvasH / 2.0) / this.zoom + this.camY;
        for (Node node : this.nodes.values()) {
            if (Math.abs(treeX - node.x) <= NODE / 2.0 + 1 && Math.abs(treeY - node.y) <= NODE / 2.0 + 1) {
                return node;
            }
        }
        return null;
    }

    private void clampCamera() {
        Band first = this.bands.get(0);
        Band last = this.bands.get(this.bands.size() - 1);
        this.camX = MathHelper.clamp(this.camX, first.left, last.right);
        this.camY = MathHelper.clamp(this.camY, BAND_TOP, BAND_BOTTOM);
    }

    private void select(Mission mission) {
        this.selected = mission;
        this.updateActionButton();
    }

    // ------------------------------------------------------------------ details

    /** Draws the selected mission's details. Returns the reward item under the mouse, if any. */
    @Nullable
    private ItemStack renderDetails(DrawContext context, int mouseX, int mouseY) {
        Mission mission = this.selected;
        AllianceUi.panel(context, this.textRenderer, MissionText.title(mission), this.detailX, this.detailY, this.detailW, this.detailH);
        int bodyTop = this.detailY + 16;
        int bodyBottom = this.detailY + this.detailH - 4;

        // Who you'll be up against, turning slowly.
        int previewW = MathHelper.clamp(this.detailW / 4, 60, 100);
        int previewX = this.detailX + 4;
        context.fill(previewX, bodyTop, previewX + previewW, bodyBottom, 0x80000000);
        context.drawBorder(previewX, bodyTop, previewW, bodyBottom - bodyTop, AllianceUi.BORDER_DIM);
        LivingEntity preview = this.preview(mission.previewType());
        if (preview != null) {
            int boxH = bodyBottom - bodyTop;
            // Models reach a little past their hitbox, so leave some room around it.
            int size = (int) Math.min((boxH - 14) / (preview.getHeight() * 1.15f), (previewW - 6) / (preview.getWidth() * 1.7f));
            context.enableScissor(previewX + 1, bodyTop + 1, previewX + previewW - 1, bodyBottom - 1);
            CharacterPreview.drawSpinning(context, previewX + previewW / 2, bodyBottom - 5, size, this.previewYaw, preview);
            context.disableScissor();
        }

        // Rewards and the action button on the right.
        int columnW = this.rewardColumnWidth();
        int columnX = this.detailX + this.detailW - columnW - 4;
        ItemStack hoveredItem = null;
        int y = bodyTop;
        context.drawTextWithShadow(this.textRenderer, Text.translatable("screen.skibidi.story.rewards"), columnX, y, AllianceUi.TITLE);
        y += 11;
        context.drawTextWithShadow(this.textRenderer, Text.translatable("screen.skibidi.story.reward_points", mission.points()),
                columnX, y, AllianceUi.HIGHLIGHT);
        y += 10;
        for (ItemStack reward : mission.rewards()) {
            context.drawItem(reward, columnX, y);
            context.drawTextWithShadow(this.textRenderer, this.trim(reward.getName(), columnW - 20),
                    columnX + 19, y + 4, AllianceUi.TEXT);
            if (mouseX >= columnX && mouseX < columnX + 16 && mouseY >= y && mouseY < y + 16) {
                hoveredItem = reward;
            }
            y += 17;
        }
        Text note = this.statusNote(mission);
        if (note != null) {
            List<OrderedText> lines = this.textRenderer.wrapLines(note, columnW);
            int noteY = this.actionButton.getY() - 2 - lines.size() * 9;
            for (int i = 0; i < lines.size(); i++) {
                context.drawTextWithShadow(this.textRenderer, lines.get(i), columnX, noteY + i * 9, AllianceUi.TEXT_DIM);
            }
        }

        // The story of the episode, and the objectives, in the middle.
        int textX = previewX + previewW + 6;
        int textW = columnX - 6 - textX;
        y = bodyTop;
        Season season = StoryMissions.seasonOf(mission);
        context.drawTextWithShadow(this.textRenderer, this.trim(Text.translatable(season.translationKey()), textW),
                textX, y, AllianceUi.TITLE);
        y += 11;
        int objectivesHeight = 12 + mission.stages().size() * 9;
        int descriptionLines = Math.max(1, (bodyBottom - objectivesHeight - y - 2) / 9);
        List<OrderedText> description = this.textRenderer.wrapLines(Text.translatable(mission.descriptionKey()), textW);
        for (int i = 0; i < Math.min(description.size(), descriptionLines); i++) {
            context.drawTextWithShadow(this.textRenderer, description.get(i), textX, y, AllianceUi.TEXT);
            y += 9;
        }
        y = bodyBottom - objectivesHeight;
        context.drawTextWithShadow(this.textRenderer, Text.translatable("screen.skibidi.story.objectives"), textX, y, AllianceUi.TITLE);
        y += 11;
        for (int i = 0; i < mission.stages().size(); i++) {
            Text line = Text.literal((i + 1) + ". ").append(MissionText.objectiveWithSize(mission, i));
            context.drawTextWithShadow(this.textRenderer, this.trim(line, textW), textX, y, AllianceUi.TEXT);
            y += 9;
        }
        return hoveredItem;
    }

    @Nullable
    private Text statusNote(Mission mission) {
        Status status = this.status(mission);
        if (status == Status.LOCKED) {
            Mission previous = StoryMissions.previous(mission);
            return previous != null ? Text.translatable("screen.skibidi.story.note.locked", previous.episode()) : null;
        }
        if (status == Status.AVAILABLE && ClientStoryState.status() != null) {
            return Text.translatable("screen.skibidi.story.note.busy");
        }
        if (status == Status.ACTIVE) {
            return Text.translatable("screen.skibidi.story.note.active");
        }
        return status == Status.AVAILABLE ? Text.translatable("screen.skibidi.story.note.available") : null;
    }

    private OrderedText trim(Text text, int width) {
        return MissionText.fit(this.textRenderer, text, width);
    }

    @Nullable
    private LivingEntity preview(EntityType<?> type) {
        if (this.client.world == null) {
            return null;
        }
        return this.previews.computeIfAbsent(type, key -> {
            Entity entity = key.create(this.client.world);
            return entity instanceof LivingEntity living ? living : null;
        });
    }

    // ------------------------------------------------------------------ input

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        Season season = this.seasonAt(mouseX, mouseY);
        if (season != null) {
            StoryData data = ClientStoryState.data();
            Mission pick = season.missions().stream().filter(mission -> !data.isCompleted(mission.id())).findFirst()
                    .orElse(season.missions().get(0));
            this.select(pick);
            this.camX = this.nodes.get(pick).x;
            this.clampCamera();
            return true;
        }
        if (this.inCanvas(mouseX, mouseY)) {
            this.panning = true;
            this.panDistance = 0.0;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (this.panning) {
            this.camX -= (float) (deltaX / this.zoom);
            this.camY -= (float) (deltaY / this.zoom);
            this.panDistance += Math.abs(deltaX) + Math.abs(deltaY);
            this.clampCamera();
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.panning) {
            this.panning = false;
            // A press that barely moved is a click on a mission rather than a drag.
            Node node = this.panDistance < 4.0 ? this.nodeAt(mouseX, mouseY) : null;
            if (node != null) {
                this.select(node.mission);
            }
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (this.inCanvas(mouseX, mouseY)) {
            this.zoom = MathHelper.clamp(this.zoom * (amount > 0 ? 1.15f : 1 / 1.15f), MIN_ZOOM, MAX_ZOOM);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (ClientStoryState.isOpenKey(keyCode, scanCode)) {
            this.close();
            return true;
        }
        // Left and right step through the missions in story order.
        List<Mission> order = new ArrayList<>(this.nodes.keySet());
        int index = order.indexOf(this.selected);
        int step = keyCode == GLFW.GLFW_KEY_LEFT ? -1 : keyCode == GLFW.GLFW_KEY_RIGHT ? 1 : 0;
        if (step != 0 && index + step >= 0 && index + step < order.size()) {
            this.select(order.get(index + step));
            this.camX = this.nodes.get(this.selected).x;
            this.clampCamera();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
