package io.github.tamawish.rwr.config;

import io.github.tamawish.rwr.multiverse.SeedPolicy;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

/** Loads, validates, and atomically persists the versioned YAML configuration. */
public final class ConfigRepository {
  private static final int CONFIG_VERSION = 5;
  private static final int MANAGED_WORLDS_VERSION = 1;
  private static final long MAX_CONFIG_BYTES = 1_048_576L;
  private static final Set<String> V4_MARKERS =
      Set.of(
          "worldName",
          "worldSeed",
          "resetInterval",
          "warningTimes",
          "world-reset",
          "resource-world");

  private final Path configFile;
  private final Path managedWorldsFile;
  private final WorldCatalogView catalog;
  private final ConfigValidator validator;

  /**
   * Creates a repository for one configuration file.
   *
   * @param configFile YAML file to read and replace atomically
   * @param catalog provider catalog used during validation
   */
  public ConfigRepository(Path configFile, WorldCatalogView catalog) {
    this(configFile, configFile.resolveSibling("managed-worlds.yml"), catalog);
  }

  public ConfigRepository(Path configFile, Path managedWorldsFile, WorldCatalogView catalog) {
    this.configFile = configFile;
    this.managedWorldsFile = managedWorldsFile;
    this.catalog = catalog;
    this.validator = new ConfigValidator();
  }

  /**
   * Loads and validates the configured YAML file.
   *
   * @return valid settings or a result containing migration or validation issues
   */
  public ConfigLoadResult load() {
    if (!Files.isRegularFile(configFile)) {
      return ConfigLoadResult.invalid(
          List.of(new ConfigIssue("config.yml", "file does not exist")));
    }

    YamlNode yaml;
    try {
      long size = Files.size(configFile);
      if (size > MAX_CONFIG_BYTES) {
        throw new IOException("file exceeds " + MAX_CONFIG_BYTES + " bytes");
      }
      LoaderOptions options = loaderOptions();
      Object loaded =
          new Yaml(new SafeConstructor(options))
              .load(Files.readString(configFile, StandardCharsets.UTF_8));
      yaml = YamlNode.root(loaded);
    } catch (IOException | RuntimeException exception) {
      return ConfigLoadResult.invalid(
          List.of(new ConfigIssue("config.yml", "cannot be read: " + exception.getMessage())));
    }

    ConfigLoadResult versionResult = inspectVersion(yaml);
    if (versionResult != null) {
      return versionResult;
    }

    List<ConfigIssue> issues = new ArrayList<>();
    YamlNode managed = loadManagedWorlds(issues);
    PluginSettings settings = parse(yaml, managed, issues);
    issues.addAll(validator.validate(settings, catalog));
    return issues.isEmpty() ? ConfigLoadResult.valid(settings) : ConfigLoadResult.invalid(issues);
  }

  /**
   * Validates and atomically replaces the configured YAML file.
   *
   * @param settings settings to serialize
   * @throws IOException if the temporary file cannot be written or moved
   * @throws ConfigValidationException if the settings fail validation
   */
  public void save(PluginSettings settings) throws IOException, ConfigValidationException {
    List<ConfigIssue> issues = validator.validate(settings, catalog);
    if (!issues.isEmpty()) {
      throw new ConfigValidationException(issues);
    }

    ConfigLoadResult current = load();
    if (!current.valid()) {
      throw new IOException("current configuration is invalid and will not be overwritten");
    }
    boolean managedChanged =
        !Files.isRegularFile(managedWorldsFile)
            || !current.settings().worlds().equals(settings.worlds())
            || !current.settings().teleport().worlds().equals(settings.teleport().worlds());
    if (managedChanged) {
      writeAtomically(managedWorldsFile, managedContent(settings));
    }
    if (!sameGlobals(current.settings(), settings)) {
      YamlNode document = loadDocumentForUpdate(configFile);
      writeAtomically(configFile, yamlWriter().dump(serializeGlobals(document, settings)));
    }
  }

  public void initializeManagedWorldsFile() throws IOException, ConfigValidationException {
    if (Files.isRegularFile(managedWorldsFile)) {
      return;
    }
    ConfigLoadResult loaded = load();
    if (!loaded.valid()) {
      throw new IOException("configuration is invalid and cannot be migrated");
    }
    writeAtomically(managedWorldsFile, managedContent(loaded.settings()));
  }

  private static String managedContent(PluginSettings settings) {
    return "# Managed by ResourceWorldResetter. Use /rwr gui to configure resource worlds.\n"
        + yamlWriter().dump(serializeManaged(settings));
  }

  private static boolean sameGlobals(PluginSettings left, PluginSettings right) {
    return left.configVersion() == right.configVersion()
        && left.timezone().equals(right.timezone())
        && left.defaultHubWorld().equals(right.defaultHubWorld())
        && left.resetPolicy().equals(right.resetPolicy())
        && left.teleport().autoDiscover() == right.teleport().autoDiscover()
        && left.teleport().defaultEnabled() == right.teleport().defaultEnabled()
        && left.teleport().showLocked() == right.teleport().showLocked();
  }

  private static void writeAtomically(Path file, String content) throws IOException {
    Path parent = file.toAbsolutePath().getParent();
    if (parent == null) {
      throw new IOException("Configuration path has no parent directory");
    }
    Files.createDirectories(parent);
    Path temporary = Files.createTempFile(parent, file.getFileName().toString() + '.', ".tmp");
    try {
      Files.writeString(temporary, content, StandardCharsets.UTF_8);
      try {
        Files.move(
            temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
      } catch (AtomicMoveNotSupportedException exception) {
        Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
      }
    } finally {
      Files.deleteIfExists(temporary);
    }
  }

  private YamlNode loadDocumentForUpdate(Path file) throws IOException {
    if (!Files.isRegularFile(file)) {
      return new YamlNode(new LinkedHashMap<>());
    }
    long size = Files.size(file);
    if (size > MAX_CONFIG_BYTES) {
      throw new IOException("file exceeds " + MAX_CONFIG_BYTES + " bytes");
    }
    try {
      Object loaded =
          new Yaml(new SafeConstructor(loaderOptions()))
              .load(Files.readString(file, StandardCharsets.UTF_8));
      if (loaded != null && !(loaded instanceof Map)) {
        throw new IOException("configuration root must be a mapping");
      }
      return YamlNode.root(loaded);
    } catch (RuntimeException exception) {
      throw new IOException("current configuration cannot be parsed", exception);
    }
  }

  private static LoaderOptions loaderOptions() {
    LoaderOptions options = new LoaderOptions();
    options.setCodePointLimit((int) MAX_CONFIG_BYTES);
    options.setMaxAliasesForCollections(50);
    options.setNestingDepthLimit(50);
    options.setAllowDuplicateKeys(false);
    return options;
  }

  private YamlNode loadManagedWorlds(List<ConfigIssue> issues) {
    if (!Files.isRegularFile(managedWorldsFile)) {
      return null;
    }
    try {
      YamlNode managed = loadDocumentForUpdate(managedWorldsFile);
      if (!managed.isInt("managed-worlds-version")
          || managed.getInt("managed-worlds-version") != MANAGED_WORLDS_VERSION) {
        issues.add(
            new ConfigIssue(
                "managed-worlds.yml.managed-worlds-version", "integer value 1 is required"));
      }
      return managed;
    } catch (IOException exception) {
      issues.add(
          new ConfigIssue("managed-worlds.yml", "cannot be read: " + exception.getMessage()));
      return new YamlNode(new LinkedHashMap<>());
    }
  }

  private static Yaml yamlWriter() {
    DumperOptions options = new DumperOptions();
    options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
    options.setPrettyFlow(false);
    options.setIndent(2);
    options.setIndicatorIndent(2);
    options.setIndentWithIndicator(true);
    options.setSplitLines(false);
    return new Yaml(options);
  }

  private ConfigLoadResult inspectVersion(YamlNode yaml) {
    if (!yaml.isInt("config-version")) {
      boolean legacy = V4_MARKERS.stream().anyMatch(yaml::contains);
      if (legacy) {
        return migration(
            "A v4 configuration was detected. Create a new v5 config and migrate values manually.");
      }
      return ConfigLoadResult.invalid(
          List.of(new ConfigIssue("config-version", "integer value 5 is required")));
    }
    int version = yaml.getInt("config-version");
    if (version < CONFIG_VERSION) {
      return migration("Configuration version " + version + " cannot be loaded as v5.");
    }
    if (version > CONFIG_VERSION) {
      return ConfigLoadResult.invalid(
          List.of(
              new ConfigIssue(
                  "config-version", "version " + version + " is newer than this plugin supports")));
    }
    return null;
  }

  private static ConfigLoadResult migration(String message) {
    return ConfigLoadResult.migrationRequired(List.of(new ConfigIssue("config-version", message)));
  }

  private PluginSettings parse(YamlNode yaml, YamlNode managed, List<ConfigIssue> issues) {
    ZoneId timezone = parseZone(yaml, issues);
    String hub = requiredString(yaml, "default-hub-world", issues);
    ResetPolicySettings resetPolicy = parseResetPolicy(yaml, issues);
    Map<String, ManagedWorldSettings> worlds =
        parseWorlds(managed == null ? yaml : managed, hub, issues);
    TeleportSettings teleport = parseTeleport(yaml, managed, issues);
    return new PluginSettings(CONFIG_VERSION, timezone, hub, resetPolicy, worlds, teleport);
  }

  private static ZoneId parseZone(YamlNode yaml, List<ConfigIssue> issues) {
    String value = requiredString(yaml, "timezone", issues);
    if (value.isBlank()) {
      return ZoneId.of("UTC");
    }
    try {
      return ZoneId.of(value);
    } catch (DateTimeException exception) {
      issues.add(new ConfigIssue("timezone", "unknown time zone '" + value + "'"));
      return ZoneId.of("UTC");
    }
  }

  private static ResetPolicySettings parseResetPolicy(YamlNode yaml, List<ConfigIssue> issues) {
    YamlNode section = requiredSection(yaml, "reset-policy", issues);
    if (section == null) {
      return new ResetPolicySettings(0, 0, false);
    }
    return new ResetPolicySettings(
        requiredInt(section, "max-safe-retries", "reset-policy.max-safe-retries", issues),
        requiredInt(section, "retry-delay-seconds", "reset-policy.retry-delay-seconds", issues),
        requiredBoolean(
            section, "broadcast-completion", "reset-policy.broadcast-completion", issues));
  }

  private Map<String, ManagedWorldSettings> parseWorlds(
      YamlNode yaml, String hub, List<ConfigIssue> issues) {
    YamlNode section = requiredSection(yaml, "worlds", issues);
    Map<String, ManagedWorldSettings> worlds = new LinkedHashMap<>();
    if (section == null) {
      return worlds;
    }
    for (String id : section.keys()) {
      String path = "worlds." + id;
      YamlNode world = section.section(id);
      if (world == null) {
        issues.add(new ConfigIssue(path, "must be a section"));
        continue;
      }
      String multiverseWorld =
          requiredString(world, "multiverse-world", path + ".multiverse-world", issues);
      String displayName = requiredString(world, "display-name", path + ".display-name", issues);
      boolean enabled = requiredBoolean(world, "enabled", path + ".enabled", issues);
      if (world.contains("managed") && !world.isBoolean("managed")) {
        issues.add(new ConfigIssue(path + ".managed", "must be a boolean when present"));
      }
      boolean managed = world.isBoolean("managed") && world.getBoolean("managed");
      ScheduleSettings schedule = parseSchedule(world, path, issues);
      List<Integer> warnings = parseWarningMinutes(world, path, issues);
      RegenerationSettings regeneration = parseRegeneration(world, path, issues);
      EvacuationSettings evacuation = parseEvacuation(world, path, issues);
      WorldOperationalState state =
          WorldStateResolver.resolve(multiverseWorld, enabled, managed, hub, catalog);
      worlds.put(
          id,
          new ManagedWorldSettings(
              id,
              multiverseWorld,
              displayName,
              enabled,
              managed,
              schedule,
              warnings,
              regeneration,
              evacuation,
              state));
    }
    return worlds;
  }

  private static ScheduleSettings parseSchedule(
      YamlNode world, String path, List<ConfigIssue> issues) {
    YamlNode section = requiredSection(world, "schedule", path + ".schedule", issues);
    if (section == null) {
      return null;
    }
    ScheduleType type =
        parseEnum(
            section,
            "type",
            path + ".schedule.type",
            ScheduleType.class,
            ScheduleType.DAILY,
            issues);
    LocalTime time = parseTime(section, path + ".schedule.time", issues);
    DayOfWeek dayOfWeek = parseOptionalDay(section, path, issues);
    int dayOfMonth =
        optionalInt(section, "day-of-month", 0, path + ".schedule.day-of-month", issues);
    int intervalMinutes =
        optionalInt(section, "interval-minutes", 0, path + ".schedule.interval-minutes", issues);
    return new ScheduleSettings(type, time, dayOfWeek, dayOfMonth, intervalMinutes);
  }

  private static LocalTime parseTime(YamlNode section, String path, List<ConfigIssue> issues) {
    if (!section.contains("time")) {
      return null;
    }
    if (!section.isString("time")) {
      issues.add(new ConfigIssue(path, "must be a string in HH:mm format"));
      return null;
    }
    try {
      return LocalTime.parse(section.getString("time", ""));
    } catch (DateTimeParseException exception) {
      issues.add(new ConfigIssue(path, "must use valid HH:mm format"));
      return null;
    }
  }

  private static DayOfWeek parseOptionalDay(
      YamlNode section, String path, List<ConfigIssue> issues) {
    if (!section.contains("day-of-week")) {
      return null;
    }
    return parseEnum(
        section, "day-of-week", path + ".schedule.day-of-week", DayOfWeek.class, null, issues);
  }

  private static List<Integer> parseWarningMinutes(
      YamlNode world, String path, List<ConfigIssue> issues) {
    if (world.isList("warning-minutes")) {
      return parseIntegerList(world, "warning-minutes", path + ".warning-minutes", issues);
    }
    if (world.isList("warnings")) {
      List<Integer> migrated = new ArrayList<>();
      for (int seconds : parseIntegerList(world, "warnings", path + ".warnings", issues)) {
        if (seconds >= 60) {
          int minutes = seconds / 60;
          if (!migrated.contains(minutes)) {
            migrated.add(minutes);
          }
        }
      }
      migrated.sort((left, right) -> Integer.compare(right, left));
      return List.copyOf(migrated);
    }
    issues.add(new ConfigIssue(path + ".warning-minutes", "must be a list of integer minutes"));
    return List.of();
  }

  private static List<Integer> parseIntegerList(
      YamlNode world, String key, String path, List<ConfigIssue> issues) {
    if (!world.isList(key)) {
      return List.of();
    }
    List<Integer> warnings = new ArrayList<>();
    for (Object item : world.getList(key, List.of())) {
      if (item instanceof Integer value) {
        warnings.add(value);
      } else {
        issues.add(new ConfigIssue(path, "contains a non-integer value"));
      }
    }
    return warnings;
  }

  private static RegenerationSettings parseRegeneration(
      YamlNode world, String path, List<ConfigIssue> issues) {
    YamlNode section = requiredSection(world, "regeneration", path + ".regeneration", issues);
    if (section == null) {
      return null;
    }
    SeedPolicy seedPolicy =
        parseEnum(
            section,
            "seed-policy",
            path + ".regeneration.seed-policy",
            SeedPolicy.class,
            SeedPolicy.SAME,
            issues);
    Long fixedSeed = optionalLong(section, "fixed-seed", path + ".regeneration.fixed-seed", issues);
    return new RegenerationSettings(
        seedPolicy,
        seedPolicy == SeedPolicy.FIXED ? fixedSeed : null,
        requiredBoolean(
            section, "keep-world-config", path + ".regeneration.keep-world-config", issues),
        requiredBoolean(section, "keep-gamerules", path + ".regeneration.keep-gamerules", issues),
        requiredBoolean(
            section, "keep-world-border", path + ".regeneration.keep-world-border", issues));
  }

  private static EvacuationSettings parseEvacuation(
      YamlNode world, String path, List<ConfigIssue> issues) {
    YamlNode section = requiredSection(world, "evacuation", path + ".evacuation", issues);
    if (section == null) {
      return null;
    }
    return new EvacuationSettings(
        requiredBoolean(section, "enabled", path + ".evacuation.enabled", issues),
        optionalString(section, "destination", path + ".evacuation.destination", issues));
  }

  private static TeleportSettings parseTeleport(
      YamlNode yaml, YamlNode managed, List<ConfigIssue> issues) {
    YamlNode section = requiredSection(yaml, "teleport", issues);
    if (section == null) {
      return new TeleportSettings(false, false, true, Map.of());
    }
    boolean autoDiscover =
        requiredBoolean(section, "auto-discover", "teleport.auto-discover", issues);
    boolean defaultEnabled =
        requiredBoolean(section, "default-enabled", "teleport.default-enabled", issues);
    boolean showLocked = requiredBoolean(section, "show-locked", "teleport.show-locked", issues);
    YamlNode worldsSection =
        managed == null ? section.section("worlds") : managed.section("teleport-worlds");
    Map<String, TeleportDestinationSettings> worlds = new LinkedHashMap<>();
    if (worldsSection != null) {
      for (String worldName : worldsSection.keys()) {
        String path = "teleport.worlds." + worldName;
        YamlNode destination = worldsSection.section(worldName);
        if (destination == null) {
          issues.add(new ConfigIssue(path, "must be a section"));
          continue;
        }
        worlds.put(
            worldName,
            new TeleportDestinationSettings(
                requiredBoolean(destination, "enabled", path + ".enabled", issues),
                optionalString(destination, "permission", path + ".permission", issues)));
      }
    }
    return new TeleportSettings(autoDiscover, defaultEnabled, showLocked, worlds);
  }

  private static Map<String, Object> serializeGlobals(YamlNode yaml, PluginSettings settings) {
    yaml.set("config-version", settings.configVersion());
    yaml.set("timezone", settings.timezone().getId());
    yaml.set("default-hub-world", settings.defaultHubWorld());
    yaml.set("reset-policy.max-safe-retries", settings.resetPolicy().maxSafeRetries());
    yaml.set("reset-policy.retry-delay-seconds", settings.resetPolicy().retryDelaySeconds());
    yaml.set("reset-policy.broadcast-completion", settings.resetPolicy().broadcastCompletion());
    yaml.set("teleport.auto-discover", settings.teleport().autoDiscover());
    yaml.set("teleport.default-enabled", settings.teleport().defaultEnabled());
    yaml.set("teleport.show-locked", settings.teleport().showLocked());
    return yaml.raw();
  }

  private static Map<String, Object> serializeManaged(PluginSettings settings) {
    YamlNode yaml = new YamlNode(new LinkedHashMap<>());
    yaml.set("managed-worlds-version", MANAGED_WORLDS_VERSION);
    yaml.set("worlds", new LinkedHashMap<String, Object>());
    for (ManagedWorldSettings world : settings.worlds().values()) {
      String path = "worlds." + world.id();
      yaml.set(path + ".multiverse-world", world.multiverseWorld());
      yaml.set(path + ".display-name", world.displayName());
      yaml.set(path + ".enabled", world.enabled());
      yaml.set(path + ".managed", world.managed());
      writeSchedule(yaml, path, world.schedule());
      yaml.set(path + ".warning-minutes", YamlNode.copyList(world.warnings()));
      writeRegeneration(yaml, path, world.regeneration());
      yaml.set(path + ".evacuation.enabled", world.evacuation().enabled());
      yaml.set(path + ".evacuation.destination", world.evacuation().destination());
    }
    yaml.set("teleport-worlds", new LinkedHashMap<String, Object>());
    YamlNode teleportWorlds = yaml.section("teleport-worlds");
    settings
        .teleport()
        .worlds()
        .forEach(
            (name, destination) -> {
              teleportWorlds.setLiteral(name, new LinkedHashMap<String, Object>());
              YamlNode destinationNode = teleportWorlds.section(name);
              destinationNode.setLiteral("enabled", destination.enabled());
              destinationNode.setLiteral("permission", destination.permission());
            });
    return yaml.raw();
  }

  private static void writeSchedule(YamlNode yaml, String path, ScheduleSettings schedule) {
    yaml.set(path + ".schedule.type", schedule.type().name());
    if (schedule.time() != null) {
      yaml.set(path + ".schedule.time", schedule.time().toString());
    }
    if (schedule.dayOfWeek() != null) {
      yaml.set(path + ".schedule.day-of-week", schedule.dayOfWeek().name());
    }
    yaml.set(path + ".schedule.day-of-month", schedule.dayOfMonth());
    yaml.set(path + ".schedule.interval-minutes", schedule.intervalMinutes());
  }

  private static void writeRegeneration(
      YamlNode yaml, String path, RegenerationSettings regeneration) {
    yaml.set(path + ".regeneration.seed-policy", regeneration.seedPolicy().name());
    if (regeneration.fixedSeed() != null) {
      yaml.set(path + ".regeneration.fixed-seed", regeneration.fixedSeed());
    }
    yaml.set(path + ".regeneration.keep-world-config", regeneration.keepWorldConfig());
    yaml.set(path + ".regeneration.keep-gamerules", regeneration.keepGameRules());
    yaml.set(path + ".regeneration.keep-world-border", regeneration.keepWorldBorder());
  }

  private static YamlNode requiredSection(YamlNode parent, String key, List<ConfigIssue> issues) {
    return requiredSection(parent, key, key, issues);
  }

  private static YamlNode requiredSection(
      YamlNode parent, String key, String path, List<ConfigIssue> issues) {
    YamlNode section = parent.section(key);
    if (section == null) {
      issues.add(new ConfigIssue(path, "section is required"));
    }
    return section;
  }

  private static String requiredString(YamlNode section, String key, List<ConfigIssue> issues) {
    return requiredString(section, key, key, issues);
  }

  private static String requiredString(
      YamlNode section, String key, String path, List<ConfigIssue> issues) {
    if (!section.isString(key) || section.getString(key, "").isBlank()) {
      issues.add(new ConfigIssue(path, "non-blank string is required"));
      return "";
    }
    return section.getString(key, "").trim();
  }

  private static String optionalString(
      YamlNode section, String key, String path, List<ConfigIssue> issues) {
    if (!section.contains(key)) {
      return "";
    }
    if (!section.isString(key)) {
      issues.add(new ConfigIssue(path, "must be a string"));
      return "";
    }
    return section.getString(key, "").trim();
  }

  private static boolean requiredBoolean(
      YamlNode section, String key, String path, List<ConfigIssue> issues) {
    if (!section.isBoolean(key)) {
      issues.add(new ConfigIssue(path, "boolean is required"));
      return false;
    }
    return section.getBoolean(key);
  }

  private static int requiredInt(
      YamlNode section, String key, String path, List<ConfigIssue> issues) {
    if (!section.isInt(key)) {
      issues.add(new ConfigIssue(path, "integer is required"));
      return 0;
    }
    return section.getInt(key);
  }

  private static int optionalInt(
      YamlNode section, String key, int fallback, String path, List<ConfigIssue> issues) {
    if (!section.contains(key)) {
      return fallback;
    }
    return requiredInt(section, key, path, issues);
  }

  private static Long optionalLong(
      YamlNode section, String key, String path, List<ConfigIssue> issues) {
    if (!section.contains(key)) {
      return null;
    }
    Object value = section.get(key);
    if (value instanceof Number number) {
      return number.longValue();
    }
    issues.add(new ConfigIssue(path, "signed 64-bit integer is required"));
    return null;
  }

  private static <E extends Enum<E>> E parseEnum(
      YamlNode section,
      String key,
      String path,
      Class<E> type,
      E fallback,
      List<ConfigIssue> issues) {
    if (!section.isString(key)) {
      issues.add(new ConfigIssue(path, "string value is required"));
      return fallback;
    }
    String value = section.getString(key, "").toUpperCase(Locale.ROOT);
    try {
      return Enum.valueOf(type, value);
    } catch (IllegalArgumentException exception) {
      issues.add(new ConfigIssue(path, "unsupported value '" + value + "'"));
      return fallback;
    }
  }
}
