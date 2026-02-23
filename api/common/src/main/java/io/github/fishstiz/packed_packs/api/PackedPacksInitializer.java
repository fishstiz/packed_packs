package io.github.fishstiz.packed_packs.api;

/**
 * The entry point for initializing integration with Packed Packs.
 * </p>
 * <h3>Registration</h3>
 * <h4>Fabric:</h4>
 * Register your implementation in {@code fabric.mod.json} under
 * the {@code "packed_packs"} entrypoint key:
 * <pre>{@code
 * "entrypoints": {
 *   "packed_packs": [
 *     "com.example.mod.PackedPacksIntegration"
 *   ]
 * }
 * }</pre>
 * <p>
 * <h4>NeoForge:</h4>
 * Use the standard Java {@link java.util.ServiceLoader} SPI.
 * Create a file at {@code META-INF/services/io.github.fishstiz.packed_packs.api.PackedPacksInitializer}
 * containing the fully qualified name of your implementation class:
 * <pre>{@code
 * com.example.mod.PackedPacksIntegration
 * }
 * </pre>
 */
public interface PackedPacksInitializer {
    /**
     * Runs the initializer.
     * <p>
     * <b>Note:</b> This method may be called off the main thread.
     *
     * @param api the API instance to access Packed Packs services
     */
    void onInitialize(PackedPacksApi api);
}
