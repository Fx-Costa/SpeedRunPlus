package com.fx.srp.managers.util;

import com.fx.srp.SpeedRunPlus;
import com.fx.srp.config.ConfigHandler;
import com.fx.srp.model.seed.SeedCategory;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.command.CommandSender;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Manages the generation, loading, and selection of seeds for filtered world generation.
 * <p>
 * The {@code SeedManager} is responsible for:
 * </p>
 * <ul>
 *     <li>Creating CSV files for each seed category if they do not exist</li>
 *     <li>Loading seeds from CSV files for each {@link SeedCategory.SeedType}</li>
 *     <li>Selecting a random seed based on category weights and available seeds</li>
 *     <li>Fetching and adding new seeds asynchronously from FSGs Practice seeds API</li>
 * </ul>
 *
 * <p>
 * Seed files are stored in the plugin's {@code /seeds} folder and may be
 * edited by the server owner to customize world generation behavior.
 * </p>
 */
public class SeedManager {

    private final Logger logger = Bukkit.getLogger();
    private final ConfigHandler configHandler = ConfigHandler.getInstance();
    private final SpeedRunPlus plugin;

    private static final String SEED_FILE_EXTENSION = ".csv";

    // Seeds
    private final Map<SeedCategory.SeedType, File> seedFiles = new ConcurrentHashMap<>();
    private final List<SeedCategory> seedCategories = new CopyOnWriteArrayList<>();
    private int totalSeedWeight;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    /**
     * Initializes the SeedManager, creates seed files if missing, and loads seeds for each category.
     *
     * @param plugin The main plugin instance, used to locate the plugin data folder
     *               and for scheduling asynchronous tasks.
     */
    public SeedManager(SpeedRunPlus plugin) {
        this.plugin = plugin;

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
     * <p>
     * If the RANDOM category is selected, {@code null} is returned. Otherwise, a seed
     * from the chosen category is randomly picked.
     * </p>
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

    /**
     * Asynchronously adds a number of seeds to the given {@link SeedCategory.SeedType}.
     * <p>
     * This method schedules the addition to run on a separate thread to avoid blocking
     * the main server thread. Newly added seeds are persisted to their corresponding CSV files and are written to
     * memory to avoid having to restart/reload the server.
     * </p>
     *
     * @param seedType the category of seeds to add
     * @param amount   the number of seeds to add (between 1 and 10 (inclusive))
     * @param sender   the {@code CommandSender} responsible for adding the seed
     */
    public void addSeedAsync(SeedCategory.SeedType seedType, int amount, CommandSender sender) {
        if (amount < 1) {
            sender.sendMessage(Color.RED + "The amount must be greater than 0!");
            return;
        }
        if (amount > 10) {
            sender.sendMessage(Color.RED + "The amount must be less than 10!");
            return;
        }
        if (seedType == SeedCategory.SeedType.RANDOM) {
            sender.sendMessage(Color.RED + "No need to add seeds of this type!");
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            int newSeedCount = requestSeeds(seedType, amount);

            // Feedback
            Color color = newSeedCount == amount ? Color.GREEN : newSeedCount > amount / 2 ? Color.YELLOW : Color.RED;
            String successMessage = newSeedCount > 0 ? "Successfully" : "Unsuccessfully";
            String message = String.format(
                    "%s %s added %d new %s seeds!",
                    color,
                    successMessage,
                    newSeedCount,
                    seedType.name()
            );
            sender.sendMessage(message);
        });
    }

    private int requestSeeds(SeedCategory.SeedType seedType, int amount) {
        File seedFile = seedFiles.get(seedType);
        if (seedFile == null) return 0;

        // Find category in memory
        SeedCategory category = seedCategories.stream()
                .filter(c -> c.getSeedType() == seedType)
                .findFirst()
                .orElse(null);

        if (category == null) return 0;

        List<Long> existingSeeds = category.getSeeds();
        List<Long> newSeeds = new ArrayList<>();

        try {
            // Construct the API endpoint from the given seed type
            URI uri = new URI(configHandler.getFilteredSeedsApi().toString() + seedType.getFsgName());

            for (int i = 1; i <= amount; i++) {
                // Make the request to the API
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(uri)
                        .header("Accept", "application/json")
                        .GET()
                        .build();

                // Parse and add the new seeds
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                Optional<Long> seed = parseResponse(response.body());
                seed.ifPresent(newSeeds::add);
            }
        } catch (URISyntaxException | InterruptedException | IOException e) {
            logger.warning("[SRP] Failed to fetch seed: " + e.getMessage());
        }

        // Persist seeds to the seed file
        persistSeeds(seedType, newSeeds);

        // Write the seeds to memory (ensuring that a reload is not necessary)
        existingSeeds.addAll(newSeeds);

        logger.info("[SRP] Added " + newSeeds.size() + " seeds to " + seedType.name() + "!");
        return newSeeds.size();
    }

    private Optional<Long> parseResponse(String body) {
        if (body == null || body.isBlank()) return Optional.empty();
        try {
            int start = body.indexOf("\"seed\":\"") + 8;
            int end = body.indexOf('"', start);
            return Optional.of(Long.parseLong(body.substring(start, end)));
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            return Optional.empty();
        }
    }

    private void persistSeeds(SeedCategory.SeedType seedType, List<Long> seeds) {
        File seedFile = seedFiles.get(seedType);
        if (seedFile == null || seeds.isEmpty()) return;

        try (BufferedWriter writer = Files.newBufferedWriter(seedFile.toPath(), StandardCharsets.UTF_8,
                java.nio.file.StandardOpenOption.CREATE,
                java.nio.file.StandardOpenOption.APPEND)) {

            for (Long seed : seeds) {
                writer.write(seed.toString());
                writer.newLine();
            }

        } catch (IOException e) {
            logger.warning("[SRP] Failed to persist seeds to file " + seedFile.getName() + ": " + e.getMessage());
        }
    }
}
