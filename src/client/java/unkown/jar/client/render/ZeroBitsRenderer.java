package unkown.jar.client.render;

import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import unkown.jar.UnkownJar;
import unkown.jar.entity.ModEntities;
import unkown.jar.entity.ZeroBitsEntity;

/** Renderer, render state and model of "0 Bits", all in one file. No red anywhere on this monster. */
public class ZeroBitsRenderer extends HumanoidMobRenderer<ZeroBitsEntity, ZeroState, ZeroModel> {
	private static final Identifier TEXTURE = UnkownJar.id("textures/entity/zero_bits.png");

	/** Called from the client initializer. */
	public static void init() {
		EntityModelLayerRegistry.registerModelLayer(ZeroModel.LAYER, ZeroModel::createBodyLayer);
		EntityRendererRegistry.register(ModEntities.ZERO_BITS, ZeroBitsRenderer::new);
	}

	public ZeroBitsRenderer(EntityRendererProvider.Context context) {
		super(context, new ZeroModel(context.bakeLayer(ZeroModel.LAYER)), 0.9F);
	}

	@Override
	public ZeroState createRenderState() {
		return new ZeroState();
	}

	@Override
	public void extractRenderState(ZeroBitsEntity entity, ZeroState state, float partialTick) {
		super.extractRenderState(entity, state, partialTick);
		state.attackTicks = entity.getAttackTicks();
		state.aggro = entity.isAggro();
		state.hunting = entity.isHunting();
		state.lightCoords = 0xF000F0; // full-bright: the white 0 glows in the dark
	}

	@Override
	public Identifier getTextureLocation(ZeroState state) {
		return TEXTURE;
	}
}

class ZeroState extends HumanoidRenderState {
	public int attackTicks;
	public boolean aggro;
	public boolean hunting;
}

/**
 * 0 Bits after the concept sheet: about 4.5 blocks tall, deeply hunched, a big cube head with a glowing slashed 0,
 * a jaw full of teeth that only opens while it hunts, very long arms with five long claws, long legs with heavy boots,
 * sixteen long cable strands from head and back, ragged fringes on the body and floating glitch shards.
 * Black, gray and white only.
 */
class ZeroModel extends HumanoidModel<ZeroState> {
	static final ModelLayerLocation LAYER = new ModelLayerLocation(UnkownJar.id("zero_bits"), "main");

	private static final int HEAD_STR = 8;
	private static final int BACK_STR = 8;
	private static final int STR = HEAD_STR + BACK_STR;
	private static final int SEG = 5;
	private static final int FING = 5;
	private static final int FRINGE = 8;
	private static final int SHARDS = 10;
	private static final int ATTACK_DURATION = 14;
	private static final float[] LEN = {11, 11, 10, 9, 8};
	private static final float[] W = {1.4F, 1.2F, 1.0F, 0.9F, 0.7F};

	private final ModelPart[][] strand = new ModelPart[STR][SEG];
	private final ModelPart[] fringe = new ModelPart[FRINGE];
	private final ModelPart[] foreArm = new ModelPart[2];
	private final ModelPart[][] fingers = new ModelPart[2][FING];
	private final ModelPart[][] tips = new ModelPart[2][FING];
	private final ModelPart[] shin = new ModelPart[2];
	private final ModelPart[] boot = new ModelPart[2];
	private final ModelPart[] shards = new ModelPart[SHARDS];
	private final ModelPart jaw;

	ZeroModel(ModelPart root) {
		super(root);
		ModelPart headPart = root.getChild("head");
		ModelPart bodyPart = root.getChild("body");
		this.jaw = headPart.getChild("jaw");
		for (int i = 0; i < STR; i++) {
			ModelPart parent = i < HEAD_STR ? headPart.getChild("h" + i) : bodyPart.getChild("b" + (i - HEAD_STR));
			for (int j = 0; j < SEG; j++) {
				ModelPart seg = parent.getChild("s" + j);
				this.strand[i][j] = seg;
				parent = seg;
			}
		}
		for (int i = 0; i < FRINGE; i++) {
			this.fringe[i] = bodyPart.getChild("fr" + i);
		}
		String[] arms = {"right_arm", "left_arm"};
		String[] legs = {"right_leg", "left_leg"};
		for (int s = 0; s < 2; s++) {
			ModelPart fore = root.getChild(arms[s]).getChild("fore");
			this.foreArm[s] = fore;
			ModelPart hand = fore.getChild("hand");
			for (int i = 0; i < FING; i++) {
				ModelPart f = hand.getChild("f" + i);
				this.fingers[s][i] = f;
				this.tips[s][i] = f.getChild("tip");
			}
			ModelPart sh = root.getChild(legs[s]).getChild("shin");
			this.shin[s] = sh;
			this.boot[s] = sh.getChild("boot");
		}
		for (int i = 0; i < SHARDS; i++) {
			this.shards[i] = root.getChild("shard" + i);
		}
	}

	private static void addStrand(PartDefinition holder, String name, float x, float y, float z, float xRot, float zRot) {
		PartDefinition parent = holder.addOrReplaceChild(name, CubeListBuilder.create(), PartPose.offsetAndRotation(x, y, z, xRot, 0.0F, zRot));
		for (int j = 0; j < SEG; j++) {
			float w = W[j];
			parent = parent.addOrReplaceChild("s" + j,
					CubeListBuilder.create().texOffs(84, 0).addBox(-w / 2, 0.0F, -w / 2, w, LEN[j], w),
					PartPose.offset(0.0F, j == 0 ? 0.0F : LEN[j - 1], 0.0F));
		}
	}

	static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		// ---- cube head with the glowing 0, neck, jaw with teeth, cable strands at the back
		PartDefinition head = root.addOrReplaceChild("head",
				CubeListBuilder.create().texOffs(0, 0).addBox(-5.5F, -11.0F, -5.5F, 11.0F, 11.0F, 11.0F)
						.texOffs(84, 0).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 3.0F, 3.0F),
				PartPose.offset(0.0F, -36.0F, 0.0F));
		head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO); // HumanoidModel needs "hat" below "head"
		head.addOrReplaceChild("jaw",
				CubeListBuilder.create().texOffs(46, 0).addBox(-4.0F, 0.0F, 0.0F, 8.0F, 3.0F, 9.0F),
				PartPose.offset(0.0F, -0.5F, -4.5F));
		for (int i = 0; i < HEAD_STR; i++) {
			addStrand(head, "h" + i, -4.9F + i * 1.4F, -9.0F + (i % 4) * 1.8F, 5.6F, 0.15F + (i % 2) * 0.1F, (i - 3.5F) * 0.05F);
		}

		// ---- torso with strands down the back and ragged fringes
		PartDefinition body = root.addOrReplaceChild("body",
				CubeListBuilder.create().texOffs(0, 24).addBox(-4.5F, 0.0F, -2.5F, 9.0F, 20.0F, 5.0F),
				PartPose.offset(0.0F, -34.0F, 0.0F));
		for (int i = 0; i < BACK_STR; i++) {
			float side = i % 2 == 0 ? -1.0F : 1.0F;
			addStrand(body, "b" + i, side * (2.0F + (i / 2) * 0.9F), 1.0F + (i / 2) * 3.5F, 2.7F, 0.2F + (i / 2) * 0.08F, side * 0.12F);
		}
		for (int i = 0; i < FRINGE; i++) {
			float x = -4.0F + i * 1.15F;
			body.addOrReplaceChild("fr" + i,
					CubeListBuilder.create().texOffs(84, 0).addBox(-0.6F, 0.0F, -0.3F, 1.2F, 11.0F + (i % 3) * 3.0F, 0.6F),
					PartPose.offset(x, 17.0F, i % 2 == 0 ? -2.4F : 2.4F));
		}

		// ---- very long arms, five long claws per hand
		for (int s = 0; s < 2; s++) {
			float x = s == 0 ? -6.3F : 6.3F;
			PartDefinition arm = root.addOrReplaceChild(s == 0 ? "right_arm" : "left_arm",
					CubeListBuilder.create().texOffs(84, 0).addBox(-1.2F, -1.0F, -1.2F, 2.4F, 21.0F, 2.4F),
					PartPose.offset(x, -33.0F, 0.0F));
			PartDefinition fore = arm.addOrReplaceChild("fore",
					CubeListBuilder.create().texOffs(84, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 20.0F, 2.0F),
					PartPose.offset(0.0F, 20.0F, 0.0F));
			PartDefinition hand = fore.addOrReplaceChild("hand",
					CubeListBuilder.create().texOffs(84, 0).addBox(-1.3F, 0.0F, -1.0F, 2.6F, 4.0F, 2.0F),
					PartPose.offset(0.0F, 20.0F, 0.0F));
			for (int i = 0; i < FING; i++) {
				PartDefinition finger = hand.addOrReplaceChild("f" + i,
						CubeListBuilder.create().texOffs(84, 0).addBox(-0.3F, 0.0F, -0.3F, 0.6F, 8.0F, 0.6F),
						PartPose.offsetAndRotation(-1.0F + i * 0.5F, 4.0F, 0.0F, 0.3F, 0.0F, (i - 2.0F) * 0.22F));
				finger.addOrReplaceChild("tip",
						CubeListBuilder.create().texOffs(84, 0).addBox(-0.25F, 0.0F, -0.25F, 0.5F, 6.0F, 0.5F),
						PartPose.offsetAndRotation(0.0F, 8.0F, 0.0F, 0.9F, 0.0F, 0.0F));
			}
		}

		// ---- long legs, heavy boots
		for (int s = 0; s < 2; s++) {
			float x = s == 0 ? -3.4F : 3.4F;
			PartDefinition leg = root.addOrReplaceChild(s == 0 ? "right_leg" : "left_leg",
					CubeListBuilder.create().texOffs(84, 0).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 18.0F, 4.0F),
					PartPose.offset(x, -14.0F, 0.0F));
			PartDefinition sh = leg.addOrReplaceChild("shin",
					CubeListBuilder.create().texOffs(84, 0).addBox(-1.8F, 0.0F, -1.8F, 3.6F, 15.0F, 3.6F),
					PartPose.offset(0.0F, 18.0F, 0.0F));
			sh.addOrReplaceChild("boot",
					CubeListBuilder.create().texOffs(0, 52).addBox(-3.0F, 0.0F, -6.0F, 6.0F, 5.0F, 10.0F),
					PartPose.offset(0.0F, 15.0F, 0.0F));
		}

		for (int i = 0; i < SHARDS; i++) {
			root.addOrReplaceChild("shard" + i,
					CubeListBuilder.create().texOffs(84, 0).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F),
					PartPose.ZERO);
		}
		return LayerDefinition.create(mesh, 128, 128);
	}

	@Override
	public void setupAnim(ZeroState state) {
		super.setupAnim(state);
		float t = state.ageInTicks;
		float spd = Mth.clamp(state.walkAnimationSpeed, 0.0F, 1.0F);
		float walk = state.walkAnimationPos;
		float aggro = state.aggro ? 1.0F : 0.0F;

		// ---- deeply hunched; the aggro form hunches even more and its head hangs lower
		float hunch = state.aggro ? 0.85F : (state.hunting ? 0.62F : 0.42F);
		this.body.xRot = hunch + 0.03F * Mth.sin(t * 0.06F);
		this.head.xRot += 0.7F + 0.2F * aggro;
		this.head.zRot = 0.05F * Mth.sin(t * 0.05F);
		int glitch = (int) t % 83;
		if (glitch < 2) {
			this.head.zRot = glitch == 0 ? 0.45F : -0.4F;
			this.head.yRot += glitch == 0 ? 0.55F : -0.5F;
		}

		// ---- the mouth only opens while it hunts, and wide when aggressive
		float open = state.hunting ? (state.aggro ? 1.0F : 0.5F) : 0.0F;
		this.jaw.xRot = -open * 0.9F - (open > 0.0F ? 0.12F * Mth.sin(t * (state.aggro ? 1.7F : 0.8F)) : 0.0F);

		// ---- long arms hanging forward, claws twitching and spreading
		float sway = 0.05F * Mth.sin(t * 0.07F);
		this.rightArm.xRot = 0.35F + sway + Mth.cos(walk * 0.6662F + (float) Math.PI) * 0.6F * spd;
		this.leftArm.xRot = 0.35F - sway + Mth.cos(walk * 0.6662F) * 0.6F * spd;
		this.rightArm.zRot = 0.07F + 0.06F * aggro;
		this.leftArm.zRot = -0.07F - 0.06F * aggro;
		this.foreArm[0].xRot = -0.35F;
		this.foreArm[1].xRot = -0.35F;
		float spread = 1.0F + 0.6F * aggro;

		// ---- ATTACK animation: claws up and back, then a heavy slash down
		if (state.attackTicks > 0) {
			float p = 1.0F - state.attackTicks / (float) ATTACK_DURATION;
			if (p < 0.45F) {
				float k = p / 0.45F;
				this.rightArm.xRot = -2.6F * k;
				this.leftArm.xRot = -2.3F * k;
				this.foreArm[0].xRot = -0.9F * k;
				this.foreArm[1].xRot = -0.9F * k;
				this.body.xRot -= 0.3F * k;
				spread += 0.8F * k;
			} else {
				float k = (p - 0.45F) / 0.55F;
				float e = 1.0F - (1.0F - k) * (1.0F - k);
				this.rightArm.xRot = -2.6F + 3.3F * e;
				this.leftArm.xRot = -2.3F + 3.0F * e;
				this.foreArm[0].xRot = -0.9F + 0.9F * e;
				this.foreArm[1].xRot = -0.9F + 0.9F * e;
				this.body.xRot += 0.35F * e;
				spread += 0.8F;
			}
		}
		for (int s = 0; s < 2; s++) {
			for (int i = 0; i < FING; i++) {
				float tw = Mth.sin(t * 0.28F + i * 1.1F + s * 2.3F);
				this.fingers[s][i].xRot = 0.3F + 0.12F * tw;
				this.fingers[s][i].zRot = (i - 2.0F) * 0.22F * spread;
				this.tips[s][i].xRot = 0.9F + 0.2F * tw;
			}
		}

		// ---- legs: long strides, bent knees
		this.shin[0].xRot = 0.15F + Math.max(0.0F, -this.rightLeg.xRot) * 1.1F;
		this.shin[1].xRot = 0.15F + Math.max(0.0F, -this.leftLeg.xRot) * 1.1F;
		this.boot[0].xRot = -this.shin[0].xRot * 0.5F;
		this.boot[1].xRot = -this.shin[1].xRot * 0.5F;

		// ---- cable strands: flowing; they flare out wide in the aggro form
		for (int i = 0; i < STR; i++) {
			float ph = i * 0.83F;
			float side = i < HEAD_STR ? (i - 3.5F) * 0.1F : (i % 2 == 0 ? -0.2F : 0.2F);
			for (int j = 0; j < SEG; j++) {
				float amp = 0.09F + 0.07F * j + spd * 0.10F * j + aggro * 0.04F * j;
				float x = amp * Mth.sin(t * 0.085F + ph + j * 0.7F);
				float z = amp * 0.8F * Mth.sin(t * 0.07F + ph * 1.3F + j * 0.6F);
				this.strand[i][j].xRot = (j == 0 ? 0.0F : 0.12F) + x;
				this.strand[i][j].zRot = z + (j == 0 ? side * (1.0F + 2.4F * aggro) : 0.0F);
			}
		}
		for (int i = 0; i < FRINGE; i++) {
			this.fringe[i].xRot = 0.08F * Mth.sin(t * 0.1F + i * 1.3F) + spd * 0.2F;
			this.fringe[i].zRot = 0.06F * Mth.sin(t * 0.08F + i * 0.9F);
		}

		// ---- floating glitch shards, flickering
		float orbit = t * 0.022F;
		for (int i = 0; i < SHARDS; i++) {
			float a = orbit + i * 0.8F;
			float r = 10.0F + 7.0F * Mth.sin(t * 0.04F + i * 1.7F);
			this.shards[i].x = Mth.cos(a) * r;
			this.shards[i].z = Mth.sin(a) * r;
			this.shards[i].y = -36.0F + 44.0F * (0.5F + 0.5F * Mth.sin(t * 0.03F + i * 2.1F));
			this.shards[i].xRot = t * 0.08F + i;
			this.shards[i].yRot = t * 0.11F + i * 2.0F;
			this.shards[i].visible = ((int) (t + i * 7) % 27) > 2;
		}
	}
}
