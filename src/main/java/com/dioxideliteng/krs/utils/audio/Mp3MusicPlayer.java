package com.dioxideliteng.krs.utils.audio;

import com.mojang.logging.LogUtils;
import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.JavaSoundAudioDevice;
import javazoom.jl.player.advanced.AdvancedPlayer;
import org.slf4j.Logger;

import java.io.BufferedInputStream;
import java.io.InputStream;

public final class Mp3MusicPlayer implements AutoCloseable {

    private static final Logger LOGGER = LogUtils.getLogger();

    private final Object lock = new Object();
    private final String resourcePath;
    private boolean playing;
    private int generation;
    private int playbackFramePosition;
    private int nextStartFrame;
    private volatile float volume = 1f;
    private Thread playbackThread;
    private TrackingAdvancedPlayer activePlayer;

    public Mp3MusicPlayer(String resourcePath) {
        this.resourcePath = resourcePath;
    }

    public void playLoop() {
        synchronized (lock) {
            if (playing)
                return;

            playing = true;
            int threadGeneration = ++generation;
            playbackThread = new Thread(() -> playbackLoop(threadGeneration), "krs-mp3-music-player");
            playbackThread.setDaemon(true);
            playbackThread.start();
        }
    }

    public void stop() {
        stopAndGetFramePosition();
    }

    public int stopAndGetFramePosition() {
        TrackingAdvancedPlayer playerToClose;
        int framePosition;

        synchronized (lock) {
            if (!playing && playbackThread == null)
                return playbackFramePosition;

            playing = false;
            generation++;
            playerToClose = activePlayer;
            framePosition = playerToClose == null ? playbackFramePosition : playerToClose.getFramePosition();
            activePlayer = null;

            if (playbackThread != null)
                playbackThread.interrupt();
        }

        if (playerToClose != null) {
            closePlayer(playerToClose);
            framePosition = playerToClose.getFramePosition();
        }

        synchronized (lock) {
            playbackFramePosition = framePosition;
            nextStartFrame = framePosition;
        }

        return framePosition;
    }

    public void setVolume(float volume) {
        this.volume = clampVolume(volume);
    }

    public float getVolume() {
        return volume;
    }

    public void setStartFrame(int frame) {
        synchronized (lock) {
            nextStartFrame = clampFrame(frame);
            if (!playing)
                playbackFramePosition = nextStartFrame;
        }
    }

    public int getFramePosition() {
        synchronized (lock) {
            return activePlayer == null ? playbackFramePosition : activePlayer.getFramePosition();
        }
    }

    private void playbackLoop(int threadGeneration) {
        while (isPlaying(threadGeneration)) {
            TrackingAdvancedPlayer player = null;
            int startFrame;

            try (InputStream stream = openResourceStream()) {
                if (stream == null) {
                    LOGGER.warn("Could not find MP3 resource: {}", resourcePath);
                    break;
                }

                synchronized (lock) {
                    startFrame = nextStartFrame;
                    playbackFramePosition = startFrame;
                }

                player = new TrackingAdvancedPlayer(new BufferedInputStream(stream), new VolumeAudioDevice(), startFrame);
                synchronized (lock) {
                    if (!isPlayingLocked(threadGeneration))
                        break;

                    activePlayer = player;
                }

                player.playFromStartFrame();

                synchronized (lock) {
                    if (activePlayer == player)
                        activePlayer = null;

                    playbackFramePosition = player.getFramePosition();
                    if (isPlayingLocked(threadGeneration)) {
                        nextStartFrame = 0;
                        playbackFramePosition = 0;
                    }
                }
            } catch (JavaLayerException | RuntimeException e) {
                if (isPlaying(threadGeneration))
                    LOGGER.warn("Failed to play MP3 resource: {}", resourcePath, e);
                break;
            } catch (Exception e) {
                if (isPlaying(threadGeneration))
                    LOGGER.warn("Failed to load MP3 resource: {}", resourcePath, e);
                break;
            } finally {
                try {
                    if (player != null)
                        closePlayer(player);
                } finally {
                    synchronized (lock) {
                        if (player != null && activePlayer == player) {
                            playbackFramePosition = player.getFramePosition();
                            activePlayer = null;
                        }
                    }
                }
            }
        }

        Thread currentThread = Thread.currentThread();
        synchronized (lock) {
            if (playbackThread == currentThread)
                playbackThread = null;

            if (generation == threadGeneration) {
                playing = false;
                activePlayer = null;
            }
        }
    }

    private InputStream openResourceStream() {
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        if (contextClassLoader != null) {
            InputStream stream = contextClassLoader.getResourceAsStream(resourcePath);
            if (stream != null)
                return stream;
        }

        return Mp3MusicPlayer.class.getClassLoader().getResourceAsStream(resourcePath);
    }

    private boolean isPlaying(int threadGeneration) {
        synchronized (lock) {
            return isPlayingLocked(threadGeneration);
        }
    }

    private boolean isPlayingLocked(int threadGeneration) {
        return playing && generation == threadGeneration;
    }

    private void closePlayer(TrackingAdvancedPlayer player) {
        try {
            player.close();
        } catch (RuntimeException exception) {
            LOGGER.warn("Failed to close MP3 playback resources: {}", resourcePath, exception);
        }
    }

    private float clampVolume(float value) {
        if (Float.isNaN(value))
            return 1f;

        return Math.max(0f, Math.min(1f, value));
    }

    private int clampFrame(int frame) {
        return Math.max(0, frame);
    }

    private final class TrackingAdvancedPlayer extends AdvancedPlayer {

        private final int startFrame;
        private volatile int framePosition;

        private TrackingAdvancedPlayer(InputStream stream, VolumeAudioDevice audioDevice, int startFrame) throws JavaLayerException {
            super(stream, audioDevice);
            this.startFrame = clampFrame(startFrame);
            this.framePosition = this.startFrame;
        }

        private void playFromStartFrame() throws JavaLayerException {
            play(startFrame, Integer.MAX_VALUE);
        }

        private int getFramePosition() {
            return framePosition;
        }

        @Override
        protected boolean decodeFrame() throws JavaLayerException {
            boolean decoded = super.decodeFrame();
            if (decoded)
                framePosition++;

            return decoded;
        }
    }

    private final class VolumeAudioDevice extends JavaSoundAudioDevice {

        private short[] scaledSamples = new short[0];

        @Override
        protected void writeImpl(short[] samples, int offs, int len) throws JavaLayerException {
            float currentVolume = volume;
            if (currentVolume >= 0.999f) {
                super.writeImpl(samples, offs, len);
                return;
            }

            if (scaledSamples.length < len)
                scaledSamples = new short[len];

            for (int i = 0; i < len; i++)
                scaledSamples[i] = (short) Math.round(samples[offs + i] * currentVolume);

            super.writeImpl(scaledSamples, 0, len);
        }
    }

    @Override
    public void close() {
        stop();
    }
}
