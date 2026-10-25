package unkown.jar.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import unkown.jar.client.render.BrokenFilesRenderer;
import unkown.jar.client.render.ErrorRenderer;
import unkown.jar.client.render.UnknownModel;
import unkown.jar.client.render.ZeroBitsRenderer;
import unkown.jar.client.render.UnknownRenderer;
import unkown.jar.entity.ModEntities;

public class UnkownJarClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		if (unkown.jar.UnkownConfig.get().safeMode >= 1) {
			unkown.jar.UnkownJar.LOGGER.warn("unknown.jar: safeMode - client rendering disabled");
			return;
		}
		EntityModelLayerRegistry.registerModelLayer(UnknownModel.LAYER, UnknownModel::createBodyLayer);
		EntityRendererRegistry.register(ModEntities.UNKNOWN, UnknownRenderer::new);
		EntityRendererRegistry.register(ModEntities.NEB, UnknownRenderer::new);
		ErrorRenderer.init();
		BrokenFilesRenderer.init();
		ZeroBitsRenderer.init();
		unkown.jar.UnkownJar.LOGGER.info("unknown.jar: renderers registered");
		DeathCrash.init();
		unkown.jar.UnkownJar.LOGGER.info("unknown.jar: client ready");
	}
}
