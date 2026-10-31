package kr.pokeeditui;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.abilities.Abilities;
import com.cobblemon.mod.common.api.abilities.AbilityTemplate;
import com.cobblemon.mod.common.api.abilities.PotentialAbility;
import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.api.moves.MoveSet;
import com.cobblemon.mod.common.api.moves.MoveTemplate;
import com.cobblemon.mod.common.api.moves.Moves;
import com.cobblemon.mod.common.api.pokemon.Natures;
import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import com.cobblemon.mod.common.api.storage.party.PartyPosition;
import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore;
import com.cobblemon.mod.common.api.types.tera.TeraType;
import com.cobblemon.mod.common.api.types.tera.TeraTypes;
import com.cobblemon.mod.common.battles.BattleRegistry;
import com.cobblemon.mod.common.net.messages.client.storage.party.SetPartyPokemonPacket;
import com.cobblemon.mod.common.pokemon.Gender;
import com.cobblemon.mod.common.pokemon.Nature;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.abilities.HiddenAbility;
import kotlin.Unit;
import kr.pokeeditui.net.ApplyEditPayload;
import kr.pokeeditui.net.PartySnapshotPayload;
import kr.pokeeditui.net.PokemonEntry;
import kr.pokeeditui.net.PokemonEntry.AbilityOption;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Server side: builds party snapshots and applies edits, mirroring what /pokeedit can change. */
public final class PartyEditor {
    public static final int PERMISSION_LEVEL = 2;
    public static final Stats[] STATS = {
        Stats.HP, Stats.ATTACK, Stats.DEFENCE, Stats.SPECIAL_ATTACK, Stats.SPECIAL_DEFENCE, Stats.SPEED
    };
    private static final int MAX_IV = 31;
    private static final int MAX_EV = 252;
    private static final int MAX_EV_TOTAL = 510;
    private static final int MAX_FRIENDSHIP = 255;
    /** Slots from here on are the pokemon of the player's poke-trainer trainer (slot - TRAINER_SLOT = team index). */
    public static final int TRAINER_SLOT = 100;

    public static boolean trainerInstalled() {
        return net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("poketrainer");
    }

    private PartyEditor() {
    }

    /** Sends the party of target to viewer, if viewer may edit and has the client mod. */
    public static boolean open(ServerPlayerEntity viewer, ServerPlayerEntity target) {
        if (!viewer.hasPermissionLevel(PERMISSION_LEVEL)) {
            viewer.sendMessage(Text.translatable("pokeeditui.message.no_permission").formatted(Formatting.RED));
            return false;
        }

        if (!ServerPlayNetworking.canSend(viewer, PartySnapshotPayload.ID)) {
            viewer.sendMessage(Text.translatable("pokeeditui.message.client_missing").formatted(Formatting.RED));
            return false;
        }

        ServerPlayNetworking.send(viewer, snapshot(target));
        return true;
    }

    /** Sends the pokemon of target's trainer (poke-trainer) to viewer, in the same screen as a party. */
    public static boolean openTrainer(ServerPlayerEntity viewer, ServerPlayerEntity target) {
        if (!viewer.hasPermissionLevel(PERMISSION_LEVEL)) {
            viewer.sendMessage(Text.translatable("pokeeditui.message.no_permission").formatted(Formatting.RED));
            return false;
        }

        if (!ServerPlayNetworking.canSend(viewer, PartySnapshotPayload.ID)) {
            viewer.sendMessage(Text.translatable("pokeeditui.message.client_missing").formatted(Formatting.RED));
            return false;
        }

        if (!trainerInstalled() || TrainerBridge.name(target) == null) {
            viewer.sendMessage(Text.literal(target.getGameProfile().getName() + " 님은 트레이너가 없습니다.").formatted(Formatting.RED));
            return false;
        }

        if (TrainerBridge.team(target).isEmpty()) {
            viewer.sendMessage(Text.literal("트레이너에게 맡긴 포켓몬이 없습니다.").formatted(Formatting.RED));
            return false;
        }

        ServerPlayNetworking.send(viewer, trainerSnapshot(target));
        return true;
    }

    public static PartySnapshotPayload trainerSnapshot(ServerPlayerEntity target) {
        int maxLevel = Cobblemon.INSTANCE.getConfig().getMaxPokemonLevel();
        List<PokemonEntry> entries = new ArrayList<>();
        List<Pokemon> team = TrainerBridge.team(target);

        for (int i = 0; i < team.size(); i++) {
            entries.add(entryOf(TRAINER_SLOT + i, team.get(i), maxLevel));
        }

        // the screen titles this "<name>의 파티"
        return new PartySnapshotPayload(target.getUuid(), target.getGameProfile().getName() + "의 트레이너 "
            + TrainerBridge.name(target), entries);
    }

    public static PartySnapshotPayload snapshot(ServerPlayerEntity target) {
        PlayerPartyStore party = Cobblemon.INSTANCE.getStorage().getParty(target);
        int maxLevel = Cobblemon.INSTANCE.getConfig().getMaxPokemonLevel();
        List<PokemonEntry> entries = new ArrayList<>();

        for (int slot = 0; slot < party.size(); slot++) {
            Pokemon pokemon = party.get(slot);

            if (pokemon != null) {
                entries.add(entryOf(slot, pokemon, maxLevel));
            }
        }

        return new PartySnapshotPayload(target.getUuid(), target.getGameProfile().getName(), entries);
    }

    private static PokemonEntry entryOf(int slot, Pokemon pokemon, int maxLevel) {
        List<AbilityOption> abilities = new ArrayList<>();
        String current = pokemon.getAbility().getName();
        boolean hasCurrent = false;

        for (PotentialAbility potential : pokemon.getForm().getAbilities()) {
            AbilityTemplate template = potential.getTemplate();

            if (abilities.stream().noneMatch(a -> a.name().equals(template.getName()))) {
                abilities.add(new AbilityOption(template.getName(), template.getDisplayName(), potential instanceof HiddenAbility));
                hasCurrent |= template.getName().equals(current);
            }
        }

        if (!hasCurrent) {
            abilities.add(new AbilityOption(current, pokemon.getAbility().getTemplate().getDisplayName(), false));
        }

        int[] ivs = new int[PokemonEntry.STATS];
        int[] evs = new int[PokemonEntry.STATS];

        for (int i = 0; i < STATS.length; i++) {
            ivs[i] = pokemon.getIvs().getOrDefault(STATS[i]);
            evs[i] = pokemon.getEvs().getOrDefault(STATS[i]);
        }

        String[] moves = new String[PokemonEntry.MOVES];
        List<Move> moveList = pokemon.getMoveSet().getMovesWithNulls();

        for (int i = 0; i < moves.length; i++) {
            Move move = i < moveList.size() ? moveList.get(i) : null;
            moves[i] = move != null ? move.getName() : "";
        }

        return new PokemonEntry(
            slot,
            pokemon.getUuid(),
            pokemon.getDisplayName(false),
            pokemon.getSpecies().getResourceIdentifier().toString(),
            pokemon.getLevel(),
            maxLevel,
            pokemon.getShiny(),
            pokemon.getGender().name(),
            pokemon.getForm().getMaleRatio(),
            pokemon.getNature().getName().toString(),
            current,
            abilities,
            pokemon.getFriendship(),
            pokemon.getTeraType().getId().toString(),
            ivs,
            evs,
            BaseStats.formBase(pokemon),
            BaseStats.effective(pokemon),
            moves,
            pokemon.getForm().getMoves().getAllLegalMoves().stream()
                .map(MoveTemplate::getName)
                .sorted()
                .toList());
    }

    public static void apply(ServerPlayerEntity editor, ApplyEditPayload edit) {
        if (!editor.hasPermissionLevel(PERMISSION_LEVEL)) {
            editor.sendMessage(Text.translatable("pokeeditui.message.no_permission").formatted(Formatting.RED));
            return;
        }

        ServerPlayerEntity target = editor.getServer().getPlayerManager().getPlayer(edit.target());

        if (target == null) {
            editor.sendMessage(Text.translatable("pokeeditui.message.target_offline").formatted(Formatting.RED));
            return;
        }

        if (BattleRegistry.getBattleByParticipatingPlayer(target) != null) {
            editor.sendMessage(Text.translatable("pokeeditui.message.in_battle").formatted(Formatting.RED));
            return;
        }

        boolean trainer = edit.slot() >= TRAINER_SLOT;
        PlayerPartyStore party = Cobblemon.INSTANCE.getStorage().getParty(target);
        Pokemon pokemon;

        if (trainer) {
            if (!trainerInstalled()) {
                return;
            }

            if (TrainerBridge.busy(target)) {
                editor.sendMessage(Text.literal("트레이너가 배틀 중에는 포켓몬을 바꿀 수 없습니다.").formatted(Formatting.RED));
                return;
            }

            List<Pokemon> team = TrainerBridge.team(target);
            int index = edit.slot() - TRAINER_SLOT;
            pokemon = index < team.size() ? team.get(index) : null;
        } else {
            pokemon = edit.slot() >= 0 && edit.slot() < party.size() ? party.get(edit.slot()) : null;
        }

        if (pokemon == null || !pokemon.getUuid().equals(edit.pokemon())) {
            editor.sendMessage(Text.translatable("pokeeditui.message.pokemon_changed").formatted(Formatting.RED));
            ServerPlayNetworking.send(editor, trainer ? trainerSnapshot(target) : snapshot(target));
            return;
        }

        // validate everything first, so an edit is applied completely or not at all
        int maxLevel = Cobblemon.INSTANCE.getConfig().getMaxPokemonLevel();
        int level = clamp(edit.level(), 1, maxLevel);

        Gender gender;
        try {
            gender = Gender.valueOf(edit.gender().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            fail(editor, "pokeeditui.message.invalid_gender");
            return;
        }

        float maleRatio = pokemon.getForm().getMaleRatio();
        boolean genderAllowed = maleRatio < 0 ? gender == Gender.GENDERLESS
            : maleRatio == 0 ? gender == Gender.FEMALE
            : maleRatio == 1 ? gender == Gender.MALE
            : gender != Gender.GENDERLESS;

        if (!genderAllowed) {
            fail(editor, "pokeeditui.message.invalid_gender");
            return;
        }

        Identifier natureId = Identifier.tryParse(edit.nature());
        Nature nature = natureId != null ? Natures.getNature(natureId) : null;

        if (nature == null) {
            fail(editor, "pokeeditui.message.invalid_nature");
            return;
        }

        AbilityTemplate abilityTemplate = Abilities.get(edit.ability());

        if (abilityTemplate == null) {
            fail(editor, "pokeeditui.message.invalid_ability");
            return;
        }

        Identifier teraId = Identifier.tryParse(edit.teraType());
        TeraType teraType = teraId != null ? TeraTypes.get(teraId) : null;

        if (teraType == null) {
            fail(editor, "pokeeditui.message.invalid_tera");
            return;
        }

        int[] ivs = new int[PokemonEntry.STATS];
        int[] evs = new int[PokemonEntry.STATS];
        int evTotal = 0;

        for (int i = 0; i < PokemonEntry.STATS; i++) {
            ivs[i] = clamp(edit.ivs()[i], 0, MAX_IV);
            evs[i] = clamp(edit.evs()[i], 0, MAX_EV);
            evTotal += evs[i];
        }

        if (evTotal > MAX_EV_TOTAL) {
            fail(editor, "pokeeditui.message.ev_total");
            return;
        }

        int[] formBase = BaseStats.formBase(pokemon);
        int[] baseDelta = new int[PokemonEntry.STATS];

        for (int i = 0; i < PokemonEntry.STATS; i++) {
            int base = edit.baseStats()[i];

            if (base < BaseStats.MIN || base > BaseStats.MAX) {
                fail(editor, "pokeeditui.message.invalid_base");
                return;
            }

            baseDelta[i] = base - formBase[i];
        }

        List<Move> currentMoves = pokemon.getMoveSet().getMovesWithNulls();
        List<Move> moves = new ArrayList<>();

        for (int i = 0; i < PokemonEntry.MOVES; i++) {
            String name = edit.moves()[i].trim().toLowerCase(Locale.ROOT);

            if (name.isEmpty()) {
                continue;
            }

            Move current = i < currentMoves.size() ? currentMoves.get(i) : null;

            if (current != null && current.getName().equals(name)) {
                moves.add(current); // unchanged, keep its PP and PP ups
                continue;
            }

            MoveTemplate template = Moves.getByName(name);

            if (template == null) {
                editor.sendMessage(Text.translatable("pokeeditui.message.invalid_move", name).formatted(Formatting.RED));
                return;
            }

            moves.add(template.create());
        }

        if (moves.isEmpty()) {
            fail(editor, "pokeeditui.message.no_moves");
            return;
        }

        // apply
        pokemon.setLevel(level);
        pokemon.setShiny(edit.shiny());
        pokemon.setGender(gender);
        pokemon.setNature(nature);

        if (!abilityTemplate.getName().equals(pokemon.getAbility().getName())) {
            PotentialAbility potential = null;

            for (PotentialAbility candidate : pokemon.getForm().getAbilities()) {
                if (candidate.getTemplate().getName().equals(abilityTemplate.getName())) {
                    potential = candidate;
                    break;
                }
            }

            pokemon.updateAbility(potential != null
                ? abilityTemplate.create(false, potential.getPriority())
                : abilityTemplate.create(true, Priority.LOWEST));
        }

        pokemon.setFriendship(clamp(edit.friendship(), 0, MAX_FRIENDSHIP), true);
        pokemon.setTeraType(teraType);

        for (int i = 0; i < STATS.length; i++) {
            pokemon.getIvs().set(STATS[i], ivs[i]);
            pokemon.getEvs().set(STATS[i], 0); // reset first so the EV total never overflows midway
        }

        for (int i = 0; i < STATS.length; i++) {
            pokemon.getEvs().set(STATS[i], evs[i]);
        }

        boolean baseChanged = !Arrays.equals(baseDelta,
            Objects.requireNonNullElse(BaseStats.delta(pokemon), new int[PokemonEntry.STATS]));

        if (baseChanged) {
            BaseStats.setDelta(pokemon, baseDelta);
            pokemon.heal(); // max HP changed, start from full instead of an odd fraction
        }

        MoveSet moveSet = pokemon.getMoveSet();
        moveSet.doWithoutEmitting(() -> {
            moveSet.clear();
            moves.forEach(moveSet::add);
            return Unit.INSTANCE;
        });
        moveSet.update();

        if (trainer) {
            TrainerBridge.save(target);
        } else if (baseChanged) {
            // the persistent data has no update packet of its own, resend the whole pokemon so summaries show the new stats
            new SetPartyPokemonPacket(party.getUuid(), new PartyPosition(edit.slot()), registries -> pokemon)
                .sendToPlayers(party.getObservingPlayers());
        }

        editor.sendMessage(Text.translatable("pokeeditui.message.applied", pokemon.getDisplayName(false))
            .formatted(Formatting.GREEN));

        if (target != editor) {
            PokeEditUi.LOGGER.info("{} edited {} of {}", editor.getGameProfile().getName(),
                pokemon.getSpecies().getName(), target.getGameProfile().getName());
        }

        ServerPlayNetworking.send(editor, trainer ? trainerSnapshot(target) : snapshot(target));
    }

    private static void fail(ServerPlayerEntity editor, String key) {
        editor.sendMessage(Text.translatable(key).formatted(Formatting.RED));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
