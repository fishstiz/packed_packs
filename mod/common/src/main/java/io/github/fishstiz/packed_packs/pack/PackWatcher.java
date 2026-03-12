package io.github.fishstiz.packed_packs.pack;

import io.github.fishstiz.fidgetz.util.debounce.ConcurrentPollingDebouncer;
import io.github.fishstiz.fidgetz.util.debounce.PollingDebouncer;
import io.github.fishstiz.packed_packs.PackedPacks;
import io.github.fishstiz.packed_packs.api.context.ScreenContext;
import io.github.fishstiz.packed_packs.api.events.WatchEvent;
import io.github.fishstiz.packed_packs.impl.PackedPacksApiImpl;
import net.minecraft.util.Util;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static io.github.fishstiz.packed_packs.util.PackUtil.hasFolderConfig;
import static io.github.fishstiz.packed_packs.util.PackUtil.hasMcmeta;
import static java.nio.file.Files.isDirectory;
import static net.minecraft.util.Util.backgroundExecutor;

/**
 * Migrated from {@link java.nio.file.WatchService} due to registered subdirectories locking parent directory on Windows.
 *
 * @see <a href="https://bugs.openjdk.org/browse/JDK-6972833">JDK-6972833</a>
 */
public class PackWatcher implements AutoCloseable {
    private static final long POLL_INTERVAL_MS = 1000;
    private static final long DEBOUNCED_CHANGE_DELAY_MS = 1000;
    private final FileAlterationMonitor monitor = new FileAlterationMonitor(POLL_INTERVAL_MS);
    private final DirectoryListener directoryListener = new DirectoryListener();
    private final PollingDebouncer<Path> onChangeCallback;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final AtomicInteger pauseCount = new AtomicInteger(0);

    private long lastPollTime;

    public PackWatcher(ScreenContext context, Collection<Path> directories, Runnable onChangeCallback) {
        this.monitor.setThreadFactory(r -> {
            throw new IllegalStateException("PackWatcher monitor should not be creating a new thread.");
        });
        this.onChangeCallback = new ConcurrentPollingDebouncer<>(path -> {
            if (!this.closed.get() && this.pauseCount.get() == 0) {
                WatchEvent watchEvent = new WatchEvent(context, path);
                if (!PackedPacksApiImpl.getInstance().eventBus().post(watchEvent).isCanceled()) {
                    onChangeCallback.run();
                }
            }
        }, DEBOUNCED_CHANGE_DELAY_MS);
        directories.forEach(this::addDirectory);
    }

    private void addDirectory(Path directory) {
        if (!isDirectory(directory, LinkOption.NOFOLLOW_LINKS)) return;

        Path normalizedPath = directory.toAbsolutePath().normalize();
        FileAlterationObserver observer;
        try {
            observer = FileAlterationObserver.builder()
                    .setFile(directory.toFile())
                    .setFileFilter(new Filter(normalizedPath))
                    .setIOCase(IOCase.SENSITIVE).get();
        } catch (IOException e) {
            PackedPacks.LOGGER.error("[packed_packs] Failed to create observer for directory: '{}' ", directory, e);
            return;
        }

        observer.addListener(this.directoryListener);

        backgroundExecutor().execute(() -> {
            if (this.closed.get()) return;
            try {
                observer.initialize();
                if (!this.closed.get()) {
                    this.monitor.addObserver(observer);
                } else {
                    observer.destroy();
                }
            } catch (Exception e) {
                PackedPacks.LOGGER.error("[packed_packs] Failed to initialize observer for directory {}.", normalizedPath, e);
            }
        });
    }

    /**
     * poll on main thread
     */
    public void poll() {
        long currentTime = Util.getMillis();
        if (currentTime - this.lastPollTime >= this.monitor.getInterval()) {
            backgroundExecutor().execute(() -> this.monitor.getObservers().forEach(FileAlterationObserver::checkAndNotify));
            this.lastPollTime = currentTime;
        }
        this.onChangeCallback.poll();
    }

    public void pause() {
        this.pauseCount.incrementAndGet();
        this.onChangeCallback.abort();
    }

    public void resume() {
        this.pauseCount.decrementAndGet();
    }

    public void consumeChanges() {
        this.pause();
        backgroundExecutor().execute(() -> {
            this.monitor.getObservers().forEach(FileAlterationObserver::checkAndNotify);
            this.onChangeCallback.abort();
            this.resume();
        });
    }

    @Override
    public void close() {
        if (!this.closed.compareAndSet(false, true)) {
            return;
        }
        this.onChangeCallback.abort();
        backgroundExecutor().execute(() -> {
            for (FileAlterationObserver observer : this.monitor.getObservers()) {
                try {
                    observer.destroy();
                } catch (Exception e) {
                    PackedPacks.LOGGER.error("[packed_packs] Error occurred while closing observer for {}", observer.getDirectory(), e);
                }
            }
        });
    }

    private record Filter(Path root) implements FileFilter {
        private static final int DIRECTORY_PACK_DEPTH = 1;
        private static final int PACK_CONTENTS_DEPTH = 2;
        private static final int NESTED_PACK_DEPTH = 3;

        @Override
        public boolean accept(File pathname) {
            Path path = pathname.toPath();
            int depth = this.root.relativize(path.toAbsolutePath().normalize()).getNameCount();

            return switch (depth) {
                case 0, DIRECTORY_PACK_DEPTH -> true;
                case PACK_CONTENTS_DEPTH -> hasMcmeta(path.getParent()) || hasFolderConfig(path.getParent());
                case NESTED_PACK_DEPTH -> hasFolderConfig(path.getParent().getParent());
                default -> false;
            };
        }
    }

    private class DirectoryListener extends FileAlterationListenerAdaptor {
        private DirectoryListener() {
        }

        @Override
        public void onDirectoryCreate(File directory) {
            this.onEvent(directory);
        }

        @Override
        public void onDirectoryChange(File directory) {
            this.onEvent(directory);
        }

        @Override
        public void onDirectoryDelete(File directory) {
            this.onEvent(directory);
        }

        @Override
        public void onFileCreate(File file) {
            this.onEvent(file);
        }

        @Override
        public void onFileChange(File file) {
            this.onEvent(file);
        }

        @Override
        public void onFileDelete(File file) {
            this.onEvent(file);
        }

        private void onEvent(File file) {
            PackWatcher.this.onChangeCallback.accept(file.toPath());
        }
    }
}
