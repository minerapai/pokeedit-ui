package kr.pokeeditui.client;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/** A selection screen opened from the edit screen; closing it returns to the edit screen. */
abstract class PickerScreen extends Screen {
    protected final PokeEditScreen parent;

    protected PickerScreen(Text title, PokeEditScreen parent) {
        super(title);
        this.parent = parent;
    }

    PokeEditScreen parent() {
        return parent;
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
