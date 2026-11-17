package dev.omnicube.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

public final class WorldTextRenderer {
	private static final float TEXT_SCALE = 0.025F;

	private WorldTextRenderer() {
	}

	public static void draw(WorldRenderContext context, Vec3 position, Component text, int color, float scale) {
		try {
			Minecraft client = Minecraft.getInstance();
			Font font = client.font;
			Vec3 camera = context.worldState().cameraRenderState.pos;
			PoseStack matrices = context.matrices();
			MultiBufferSource consumers = context.consumers();
			if (font == null || camera == null || matrices == null || consumers == null) {
				return;
			}

			matrices.pushPose();
			matrices.translate(position.x - camera.x, position.y - camera.y, position.z - camera.z);
			matrices.mulPose(context.worldState().cameraRenderState.orientation);
			matrices.scale(-TEXT_SCALE * scale, -TEXT_SCALE * scale, TEXT_SCALE * scale);
			Font.DisplayMode displayMode = Font.DisplayMode.SEE_THROUGH;
			font.drawInBatch(text, -font.width(text) / 2.0F, -font.lineHeight / 2.0F, color, true,
					matrices.last().pose(), consumers, displayMode, 0, 0xF000F0);
			matrices.popPose();
		} catch (Throwable ignored) {
			// A bad entity or render state must not break the world render pass.
		}
	}

	public static void beam(WorldRenderContext context, Vec3 position, int color, double height) {
		try {
			Vec3 camera = context.worldState().cameraRenderState.pos;
			PoseStack matrices = context.matrices();
			if (camera == null || matrices == null) {
				return;
			}
			VertexConsumer consumer = context.consumers().getBuffer(RenderTypes.lines());
			PoseStack.Pose pose = matrices.last();
			float x = (float) (position.x - camera.x);
			float y = (float) (position.y - camera.y);
			float z = (float) (position.z - camera.z);
			float top = (float) (height);
			float radius = 0.12F;
			for (int corner = 0; corner < 4; corner++) {
				float dx = (corner == 0 || corner == 3) ? -radius : radius;
				float dz = (corner == 0 || corner == 1) ? -radius : radius;
				consumer.addVertex(pose, x + dx, y, z + dz).setColor(color).setNormal(0.0F, 1.0F, 0.0F);
				consumer.addVertex(pose, x + dx, y + top, z + dz).setColor(color).setNormal(0.0F, 1.0F, 0.0F);
			}
		} catch (Throwable ignored) {
			// Lines are optional decoration; labels still render if this fails.
		}
	}
}
