package kr.pokeeditui;

import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.relocations.graalvm.polyglot.Context;
import com.cobblemon.mod.relocations.graalvm.polyglot.Value;

/**
 * Showdown computes battle stats itself from the species' base stats. Before a battle starts, the base stat changes
 * of its pokemon are handed to Showdown (by uuid), and Battle.spreadModify is wrapped once to apply them. Both go
 * through Showdown's receiveScriptData (evaluated in index.js, where require works) instead of editing Showdown's
 * files, which Cobblemon and Mega Showdown replace.
 */
public final class ShowdownBaseStats {
    private static final String PATCH = """
        var g = globalThis;
        g.__pokeeditBase = g.__pokeeditBase || {};
        var B = require("./sim/battle").Battle;
        if (!B.prototype.__pokeeditPatched) {
          var spreadModify = B.prototype.spreadModify;
          var keys = ["hp", "atk", "def", "spa", "spd", "spe"];
          B.prototype.spreadModify = function (baseStats, set) {
            var delta = set && set.uuid ? g.__pokeeditBase[set.uuid] : null;
            if (delta) {
              var stats = {};
              for (var k in baseStats) stats[k] = baseStats[k];
              for (var i = 0; i < keys.length; i++) {
                if (keys[i] in stats) stats[keys[i]] = Math.max(%d, stats[keys[i]] + delta[i]);
              }
              baseStats = stats;
            }
            return spreadModify.call(this, baseStats, set);
          };
          B.prototype.__pokeeditPatched = true;
        }
        """.formatted(BaseStats.MIN);

    private ShowdownBaseStats() {
    }

    /** Called on Showdown's thread right before the battle is created there. */
    public static void beforeBattle(Context context, PokemonBattle battle) {
        StringBuilder js = new StringBuilder("(function () {\n").append(PATCH);

        for (BattleActor actor : battle.getActors()) {
            for (BattlePokemon battlePokemon : actor.getPokemonList()) {
                Pokemon pokemon = battlePokemon.getEffectedPokemon();
                int[] delta = BaseStats.delta(pokemon);
                String key = "g.__pokeeditBase[\"" + pokemon.getUuid() + "\"]";

                if (delta != null) {
                    js.append(key).append(" = [");

                    for (int i = 0; i < delta.length; i++) {
                        js.append(i > 0 ? "," : "").append(delta[i]);
                    }

                    js.append("];\n");
                } else {
                    js.append("delete ").append(key).append(";\n");
                }
            }
        }

        js.append("return {};\n})()");

        try {
            Value receive = context.getBindings("js").getMember("receiveScriptData");
            receive.execute("pokeeditui", js.toString());
        } catch (RuntimeException e) {
            // never break the battle over this, it then just uses the form's base stats
            PokeEditUi.LOGGER.error("Could not send base stats to Showdown", e);
        }
    }
}
