package kr.pokeeditui.mixin;

import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.battles.runner.graal.GraalShowdownService;
import com.cobblemon.mod.relocations.graalvm.polyglot.Context;
import kr.pokeeditui.ShowdownBaseStats;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hands the pokemon's own base stats to Showdown before it builds the battle. */
@Mixin(value = GraalShowdownService.class, remap = false)
public abstract class GraalShowdownServiceMixin {
    @Shadow
    public abstract Context getContext();

    @Inject(method = "startBattle", at = @At("HEAD"))
    private void pokeeditui$baseStats(PokemonBattle battle, String[] messages, CallbackInfo ci) {
        ShowdownBaseStats.beforeBattle(getContext(), battle);
    }
}
