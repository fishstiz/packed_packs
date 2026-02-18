package io.github.fishstiz.packed_packs.api;

/**
 * The main entry point for mod extensions.
 * </p>
 * <h3>Registration</h3>
 * <b>Fabric:</b> Register your implementation in {@code fabric.mod.json} under
 * the {@code "packed_packs"} entrypoint key:
 * <pre>{@code
 * "entrypoints": {
 *   "packed_packs": [
 *     "com.example.mod.PackedPacksIntegration"
 *   ]
 * }
 * }</pre>
 * <b>NeoForge:</b> Use the standard Java {@link java.util.ServiceLoader} SPI.
 * Create a file at {@code META-INF/services/io.github.fishstiz.packed_packs.api.PackedPacksInitializer}
 * containing the fully qualified name of your implementation class:
 * <pre>
 * com.example.mod.PackedPacksIntegration
 * </pre>
 */
public interface PackedPacksInitializer {
    /**
     * Called when Minecraft loads.
     * <p>
     * <b>Note:</b> This method may be called off the main (Render) thread.
     * Implementations should ensure thread safety when interacting with external
     * state and avoid calling thread-sensitive Minecraft methods.
     *
     * @param api the API instance
     */
    void onInitialize(PackedPacksApi api);
}
