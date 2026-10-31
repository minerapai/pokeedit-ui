package kr.pokeeditui.client;

import com.cobblemon.mod.common.api.moves.MoveTemplate;
import com.cobblemon.mod.common.api.moves.Moves;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;

/** Pick a move from a searchable list of the moves the pokemon can learn (or all moves). */
class MovePickerScreen extends PickerScreen {
    private static final int PANEL_WIDTH = 360;
    private static final int ROW_HEIGHT = 22;
    private static boolean showAllMoves = false;

    private final List<String> learnable;
    private final String current;
    private final Set<String> taken;
    private final Consumer<String> onPick;

    private final List<MoveTemplate> filtered = new ArrayList<>();
    private final List<ButtonWidget> rows = new ArrayList<>();
    private TextFieldWidget searchField;
    private String query = "";
    private int offset;
    private int left;
    private int listTop;

    MovePickerScreen(PokeEditScreen parent, int slot, List<String> learnable, String current, Set<String> taken,
                     Consumer<String> onPick) {
        super(Text.translatable("pokeeditui.move_picker", slot + 1), parent);
        this.learnable = learnable;
        this.current = current;
        this.taken = taken;
        this.onPick = onPick;
    }

    @Override
    protected void init() {
        left = (width - PANEL_WIDTH) / 2;
        int top = 20;

        searchField = new TextFieldWidget(textRenderer, left, top, 200, 18, Text.translatable("pokeeditui.search"));
        searchField.setPlaceholder(Text.translatable("pokeeditui.search").formatted(net.minecraft.util.Formatting.DARK_GRAY));
        searchField.setText(query);
        searchField.setChangedListener(text -> {
            query = text;
            offset = 0;
            refilter();
        });
        addDrawableChild(searchField);
        setInitialFocus(searchField);

        addDrawableChild(ButtonWidget.builder(modeLabel(), b -> {
            showAllMoves = !showAllMoves;
            b.setMessage(modeLabel());
            offset = 0;
            refilter();
        }).dimensions(left + 206, top - 1, PANEL_WIDTH - 206, 20).build());

        listTop = top + 36;
        int rowCount = Math.max(3, (height - listTop - 34) / ROW_HEIGHT);
        rows.clear();

        for (int i = 0; i < rowCount; i++) {
            int index = i;
            ButtonWidget row = ButtonWidget.builder(Text.empty(), b -> pick(index))
                .dimensions(left, listTop + i * ROW_HEIGHT, 150, 20).build();
            rows.add(addDrawableChild(row));
        }

        int bottom = listTop + rowCount * ROW_HEIGHT + 4;
        addDrawableChild(ButtonWidget.builder(Text.translatable("pokeeditui.move_clear"), b -> {
            onPick.accept("");
            close();
        }).dimensions(left, bottom, 120, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.cancel"), b -> close())
            .dimensions(left + PANEL_WIDTH - 120, bottom, 120, 20).build());

        refilter();
    }

    private Text modeLabel() {
        return Text.translatable(showAllMoves ? "pokeeditui.moves_all" : "pokeeditui.moves_learnable");
    }

    private void refilter() {
        filtered.clear();
        String q = query.trim().toLowerCase(Locale.ROOT);
        List<MoveTemplate> source = new ArrayList<>();

        if (showAllMoves) {
            source.addAll(Moves.all());
        } else {
            for (String name : learnable) {
                MoveTemplate template = Moves.getByName(name);

                if (template != null) {
                    source.add(template);
                }
            }
        }

        for (MoveTemplate template : source) {
            if (q.isEmpty() || template.getName().contains(q)
                || template.getDisplayName().getString().toLowerCase(Locale.ROOT).contains(q)) {
                filtered.add(template);
            }
        }

        filtered.sort(Comparator.comparing(t -> t.getDisplayName().getString()));
        refreshRows();
    }

    private void refreshRows() {
        offset = Math.max(0, Math.min(offset, Math.max(0, filtered.size() - rows.size())));

        for (int i = 0; i < rows.size(); i++) {
            ButtonWidget row = rows.get(i);
            int index = offset + i;

            if (index >= filtered.size()) {
                row.visible = false;
                continue;
            }

            MoveTemplate template = filtered.get(index);
            row.visible = true;
            row.setMessage(template.getDisplayName());
            row.active = !template.getName().equals(current) && !taken.contains(template.getName());
            row.setTooltip(Tooltip.of(template.getDescription()));
        }
    }

    private void pick(int row) {
        int index = offset + row;

        if (index < filtered.size()) {
            onPick.accept(filtered.get(index).getName());
            close();
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        offset -= (int) Math.signum(verticalAmount) * 3;
        refreshRows();
        return true;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 6, 0xFFFFFF);

        int headerY = listTop - 11;
        context.drawTextWithShadow(textRenderer, Text.translatable("pokeeditui.col.type"), left + 156, headerY, 0xA0A0A0);
        context.drawTextWithShadow(textRenderer, Text.translatable("pokeeditui.col.category"), left + 216, headerY, 0xA0A0A0);
        context.drawTextWithShadow(textRenderer, Text.translatable("pokeeditui.col.power"), left + 276, headerY, 0xA0A0A0);
        context.drawTextWithShadow(textRenderer, Text.translatable("pokeeditui.col.pp"), left + 320, headerY, 0xA0A0A0);

        for (int i = 0; i < rows.size(); i++) {
            int index = offset + i;

            if (index >= filtered.size()) {
                break;
            }

            MoveTemplate template = filtered.get(index);
            int y = listTop + i * ROW_HEIGHT + 6;
            context.drawTextWithShadow(textRenderer, template.getElementalType().getDisplayName(), left + 156, y,
                0xFF000000 | template.getElementalType().getHue());
            context.drawTextWithShadow(textRenderer, template.getDamageCategory().getDisplayName(), left + 216, y, 0xE0E0E0);
            String power = template.getPower() > 0 ? Integer.toString((int) template.getPower()) : "-";
            context.drawTextWithShadow(textRenderer, Text.literal(power), left + 276, y, 0xE0E0E0);
            context.drawTextWithShadow(textRenderer, Text.literal(Integer.toString(template.getMaxPp())), left + 320, y, 0xE0E0E0);
        }

        if (filtered.isEmpty()) {
            context.drawTextWithShadow(textRenderer, Text.translatable("pokeeditui.no_results"), left, listTop + 6, 0xA0A0A0);
        } else if (filtered.size() > rows.size()) {
            String range = (offset + 1) + "-" + Math.min(filtered.size(), offset + rows.size()) + " / " + filtered.size();
            context.drawCenteredTextWithShadow(textRenderer, Text.literal(range), width / 2,
                listTop + rows.size() * ROW_HEIGHT + 10, 0xA0A0A0);
        }
    }
}
