package com.fx.srp.managers.util;

import com.fx.srp.SpeedRunPlus;
import com.fx.srp.config.ConfigHandler;
import com.fx.srp.model.seed.SeedCategory;
import org.bukkit.Bukkit;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Manages the generation, loading, and selection of seeds for filtered world generation.
 *
 * <p>The {@code SeedManager} is responsible for:</p>
 * <ul>
 *     <li>Creating CSV files for each seed category if they do not exist</li>
 *     <li>Loading seeds from CSV files for each {@link SeedCategory.SeedType}</li>
 *     <li>Selecting a random seed based on category weights and available seeds</li>
 * </ul>
 *
 * <p>Seed files are stored in the plugin's {@code /seeds} folder and may be
 * edited by the server owner to customize world generation behavior.</p>
 */
public class SeedManager {

    private final Logger logger = Bukkit.getLogger();
    private final ConfigHandler configHandler = ConfigHandler.getInstance();

    private static final String SEED_FILE_EXTENSION = ".csv";

    // Seeds
    private final Map<SeedCategory.SeedType, File> seedFiles = new ConcurrentHashMap<>();
    private final List<SeedCategory> seedCategories = new ArrayList<>();
    private int totalSeedWeight;

    /**
     * Initializes the SeedManager, creates seed files if missing, and loads seeds for each category.
     *
     * @param plugin The main plugin instance, used to locate the plugin data folder.
     */
    public SeedManager(SpeedRunPlus plugin) {
        createSeedFiles(plugin.getDataFolder());

        // Initialize seeds
        Arrays.stream(SeedCategory.SeedType.values()).forEach(seedType -> {
            int weight = configHandler.getSeedWeight(seedType);
            List<Long> seeds = loadSeeds(seedType);

            // Premature exit if the weight is non-positive or if no seeds are present
            if (!seedType.equals(SeedCategory.SeedType.RANDOM) && (weight < 1 || seeds.isEmpty())) return;

            seedCategories.add(new SeedCategory(seedType, weight, seeds));
            totalSeedWeight += weight;
        });
    }

    private void createSeedFiles(File dataDirectory){
        File seedsDir = new File(dataDirectory, "seeds");
        if (!seedsDir.exists() && !seedsDir.mkdir()) {
            configHandler.setFilteredSeeds(false);
            logger.warning("[SRP] Failed to create seeds directory, filtered seed generation is disabled!");
            return;
        }

        Arrays.stream(SeedCategory.SeedType.values())
                .filter(seedType -> seedType != SeedCategory.SeedType.RANDOM)
                .forEach(seedType -> createSeedFile(seedsDir, seedType));
    }

    private void createSeedFile(File seedsDir, SeedCategory.SeedType seedType) {
        File seedFile = new File(seedsDir, seedType.name() + SEED_FILE_EXTENSION);
        seedFiles.put(seedType, seedFile);
        if (seedFile.exists()) return;

        // Create the file
        try {
            if (!seedFile.createNewFile()) {
                logger.warning("[SRP] Failed to create seed file: " + seedType.name() + ", category is disabled!");
            }
        } catch (IOException ignored) {}
    }

    private List<Long> loadSeeds(SeedCategory.SeedType seedType) {
        // Do not load seeds of type random
        if (seedType == SeedCategory.SeedType.RANDOM) return Collections.emptyList();

        // Ensure the seed file exists
        File seedFile = seedFiles.get(seedType);
        if (!seedFile.exists()) return Collections.emptyList();

        // Parse each seed in the (CSV) file
        try (Stream<String> lines = Files.lines(seedFile.toPath())) {
            return lines.map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(s -> parseSeed(seedFile, s))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

        } catch (IOException ex) {
            logger.warning("[SRP] Failed to read seed file: " + seedFile.getPath());
            return Collections.emptyList();
        }
    }

    private Long parseSeed(File file, String seedString) {
        try {
            return Long.parseLong(seedString);
        } catch (NumberFormatException ex) {
            logger.warning("[SRP] Invalid seed in " + file.getName() + ": " + seedString);
            return null;
        }
    }

    /**
     * Selects a random seed from the available categories based on configured weights.
     *
     * <p>If the RANDOM category is selected, {@code null} is returned. Otherwise, a seed
     * from the chosen category is randomly picked.</p>
     *
     * @return A randomly selected seed value, or {@code null} if no suitable seed is available
     *         or the RANDOM category was selected.
     */
    public Long selectSeed() {
        if (seedCategories.isEmpty() || totalSeedWeight < 1) return null;

        // Roll a random number within the sum of weights
        int weightRoll = ThreadLocalRandom.current().nextInt(totalSeedWeight);
        int cumulativeWeight = 0;

        // Pick a seed category
        for (SeedCategory category : seedCategories) {
            cumulativeWeight += category.getWeight();

            if (weightRoll < cumulativeWeight) {
                // Return null in case the RANDOM seed category was selected
                if (category.getSeedType() == SeedCategory.SeedType.RANDOM) return null;

                // Roll a random number within the lengths of seeds in the category
                List<Long> seeds = category.getSeeds();
                final int seedRoll = ThreadLocalRandom.current().nextInt(seeds.size());
                Long seed = seeds.get(seedRoll);

                logger.info("[SRP] Picked seed category: " + category.getSeedType().name() + ", seed: " + seed);
                return seed;
            }
        }

        return null;
    }
}
