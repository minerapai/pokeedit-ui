package kr.pokeeditui.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/** Shows all options at once as a grid of buttons (natures, tera types, ...). */
class GridPickerScreen<T> extends PickerScreen {
    private final List<T> values;
    private final T current;
    private final Function<T, Text> label;
    private final Function<T, Text> tooltip;
    private final Consumer<T> onPick;
    private final int columns;
    private final int buttonWidth;

    GridPickerScreen(PokeEditScreen parent, Text title, List<T> values, T current, Function<T, Text> label,
                     Function<T, Text> tooltip, int columns, int buttonWidth, Consumer<T> onPick) {
        super(title, parent);
        this.values = values;
        this.current = current;
        this.label = label;
        this.tooltip = tooltip;
        this.columns = columns;
        this.buttonWidth = buttonWidth;
        this.onPick = onPick;
    }

    @Override
    protected void init() {
        int rows = (values.size() + columns - 1) / columns;
        int gridWidth = columns * (buttonWidth + 4) - 4;
        int gridHeight = rows * 24 - 4;
        int left = (width - gridWidth) / 2;
        int top = Math.max(20, (height - gridHeight - 30) / 2);

        for (int i = 0; i < values.size(); i++) {
            T value = values.get(i);
            ButtonWidget button = ButtonWidget.builder(label.apply(value), b -> {
                onPick.accept(value);
                close();
            }).dimensions(left + (i % columns) * (buttonWidth + 4), top + (i / columns) * 24, buttonWidth, 20).build();

            Text tip = tooltip != null ? tooltip.apply(value) : null;

            if (tip != null) {
                button.setTooltip(Tooltip.of(tip));
            }

            button.active = !value.equals(current);
            addDrawableChild(button);
        }

        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.cancel"), b -> close())
            .dimensions(width / 2 - 50, top + gridHeight + 10, 100, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        int rows = (values.size() + columns - 1) / columns;
        int top = Math.max(20, (height - (rows * 24 - 4) - 30) / 2);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, top - 14, 0xFFFFFF);
    }
}
