package kr.pokeeditui.client;

import kr.pokeeditui.net.PokemonEntry;

/**
 * Unsaved edits of the selected pokemon. Kept outside the widgets so the values survive
 * re-creating the screen (resizing, opening a picker and coming back).
 */
final class EditState {
    String level;
    boolean shiny;
    String gender;
    String nature;
    String ability;
    String friendship;
    String teraType;
    final String[] ivs = new String[PokemonEntry.STATS];
    final String[] evs = new String[PokemonEntry.STATS];
    final String[] baseStats = new String[PokemonEntry.STATS];
    final String[] moves = new String[PokemonEntry.MOVES];

    static EditState of(PokemonEntry entry) {
        EditState state = new EditState();
        state.level = Integer.toString(entry.level());
        state.shiny = entry.shiny();
        state.gender = entry.gender();
        state.nature = entry.nature();
        state.ability = entry.ability();
        state.friendship = Integer.toString(entry.friendship());
        state.teraType = entry.teraType();

        for (int i = 0; i < PokemonEntry.STATS; i++) {
            state.ivs[i] = Integer.toString(entry.ivs()[i]);
            state.evs[i] = Integer.toString(entry.evs()[i]);
            state.baseStats[i] = Integer.toString(entry.baseStats()[i]);
        }

        System.arraycopy(entry.moves(), 0, state.moves, 0, PokemonEntry.MOVES);
        return state;
    }

    static int parse(String text) {
        return text == null || text.isEmpty() ? 0 : Integer.parseInt(text);
    }
}
