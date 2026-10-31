package kr.pokeeditui.net;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.UUID;

/** Client -> server: the edited values of one party pokemon. */
public record ApplyEditPayload(
    UUID target,
    int slot,
    UUID pokemon,
    int level,
    boolean shiny,
    String gender,
    String nature,
    String ability,
    int friendship,
    String teraType,
    int[] ivs,
    int[] evs,
    int[] baseStats,
    String[] moves
) implements CustomPayload {
    public static final Id<ApplyEditPayload> ID = new Id<>(Identifier.of("pokeeditui", "apply_edit"));
    public static final PacketCodec<RegistryByteBuf, ApplyEditPayload> CODEC = PacketCodec.of(
        ApplyEditPayload::write, ApplyEditPayload::read);

    private void write(RegistryByteBuf buf) {
        buf.writeUuid(target);
        buf.writeVarInt(slot);
        buf.writeUuid(pokemon);
        buf.writeVarInt(level);
        buf.writeBoolean(shiny);
        buf.writeString(gender);
        buf.writeString(nature);
        buf.writeString(ability);
        buf.writeVarInt(friendship);
        buf.writeString(teraType);
        PokemonEntry.writeInts(buf, ivs);
        PokemonEntry.writeInts(buf, evs);
        PokemonEntry.writeInts(buf, baseStats);

        for (String move : moves) {
            buf.writeString(move);
        }
    }

    private static ApplyEditPayload read(RegistryByteBuf buf) {
        UUID target = buf.readUuid();
        int slot = buf.readVarInt();
        UUID pokemon = buf.readUuid();
        int level = buf.readVarInt();
        boolean shiny = buf.readBoolean();
        String gender = buf.readString();
        String nature = buf.readString();
        String ability = buf.readString();
        int friendship = buf.readVarInt();
        String teraType = buf.readString();
        int[] ivs = PokemonEntry.readInts(buf);
        int[] evs = PokemonEntry.readInts(buf);
        int[] baseStats = PokemonEntry.readInts(buf);
        String[] moves = new String[PokemonEntry.MOVES];

        for (int i = 0; i < moves.length; i++) {
            moves[i] = buf.readString();
        }

        return new ApplyEditPayload(target, slot, pokemon, level, shiny, gender, nature, ability, friendship,
            teraType, ivs, evs, baseStats, moves);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
