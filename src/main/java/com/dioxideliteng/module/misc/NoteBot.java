package com.dioxideliteng.module.misc;

import com.dioxideliteng.event.Event;
import com.dioxideliteng.event.Events;
import com.dioxideliteng.module.Category;
import com.dioxideliteng.module.Feature;
import com.dioxideliteng.setting.BooleanSetting;
import com.dioxideliteng.setting.ModeSetting;
import com.dioxideliteng.setting.NumberSetting;
import com.dioxideliteng.setting.TextSetting;
import com.dioxideliteng.util.krs.BehaviorUtils;
import com.dioxideliteng.util.krs.ChatUtil;
import com.dioxideliteng.util.krs.PacketUtil;
import com.dioxideliteng.util.krs.PlayerUtil;
import com.dioxideliteng.util.krs.RotationManager;
import com.dioxideliteng.util.krs.notebot.NbsSongDecoder;
import com.dioxideliteng.util.krs.notebot.Note;
import com.dioxideliteng.util.krs.notebot.Song;
import com.mojang.logging.LogUtils;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import mixins.MultiPlayerGameModeAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

/**
 * Ported from Krs (com.instrumentalist.krs.hacks.features.level.NoteBot).
 * Krs parity ("Loads an NBS file, tunes nearby note blocks, and plays the song"):
 * loads an .nbs song asynchronously, scans reachable note blocks with air above,
 * maps required notes (exact instrument first, then tunable), tunes them via
 * use-item sequences at Tune Delay with Concurrent Tune Blocks per tick, verifies
 * after Recheck Delay, then plays each song tick with START_DESTROY_BLOCK
 * sequences — polyphonically or one note at a time, optionally looping.
 * Notable adaptations vs the original:
 *  - mc.gameMode.startPrediction is private in 26.3; calls go through
 *    mixins/MultiPlayerGameModeAccessor#invokeStartPrediction.
 *  - Client.rotationManager replaced with RotationManager.INSTANCE.
 *  - Krs tempEnabled/setState replaced with isEnabled/setEnabled.
 *  - Krs ChatUtil.printChat replaced with the ported util.krs.ChatUtil.printChat.
 *  - player.swing(InteractionHand.MAIN_HAND) replaced with the 26.3 three-arg
 *    swing(hand, SwingAnimation.DEFAULT, true).
 *  - Krs tag() replaced with setSuffix (recomputed on tick, same content).
 *  - Krs description() has no Feature equivalent and is dropped.
 */
public class NoteBot extends Feature {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final long LOAD_TIMEOUT_NANOS = 60_000_000_000L;

    private final TextSetting nbsPath = new TextSetting("NBS Path", this, "");
    private final ModeSetting mode = new ModeSetting("Mode", this, "Exact Instruments",
            new String[]{"Exact Instruments", "Any Instrument"});
    private final ModeSetting instrumentDetect = new ModeSetting("Instrument Detect", this, "Block State",
            new String[]{"Block State", "Below Block"});
    private final NumberSetting tuneDelay = new NumberSetting("Tune Delay", this, 1.0, 1.0, 20.0, 1.0);
    private final NumberSetting concurrentTuneBlocks = new NumberSetting("Concurrent Tune Blocks", this, 1.0, 1.0, 20.0, 1.0);
    private final NumberSetting recheckDelay = new NumberSetting("Recheck Delay", this, 10.0, 1.0, 40.0, 1.0);
    private final BooleanSetting polyphonic = new BooleanSetting("Polyphonic", this, true);
    private final BooleanSetting autoRotate = new BooleanSetting("Auto Rotate", this, true);
    private final BooleanSetting swingArm = new BooleanSetting("Swing Arm", this, true);
    private final BooleanSetting roundOutOfRange = new BooleanSetting("Round Out Of Range", this, false);
    private final BooleanSetting loop = new BooleanSetting("Loop", this, false);

    private final Map<Note, BlockPos> noteBlockPositions = new LinkedHashMap<>();
    private final Map<BlockPos, Integer> tuneHits = new LinkedHashMap<>();
    private CompletableFuture<Song> loadingSong;
    private Song song;
    private Stage stage = Stage.Idle;
    private long loadStartedAt;
    private int currentTick;
    private int tuneTickCounter;
    private int waitTicks;
    private boolean tunedSinceCheck;
    private boolean activeExactInstruments;
    private boolean activeBelowBlockDetection;
    private boolean controllingRotation;

    private final RotationManager rotationManager = RotationManager.INSTANCE;

    public NoteBot() {
        super("Note Bot", Category.MISC);
        this.instrumentDetect.setVisible(this::isExactModeSelected);
    }

    @Override
    public void onEnable() {
        resetRuntime();

        if (mc.player == null || mc.level == null || mc.gameMode == null) {
            failAndDisable("Join a world before enabling the module.", null);
            return;
        }

        Path path = resolveSongPath();
        if (path == null)
            return;

        activeExactInstruments = isExactModeSelected();
        activeBelowBlockDetection = this.instrumentDetect.is("Below Block");
        boolean exactInstruments = activeExactInstruments;
        boolean shouldRoundOutOfRange = this.roundOutOfRange.getValue();
        stage = Stage.Loading;
        loadStartedAt = System.nanoTime();
        notifyPlayer("Loading " + path.getFileName() + "...");
        loadingSong = CompletableFuture.supplyAsync(() -> {
            try {
                return NbsSongDecoder.decode(path, exactInstruments, shouldRoundOutOfRange);
            } catch (Exception exception) {
                throw new CompletionException(exception);
            }
        });
    }

    @Override
    public void onDisable() {
        boolean shouldReleaseRotation = controllingRotation;
        resetRuntime();
        BehaviorUtils.noKillAura = false;
        if (shouldReleaseRotation && this.rotationManager != null)
            this.rotationManager.stopRotation();
    }

    @Override
    public void onEvent(Event event) {
        if (event == Events.WORLD) {
            if (isEnabled())
                setEnabled(false);
        } else if (event == Events.TICK) {
            if (mc.player == null || mc.level == null || mc.gameMode == null) {
                failAndDisable("World is no longer available.", null);
                return;
            }

            this.setSuffix(buildTag());

            BehaviorUtils.noKillAura = true;
            if (!this.autoRotate.getValue() && controllingRotation) {
                this.rotationManager.stopRotation();
                controllingRotation = false;
            }

            switch (stage) {
                case Loading -> pollSongLoad();
                case SetUp -> setUpNoteBlocks();
                case Tuning -> tuneNoteBlocks();
                case WaitingToVerify -> waitToVerifyTuning();
                case Playing -> playCurrentTick();
                case Idle -> {
                }
            }
        }
    }

    private String buildTag() {
        if (stage == Stage.Playing && song != null)
            return currentTick + "/" + song.lastTick();
        return stage.displayName;
    }

    private Path resolveSongPath() {
        String rawPath = this.nbsPath.getValue() == null ? "" : this.nbsPath.getValue().trim();
        if (rawPath.length() >= 2) {
            char first = rawPath.charAt(0);
            char last = rawPath.charAt(rawPath.length() - 1);
            if (first == last && (first == '\"' || first == '\''))
                rawPath = rawPath.substring(1, rawPath.length() - 1).trim();
        }

        if (rawPath.isEmpty()) {
            failAndDisable("Set NBS Path before enabling the module.", null);
            return null;
        }

        final Path configuredPath;
        try {
            configuredPath = Path.of(rawPath);
        } catch (InvalidPathException exception) {
            failAndDisable("NBS Path is invalid: " + exception.getMessage(), exception);
            return null;
        }

        Path resolvedPath = configuredPath.isAbsolute()
                ? configuredPath.normalize()
                : mc.gameDirectory.toPath().resolve(configuredPath).normalize();
        String fileName = resolvedPath.getFileName() == null ? "" : resolvedPath.getFileName().toString();

        if (!fileName.toLowerCase(Locale.ROOT).endsWith(".nbs")) {
            failAndDisable("NBS Path must point to an .nbs file.", null);
            return null;
        }
        if (!Files.isRegularFile(resolvedPath) || !Files.isReadable(resolvedPath)) {
            failAndDisable("NBS file was not found or is not readable: " + resolvedPath, null);
            return null;
        }

        return resolvedPath;
    }

    private void pollSongLoad() {
        CompletableFuture<Song> future = loadingSong;
        if (future == null) {
            failAndDisable("Song loading stopped unexpectedly.", null);
            return;
        }

        if (!future.isDone()) {
            if (System.nanoTime() - loadStartedAt > LOAD_TIMEOUT_NANOS) {
                future.cancel(true);
                failAndDisable("Loading the NBS file timed out.", null);
            }
            return;
        }

        loadingSong = null;
        try {
            song = future.join();
        } catch (CancellationException exception) {
            failAndDisable("Loading the NBS file was cancelled.", exception);
            return;
        } catch (CompletionException exception) {
            Throwable cause = exception.getCause() == null ? exception : exception.getCause();
            failAndDisable("Could not load the NBS file: " + readableMessage(cause), cause);
            return;
        }

        String songName = song.title().isBlank() ? "NBS song" : displayMetadata(song.title());
        String author = song.author().isBlank() ? "" : " by " + displayMetadata(song.author());
        notifyPlayer("Loaded " + songName + author + " (" + song.requirements().size() + " unique notes).");

        if (song.ignoredCustomNotes() > 0)
            notifyPlayer("Ignored " + song.ignoredCustomNotes() + " custom-instrument note(s).");
        if (song.ignoredOutOfRangeNotes() > 0)
            notifyPlayer("Ignored " + song.ignoredOutOfRangeNotes() + " out-of-range note(s).");
        if (song.roundedOutOfRangeNotes() > 0)
            notifyPlayer("Rounded " + song.roundedOutOfRangeNotes() + " out-of-range note(s).");

        stage = Stage.SetUp;
    }

    private void setUpNoteBlocks() {
        List<ScannedNoteBlock> scannedBlocks = scanForNoteBlocks();
        if (scannedBlocks.isEmpty()) {
            failAndDisable("No reachable note blocks with air above them were found.", null);
            return;
        }

        mapRequiredNotes(scannedBlocks);
        if (noteBlockPositions.isEmpty()) {
            failAndDisable("Nearby note blocks cannot play any notes required by this song.", null);
            return;
        }

        if (!prepareTuneHits())
            return;

        int missingNotes = song.requirements().size() - noteBlockPositions.size();
        String missingSuffix = missingNotes > 0 ? "; " + missingNotes + " missing" : "";
        notifyPlayer("Mapped " + noteBlockPositions.size() + "/" + song.requirements().size() + " unique notes" + missingSuffix + ".");
        stage = Stage.Tuning;
    }

    private List<ScannedNoteBlock> scanForNoteBlocks() {
        List<ScannedNoteBlock> scannedBlocks = new ArrayList<>();
        int radius = (int) Math.ceil(mc.player.blockInteractionRange()) + 1;
        BlockPos origin = mc.player.blockPosition();

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = origin.offset(x, y, z);
                    BlockState state = mc.level.getBlockState(pos);
                    if (state.getBlock() != Blocks.NOTE_BLOCK || !mc.level.getBlockState(pos.above()).isAir())
                        continue;
                    if (!mc.player.isWithinBlockInteractionRange(pos, 1.0))
                        continue;

                    NoteBlockInstrument instrument = null;
                    if (activeExactInstruments) {
                        instrument = activeBelowBlockDetection
                                ? mc.level.getBlockState(pos.below()).instrument()
                                : state.getValue(NoteBlock.INSTRUMENT);
                    }

                    scannedBlocks.add(new ScannedNoteBlock(
                            pos.immutable(),
                            new Note(instrument, state.getValue(NoteBlock.NOTE)),
                            mc.player.distanceToSqr(Vec3.atCenterOf(pos))
                    ));
                }
            }
        }

        scannedBlocks.sort(Comparator.comparingDouble(ScannedNoteBlock::distanceSquared));
        return scannedBlocks;
    }

    private void mapRequiredNotes(List<ScannedNoteBlock> scannedBlocks) {
        noteBlockPositions.clear();
        List<Note> requiredNotes = new ArrayList<>(song.requirements());
        requiredNotes.sort(Comparator
                .comparingInt((Note note) -> note.instrument() == null ? -1 : note.instrument().ordinal())
                .thenComparingInt(Note::level));

        Set<BlockPos> usedPositions = new LinkedHashSet<>();

        for (Note required : requiredNotes) {
            ScannedNoteBlock exactMatch = findAvailableBlock(scannedBlocks, usedPositions, required, true);
            if (exactMatch != null) {
                noteBlockPositions.put(required, exactMatch.pos());
                usedPositions.add(exactMatch.pos());
            }
        }

        for (Note required : requiredNotes) {
            if (noteBlockPositions.containsKey(required))
                continue;

            ScannedNoteBlock tunableMatch = findAvailableBlock(scannedBlocks, usedPositions, required, false);
            if (tunableMatch != null) {
                noteBlockPositions.put(required, tunableMatch.pos());
                usedPositions.add(tunableMatch.pos());
            }
        }

        if (noteBlockPositions.size() < requiredNotes.size()) {
            List<String> examples = new ArrayList<>();
            for (Note required : requiredNotes) {
                if (noteBlockPositions.containsKey(required))
                    continue;
                examples.add(formatNote(required));
                if (examples.size() == 5)
                    break;
            }
            notifyPlayer("Missing note blocks for: " + String.join(", ", examples)
                    + (requiredNotes.size() - noteBlockPositions.size() > examples.size() ? ", ..." : ""));
        }
    }

    private ScannedNoteBlock findAvailableBlock(
            List<ScannedNoteBlock> scannedBlocks,
            Set<BlockPos> usedPositions,
            Note required,
            boolean requireCurrentNote
    ) {
        for (ScannedNoteBlock scanned : scannedBlocks) {
            if (usedPositions.contains(scanned.pos()))
                continue;
            if (activeExactInstruments && scanned.note().instrument() != required.instrument())
                continue;
            if (requireCurrentNote && scanned.note().level() != required.level())
                continue;
            return scanned;
        }
        return null;
    }

    private boolean prepareTuneHits() {
        tuneHits.clear();

        for (Map.Entry<Note, BlockPos> entry : noteBlockPositions.entrySet()) {
            BlockState state = mc.level.getBlockState(entry.getValue());
            if (state.getBlock() != Blocks.NOTE_BLOCK) {
                failAndDisable("A mapped note block is no longer available.", null);
                return false;
            }

            int currentLevel = state.getValue(NoteBlock.NOTE);
            int requiredHits = Math.floorMod(entry.getKey().level() - currentLevel, 25);
            if (requiredHits > 0)
                tuneHits.put(entry.getValue(), requiredHits);
        }

        tuneTickCounter = 0;
        return true;
    }

    private void tuneNoteBlocks() {
        if (tuneHits.isEmpty()) {
            if (tunedSinceCheck) {
                tunedSinceCheck = false;
                waitTicks = (int) this.recheckDelay.getValue();
                stage = Stage.WaitingToVerify;
                return;
            }

            startPlayback();
            return;
        }

        if (++tuneTickCounter < (int) this.tuneDelay.getValue())
            return;
        tuneTickCounter = 0;

        int tunedThisTick = 0;
        Iterator<Map.Entry<BlockPos, Integer>> iterator = tuneHits.entrySet().iterator();
        while (iterator.hasNext() && tunedThisTick < (int) this.concurrentTuneBlocks.getValue()) {
            Map.Entry<BlockPos, Integer> entry = iterator.next();
            BlockPos pos = entry.getKey();

            if (!canReachMappedBlock(pos)) {
                failAndDisable("Move closer to the mapped note blocks and try again.", null);
                return;
            }

            if (this.autoRotate.getValue())
                rotateTo(pos);

            try {
                BlockHitResult hitResult = PlayerUtil.INSTANCE.blockHitResult(pos);
                ((MultiPlayerGameModeAccessor) mc.gameMode).invokeStartPrediction(mc.level, sequence ->
                        new ServerboundUseItemOnPacket(InteractionHand.MAIN_HAND, hitResult, sequence));
            } catch (RuntimeException exception) {
                failAndDisable("Could not tune a note block: " + readableMessage(exception), exception);
                return;
            }

            int remainingHits = entry.getValue() - 1;
            if (remainingHits <= 0)
                iterator.remove();
            else
                entry.setValue(remainingHits);

            tunedSinceCheck = true;
            tunedThisTick++;
        }

        if (tunedThisTick > 0 && this.swingArm.getValue())
            mc.player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, true);
    }

    private void waitToVerifyTuning() {
        if (--waitTicks > 0)
            return;

        if (!prepareTuneHits())
            return;

        stage = Stage.Tuning;
    }

    private void startPlayback() {
        if (mc.player.getAbilities().instabuild) {
            failAndDisable("Survival or adventure mode is required; creative attacks break note blocks.", null);
            return;
        }

        currentTick = 0;
        stage = Stage.Playing;
        notifyPlayer("Tuning complete. Playing.");
    }

    private void playCurrentTick() {
        if (song == null) {
            failAndDisable("No song is loaded.", null);
            return;
        }

        if (currentTick > song.lastTick()) {
            onSongFinished();
            return;
        }

        List<Note> notes = song.notesAt(currentTick);
        if (!notes.isEmpty()) {
            List<BlockPos> playablePositions = new ArrayList<>(notes.size());
            for (Note note : notes) {
                BlockPos pos = noteBlockPositions.get(note);
                if (pos != null)
                    playablePositions.add(pos);
            }

            if (!playablePositions.isEmpty()) {
                int noteLimit = this.polyphonic.getValue() ? playablePositions.size() : 1;
                for (int i = 0; i < noteLimit; i++) {
                    if (!canReachMappedBlock(playablePositions.get(i))) {
                        failAndDisable("Move closer to the mapped note blocks and try again.", null);
                        return;
                    }
                }

                if (this.autoRotate.getValue())
                    rotateTo(playablePositions.getFirst());
                if (this.swingArm.getValue())
                    mc.player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, true);

                for (int i = 0; i < noteLimit; i++) {
                    BlockPos pos = playablePositions.get(i);
                    try {
                        ((MultiPlayerGameModeAccessor) mc.gameMode).invokeStartPrediction(mc.level, sequence -> new ServerboundPlayerActionPacket(
                                ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,
                                pos,
                                Direction.DOWN,
                                sequence
                        ));
                    } catch (RuntimeException exception) {
                        failAndDisable("Could not play a note block: " + readableMessage(exception), exception);
                        return;
                    }
                }
            }
        }

        currentTick++;
    }

    private void onSongFinished() {
        if (this.loop.getValue()) {
            if (!prepareTuneHits())
                return;
            tunedSinceCheck = false;
            stage = Stage.Tuning;
            notifyPlayer("Song finished; starting the next loop.");
            return;
        }

        notifyPlayer("Song finished.");
        setEnabled(false);
    }

    private boolean canReachMappedBlock(BlockPos pos) {
        return mc.level.getBlockState(pos).getBlock() == Blocks.NOTE_BLOCK
                && mc.level.getBlockState(pos.above()).isAir()
                && mc.player.isWithinBlockInteractionRange(pos, 1.0);
    }

    private void rotateTo(BlockPos pos) {
        float[] rotations = this.rotationManager.getRotationsTo(
                pos.getX() + 0.5,
                pos.getY() + 0.5,
                pos.getZ() + 0.5
        );
        this.rotationManager.startRotation(rotations[0], rotations[1], 180.0f);
        controllingRotation = true;
        PacketUtil.sendPacket(new ServerboundMovePlayerPacket.Rot(
                rotations[0],
                rotations[1],
                mc.player.onGround(),
                mc.player.horizontalCollision
        ));
    }

    private boolean isExactModeSelected() {
        return this.mode.is("Exact Instruments");
    }

    private void resetRuntime() {
        CompletableFuture<Song> future = loadingSong;
        loadingSong = null;
        if (future != null)
            future.cancel(true);

        noteBlockPositions.clear();
        tuneHits.clear();
        song = null;
        stage = Stage.Idle;
        loadStartedAt = 0L;
        currentTick = 0;
        tuneTickCounter = 0;
        waitTicks = 0;
        tunedSinceCheck = false;
        activeExactInstruments = false;
        activeBelowBlockDetection = false;
        controllingRotation = false;
    }

    private void failAndDisable(String message, Throwable failure) {
        notifyPlayer(message);
        if (failure != null)
            LOGGER.error("Note Bot failed: {}", message, failure);
        if (isEnabled())
            setEnabled(false);
    }

    private void notifyPlayer(String message) {
        ChatUtil.printChat("[Note Bot] " + message);
    }

    private static String readableMessage(Throwable failure) {
        String message = failure.getMessage();
        return message == null || message.isBlank() ? failure.getClass().getSimpleName() : message;
    }

    private static String displayMetadata(String value) {
        String normalized = value.replace('\r', ' ').replace('\n', ' ').trim();
        return normalized.length() <= 96 ? normalized : normalized.substring(0, 93) + "...";
    }

    private static String formatNote(Note note) {
        String instrument = note.instrument() == null ? "Any" : note.instrument().name();
        return instrument + ":" + note.level();
    }

    private record ScannedNoteBlock(BlockPos pos, Note note, double distanceSquared) {
    }

    private enum Stage {
        Idle("Idle"),
        Loading("Loading"),
        SetUp("Setup"),
        Tuning("Tuning"),
        WaitingToVerify("Verifying"),
        Playing("Playing");

        private final String displayName;

        Stage(String displayName) {
            this.displayName = displayName;
        }
    }
}
