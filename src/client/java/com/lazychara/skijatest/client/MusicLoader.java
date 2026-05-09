package com.lazychara.skijatest.client;

import net.fabricmc.loader.api.FabricLoader;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.images.Artwork;
import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.SampleBuffer;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.SourceDataLine;
import java.io.BufferedInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import java.util.logging.Level;

public class MusicLoader {

    private static final Path MUSIC_DIR = FabricLoader.getInstance().getGameDir().resolve("lazychara/music");
    private static final List<MusicTrack> tracks = new ArrayList<>();

    private static Thread playbackThread;
    private static long playbackSerial;
    private static volatile boolean stopRequested;
    private static volatile boolean paused = true;
    private static volatile float volume = 0.78f;
    private static volatile long currentMicros;
    private static volatile MusicTrack currentTrack;
    private static SourceDataLine currentLine;

    static {
        suppressJAudioTaggerLogs();
    }

    private static void suppressJAudioTaggerLogs() {
        java.util.logging.Logger.getLogger("org.jaudiotagger").setLevel(Level.WARNING);
        java.util.logging.Logger.getLogger("org.jaudiotagger.audio").setLevel(Level.WARNING);
        java.util.logging.Logger.getLogger("org.jaudiotagger.audio.flac").setLevel(Level.WARNING);
        java.util.logging.Logger.getLogger("org.jaudiotagger.audio.flac.FlacInfoReader").setLevel(Level.WARNING);
    }

    public static void init() {
        suppressJAudioTaggerLogs();
        try {
            Files.createDirectories(MUSIC_DIR);
            scanMusic();
        } catch (Exception ignored) {
        }
    }

    public static void rescan() {
        suppressJAudioTaggerLogs();
        scanMusic();
    }

    private static void scanMusic() {
        tracks.clear();
        if (!Files.exists(MUSIC_DIR)) return;

        try (Stream<Path> stream = Files.list(MUSIC_DIR)) {
            stream.filter(p -> {
                        String name = p.getFileName().toString().toLowerCase();
                        return Files.isRegularFile(p)
                                && (name.endsWith(".mp3") || name.endsWith(".flac"));
                    })
                    .sorted()
                    .forEach(p -> {
                        try {
                            MusicTrack track = loadTrack(p);
                            if (track != null) {
                                tracks.add(track);
                            }
                        } catch (Exception ignored) {
                        }
                    });
        } catch (Exception ignored) {
        }
    }

    private static MusicTrack loadTrack(Path path) {
        String fileName = path.getFileName().toString();
        String format = fileName.contains(".")
                ? fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase()
                : "unknown";

        int duration = 0;
        int sampleRate = 0;
        int bitDepth = 0;
        String title  = fileName;
        String artist = "Unknown";
        String album  = "Unknown";
        String lyrics = "";
        byte[] coverArt = null;
        String coverMimeType = null;

        try {
            AudioFile audioFile = AudioFileIO.read(path.toFile());
            AudioHeader header = audioFile.getAudioHeader();
            Tag tag = audioFile.getTag();

            duration = (header != null) ? header.getTrackLength() : 0;
            sampleRate = readSampleRate(header);
            bitDepth = readBitDepth(header);

            if (tag != null) {
                String t = tag.getFirst(FieldKey.TITLE);
                if (t != null && !t.isBlank()) title = t;

                String a = tag.getFirst(FieldKey.ARTIST);
                if (a != null && !a.isBlank()) artist = a;

                String al = tag.getFirst(FieldKey.ALBUM);
                if (al != null && !al.isBlank()) album = al;

                String l = tag.getFirst(FieldKey.LYRICS);
                if (l != null && !l.isBlank()) lyrics = l;

                Artwork artwork = tag.getFirstArtwork();
                if (artwork != null) {
                    coverArt = artwork.getBinaryData();
                    coverMimeType = artwork.getMimeType();
                }
            }
        } catch (Exception ignored) {
        }

        String qualityLabel = makeQualityLabel(format, sampleRate, bitDepth);
        return new MusicTrack(path, title, artist, album, duration,
                format, coverArt, coverMimeType, lyrics, qualityLabel);
    }

    private static int readSampleRate(AudioHeader header) {
        if (header == null) return 0;
        try { return header.getSampleRateAsNumber(); } catch (Exception ignored) {}
        try { return Integer.parseInt(header.getSampleRate()); } catch (Exception ignored) {}
        return 0;
    }

    private static int readBitDepth(AudioHeader header) {
        if (header == null) return 0;
        String[] methodNames = {"getBitsPerSample", "getBitDepth"};
        for (String name : methodNames) {
            try {
                Object value = header.getClass().getMethod(name).invoke(header);
                if (value instanceof Number n) return n.intValue();
                if (value instanceof String s && !s.isBlank()) return Integer.parseInt(s.trim());
            } catch (Exception ignored) {}
        }
        return 0;
    }

    private static String makeQualityLabel(String format, int sampleRate, int bitDepth) {
        if (!"flac".equalsIgnoreCase(format)) return "";
        if (sampleRate > 48_000 || bitDepth > 16) return "高解析度无损";
        return "无损";
    }

    public static List<MusicTrack> getTracks() {
        return Collections.unmodifiableList(tracks);
    }

    public static Path getMusicDir() {
        return MUSIC_DIR;
    }

    public static synchronized void play(MusicTrack track) {
        playFrom(track, 0f);
    }

    public static synchronized void playFrom(MusicTrack track, float startSeconds) {
        if (track == null) return;
        stopPlaybackInternal(false);
        long serial = ++playbackSerial;
        currentTrack = track;
        paused = false;
        stopRequested = false;
        currentMicros = Math.max(0L, (long) (startSeconds * 1_000_000L));
        playbackThread = new Thread(() -> playbackLoop(track, Math.max(0f, startSeconds), serial), "SkijaTest-MusicLoader-Playback");
        playbackThread.setDaemon(true);
        playbackThread.start();
    }

    public static synchronized void seek(float seconds) {
        MusicTrack track = currentTrack;
        if (track == null) return;
        boolean shouldPlay = !paused;
        playFrom(track, Math.max(0f, Math.min(seconds, Math.max(0, track.duration()))));
        if (!shouldPlay) pause();
    }

    public static synchronized void pause() {
        paused = true;
        if (currentLine != null) currentLine.stop();
    }

    public static synchronized void resume() {
        if (currentTrack == null) return;
        paused = false;
        if (currentLine != null) currentLine.start();
    }

    public static synchronized void toggle(MusicTrack track) {
        if (currentTrack != track) {
            play(track);
        } else if (paused) {
            resume();
        } else {
            pause();
        }
    }

    public static synchronized void stopPlayback() {
        stopPlaybackInternal(true);
    }

    private static synchronized void stopPlaybackInternal(boolean restoreMusic) {
        stopRequested = true;
        playbackSerial++;
        paused = true;
        if (currentLine != null) {
            try { currentLine.stop(); } catch (Exception ignored) {}
            try { currentLine.close(); } catch (Exception ignored) {}
            currentLine = null;
        }
        if (playbackThread != null) {
            playbackThread.interrupt();
            playbackThread = null;
        }
        currentTrack = null;
        currentMicros = 0;
    }

    private static boolean isPlaybackCancelled(long serial) {
        return stopRequested || serial != playbackSerial || Thread.currentThread().isInterrupted();
    }

    private static void playbackLoop(MusicTrack track, float startSeconds, long serial) {
        if ("mp3".equalsIgnoreCase(track.format())) {
            playbackLoopMp3(track, startSeconds, serial);
        } else {
            playbackLoopJavaSound(track, startSeconds, serial);
        }
    }

    private static void playbackLoopJavaSound(MusicTrack track, float startSeconds, long serial) {
        try (AudioInputStream encoded = AudioSystem.getAudioInputStream(track.filePath().toFile());
             AudioInputStream pcm = AudioSystem.getAudioInputStream(AudioFormat.Encoding.PCM_SIGNED, encoded)) {
            AudioFormat decoded = pcm.getFormat();
            if (decoded.isBigEndian() || decoded.getSampleSizeInBits() != 16) {
                decoded = new AudioFormat(
                        AudioFormat.Encoding.PCM_SIGNED,
                        decoded.getSampleRate(),
                        16,
                        decoded.getChannels(),
                        decoded.getChannels() * 2,
                        decoded.getSampleRate(),
                        false
                );
            }
            {
                long startMicros = Math.max(0L, (long) (startSeconds * 1_000_000L));
                long bytesToSkip = Math.max(0L, (long) (startSeconds * decoded.getSampleRate()) * decoded.getFrameSize());
                while (bytesToSkip > 0) {
                    long skipped = pcm.skip(bytesToSkip);
                    if (skipped <= 0) break;
                    bytesToSkip -= skipped;
                }
                SourceDataLine line = AudioSystem.getSourceDataLine(decoded);
                if (isPlaybackCancelled(serial)) return;
                currentLine = line;
                line.open(decoded);
                applyPlaybackVolume(line);
                line.start();
                byte[] buffer = new byte[8192];
                int read;
                while (!isPlaybackCancelled(serial) && (read = pcm.read(buffer, 0, buffer.length)) != -1) {
                    while (paused && !isPlaybackCancelled(serial)) Thread.sleep(25L);
                    if (isPlaybackCancelled(serial)) break;
                    applyPlaybackVolume(line);
                    line.write(buffer, 0, read);
                    currentMicros = startMicros + line.getMicrosecondPosition();
                }
                if (!isPlaybackCancelled(serial)) line.drain();
            }
        } catch (InterruptedException ignored) {
        } catch (Exception ignored) {
        } finally {
            SourceDataLine line = currentLine;
            if (line != null && serial == playbackSerial) {
                try { line.stop(); } catch (Exception ignored) {}
                try { line.close(); } catch (Exception ignored) {}
            }
            if (serial == playbackSerial) currentLine = null;
            if (serial == playbackSerial && !stopRequested) paused = true;
        }
    }

    private static void playbackLoopMp3(MusicTrack track, float startSeconds, long serial) {
        long startMicros = Math.max(0L, (long) (startSeconds * 1_000_000L));
        long playedMicros = 0L;
        try (BufferedInputStream in = new BufferedInputStream(Files.newInputStream(track.filePath()))) {
            Bitstream bitstream = new Bitstream(in);
            Decoder decoder = new Decoder();
            SourceDataLine line = null;
            Header header;
            double skippedMs = 0.0;
            while (!isPlaybackCancelled(serial) && (header = bitstream.readFrame()) != null) {
                SampleBuffer output = (SampleBuffer) decoder.decodeFrame(header, bitstream);
                if (skippedMs + header.ms_per_frame() < startSeconds * 1000.0) {
                    skippedMs += header.ms_per_frame();
                    bitstream.closeFrame();
                    continue;
                }
                if (line == null) {
                    AudioFormat format = new AudioFormat(
                            AudioFormat.Encoding.PCM_SIGNED,
                            output.getSampleFrequency(),
                            16,
                            output.getChannelCount(),
                            output.getChannelCount() * 2,
                            output.getSampleFrequency(),
                            false
                    );
                    line = AudioSystem.getSourceDataLine(format);
                    if (isPlaybackCancelled(serial)) return;
                    currentLine = line;
                    line.open(format);
                    applyPlaybackVolume(line);
                    line.start();
                }
                while (paused && !isPlaybackCancelled(serial)) Thread.sleep(25L);
                if (isPlaybackCancelled(serial)) break;
                applyPlaybackVolume(line);
                byte[] pcm = shortsToLittleEndian(output.getBuffer(), output.getBufferLength());
                line.write(pcm, 0, pcm.length);
                playedMicros += (long) (header.ms_per_frame() * 1000.0);
                currentMicros = startMicros + playedMicros;
                bitstream.closeFrame();
            }
            if (line != null && !isPlaybackCancelled(serial)) line.drain();
            try { bitstream.close(); } catch (Exception ignored) {}
        } catch (InterruptedException ignored) {
        } catch (Exception ignored) {
        } finally {
            SourceDataLine line = currentLine;
            if (line != null && serial == playbackSerial) {
                try { line.stop(); } catch (Exception ignored) {}
                try { line.close(); } catch (Exception ignored) {}
            }
            if (serial == playbackSerial) currentLine = null;
            if (serial == playbackSerial && !stopRequested) paused = true;
        }
    }

    private static byte[] shortsToLittleEndian(short[] samples, int len) {
        byte[] out = new byte[len * 2];
        for (int i = 0, j = 0; i < len; i++) {
            short s = samples[i];
            out[j++] = (byte) (s & 0xFF);
            out[j++] = (byte) ((s >>> 8) & 0xFF);
        }
        return out;
    }

    private static void applyPlaybackVolume(SourceDataLine line) {
        if (line == null || !line.isControlSupported(FloatControl.Type.MASTER_GAIN)) return;
        FloatControl gain = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
        float v = Math.max(0.0001f, Math.min(1f, volume));
        float db = (float) (20.0 * Math.log10(v));
        db = Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), db));
        gain.setValue(db);
    }



    public static void setVolume(float value) {
        volume = Math.max(0f, Math.min(1f, value));
        applyPlaybackVolume(currentLine);
    }

    public static float getVolume() {
        return volume;
    }

    public static boolean isPaused() {
        return paused;
    }

    public static boolean isPlaying(MusicTrack track) {
        return currentTrack == track && !paused;
    }

    public static float getCurrentSeconds() {
        return currentMicros / 1_000_000f;
    }

    public static MusicTrack getCurrentTrack() {
        return currentTrack;
    }

    public record MusicTrack(
            Path   filePath,
            String title,
            String artist,
            String album,
            int    duration,
            String format,
            byte[] coverArt,
            String coverMimeType,
            String lyrics,
            String qualityLabel
    ) {
        public boolean hasCoverArt() {
            return coverArt != null && coverArt.length > 0;
        }

        public boolean hasLyrics() {
            return lyrics != null && !lyrics.isEmpty();
        }

        public String durationFormatted() {
            return (duration / 60) + ":" + String.format("%02d", duration % 60);
        }
    }
}
