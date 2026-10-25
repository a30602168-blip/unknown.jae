package unkown.jar;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/** Simple JSON config at config/unkownjar.json (created on first start). */
public final class UnkownConfig {
	/** 0 = automatic by in-game day, 1-4 = force a phase (for testing). */
	public int forcePhase = 0;
	/** Multiplier for the pause between encounters. 1.0 = normal, 0.1 = very often (testing). */
	public double encounterIntervalMultiplier = 1.0;
	/** Delay after entering the world until the first encounter. */
	public int firstEncounterDelaySeconds = 60;
	/** Phase 4 may kick caught players back to the main menu (safe: no crash, nothing is deleted). */
	public boolean disconnectEnabled = true;
	/** Chance (0-1) that a caught player is actually disconnected in phase 4. */
	public double disconnectChance = 0.85;
	/** Creative players are never hurt or disconnected. */
	public boolean creativePlayersImmune = true;
	public double damageMultiplier = 1.0;
	public double huntSpeedMultiplier = 1.0;
	/** Occasional whispers without a visible entity. */
	public boolean phantomSounds = true;
	/** If an unknown entity kills the player, that player's Minecraft client crashes shortly after. */
	public boolean crashOnDeath = true;
	/** Jumpscare (full-screen face + very loud sound) when an unknown entity kills you. WARNING: loud and flashing. */
	public boolean jumpscare = true;
	/** The entity sometimes writes lines into the chat. */
	public boolean chatMessages = true;
	/** One-time random event: "unknown joined the game" in chat (once per player and world). */
	public boolean joinEvent = true;
	/** ERROR (script monster) may replace a normal encounter from day 2 on. */
	public boolean errorEnabled = true;
	/** Chance (0-1) that an encounter from phase 2 on is ERROR instead of unknown. */
	public double errorChance = 0.15;
	/**
	 * Troubleshooting switch. 0 = normal. 1 = no client rendering, no encounters (entities are registered but never spawn).
	 * 2 = the mod registers nothing at all (only loads config). Use it to find out which part causes a black screen.
	 */
	public int safeMode = 0;
	/** Broken Files may appear at night from day 3 on. */
	public boolean brokenFilesEnabled = true;
	/** Chance (0-1) that a night encounter from day 3 on is Broken Files. */
	public double brokenFilesChance = 0.12;
	/** Broken Files smashes the blocks in its way while it hunts you. Set to false to protect your builds. */
	public boolean brokenFilesBreakBlocks = true;
	/** 0 Bits may appear at night from day 2 on. */
	public boolean zeroBitsEnabled = true;
	public double zeroBitsChance = 0.10;
	/** 0 Bits smashes the blocks in its way while it hunts you. */
	public boolean zeroBitsBreakBlocks = true;
	public boolean enabled = true;

	private static UnkownConfig instance = new UnkownConfig();
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	public static UnkownConfig get() {
		return instance;
	}

	public static void load() {
		Path path = FabricLoader.getInstance().getConfigDir().resolve("unkownjar.json");
		try {
			if (Files.exists(path)) {
				try (Reader reader = Files.newBufferedReader(path)) {
					UnkownConfig loaded = GSON.fromJson(reader, UnkownConfig.class);
					if (loaded != null) {
						instance = loaded;
					}
				}
			}
			try (Writer writer = Files.newBufferedWriter(path)) {
				GSON.toJson(instance, writer);
			}
		} catch (Exception e) {
			UnkownJar.LOGGER.warn("Could not read/write unkownjar.json, using defaults.", e);
		}
	}
}
