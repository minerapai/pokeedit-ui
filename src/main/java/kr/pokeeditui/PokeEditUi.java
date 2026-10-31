package kr.pokeeditui;

import kr.pokeeditui.net.ApplyEditPayload;
import kr.pokeeditui.net.OpenRequestPayload;
import kr.pokeeditui.net.PartySnapshotPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Only adds network payloads and a command (no blocks, items or other registry content),
 * so players without the mod can still join; they just cannot open the screen.
 */
public class PokeEditUi implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("pokeeditui");

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.playC2S().register(OpenRequestPayload.ID, OpenRequestPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ApplyEditPayload.ID, ApplyEditPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(PartySnapshotPayload.ID, PartySnapshotPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(OpenRequestPayload.ID,
            (payload, context) -> PartyEditor.open(context.player(), context.player()));
        ServerPlayNetworking.registerGlobalReceiver(ApplyEditPayload.ID,
            (payload, context) -> PartyEditor.apply(context.player(), payload));

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            dispatcher.register(CommandManager.literal("pokeeditui")
                .requires(source -> source.hasPermissionLevel(PartyEditor.PERMISSION_LEVEL))
                .executes(ctx -> {
                    ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
                    return PartyEditor.open(player, player) ? 1 : 0;
                })
                .then(CommandManager.literal("trainer")
                    .executes(ctx -> {
                        ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
                        return PartyEditor.openTrainer(player, player) ? 1 : 0;
                    })
                    .then(CommandManager.argument("player", EntityArgumentType.player())
                        .executes(ctx -> PartyEditor.openTrainer(ctx.getSource().getPlayerOrThrow(),
                            EntityArgumentType.getPlayer(ctx, "player")) ? 1 : 0)))
                .then(CommandManager.argument("player", EntityArgumentType.player())
                    .executes(ctx -> PartyEditor.open(ctx.getSource().getPlayerOrThrow(),
                        EntityArgumentType.getPlayer(ctx, "player")) ? 1 : 0))));
    }
}
