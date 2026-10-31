package kr.pokeeditui.client;

import com.cobblemon.mod.common.api.moves.MoveTemplate;
import com.cobblemon.mod.common.api.moves.Moves;
import com.cobblemon.mod.common.api.pokemon.Natures;
import com.cobblemon.mod.common.api.pokemon.stats.Stat;
import com.cobblemon.mod.common.api.types.tera.TeraType;
import com.cobblemon.mod.common.api.types.tera.TeraTypes;
import com.cobblemon.mod.common.pokemon.Nature;
import kr.pokeeditui.BaseStats;
import kr.pokeeditui.net.ApplyEditPayload;
import kr.pokeeditui.net.PartySnapshotPayload;
import kr.pokeeditui.net.PokemonEntry;
import kr.pokeeditui.net.PokemonEntry.AbilityOption;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/** Edit screen for one player's party. All values are validated again by the server. */
public class PokeEditScreen extends Screen {
    private static final int PANEL_WIDTH = 400;
    private static final int PANEL_HEIGHT = 244;
    private static final int LIST_WIDTH = 110;
    private static final int MAX_IV = 31;
    private static final int MAX_EV = 252;
    private static final int MAX_EV_TOTAL = 510;
    private static final int MAX_FRIENDSHIP = 255;
    private static final int TEXT = 0xFFFFFF;
    private static final int MUTED = 0xA0A0A0;
    private static final int ERROR = 0xFF5555;
    private static final String[] STAT_KEYS = {"hp", "atk", "def", "spa", "spd", "spe"};

    private PartySnapshotPayload snapshot;
    private int selected;
    private EditState state;

    private int left;
    private int top;
    private int col;

    private TextFieldWidget levelField;
    private TextFieldWidget friendshipField;
    private final TextFieldWidget[] ivFields = new TextFieldWidget[PokemonEntry.STATS];
    private final TextFieldWidget[] evFields = new TextFieldWidget[PokemonEntry.STATS];
    private final TextFieldWidget[] baseFields = new TextFieldWidget[PokemonEntry.STATS];
    private ButtonWidget applyButton;

    public PokeEditScreen(PartySnapshotPayload snapshot) {
        super(Text.translatable("pokeeditui.title"));
        this.snapshot = snapshot;
        resetState();
    }

    public boolean isFor(UUID target) {
        return snapshot.target().equals(target);
    }

    /** New data from the server (after applying), keeps the selected slot if it still exists. */
    public void update(PartySnapshotPayload snapshot) {
        int slot = current() != null ? current().slot() : -1;
        this.snapshot = snapshot;
        this.selected = 0;

        for (int i = 0; i < snapshot.party().size(); i++) {
            if (snapshot.party().get(i).slot() == slot) {
                this.selected = i;
            }
        }

        resetState();

        if (client != null && client.currentScreen == this) {
            clearAndInit();
        }
    }

    private void resetState() {
        PokemonEntry entry = current();
        state = entry != null ? EditState.of(entry) : null;
    }

    private PokemonEntry current() {
        return selected < snapshot.party().size() ? snapshot.party().get(selected) : null;
    }

    @Override
    protected void init() {
        applyButton = null; // widgets are recreated below, skip validation until they all exist
        left = (width - PANEL_WIDTH) / 2;
        top = Math.max(4, (height - PANEL_HEIGHT) / 2);
        col = left + LIST_WIDTH + 8;

        List<PokemonEntry> party = snapshot.party();

        for (int i = 0; i < party.size(); i++) {
            PokemonEntry entry = party.get(i);
            int index = i;
            MutableText label = entry.displayName().copy().append(Text.literal(" Lv." + entry.level()).formatted(Formatting.GRAY));
            ButtonWidget button = ButtonWidget.builder(label, b -> {
                selected = index;
                resetState(); // switching pokemon discards unapplied edits
                clearAndInit();
            }).dimensions(left, top + 14 + i * 22, LIST_WIDTH, 20).build();
            button.active = i != selected;
            addDrawableChild(button);
        }

        addDrawableChild(ButtonWidget.builder(Text.translatable("pokeeditui.close"), b -> close())
            .dimensions(left, top + PANEL_HEIGHT - 20, LIST_WIDTH, 20).build());

        PokemonEntry entry = current();

        if (entry == null) {
            return;
        }

        // row 1: level, shiny, gender
        levelField = numberField(col + 34, top, 30, state.level, s -> state.level = s);
        addDrawableChild(CyclingButtonWidget.onOffBuilder(state.shiny)
            .build(col + 70, top, 90, 20, Text.translatable("pokeeditui.shiny"), (b, v) -> state.shiny = v));
        addDrawableChild(CyclingButtonWidget.<String>builder(g -> Text.translatable("pokeeditui.gender." + g.toLowerCase(Locale.ROOT)))
            .values(genders(entry))
            .initially(genders(entry).contains(state.gender) ? state.gender : genders(entry).get(0))
            .build(col + 164, top, 118, 20, Text.translatable("pokeeditui.gender"), (b, v) -> state.gender = v));

        // row 2: nature (grid picker), ability
        Nature nature = nature(state.nature);
        ButtonWidget natureButton = addDrawableChild(ButtonWidget.builder(
            Text.translatable("pokeeditui.nature").append(": ").append(natureName(nature)),
            b -> openNaturePicker()).dimensions(col, top + 24, 138, 20).build());
        natureButton.setTooltip(Tooltip.of(natureEffect(nature)));

        AbilityOption currentAbility = entry.abilityOptions().stream()
            .filter(a -> a.name().equals(state.ability))
            .findFirst().orElse(entry.abilityOptions().get(0));
        addDrawableChild(CyclingButtonWidget.<AbilityOption>builder(a -> {
                MutableText text = Text.translatable(a.displayKey());
                return a.hidden() ? text.append(Text.translatable("pokeeditui.ability.hidden").formatted(Formatting.GOLD)) : text;
            })
            .values(entry.abilityOptions())
            .initially(currentAbility)
            .build(col + 142, top + 24, 140, 20, Text.translatable("pokeeditui.ability"), (b, v) -> state.ability = v.name()));

        // row 3: friendship, tera type (grid picker)
        friendshipField = numberField(col + 44, top + 48, 30, state.friendship, s -> state.friendship = s);
        TeraType tera = teraType(state.teraType);
        addDrawableChild(ButtonWidget.builder(
            Text.translatable("pokeeditui.tera").append(": ").append(tera != null ? tera.getDisplayName() : Text.literal("?")),
            b -> openTeraPicker()).dimensions(col + 80, top + 48, 202, 20).build());

        // IVs / EVs / base stats
        for (int i = 0; i < PokemonEntry.STATS; i++) {
            int stat = i;
            ivFields[i] = numberField(col + 44 + i * 40, top + 86, 36, state.ivs[i], s -> state.ivs[stat] = s);
            evFields[i] = numberField(col + 44 + i * 40, top + 108, 36, state.evs[i], s -> state.evs[stat] = s);
            baseFields[i] = numberField(col + 44 + i * 40, top + 130, 36, state.baseStats[i], s -> state.baseStats[stat] = s);
        }

        addDrawableChild(ButtonWidget.builder(Text.translatable("pokeeditui.iv_max"), b -> {
            for (TextFieldWidget field : ivFields) {
                field.setText(Integer.toString(MAX_IV));
            }
        }).dimensions(col, top + 152, 92, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("pokeeditui.ev_reset"), b -> {
            for (TextFieldWidget field : evFields) {
                field.setText("0");
            }
        }).dimensions(col + 95, top + 152, 92, 20).build());
        ButtonWidget baseReset = addDrawableChild(ButtonWidget.builder(Text.translatable("pokeeditui.base_reset"), b -> {
            for (int i = 0; i < PokemonEntry.STATS; i++) {
                baseFields[i].setText(Integer.toString(entry.formBase()[i]));
            }
        }).dimensions(col + 190, top + 152, 92, 20).build());
        baseReset.setTooltip(Tooltip.of(Text.translatable("pokeeditui.base_original", formBaseText(entry))));

        // moves (list picker)
        for (int i = 0; i < PokemonEntry.MOVES; i++) {
            int slot = i;
            MoveTemplate move = move(state.moves[i]);
            Text label = state.moves[i].isBlank() ? Text.translatable("pokeeditui.move_empty").formatted(Formatting.DARK_GRAY)
                : move != null ? move.getDisplayName() : Text.literal(state.moves[i]).formatted(Formatting.RED);
            ButtonWidget button = addDrawableChild(ButtonWidget.builder(label, b -> openMovePicker(slot))
                .dimensions(col + i * 71, top + 190, 69, 20).build());

            if (move != null) {
                button.setTooltip(Tooltip.of(move.getDescription()));
            }
        }

        // bottom: apply, revert
        applyButton = addDrawableChild(ButtonWidget.builder(Text.translatable("pokeeditui.apply"), b -> apply())
            .dimensions(col, top + PANEL_HEIGHT - 20, 138, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("pokeeditui.revert"), b -> {
            resetState();
            clearAndInit();
        }).dimensions(col + 142, top + PANEL_HEIGHT - 20, 140, 20).build());

        refreshValidity();
    }

    private TextFieldWidget numberField(int x, int y, int w, String value, Consumer<String> setter) {
        TextFieldWidget field = new TextFieldWidget(textRenderer, x, y + 2, w, 16, Text.empty());
        field.setMaxLength(3);
        field.setTextPredicate(s -> s.matches("\\d{0,3}"));
        field.setText(value);
        field.setChangedListener(s -> {
            setter.accept(s);
            refreshValidity();
        });
        return addDrawableChild(field);
    }

    /** e.g. "45/49/49/65/65/45" */
    private static String formBaseText(PokemonEntry entry) {
        StringBuilder text = new StringBuilder();

        for (int i = 0; i < PokemonEntry.STATS; i++) {
            text.append(i > 0 ? "/" : "").append(entry.formBase()[i]);
        }

        return text.toString();
    }

    private void openNaturePicker() {
        List<Nature> natures = new ArrayList<>(Natures.all());
        natures.sort(Comparator.comparing(n -> natureName(n).getString()));
        client.setScreen(new GridPickerScreen<>(this, Text.translatable("pokeeditui.nature_picker"), natures,
            nature(state.nature), n -> natureLabel(n), PokeEditScreen::natureEffect, 5, 76,
            n -> state.nature = n.getName().toString()));
    }

    private void openTeraPicker() {
        List<TeraType> types = new ArrayList<>();
        TeraTypes.INSTANCE.iterator().forEachRemaining(types::add);
        client.setScreen(new GridPickerScreen<>(this, Text.translatable("pokeeditui.tera_picker"), types,
            teraType(state.teraType), TeraType::getDisplayName, null, 5, 76,
            t -> state.teraType = t.getId().toString()));
    }

    private void openMovePicker(int slot) {
        Set<String> taken = new HashSet<>();

        for (int i = 0; i < PokemonEntry.MOVES; i++) {
            if (i != slot && !state.moves[i].isBlank()) {
                taken.add(state.moves[i]);
            }
        }

        client.setScreen(new MovePickerScreen(this, slot, current().learnableMoves(), state.moves[slot], taken,
            name -> state.moves[slot] = name));
    }

    private static Nature nature(String id) {
        return Natures.all().stream().filter(n -> n.getName().toString().equals(id)).findFirst().orElse(null);
    }

    private static TeraType teraType(String id) {
        List<TeraType> types = new ArrayList<>();
        TeraTypes.INSTANCE.iterator().forEachRemaining(types::add);
        return types.stream().filter(t -> t.getId().toString().equals(id)).findFirst().orElse(null);
    }

    private static Text natureName(Nature nature) {
        return nature != null ? Text.translatable(nature.getDisplayName()) : Text.literal("?");
    }

    /** Nature name with its stat changes, e.g. "Adamant +Atk -SpA". */
    private static Text natureLabel(Nature nature) {
        MutableText text = natureName(nature).copy();
        Stat up = nature.getIncreasedStat();
        Stat down = nature.getDecreasedStat();

        if (up != null && down != null && up != down) {
            text.append(Text.literal(" +").formatted(Formatting.RED)).append(statShort(up).copy().formatted(Formatting.RED))
                .append(Text.literal(" -").formatted(Formatting.BLUE)).append(statShort(down).copy().formatted(Formatting.BLUE));
        }

        return text;
    }

    private static Text natureEffect(Nature nature) {
        if (nature == null) {
            return Text.empty();
        }

        Stat up = nature.getIncreasedStat();
        Stat down = nature.getDecreasedStat();

        if (up == null || down == null || up == down) {
            return Text.translatable("pokeeditui.nature_neutral");
        }

        return Text.translatable("pokeeditui.nature_effect", up.getDisplayName(), down.getDisplayName());
    }

    private static Text statShort(Stat stat) {
        String id = stat.getShowdownId();

        for (int i = 0; i < STAT_KEYS.length; i++) {
            if (STAT_KEYS[i].equals(id)) {
                return Text.translatable("pokeeditui.stat." + STAT_KEYS[i]);
            }
        }

        return stat.getDisplayName();
    }

    private static List<String> genders(PokemonEntry entry) {
        float ratio = entry.maleRatio();

        if (ratio < 0) {
            return List.of("GENDERLESS");
        } else if (ratio == 0) {
            return List.of("FEMALE");
        } else if (ratio == 1) {
            return List.of("MALE");
        }

        return List.of("MALE", "FEMALE");
    }

    private static MoveTemplate move(String name) {
        return name == null || name.isBlank() ? null : Moves.getByName(name.trim().toLowerCase(Locale.ROOT));
    }

    private int evTotal() {
        int total = 0;

        for (String ev : state.evs) {
            total += EditState.parse(ev);
        }

        return total;
    }

    /** Colors invalid inputs and disables the apply button while something is invalid. */
    private void refreshValidity() {
        if (applyButton == null) {
            return;
        }

        boolean valid = true;
        int level = EditState.parse(state.level);
        valid &= color(levelField, level >= 1 && level <= current().maxLevel());
        valid &= color(friendshipField, EditState.parse(state.friendship) <= MAX_FRIENDSHIP);

        for (int i = 0; i < PokemonEntry.STATS; i++) {
            valid &= color(ivFields[i], EditState.parse(state.ivs[i]) <= MAX_IV);
            valid &= color(evFields[i], EditState.parse(state.evs[i]) <= MAX_EV);
            int base = EditState.parse(state.baseStats[i]);
            valid &= color(baseFields[i], base >= BaseStats.MIN && base <= BaseStats.MAX);
        }

        valid &= evTotal() <= MAX_EV_TOTAL;
        boolean anyMove = false;

        for (String move : state.moves) {
            boolean empty = move.isBlank();
            anyMove |= !empty;
            valid &= empty || move(move) != null;
        }

        applyButton.active = valid && anyMove && nature(state.nature) != null && teraType(state.teraType) != null;
    }

    private static boolean color(TextFieldWidget field, boolean ok) {
        field.setEditableColor(ok ? 0xE0E0E0 : ERROR);
        return ok;
    }

    private void apply() {
        PokemonEntry entry = current();

        if (entry == null || !applyButton.active) {
            return;
        }

        int[] ivs = new int[PokemonEntry.STATS];
        int[] evs = new int[PokemonEntry.STATS];
        int[] baseStats = new int[PokemonEntry.STATS];

        for (int i = 0; i < PokemonEntry.STATS; i++) {
            ivs[i] = EditState.parse(state.ivs[i]);
            evs[i] = EditState.parse(state.evs[i]);
            baseStats[i] = EditState.parse(state.baseStats[i]);
        }

        ClientPlayNetworking.send(new ApplyEditPayload(
            snapshot.target(),
            entry.slot(),
            entry.uuid(),
            EditState.parse(state.level),
            state.shiny,
            state.gender,
            state.nature,
            state.ability,
            EditState.parse(state.friendship),
            state.teraType,
            ivs,
            evs,
            baseStats,
            state.moves.clone()));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        context.drawTextWithShadow(textRenderer,
            Text.translatable("pokeeditui.party", snapshot.targetName()), left, top + 2, TEXT);

        if (current() == null) {
            context.drawTextWithShadow(textRenderer, Text.translatable("pokeeditui.empty"), col, top + 6, MUTED);
            return;
        }

        context.drawTextWithShadow(textRenderer, Text.translatable("pokeeditui.level"), col, top + 6, TEXT);
        context.drawTextWithShadow(textRenderer, Text.translatable("pokeeditui.friendship"), col, top + 54, TEXT);

        for (int i = 0; i < PokemonEntry.STATS; i++) {
            context.drawCenteredTextWithShadow(textRenderer,
                Text.translatable("pokeeditui.stat." + STAT_KEYS[i]), col + 62 + i * 40, top + 76, MUTED);
        }

        context.drawTextWithShadow(textRenderer, Text.translatable("pokeeditui.ivs"), col, top + 92, TEXT);
        context.drawTextWithShadow(textRenderer, Text.translatable("pokeeditui.evs"), col, top + 114, TEXT);
        context.drawTextWithShadow(textRenderer, Text.translatable("pokeeditui.base"), col, top + 136, TEXT);

        context.drawTextWithShadow(textRenderer, Text.translatable("pokeeditui.moves"), col, top + 179, TEXT);

        int total = evTotal();
        Text evTotal = Text.translatable("pokeeditui.ev_total", total, MAX_EV_TOTAL);
        context.drawTextWithShadow(textRenderer, evTotal, col + 282 - textRenderer.getWidth(evTotal), top + 179,
            total > MAX_EV_TOTAL ? ERROR : MUTED);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
