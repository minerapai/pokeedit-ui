package kr.pokeeditui.net;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Client -> server: open the edit screen for the own party (sent by the key binding). */
public record OpenRequestPayload() implements CustomPayload {
    public static final Id<OpenRequestPayload> ID = new Id<>(Identifier.of("pokeeditui", "open_request"));
    public static final PacketCodec<RegistryByteBuf, OpenRequestPayload> CODEC = PacketCodec.unit(new OpenRequestPayload());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
