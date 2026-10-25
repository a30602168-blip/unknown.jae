package unkown.jar.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import unkown.jar.UnkownConfig;

import java.util.UUID;

/**
 * Broken Files: "Some errors should not exist." A very tall, black night stalker with a glowing white eye.
 * It does not go through walls: while it hunts you it smashes every block in its way. It creeps closer faster when you are not looking at it, leaves a trail of
 * glitch particles, distorts your screen when it is near, and strikes with a tentacle lash after a clear warning.
 */
public class BrokenFilesEntity extends PathfinderMob {
	public static final String NAME = "Broken Files";
	private static final int LASH_DURATION = 16;     // ticks of the lash animation
	private static final int WINDUP_TICKS = 26;      // warning before every lash
	private static final int LASH_RANGE = 10;        // blocks

	private static final EntityDataAccessor<Integer> DATA_LASH =
			SynchedEntityData.defineId(BrokenFilesEntity.class, EntityDataSerializers.INT);

	private enum Mode { LURK, APPROACH, WINDUP }

	private Mode mode = Mode.LURK;
	private UUID targetId;
	private boolean initialized;
	private int life;
	private int modeTicks;
	private int cooldown = 40;
	private int soundTimer = 30;
	private int distortTimer;
	private int chatLines;
	private int hits;

	public BrokenFilesEntity(EntityType<? extends PathfinderMob> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return PathfinderMob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, 200.0)
				.add(Attributes.MOVEMENT_SPEED, 0.25)
				.add(Attributes.FOLLOW_RANGE, 100.0)
				.add(Attributes.ATTACK_DAMAGE, 9.0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
				.add(Attributes.STEP_HEIGHT, 1.3);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_LASH, 0);
	}

	public int getLashTicks() {
		return this.entityData.get(DATA_LASH);
	}

	public boolean isWindingUp() {
		return this.mode == Mode.WINDUP;
	}

	public void bindToPlayer(UUID playerId) {
		this.targetId = playerId;
	}

	// ------------------------------------------------------------------ logic (runs in aiStep so the walk animation sees the movement)

	@Override
	public void aiStep() {
		if (this.level() instanceof ServerLevel sl) {
			serverLogic(sl);
		}
		super.aiStep();
	}

	private void serverLogic(ServerLevel sl) {
		if (!this.initialized) {
			this.initialized = true;
			setCustomName(Component.literal(NAME));
			setCustomNameVisible(false);
		}
		this.life++;
		this.modeTicks++;
		if (this.cooldown > 0) {
			this.cooldown--;
		}
		int lash = getLashTicks();
		if (lash > 0) {
			this.entityData.set(DATA_LASH, lash - 1);
		}

		ServerPlayer target = resolveTarget(sl);
		if (target == null) {
			discard();
			return;
		}
		double dist = distanceTo(target);
		boolean peaceful = sl.getDifficulty() == Difficulty.PEACEFUL;
		faceTarget(target);

		switch (this.mode) {
			case LURK -> {
				if (this.modeTicks == 1) {
					Fx.play(sl, ModSounds.BROKEN_HUM, getX(), getY(), getZ(), 3.0F, 1.0F);
				}
				if (this.modeTicks > 140) {
					this.mode = Mode.APPROACH;
					this.modeTicks = 0;
				}
			}
			case APPROACH -> approach(sl, target, dist, peaceful);
			case WINDUP -> windup(sl, target, dist);
		}

		if (!isRemoved()) {
			trailAndDistortion(sl, target, dist);
			ambient(sl, target);
			if (this.life > 2400 || dist > 120.0) {
				vanish(sl);
			}
		}
	}

	private ServerPlayer resolveTarget(ServerLevel sl) {
		Player p = this.targetId == null ? sl.getNearestPlayer(this, 128.0) : sl.getPlayerByUUID(this.targetId);
		if (p instanceof ServerPlayer sp && sp.isAlive() && !sp.isSpectator()) {
			this.targetId = sp.getUUID();
			return sp;
		}
		return null;
	}

	private void faceTarget(ServerPlayer target) {
		double dx = target.getX() - getX();
		double dz = target.getZ() - getZ();
		float yaw = (float) (Mth.atan2(dz, dx) * 180.0 / Math.PI) - 90.0F;
		setYRot(yaw);
		this.yBodyRot = yaw;
		this.yHeadRot = yaw;
	}

	/** Creeps closer; much faster whenever you are NOT looking at it, almost frozen when you are. */
	private void approach(ServerLevel sl, ServerPlayer target, double dist, boolean peaceful) {
		boolean seen = isLookedAt(target);
		double speed = seen ? 0.035 : 0.17;
		if (dist > 40.0) {
			speed *= 1.8; // closes the distance in the dark faster
		}
		if (dist > 4.5) {
			walk(sl, target, speed);
		}
		if (!peaceful && this.cooldown == 0 && (dist < 9.0 || (dist < 3.0))) {
			this.mode = Mode.WINDUP;
			this.modeTicks = 0;
			Fx.play(sl, ModSounds.BROKEN_LASH, getX(), getY(), getZ(), 2.0F, 0.6F);
			Fx.play(sl, ModSounds.ERROR_STATIC, target.getX(), target.getY(), target.getZ(), 1.4F, 0.7F);
		}
	}

	/** The warning: it stops, its tentacles pull back, sparks mark the line to you. Run, or break line of sight. */
	private void windup(ServerLevel sl, ServerPlayer target, double dist) {
		if (this.modeTicks % 4 == 0) {
			Vec3 from = new Vec3(getX(), getY() + 2.2, getZ());
			Vec3 to = new Vec3(target.getX(), target.getY() + 1.0, target.getZ());
			for (int i = 1; i <= 5; i++) {
				Vec3 p = from.lerp(to, i / 6.0);
				sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, 2, 0.15, 0.15, 0.15, 0.01);
			}
		}
		if (this.modeTicks == WINDUP_TICKS) {
			this.entityData.set(DATA_LASH, LASH_DURATION);
			Fx.play(sl, ModSounds.BROKEN_LASH, getX(), getY(), getZ(), 2.5F, 1.1F);
		}
		if (this.modeTicks == WINDUP_TICKS + LASH_DURATION / 2) {
			if (distanceTo(target) <= LASH_RANGE) {
				float dmg = (float) (getAttributeValue(Attributes.ATTACK_DAMAGE) * UnkownConfig.get().damageMultiplier);
				if (target.hurtServer(sl, damageSources().mobAttack(this), dmg)) {
					this.hits++;
					Fx.play(sl, ModSounds.ERROR_HIT, target.getX(), target.getY(), target.getZ(), 1.8F, 0.7F);
					Fx.particles(sl, ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY() + 1.0, target.getZ(), 20, 0.5);
				}
			}
		}
		if (this.modeTicks > WINDUP_TICKS + LASH_DURATION) {
			this.mode = Mode.APPROACH;
			this.modeTicks = 0;
			this.cooldown = 90;
			if (this.hits >= 2) {
				say(target, "file deleted.");
				vanish(sl);
			}
		}
	}

	/** Walks toward the player with normal collisions and smashes every block that is in its way. */
	private void walk(ServerLevel sl, ServerPlayer target, double speed) {
		Vec3 to = new Vec3(target.getX() - getX(), 0.0, target.getZ() - getZ());
		double len = to.length();
		if (len < 1.0E-3) {
			return;
		}
		Vec3 dir = to.scale(1.0 / len);
		double rise = target.getY() - getY();
		double vy = getDeltaMovement().y;
		if (rise > 1.5) {
			vy = 0.22; // the target is up on a tower: climb through the debris
		}
		setDeltaMovement(dir.x * speed, vy, dir.z * speed);
		breakBlocks(sl, dir, rise > 1.5 ? 1.0 : 0.0);
	}

	/** Destroys the blocks right in front of it (not the floor, not unbreakable blocks). */
	private void breakBlocks(ServerLevel sl, Vec3 dir, double up) {
		if (!UnkownConfig.get().brokenFilesBreakBlocks || this.life % 3 != 0) {
			return;
		}
		AABB box = getBoundingBox().expandTowards(dir.x * 1.5, up * 1.5, dir.z * 1.5).inflate(0.15, 0.0, 0.15);
		int minY = Math.max(Mth.floor(box.minY), Mth.floor(getY()));
		int broken = 0;
		for (BlockPos p : BlockPos.betweenClosed(Mth.floor(box.minX), minY, Mth.floor(box.minZ),
				Mth.floor(box.maxX), Mth.floor(box.maxY), Mth.floor(box.maxZ))) {
			if (broken >= 10) {
				break;
			}
			BlockState state = sl.getBlockState(p);
			if (state.isAir() || !state.getFluidState().isEmpty() || state.getDestroySpeed(sl, p) < 0.0F
					|| state.getCollisionShape(sl, p).isEmpty()) {
				continue;
			}
			sl.destroyBlock(p.immutable(), false, this);
			broken++;
		}
	}

	private boolean isLookedAt(ServerPlayer player) {
		Vec3 view = player.getViewVector(1.0F).normalize();
		Vec3 to = new Vec3(getX() - player.getX(), getY() + getBbHeight() * 0.8 - player.getEyeY(), getZ() - player.getZ());
		double len = to.length();
		if (len < 1.0E-4) {
			return true;
		}
		return view.dot(to.scale(1.0 / len)) > 0.93;
	}

	// ------------------------------------------------------------------ atmosphere

	private void trailAndDistortion(ServerLevel sl, ServerPlayer target, double dist) {
		if (this.life % 3 == 0) {
			sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY() + 0.2, getZ(), 2, 0.4, 0.1, 0.4, 0.02);
			sl.sendParticles(ParticleTypes.ASH, getX(), getY() + 1.0, getZ(), 3, 0.5, 1.0, 0.5, 0.02);
		}
		// screen distortion when it is near: darkness pulses, glitch text flashes, brief blindness very close
		if (dist < 18.0 && !target.isCreative() && --this.distortTimer <= 0) {
			this.distortTimer = 30 + this.random.nextInt(40);
			target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 50, 0, false, false, false));
			target.connection.send(new ClientboundSetTitlesAnimationPacket(0, 3, 2));
			target.connection.send(new ClientboundSetSubtitleTextPacket(Component.empty()));
			target.connection.send(new ClientboundSetTitleTextPacket(Component.literal("\u00a74\u00a7k||||||||||||||||")));
			if (dist < 8.0) {
				target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 14, 0, false, false, false));
			}
		}
	}

	private void ambient(ServerLevel sl, ServerPlayer target) {
		if (--this.soundTimer > 0) {
			return;
		}
		Fx.play(sl, ModSounds.BROKEN_HUM, getX(), getY() + 1.5, getZ(), 2.5F, 0.9F + this.random.nextFloat() * 0.2F);
		this.soundTimer = 110 + this.random.nextInt(100);
		if (this.random.nextFloat() < 0.35F) {
			say(target, null);
		}
	}

	private static final String[] LINES = {
			"some errors should not exist.", "it is not a monster.", "it is the error in the script.",
			"broken_files.dat", "file corrupted", "missing: you", "do not open me", "0 bytes",
			"you were not supposed to find me"
	};

	private void say(ServerPlayer target, String text) {
		if (!UnkownConfig.get().chatMessages || this.chatLines >= 4) {
			return;
		}
		this.chatLines++;
		String line = text != null ? text : LINES[this.random.nextInt(LINES.length)];
		target.sendSystemMessage(Component.literal("\u00a78<\u00a77" + NAME + "\u00a78> \u00a77" + line));
	}

	public void vanish(ServerLevel sl) {
		Fx.play(sl, ModSounds.ERROR_GLITCH, getX(), getY(), getZ(), 2.0F, 0.5F);
		Fx.particles(sl, ParticleTypes.ELECTRIC_SPARK, getX(), getY() + 1.8, getZ(), 60, 0.8);
		Fx.particles(sl, ParticleTypes.LARGE_SMOKE, getX(), getY() + 1.8, getZ(), 40, 0.8);
		discard();
	}

	// ------------------------------------------------------------------ damage & sounds

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
			return super.hurtServer(level, source, amount);
		}
		if (getHealth() - amount <= getMaxHealth() * 0.2F) {
			vanish(level);
			return false;
		}
		return super.hurtServer(level, source, amount);
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return ModSounds.ERROR_GLITCH;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return ModSounds.ERROR_SCREAM;
	}

	@Override
	public boolean isPushable() {
		return false;
	}
}
