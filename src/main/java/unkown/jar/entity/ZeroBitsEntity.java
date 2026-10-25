package unkown.jar.entity;

import net.minecraft.core.BlockPos;
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
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import unkown.jar.UnkownConfig;
import unkown.jar.director.SpawnFinder;

import java.util.UUID;

/**
 * 0 Bits: "It is not just an error anymore. It is the complete deletion." About 4.5 blocks tall.
 * It hunts you until it reaches you, smashing every block in its way (it never walks through walls),
 * leaves glitch particles and 0/1 traces, teleports right next to you when you are very close
 * (with a short warning shimmer), and turns aggressive: faster, louder, taller, mouth wide open.
 */
public class ZeroBitsEntity extends PathfinderMob {
	public static final String NAME = "0 Bits";
	private static final int ATTACK_DURATION = 14;
	private static final int HUNT_MAX_TICKS = 4800; // four minutes

	private static final EntityDataAccessor<Integer> DATA_ATTACK =
			SynchedEntityData.defineId(ZeroBitsEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> DATA_AGGRO =
			SynchedEntityData.defineId(ZeroBitsEntity.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Boolean> DATA_HUNTING =
			SynchedEntityData.defineId(ZeroBitsEntity.class, EntityDataSerializers.BOOLEAN);

	private UUID targetId;
	private boolean initialized;
	private int life;
	private int huntTicks;
	private int attackCooldown = 30;
	private int blinkCooldown = 120;
	private int blinkWarning;
	private BlockPos blinkSpot;
	private int soundTimer = 40;
	private int distortTimer;
	private int chatLines;
	private boolean hitDone;

	public ZeroBitsEntity(EntityType<? extends PathfinderMob> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return PathfinderMob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, 260.0)
				.add(Attributes.MOVEMENT_SPEED, 0.25)
				.add(Attributes.FOLLOW_RANGE, 110.0)
				.add(Attributes.ATTACK_DAMAGE, 10.0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
				.add(Attributes.STEP_HEIGHT, 1.4)
				.add(Attributes.SCALE, 1.0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_ATTACK, 0);
		builder.define(DATA_AGGRO, false);
		builder.define(DATA_HUNTING, false);
	}

	public int getAttackTicks() {
		return this.entityData.get(DATA_ATTACK);
	}

	public boolean isAggro() {
		return this.entityData.get(DATA_AGGRO);
	}

	public boolean isHunting() {
		return this.entityData.get(DATA_HUNTING);
	}

	public void bindToPlayer(UUID playerId) {
		this.targetId = playerId;
	}

	// ------------------------------------------------------------------ logic (in aiStep so the walk animation sees the movement)

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
		if (this.attackCooldown > 0) {
			this.attackCooldown--;
		}
		if (this.blinkCooldown > 0) {
			this.blinkCooldown--;
		}
		int attack = getAttackTicks();
		if (attack > 0) {
			this.entityData.set(DATA_ATTACK, attack - 1);
		}

		ServerPlayer target = resolveTarget(sl);
		if (target == null) {
			discard();
			return;
		}
		double dist = distanceTo(target);
		boolean peaceful = sl.getDifficulty() == Difficulty.PEACEFUL;
		faceTarget(target);

		if (!isHunting()) {
			// it stands and watches for a moment, the eye pulsing
			if (this.life == 1) {
				Fx.play(sl, ModSounds.BROKEN_HUM, getX(), getY(), getZ(), 3.0F, 0.8F);
				say(target, "0");
			}
			if (this.life > 120) {
				this.entityData.set(DATA_HUNTING, true);
				Fx.play(sl, ModSounds.ZERO_SCREAM, getX(), getY(), getZ(), 2.5F, 0.9F);
			}
		} else {
			this.huntTicks++;
			hunt(sl, target, dist, peaceful);
		}

		if (!isRemoved()) {
			trail(sl, target, dist);
			ambient(sl, target);
			if (this.huntTicks > HUNT_MAX_TICKS || dist > 130.0) {
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

	private void hunt(ServerLevel sl, ServerPlayer target, double dist, boolean peaceful) {
		// ---- aggro phase: after 45 seconds of hunting, after taking a real beating, or after the first hit
		if (!isAggro() && (this.huntTicks > 900 || getHealth() < getMaxHealth() * 0.6F)) {
			becomeAggro(sl, target);
		}
		double speed = isAggro() ? 0.30 : 0.19;
		speed *= Math.max(0.5, UnkownConfig.get().huntSpeedMultiplier);

		// ---- very close: it teleports right next to you (after a short shimmer that shows where)
		if (this.blinkWarning > 0) {
			this.blinkWarning--;
			sl.sendParticles(ParticleTypes.END_ROD, this.blinkSpot.getX() + 0.5, this.blinkSpot.getY() + 1.0, this.blinkSpot.getZ() + 0.5, 3, 0.4, 0.8, 0.4, 0.02);
			if (this.blinkWarning == 0) {
				doBlink(sl);
			}
		} else if (dist < 14.0 && dist > 4.5 && this.blinkCooldown == 0 && !peaceful && this.random.nextInt(60) == 0) {
			planBlink(sl, target);
		}

		// ---- walk and smash
		if (dist > 2.6 && this.blinkWarning == 0) {
			walk(sl, target, speed);
		}
		if (getNavigation().isInProgress()) {
			getNavigation().stop();
		}

		// ---- claw attack
		double reach = getBbWidth() * 0.5 + target.getBbWidth() * 0.5 + 2.0;
		if (getAttackTicks() == 0 && this.attackCooldown == 0 && !peaceful
				&& distanceToSqr(target) < reach * reach && Math.abs(target.getY() - getY()) < getBbHeight()) {
			this.entityData.set(DATA_ATTACK, ATTACK_DURATION);
			this.attackCooldown = isAggro() ? 26 : 38;
			this.hitDone = false;
			Fx.play(sl, ModSounds.BROKEN_LASH, getX(), getY(), getZ(), 2.0F, 0.7F);
		}
		if (getAttackTicks() > 0 && getAttackTicks() <= ATTACK_DURATION / 2 && !this.hitDone) {
			this.hitDone = true;
			double r2 = reach + 0.9;
			if (distanceToSqr(target) < r2 * r2) {
				float dmg = (float) (getAttributeValue(Attributes.ATTACK_DAMAGE) * (isAggro() ? 1.4 : 1.0) * UnkownConfig.get().damageMultiplier);
				if (target.hurtServer(sl, damageSources().mobAttack(this), dmg)) {
					Fx.play(sl, ModSounds.ERROR_HIT, target.getX(), target.getY(), target.getZ(), 1.9F, 0.65F);
					Fx.particles(sl, ParticleTypes.END_ROD, target.getX(), target.getY() + 1.0, target.getZ(), 16, 0.5);
					if (!isAggro()) {
						becomeAggro(sl, target);
					}
				}
			}
		}
	}

	private void becomeAggro(ServerLevel sl, ServerPlayer target) {
		this.entityData.set(DATA_AGGRO, true);
		AttributeInstance scale = getAttribute(Attributes.SCALE);
		if (scale != null) {
			scale.setBaseValue(1.12); // the body changes: taller
		}
		Fx.play(sl, ModSounds.ZERO_SCREAM, getX(), getY(), getZ(), 4.0F, 0.75F);
		say(target, "there are no textures left. only 0 bits.");
	}

	/** Walks toward the player with normal collisions and destroys every block that is in its way. */
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
			vy = 0.24;
		}
		setDeltaMovement(dir.x * speed, vy, dir.z * speed);
		breakBlocks(sl, dir, rise > 1.5 ? 1.0 : 0.0);
	}

	private void breakBlocks(ServerLevel sl, Vec3 dir, double up) {
		if (!UnkownConfig.get().zeroBitsBreakBlocks || this.life % 2 != 0) {
			return;
		}
		AABB box = getBoundingBox().expandTowards(dir.x * 1.6, up * 1.6, dir.z * 1.6).inflate(0.2, 0.0, 0.2);
		int minY = Math.max(Mth.floor(box.minY), Mth.floor(getY()));
		int broken = 0;
		for (BlockPos p : BlockPos.betweenClosed(Mth.floor(box.minX), minY, Mth.floor(box.minZ),
				Mth.floor(box.maxX), Mth.floor(box.maxY), Mth.floor(box.maxZ))) {
			if (broken >= 14) {
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

	private void planBlink(ServerLevel sl, ServerPlayer target) {
		for (int i = 0; i < 10; i++) {
			double angle = this.random.nextDouble() * Math.PI * 2;
			double d = 2.8 + this.random.nextDouble() * 1.2;
			BlockPos spot = SpawnFinder.groundAt(sl, target.getX() + Math.cos(angle) * d, target.getZ() + Math.sin(angle) * d, target.getBlockY(), 4);
			if (spot != null) {
				this.blinkSpot = spot;
				this.blinkWarning = 12; // 0.6 s shimmer at the destination
				Fx.play(sl, ModSounds.ERROR_GLITCH, spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, 1.4F, 1.3F);
				return;
			}
		}
		this.blinkCooldown = 40;
	}

	private void doBlink(ServerLevel sl) {
		if (this.blinkSpot == null) {
			return;
		}
		Fx.particles(sl, ParticleTypes.END_ROD, getX(), getY() + 2.0, getZ(), 30, 0.7);
		Fx.play(sl, ModSounds.ERROR_GLITCH, getX(), getY(), getZ(), 2.0F, 0.6F);
		setPos(this.blinkSpot.getX() + 0.5, this.blinkSpot.getY(), this.blinkSpot.getZ() + 0.5);
		Fx.particles(sl, ParticleTypes.END_ROD, getX(), getY() + 2.0, getZ(), 30, 0.7);
		this.blinkCooldown = 220;
		this.attackCooldown = Math.min(this.attackCooldown, 6);
	}

	// ------------------------------------------------------------------ atmosphere

	private void trail(ServerLevel sl, ServerPlayer target, double dist) {
		if (this.life % 2 == 0) {
			sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 0.3, getZ(), 2, 0.5, 0.2, 0.5, 0.01);
			sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY() + 1.5, getZ(), 2, 0.6, 1.2, 0.6, 0.02);
			sl.sendParticles(ParticleTypes.ASH, getX(), getY() + 2.0, getZ(), 3, 0.6, 1.5, 0.6, 0.02);
		}
		// screen distortion when it is near: darkness pulses and a flicker of binary on the screen
		if (dist < 20.0 && !target.isCreative() && --this.distortTimer <= 0) {
			this.distortTimer = 26 + this.random.nextInt(30);
			target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 50, 0, false, false, false));
			target.connection.send(new ClientboundSetTitlesAnimationPacket(0, 3, 2));
			target.connection.send(new ClientboundSetSubtitleTextPacket(Component.empty()));
			target.connection.send(new ClientboundSetTitleTextPacket(Component.literal("\u00a77\u00a7k01001101 00110000 01100010")));
		}
	}

	private void ambient(ServerLevel sl, ServerPlayer target) {
		if (--this.soundTimer > 0) {
			return;
		}
		SoundEvent s = isAggro() ? ModSounds.ZERO_SCREAM : ModSounds.BROKEN_HUM;
		Fx.play(sl, s, getX(), getY() + 2.0, getZ(), isAggro() ? 3.0F : 2.5F, 0.8F + this.random.nextFloat() * 0.2F);
		this.soundTimer = (isAggro() ? 70 : 120) + this.random.nextInt(100);
		if (this.random.nextFloat() < 0.35F) {
			say(target, null);
		}
	}

	private static final String[] LINES = {
			"0", "00000000", "1", "it is not just an error anymore.", "it is the complete deletion.",
			"there are no textures left.", "0x00000000", "null", "delete", "i can not be reached"
	};

	private void say(ServerPlayer target, String text) {
		if (!UnkownConfig.get().chatMessages || this.chatLines >= 5) {
			return;
		}
		this.chatLines++;
		String line = text != null ? text : LINES[this.random.nextInt(LINES.length)];
		target.sendSystemMessage(Component.literal("\u00a78<\u00a7f" + NAME + "\u00a78> \u00a77" + line));
	}

	public void vanish(ServerLevel sl) {
		Fx.play(sl, ModSounds.ERROR_GLITCH, getX(), getY(), getZ(), 2.0F, 0.5F);
		Fx.particles(sl, ParticleTypes.END_ROD, getX(), getY() + 2.0, getZ(), 70, 0.9);
		Fx.particles(sl, ParticleTypes.LARGE_SMOKE, getX(), getY() + 2.0, getZ(), 40, 0.9);
		discard();
	}

	// ------------------------------------------------------------------ damage & sounds

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
			return super.hurtServer(level, source, amount);
		}
		if (getHealth() - amount <= getMaxHealth() * 0.15F) {
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
		return ModSounds.ZERO_SCREAM;
	}

	@Override
	public boolean isPushable() {
		return false;
	}
}
