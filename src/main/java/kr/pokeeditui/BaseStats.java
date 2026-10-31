package kr.pokeeditui;

import com.cobblemon.mod.common.api.pokemon.stats.Stat;
import com.cobblemon.mod.common.pokemon.Pokemon;
import net.minecraft.nbt.NbtCompound;

import java.util.HashMap;
import java.util.Map;

/**
 * Per pokemon base stats. Stored as the difference to the base stats of its form (in the pokemon's persistent data,
 * which Cobblemon saves and syncs to the client), so it carries over to other forms (mega evolution, ...) and evolution.
 * Applied to Cobblemon's stat calculation (StatProviderMixin) and to Showdown's (GraalShowdownServiceMixin).
 */
public final class BaseStats {
    public static final String KEY = "pokeeditui_base_delta";
    public static final int MIN = 1;
    public static final int MAX = 999;

    private BaseStats() {
    }

    /** Difference per stat (HP, Atk, Def, SpA, SpD, Spe), or null when the pokemon has its form's base stats. */
    public static int[] delta(Pokemon pokemon) {
        NbtCompound data = pokemon.getPersistentData();

        if (data == null) { // not set yet while the pokemon is being constructed
            return null;
        }

        int[] delta = data.getIntArray(KEY);
        return delta.length == PartyEditor.STATS.length ? delta : null;
    }

    public static void setDelta(Pokemon pokemon, int[] delta) {
        boolean any = false;

        for (int value : delta) {
            any |= value != 0;
        }

        if (any) {
            pokemon.getPersistentData().putIntArray(KEY, delta.clone());
        } else {
            pokemon.getPersistentData().remove(KEY);
        }
    }

    /** The base stats of the pokemon's form, without the per pokemon change. */
    public static int[] formBase(Pokemon pokemon) {
        Map<Stat, Integer> stats = pokemon.getForm().getBaseStats();
        int[] base = new int[PartyEditor.STATS.length];

        for (int i = 0; i < base.length; i++) {
            base[i] = stats.getOrDefault(PartyEditor.STATS[i], 0);
        }

        return base;
    }

    /** The pokemon's own base stats (form base stats plus its change). */
    public static int[] effective(Pokemon pokemon) {
        int[] base = formBase(pokemon);
        int[] delta = delta(pokemon);

        if (delta != null) {
            for (int i = 0; i < base.length; i++) {
                base[i] = Math.max(MIN, base[i] + delta[i]);
            }
        }

        return base;
    }

    /** formStats with the pokemon's change applied; formStats itself when it has none. */
    public static Map<Stat, Integer> apply(Map<Stat, Integer> formStats, Pokemon pokemon) {
        int[] delta = pokemon != null ? delta(pokemon) : null;

        if (delta == null) {
            return formStats;
        }

        Map<Stat, Integer> stats = new HashMap<>(formStats);

        for (int i = 0; i < delta.length; i++) {
            Stat stat = PartyEditor.STATS[i];
            Integer value = stats.get(stat);

            if (value != null) {
                stats.put(stat, Math.max(MIN, value + delta[i]));
            }
        }

        return stats;
    }
}
