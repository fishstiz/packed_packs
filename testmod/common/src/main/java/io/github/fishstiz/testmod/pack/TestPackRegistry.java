package io.github.fishstiz.testmod.pack;

import io.github.fishstiz.testmod.config.Configuration;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.RepositorySource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class TestPackRegistry {
    private static final int PACK_COUNT = 200;
    private static final List<Pack> PACKS;

    static {
        List<Pack> packs = new ArrayList<>(PACK_COUNT);
        for (int i = 1; i <= PACK_COUNT; i++) {
            String id = id(i);
            boolean compatible = Math.floorMod(id.hashCode(), 20) != 0;
            packs.add(TestPackFactory.createPack(id, title(i), description(i), compatible));
        }
        PACKS = Collections.unmodifiableList(packs);
    }

    public static String id(int number) {
        return String.format("file/test_pack_%03d", number);
    }

    public static Component title(int number) {
        return Component.literal(String.format("Pack %03d", number));
    }

    public static Component description(int number) {
        return Component.literal(String.format("Test Pack No. %03d", number)).withStyle(ChatFormatting.GRAY);
    }

    public static Pack get(int number) {
        if (number < 1 || number > PACK_COUNT) {
            throw new IndexOutOfBoundsException("Pack number must be between 1 and " + PACK_COUNT);
        }
        return PACKS.get(number - 1);
    }

    public static int size() {
        return PACK_COUNT;
    }

    public static RepositorySource asRepositorySource() {
        return repository -> {
            if (Configuration.getInstance().shouldAddTestPacks()) {
                for (Pack pack : PACKS) {
                    repository.accept(pack);
                }
            }
        };
    }

    private TestPackRegistry() {
    }
}