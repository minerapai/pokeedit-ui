package kr.pokeeditui.mixin;

import com.cobblemon.mod.common.api.pokemon.stats.Stat;
import com.cobblemon.mod.common.pokemon.FormData;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.stat.CobblemonStatProvider;
import kr.pokeeditui.BaseStats;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Map;

/** Cobblemon's stat calculation (summary, max HP, ...) uses the pokemon's own base stats. Runs on both sides. */
@Mixin(value = CobblemonStatProvider.class, remap = false)
public abstract class StatProviderMixin {
    @Redirect(method = "getStatForPokemon",
        at = @At(value = "INVOKE", target = "Lcom/cobblemon/mod/common/pokemon/FormData;getBaseStats()Ljava/util/Map;"))
    private Map<Stat, Integer> pokeeditui$baseStats(FormData form, Pokemon pokemon, Stat stat) {
        return BaseStats.apply(form.getBaseStats(), pokemon);
    }
}
