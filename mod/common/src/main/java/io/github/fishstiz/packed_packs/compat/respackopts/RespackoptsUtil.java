package io.github.fishstiz.packed_packs.compat.respackopts;

import dev.jfronny.respackopts.Respackopts;
import dev.jfronny.respackopts.RespackoptsClient;

import java.nio.file.Path;

public class RespackoptsUtil {
    /**
     * {@link dev.jfronny.respackopts.mixin.PackSelectionScreen$WatcherMixin}
     *
     * @see <a href="https://git.jfronny.dev/JfMods/Respackopts/src/branch/master/common/src/client/java/dev/jfronny/respackopts/mixin/PackSelectionScreen$WatcherMixin.java">
     * Respackopts watcher workaround
     * </a>
     */
    public static boolean isRespackOptsFile(Path path) {
        return path.getFileName().toString().endsWith(Respackopts.FILE_EXTENSION);
    }

    public static boolean isForceReload() {
        return RespackoptsClient.forcePackReload;
    }
}
