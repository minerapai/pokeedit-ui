package kr.pokeeditui.net;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Server -> client: the party of the target player, opens (or refreshes) the edit screen. */
public record PartySnapshotPayload(UUID target, String targetName, List<PokemonEntry> party) implements CustomPayload {
    public static final Id<PartySnapshotPayload> ID = new Id<>(Identifier.of("pokeeditui", "party_snapshot"));
    public static final PacketCodec<RegistryByteBuf, PartySnapshotPayload> CODEC = PacketCodec.of(
        PartySnapshotPayload::write, PartySnapshotPayload::read);

    private void write(RegistryByteBuf buf) {
        buf.writeUuid(target);
        buf.writeString(targetName);
        buf.writeVarInt(party.size());
        party.forEach(entry -> entry.write(buf));
    }

    private static PartySnapshotPayload read(RegistryByteBuf buf) {
        UUID target = buf.readUuid();
        String targetName = buf.readString();
        int size = buf.readVarInt();
        List<PokemonEntry> party = new ArrayList<>(size);

        for (int i = 0; i < size; i++) {
            party.add(PokemonEntry.read(buf));
        }

        return new PartySnapshotPayload(target, targetName, party);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
