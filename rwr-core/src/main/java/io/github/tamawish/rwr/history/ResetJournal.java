package io.github.tamawish.rwr.history;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import io.github.tamawish.rwr.reset.FailureSafety;
import io.github.tamawish.rwr.reset.ResetFailureType;
import io.github.tamawish.rwr.reset.ResetPhase;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ResetJournal {
    private static final long MAX_STATE_FILE_BYTES = 4_194_304L;
    private static final Type HISTORY_TYPE = new TypeToken<List<ResetHistoryEntry>>() {}.getType();
    private static final Type MARKERS_TYPE = new TypeToken<List<InterruptedOperationMarker>>() {}.getType();

    private final Path historyFile;
    private final Path markerFile;
    private final int historyLimit;
    private final Clock clock;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final List<ResetHistoryEntry> history;
    private final Map<String, InterruptedOperationMarker> markers;

    public ResetJournal(Path dataDirectory, int historyLimit, Clock clock) throws IOException {
        if (historyLimit < 1) {
            throw new IllegalArgumentException("historyLimit must be positive");
        }
        Files.createDirectories(dataDirectory);
        this.historyFile = dataDirectory.resolve("reset-history.json");
        this.markerFile = dataDirectory.resolve("active-resets.json");
        this.historyLimit = historyLimit;
        this.clock = clock;
        this.history = new ArrayList<>(readList(historyFile, HISTORY_TYPE));
        this.markers = new LinkedHashMap<>();
        for (InterruptedOperationMarker marker : this.<InterruptedOperationMarker>readList(markerFile, MARKERS_TYPE)) {
            markers.put(marker.operationId(), marker);
        }
        trimHistory();
    }

    public synchronized void mark(InterruptedOperationMarker marker) throws IOException {
        markers.put(marker.operationId(), marker);
        writeMarkers();
    }

    public synchronized void complete(ResetHistoryEntry entry) throws IOException {
        appendIfAbsent(entry);
        writeHistory();
        markers.remove(entry.operationId());
        writeMarkers();
    }

    public synchronized List<ResetHistoryEntry> recoverInterrupted() throws IOException {
        List<ResetHistoryEntry> recovered = new ArrayList<>();
        String recoveredAt = clock.instant().toString();
        for (InterruptedOperationMarker marker : markers.values()) {
            FailureSafety safety = marker.phase() == ResetPhase.REGENERATE || marker.phase() == ResetPhase.VERIFY
                    ? FailureSafety.AMBIGUOUS_REVIEW_REQUIRED
                    : FailureSafety.SAFE_TO_RETRY;
            ResetHistoryEntry entry = new ResetHistoryEntry(
                    marker.operationId(),
                    marker.worldId(),
                    marker.multiverseWorld(),
                    marker.startedAt(),
                    recoveredAt,
                    ResetPhase.INTERRUPTED,
                    ResetFailureType.INTERRUPTED_OPERATION,
                    safety,
                    "Server stopped during " + marker.phase()
                            + "; the operation was not resumed and requires reconciliation.");
            if (appendIfAbsent(entry)) {
                recovered.add(entry);
            }
        }
        if (!markers.isEmpty()) {
            writeHistory();
            markers.clear();
            writeMarkers();
        }
        return List.copyOf(recovered);
    }

    public synchronized List<ResetHistoryEntry> recent(int count) {
        int requested = Math.max(0, Math.min(count, history.size()));
        return List.copyOf(history.subList(history.size() - requested, history.size()));
    }

    public synchronized List<InterruptedOperationMarker> activeMarkers() {
        return List.copyOf(markers.values());
    }

    private boolean appendIfAbsent(ResetHistoryEntry entry) {
        if (history.stream().anyMatch(existing -> existing.operationId().equals(entry.operationId()))) {
            return false;
        }
        history.add(entry);
        trimHistory();
        return true;
    }

    private void trimHistory() {
        while (history.size() > historyLimit) {
            history.removeFirst();
        }
    }

    private void writeHistory() throws IOException {
        atomicWrite(historyFile, gson.toJson(history, HISTORY_TYPE));
    }

    private void writeMarkers() throws IOException {
        atomicWrite(markerFile, gson.toJson(List.copyOf(markers.values()), MARKERS_TYPE));
    }

    private <T> List<T> readList(Path file, Type type) throws IOException {
        if (!Files.isRegularFile(file)) {
            return List.of();
        }
        try {
            long size = Files.size(file);
            if (size > MAX_STATE_FILE_BYTES) {
                throw new IOException(file.getFileName() + " exceeds " + MAX_STATE_FILE_BYTES + " bytes");
            }
            List<T> values = gson.fromJson(Files.readString(file, StandardCharsets.UTF_8), type);
            return values == null ? List.of() : List.copyOf(values);
        } catch (RuntimeException exception) {
            throw new IOException("Cannot parse " + file.getFileName() + ": " + exception.getMessage(), exception);
        }
    }

    private static void atomicWrite(Path file, String contents) throws IOException {
        Path temporary = Files.createTempFile(file.getParent(), file.getFileName().toString() + '.', ".tmp");
        try {
            Files.writeString(temporary, contents, StandardCharsets.UTF_8);
            try {
                Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
