package unkown.jar.entity;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import unkown.jar.UnkownJar;

public final class ModEntities {
	public static final ResourceKey<EntityType<?>> UNKNOWN_KEY =
			ResourceKey.create(Registries.ENTITY_TYPE, UnkownJar.id("unknown"));

	public static final EntityType<UnknownEntity> UNKNOWN = Registry.register(
			BuiltInRegistries.ENTITY_TYPE,
			UNKNOWN_KEY,
			EntityType.Builder.<UnknownEntity>of(UnknownEntity::new, MobCategory.MONSTER)
					.sized(0.6F, 1.95F)
					.eyeHeight(1.74F)
					.clientTrackingRange(10)
					.fireImmune()
					.build(UNKNOWN_KEY));

	public static final ResourceKey<EntityType<?>> NEB_KEY =
			ResourceKey.create(Registries.ENTITY_TYPE, UnkownJar.id("neb"));

	/** neb ("Ben" backwards): the missing friend in monster form. Same entity class, own type so /summon unkownjar:neb works. */
	public static final EntityType<UnknownEntity> NEB = Registry.register(
			BuiltInRegistries.ENTITY_TYPE,
			NEB_KEY,
			EntityType.Builder.<UnknownEntity>of(UnknownEntity::new, MobCategory.MONSTER)
					.sized(0.6F, 1.95F)
					.eyeHeight(1.74F)
					.clientTrackingRange(10)
					.fireImmune()
					.build(NEB_KEY));

	public static final ResourceKey<EntityType<?>> ERROR_KEY =
			ResourceKey.create(Registries.ENTITY_TYPE, UnkownJar.id("error"));

	/** ERROR, the script monster: a completely separate entity (no phases), tall, with its own sounds. */
	public static final EntityType<ErrorEntity> ERROR = Registry.register(
			BuiltInRegistries.ENTITY_TYPE,
			ERROR_KEY,
			EntityType.Builder.<ErrorEntity>of(ErrorEntity::new, MobCategory.MONSTER)
					.sized(0.7F, 3.2F)
					.eyeHeight(2.9F)
					.clientTrackingRange(10)
					.fireImmune()
					.build(ERROR_KEY));

	public static final ResourceKey<EntityType<?>> BROKEN_KEY =
			ResourceKey.create(Registries.ENTITY_TYPE, UnkownJar.id("broken_files"));

	/** Broken Files: a tall night stalker that smashes through blocks. Completely separate from the phases. */
	public static final EntityType<BrokenFilesEntity> BROKEN = Registry.register(
			BuiltInRegistries.ENTITY_TYPE,
			BROKEN_KEY,
			EntityType.Builder.<BrokenFilesEntity>of(BrokenFilesEntity::new, MobCategory.MONSTER)
					.sized(0.7F, 3.5F)
					.eyeHeight(3.1F)
					.clientTrackingRange(12)
					.fireImmune()
					.build(BROKEN_KEY));

	public static final ResourceKey<EntityType<?>> ZERO_KEY =
			ResourceKey.create(Registries.ENTITY_TYPE, UnkownJar.id("zero_bits"));

	/** 0 Bits: a 4.5 block tall relentless hunter. Smashes blocks, does not pass through them. */
	public static final EntityType<ZeroBitsEntity> ZERO_BITS = Registry.register(
			BuiltInRegistries.ENTITY_TYPE,
			ZERO_KEY,
			EntityType.Builder.<ZeroBitsEntity>of(ZeroBitsEntity::new, MobCategory.MONSTER)
					.sized(0.9F, 4.4F)
					.eyeHeight(4.0F)
					.clientTrackingRange(12)
					.fireImmune()
					.build(ZERO_KEY));

	public static void init() {
		FabricDefaultAttributeRegistry.register(ZERO_BITS, ZeroBitsEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(BROKEN, BrokenFilesEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(ERROR, ErrorEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(UNKNOWN, UnknownEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(NEB, UnknownEntity.createAttributes());
	}

	private ModEntities() {
	}
}
