package kr.pokeeditui;

import com.cobblemon.mod.common.pokemon.Pokemon;
import kr.poketrainer.PokeTrainer;
import kr.poketrainer.TrainerBattles;
import kr.poketrainer.TrainerData;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;

/**
 * The pokemon a player handed to their poke-trainer trainer. Only loaded when that mod is installed (see
 * PartyEditor.trainerInstalled), so pokeedit-ui runs without it.
 */
final class TrainerBridge {
    private TrainerBridge() {
    }

    /** The trainer's name, or null when the player has none. */
    static String name(ServerPlayerEntity player) {
        TrainerData.Record record = record(player);
        return record != null ? record.name : null;
    }

    /** The trainer's team (live list), empty when there is none. */
    static List<Pokemon> team(ServerPlayerEntity player) {
        TrainerData.Record record = record(player);
        return record != null ? record.team : List.of();
    }

    static boolean busy(ServerPlayerEntity player) {
        return TrainerBattles.busy(player.getUuid());
    }

    static void save(ServerPlayerEntity player) {
        if (PokeTrainer.data() != null) {
            PokeTrainer.data().save(player.getUuid());
        }
    }

    private static TrainerData.Record record(ServerPlayerEntity player) {
        try {
            return PokeTrainer.data() != null ? PokeTrainer.data().get(player.getUuid()) : null;
        } catch (IllegalStateException e) {
            return null; // the trainer file could not be read: leave it alone
        }
    }
}
