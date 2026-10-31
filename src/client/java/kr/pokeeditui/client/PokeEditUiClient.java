package kr.pokeeditui.client;

import kr.pokeeditui.net.OpenRequestPayload;
import kr.pokeeditui.net.PartySnapshotPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

public class PokeEditUiClient implements ClientModInitializer {
    private static KeyBinding openKey;

    @Override
    public void onInitializeClient() {
        openKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.pokeeditui.open", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_U, "category.pokeeditui"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openKey.wasPressed()) {
                requestOpen(client);
            }
        });

        ClientPlayNetworking.registerGlobalReceiver(PartySnapshotPayload.ID, (payload, context) -> {
            MinecraftClient client = context.client();

            if (client.currentScreen instanceof PokeEditScreen screen && screen.isFor(payload.target())) {
                screen.update(payload);
            } else if (client.currentScreen instanceof PickerScreen picker && picker.parent().isFor(payload.target())) {
                picker.parent().update(payload);
            } else {
                client.setScreen(new PokeEditScreen(payload));
            }
        });
    }

    private static void requestOpen(MinecraftClient client) {
        if (client.player == null) {
            return;
        }

        if (ClientPlayNetworking.canSend(OpenRequestPayload.ID)) {
            ClientPlayNetworking.send(new OpenRequestPayload());
        } else {
            client.player.sendMessage(Text.translatable("pokeeditui.message.server_missing").formatted(Formatting.RED), false);
        }
    }
}
