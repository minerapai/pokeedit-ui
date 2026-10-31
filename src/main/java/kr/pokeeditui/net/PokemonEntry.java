package kr.pokeeditui.net;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Editable state of one party pokemon, sent to the client and back (as {@link ApplyEditPayload}). */
public record PokemonEntry(
    int slot,
    UUID uuid,
    Text displayName,
    String species,
    int level,
    int maxLevel,
    boolean shiny,
    String gender,
    float maleRatio,
    String nature,
    String ability,
    List<AbilityOption> abilityOptions,
    int friendship,
    String teraType,
    int[] ivs,
    int[] evs,
    int[] formBase,
    int[] baseStats,
    String[] moves,
    List<String> learnableMoves
) {
    public static final int STATS = 6;
    public static final int MOVES = 4;

    /** An ability the species can have; displayKey is a translation key. */
    public record AbilityOption(String name, String displayKey, boolean hidden) {
    }

    public void write(RegistryByteBuf buf) {
        buf.writeVarInt(slot);
        buf.writeUuid(uuid);
        TextCodecs.REGISTRY_PACKET_CODEC.encode(buf, displayName);
        buf.writeString(species);
        buf.writeVarInt(level);
        buf.writeVarInt(maxLevel);
        buf.writeBoolean(shiny);
        buf.writeString(gender);
        buf.writeFloat(maleRatio);
        buf.writeString(nature);
        buf.writeString(ability);
        buf.writeVarInt(abilityOptions.size());

        for (AbilityOption option : abilityOptions) {
            buf.writeString(option.name());
            buf.writeString(option.displayKey());
            buf.writeBoolean(option.hidden());
        }

        buf.writeVarInt(friendship);
        buf.writeString(teraType);
        writeInts(buf, ivs);
        writeInts(buf, evs);
        writeInts(buf, formBase);
        writeInts(buf, baseStats);

        for (String move : moves) {
            buf.writeString(move);
        }

        buf.writeVarInt(learnableMoves.size());
        learnableMoves.forEach(buf::writeString);
    }

    public static PokemonEntry read(RegistryByteBuf buf) {
        int slot = buf.readVarInt();
        UUID uuid = buf.readUuid();
        Text displayName = TextCodecs.REGISTRY_PACKET_CODEC.decode(buf);
        String species = buf.readString();
        int level = buf.readVarInt();
        int maxLevel = buf.readVarInt();
        boolean shiny = buf.readBoolean();
        String gender = buf.readString();
        float maleRatio = buf.readFloat();
        String nature = buf.readString();
        String ability = buf.readString();
        int optionCount = buf.readVarInt();
        List<AbilityOption> options = new ArrayList<>(optionCount);

        for (int i = 0; i < optionCount; i++) {
            options.add(new AbilityOption(buf.readString(), buf.readString(), buf.readBoolean()));
        }

        int friendship = buf.readVarInt();
        String teraType = buf.readString();
        int[] ivs = readInts(buf);
        int[] evs = readInts(buf);
        int[] formBase = readInts(buf);
        int[] baseStats = readInts(buf);
        String[] moves = new String[MOVES];

        for (int i = 0; i < MOVES; i++) {
            moves[i] = buf.readString();
        }

        int learnableCount = buf.readVarInt();
        List<String> learnable = new ArrayList<>(learnableCount);

        for (int i = 0; i < learnableCount; i++) {
            learnable.add(buf.readString());
        }

        return new PokemonEntry(slot, uuid, displayName, species, level, maxLevel, shiny, gender, maleRatio,
            nature, ability, options, friendship, teraType, ivs, evs, formBase, baseStats, moves, learnable);
    }

    static void writeInts(RegistryByteBuf buf, int[] values) {
        for (int i = 0; i < STATS; i++) {
            buf.writeVarInt(values[i]);
        }
    }

    static int[] readInts(RegistryByteBuf buf) {
        int[] values = new int[STATS];

        for (int i = 0; i < STATS; i++) {
            values[i] = buf.readVarInt();
        }

        return values;
    }
}
