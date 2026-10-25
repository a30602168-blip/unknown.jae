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
import unkown.jar.entity.BrokenFilesEntity;
import unkown.jar.entity.ModEntities;

/** Renderer, render state and model of "Broken Files", all in one file. */
public class BrokenFilesRenderer extends HumanoidMobRenderer<BrokenFilesEntity, BrokenState, BrokenModel> {
	private static final Identifier TEXTURE = UnkownJar.id("textures/entity/broken_files.png");

	/** Called from the client initializer. */
	public static void init() {
		EntityModelLayerRegistry.registerModelLayer(BrokenModel.LAYER, BrokenModel::createBodyLayer);
		EntityRendererRegistry.register(ModEntities.BROKEN, BrokenFilesRenderer::new);
	}

	public BrokenFilesRenderer(EntityRendererProvider.Context context) {
		super(context, new BrokenModel(context.bakeLayer(BrokenModel.LAYER)), 0.7F);
	}

	@Override
	public BrokenState createRenderState() {
		return new BrokenState();
	}

	@Override
	public void extractRenderState(BrokenFilesEntity entity, BrokenState state, float partialTick) {
		super.extractRenderState(entity, state, partialTick);
		state.lashTicks = entity.getLashTicks();
		state.windingUp = entity.isWindingUp();
		// full-bright: the body is black anyway, but the white eye keeps glowing in the dark
		state.lightCoords = 0xF000F0;
	}

	@Override
	public Identifier getTextureLocation(BrokenState state) {
		return TEXTURE;
	}
}

class BrokenState extends HumanoidRenderState {
	public int lashTicks;
	public boolean windingUp;
}

/**
 * Broken Files, built after the concept sheet: about 3.5 blocks tall, hunched, a cube head with a recessed white eye,
 * very long arms with five long fingers, heavy boots, twelve long tentacles hanging from the head and shoulders,
 * and glitch shards floating around the body.
 */
class BrokenModel extends HumanoidModel<BrokenState> {
	static final ModelLayerLocation LAYER = new ModelLayerLocation(UnkownJar.id("broken_files"), "main");

	private static final int HEAD_TENT = 6;
	private static final int SHOULDER_TENT = 6; // 3 per side
	private static final int TENT = HEAD_TENT + SHOULDER_TENT;
	private static final int SEG = 5;
	private static final int FING = 5;
	private static final int SHARDS = 8;
	private static final int LASH_DURATION = 16;
	private static final float[] HEAD_LEN = {8, 8, 8, 7, 6};
	private static final float[] BODY_LEN = {9, 9, 8, 7, 6};
	private static final float[] WIDTH = {1.6F, 1.4F, 1.2F, 1.0F, 0.8F};

	private final ModelPart[][] tent = new ModelPart[TENT][SEG];
	private final ModelPart[] foreArm = new ModelPart[2];
	private final ModelPart[][] fingers = new ModelPart[2][FING];
	private final ModelPart[][] tips = new ModelPart[2][FING];
	private final ModelPart[] shin = new ModelPart[2];
	private final ModelPart[] boot = new ModelPart[2];
	private final ModelPart[] shards = new ModelPart[SHARDS];

	BrokenModel(ModelPart root) {
		super(root);
		ModelPart headPart = root.getChild("head");
		ModelPart bodyPart = root.getChild("body");
		for (int i = 0; i < TENT; i++) {
			ModelPart parent = i < HEAD_TENT ? headPart.getChild("t" + i) : bodyPart.getChild("t" + i);
			for (int j = 0; j < SEG; j++) {
				ModelPart seg = parent.getChild("s" + j);
				this.tent[i][j] = seg;
				parent = seg;
			}
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

	private static void addTentacle(PartDefinition holder, String name, float x, float y, float z, float xRot, float zRot, float[] lens) {
		PartDefinition parent = holder.addOrReplaceChild(name, CubeListBuilder.create(), PartPose.offsetAndRotation(x, y, z, xRot, 0.0F, zRot));
		for (int j = 0; j < SEG; j++) {
			float w = WIDTH[j];
			float len = lens[j];
			parent = parent.addOrReplaceChild("s" + j,
					CubeListBuilder.create().texOffs(64, 0).addBox(-w / 2, 0.0F, -w / 2, w, len, w),
					PartPose.offset(0.0F, j == 0 ? 0.0F : lens[j - 1], 0.0F));
		}
	}

	static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		// ---- cube head with the glowing eye, tentacles hanging from its back
		PartDefinition head = root.addOrReplaceChild("head",
				CubeListBuilder.create().texOffs(0, 0).addBox(-4.5F, -10.0F, -4.5F, 9.0F, 10.0F, 9.0F)
						.texOffs(64, 0).addBox(-1.5F, -1.0F, -1.5F, 3.0F, 3.0F, 3.0F),
				PartPose.offset(0.0F, -21.0F, 0.0F));
		head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO); // HumanoidModel needs "hat" as a child of "head"
		for (int i = 0; i < HEAD_TENT; i++) {
			float x = -3.6F + i * 1.44F;
			addTentacle(head, "t" + i, x, -8.5F + (i % 3) * 2.2F, 4.4F, 0.18F + (i % 2) * 0.1F, (i - 2.5F) * 0.07F, HEAD_LEN);
		}

		// ---- slim, tall torso with six long tentacles from the shoulders and back
		PartDefinition body = root.addOrReplaceChild("body",
				CubeListBuilder.create().texOffs(0, 20).addBox(-3.5F, 0.0F, -2.0F, 7.0F, 16.0F, 4.0F),
				PartPose.offset(0.0F, -20.0F, 0.0F));
		for (int k = 0; k < 6; k++) {
			boolean left = k >= 3;
			int n = k % 3;
			float x = left ? 3.2F : -3.2F;
			float outward = (left ? -1.0F : 1.0F) * (0.25F + n * 0.28F);
			addTentacle(body, "t" + (HEAD_TENT + k), x, 1.0F + n * 2.5F, 2.0F, 0.22F + n * 0.12F, outward, BODY_LEN);
		}

		// ---- very long arms: upper arm, forearm, hand with five long fingers
		for (int s = 0; s < 2; s++) {
			float x = s == 0 ? -5.2F : 5.2F;
			PartDefinition arm = root.addOrReplaceChild(s == 0 ? "right_arm" : "left_arm",
					CubeListBuilder.create().texOffs(64, 0).addBox(-1.2F, -1.0F, -1.2F, 2.4F, 15.0F, 2.4F),
					PartPose.offset(x, -19.0F, 0.0F));
			PartDefinition fore = arm.addOrReplaceChild("fore",
					CubeListBuilder.create().texOffs(64, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 14.0F, 2.0F),
					PartPose.offset(0.0F, 14.0F, 0.0F));
			PartDefinition hand = fore.addOrReplaceChild("hand",
					CubeListBuilder.create().texOffs(64, 0).addBox(-1.3F, 0.0F, -1.0F, 2.6F, 3.0F, 2.0F),
					PartPose.offset(0.0F, 14.0F, 0.0F));
			for (int i = 0; i < FING; i++) {
				PartDefinition finger = hand.addOrReplaceChild("f" + i,
						CubeListBuilder.create().texOffs(64, 0).addBox(-0.3F, 0.0F, -0.3F, 0.6F, 5.5F, 0.6F),
						PartPose.offsetAndRotation(-1.0F + i * 0.5F, 3.0F, 0.0F, 0.3F, 0.0F, (i - 2.0F) * 0.22F));
				finger.addOrReplaceChild("tip",
						CubeListBuilder.create().texOffs(64, 0).addBox(-0.25F, 0.0F, -0.25F, 0.5F, 4.0F, 0.5F),
						PartPose.offsetAndRotation(0.0F, 5.5F, 0.0F, 0.9F, 0.0F, 0.0F));
			}
		}

		// ---- long legs with heavy boots
		for (int s = 0; s < 2; s++) {
			float x = s == 0 ? -2.2F : 2.2F;
			PartDefinition leg = root.addOrReplaceChild(s == 0 ? "right_leg" : "left_leg",
					CubeListBuilder.create().texOffs(64, 0).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 14.0F, 3.0F),
					PartPose.offset(x, -4.0F, 0.0F));
			PartDefinition sh = leg.addOrReplaceChild("shin",
					CubeListBuilder.create().texOffs(64, 0).addBox(-1.4F, 0.0F, -1.4F, 2.8F, 10.0F, 2.8F),
					PartPose.offset(0.0F, 14.0F, 0.0F));
			sh.addOrReplaceChild("boot",
					CubeListBuilder.create().texOffs(0, 42).addBox(-2.5F, 0.0F, -5.0F, 5.0F, 4.0F, 8.0F),
					PartPose.offset(0.0F, 10.0F, 0.0F));
		}

		// ---- glitch shards
		for (int i = 0; i < SHARDS; i++) {
			root.addOrReplaceChild("shard" + i,
					CubeListBuilder.create().texOffs(64, 0).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F),
					PartPose.ZERO);
		}
		return LayerDefinition.create(mesh, 128, 128);
	}

	@Override
	public void setupAnim(BrokenState state) {
		super.setupAnim(state);
		float t = state.ageInTicks;
		float spd = Mth.clamp(state.walkAnimationSpeed, 0.0F, 1.0F);
		float walk = state.walkAnimationPos;

		// ---- hunched, heavy posture: leaning forward, head low, looking down at you
		this.body.xRot = 0.30F + 0.03F * Mth.sin(t * 0.06F);
		this.head.xRot += 0.50F;
		this.head.zRot = 0.06F * Mth.sin(t * 0.05F);
		int glitch = (int) t % 71;
		if (glitch < 2) { // sharp glitch twitch
			this.head.zRot = glitch == 0 ? 0.5F : -0.45F;
			this.head.yRot += glitch == 0 ? 0.6F : -0.5F;
		}

		// ---- arms hang long and forward, fingers twitching
		float sway = 0.04F * Mth.sin(t * 0.07F);
		this.rightArm.xRot = 0.20F + sway + Mth.cos(walk * 0.6662F + (float) Math.PI) * 0.5F * spd;
		this.leftArm.xRot = 0.20F - sway + Mth.cos(walk * 0.6662F) * 0.5F * spd;
		this.rightArm.zRot = 0.06F;
		this.leftArm.zRot = -0.06F;
		this.foreArm[0].xRot = -0.30F;
		this.foreArm[1].xRot = -0.30F;
		float curl = state.windingUp ? 0.5F : 0.0F;
		for (int s = 0; s < 2; s++) {
			for (int i = 0; i < FING; i++) {
				float tw = Mth.sin(t * 0.25F + i * 1.1F + s * 2.3F);
				this.fingers[s][i].xRot = 0.30F + 0.12F * tw - curl * 0.3F;
				this.fingers[s][i].zRot = (i - 2.0F) * (0.22F + curl * 0.2F);
				this.tips[s][i].xRot = 0.9F + 0.2F * tw + curl;
			}
		}

		// ---- stride: long legs, knees bend, heavy boots
		this.shin[0].xRot = 0.12F + Math.max(0.0F, -this.rightLeg.xRot) * 1.1F;
		this.shin[1].xRot = 0.12F + Math.max(0.0F, -this.leftLeg.xRot) * 1.1F;
		this.boot[0].xRot = -this.shin[0].xRot * 0.5F;
		this.boot[1].xRot = -this.shin[1].xRot * 0.5F;

		// ---- LASH animation: tentacles pull back during the warning, then whip forward
		float pull = 0.0F;
		float whip = 0.0F;
		if (state.windingUp) {
			pull = 1.0F;
			this.rightArm.xRot -= 0.7F;
			this.leftArm.xRot -= 0.7F;
		}
		if (state.lashTicks > 0) {
			float p = 1.0F - state.lashTicks / (float) LASH_DURATION;
			if (p < 0.4F) {
				pull = 1.0F;
			} else {
				float k = (p - 0.4F) / 0.6F;
				whip = 1.0F - (1.0F - k) * (1.0F - k);
				pull = 0.0F;
				this.body.xRot += 0.35F * whip;
				this.head.xRot += 0.25F * whip;
				this.rightArm.xRot = -1.9F * whip;
				this.leftArm.xRot = -1.7F * whip;
			}
		}

		// ---- tentacles: flowing sway, stronger while moving; pulled back or whipping forward during the lash
		for (int i = 0; i < TENT; i++) {
			float ph = i * 0.93F;
			for (int j = 0; j < SEG; j++) {
				float amp = 0.10F + 0.07F * j + spd * 0.10F * j;
				float x = amp * Mth.sin(t * 0.09F + ph + j * 0.7F);
				float z = amp * 0.8F * Mth.sin(t * 0.075F + ph * 1.3F + j * 0.6F);
				float lash = (j == 0 ? 1.0F : 0.5F) * (0.9F * pull - 2.0F * whip);
				this.tent[i][j].xRot = (j == 0 ? 0.0F : 0.12F) + x + lash * (j == 0 ? 0.6F : 0.35F);
				this.tent[i][j].zRot = z;
			}
		}

		// ---- floating glitch shards, flickering
		float orbit = t * 0.025F;
		for (int i = 0; i < SHARDS; i++) {
			float a = orbit + i * 0.9F;
			float r = 9.0F + 6.0F * Mth.sin(t * 0.04F + i * 1.7F);
			this.shards[i].x = Mth.cos(a) * r;
			this.shards[i].z = Mth.sin(a) * r;
			this.shards[i].y = -28.0F + 36.0F * (0.5F + 0.5F * Mth.sin(t * 0.03F + i * 2.1F));
			this.shards[i].xRot = t * 0.08F + i;
			this.shards[i].yRot = t * 0.11F + i * 2.0F;
			this.shards[i].visible = ((int) (t + i * 7) % 29) > 2;
		}
	}
}
