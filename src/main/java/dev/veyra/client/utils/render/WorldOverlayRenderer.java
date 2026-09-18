package dev.veyra.client.utils.render;

import dev.veyra.client.utils.world.BedTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import org.lwjgl.opengl.GL11;

import java.util.List;

public final class WorldOverlayRenderer {
    private static final Minecraft mc = Minecraft.getMinecraft();

    private WorldOverlayRenderer() {
    }

    public static void drawLines(BlockPos pos, List<String> lines, float scale, int background) {
        if (lines == null || lines.isEmpty()) return;
        double x = pos.getX() + 0.5D - mc.getRenderManager().viewerPosX;
        double y = pos.getY() + 1.4D - mc.getRenderManager().viewerPosY;
        double z = pos.getZ() + 0.5D - mc.getRenderManager().viewerPosZ;

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        GlStateManager.rotate(-mc.getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(mc.getRenderManager().playerViewX, 1.0F, 0.0F, 0.0F);
        GlStateManager.scale(-scale, -scale, scale);
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        GlStateManager.depthMask(false);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        int width = 0;
        for (String line : lines) width = Math.max(width, mc.fontRendererObj.getStringWidth(line));
        int height = lines.size() * (mc.fontRendererObj.FONT_HEIGHT + 1);
        drawBackground(-width / 2 - 3, -2, width / 2 + 3, height + 1, background);

        int drawY = 0;
        for (String line : lines) {
            mc.fontRendererObj.drawStringWithShadow(line,
                    -mc.fontRendererObj.getStringWidth(line) / 2.0F, drawY, 0xFFFFFFFF);
            drawY += mc.fontRendererObj.FONT_HEIGHT + 1;
        }

        GlStateManager.depthMask(true);
        GlStateManager.enableDepth();
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
    }

    /** Draws a real bed-sized box rather than two unrelated full block cubes. */
    public static void drawBedBox(AxisAlignedBB worldBounds, int color, boolean outline, boolean fill,
                                  float lineWidth, float outlineOpacity, float fillOpacity) {
        if (worldBounds == null || (!outline && !fill)) return;
        AxisAlignedBB bounds = worldBounds.offset(
                -mc.getRenderManager().viewerPosX,
                -mc.getRenderManager().viewerPosY,
                -mc.getRenderManager().viewerPosZ);
        float red = (color >> 16 & 255) / 255.0F;
        float green = (color >> 8 & 255) / 255.0F;
        float blue = (color & 255) / 255.0F;

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        GlStateManager.disableCull();
        GlStateManager.depthMask(false);

        if (fill) {
            drawFilledBox(bounds, red, green, blue, clampOpacity(fillOpacity));
        }
        if (outline) {
            GL11.glLineWidth(Math.max(0.5F, Math.min(6.0F, lineWidth)));
            drawOutlinedBox(bounds, red, green, blue, clampOpacity(outlineOpacity));
        }

        GL11.glLineWidth(1.0F);
        GlStateManager.depthMask(true);
        GlStateManager.enableCull();
        GlStateManager.enableDepth();
        GlStateManager.enableLighting();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
    }

    /** Draws the entity's exact interpolated bounding box with no artificial expansion. */
    public static void drawEntityHitbox(Entity entity, int color, boolean fill,
                                        float lineWidth, float outlineOpacity, float fillOpacity) {
        if (entity == null) return;
        double interpolatedX = entity.lastTickPosX
                + (entity.posX - entity.lastTickPosX) * mc.timer.renderPartialTicks;
        double interpolatedY = entity.lastTickPosY
                + (entity.posY - entity.lastTickPosY) * mc.timer.renderPartialTicks;
        double interpolatedZ = entity.lastTickPosZ
                + (entity.posZ - entity.lastTickPosZ) * mc.timer.renderPartialTicks;
        AxisAlignedBB current = entity.getEntityBoundingBox();
        AxisAlignedBB interpolated = current.offset(
                interpolatedX - entity.posX,
                interpolatedY - entity.posY,
                interpolatedZ - entity.posZ);
        drawBedBox(interpolated, color, true, fill, lineWidth, outlineOpacity, fillOpacity);
    }

    /**
     * Renders Minecraft's actual item models above a bed. Metadata is preserved,
     * so dyed defense blocks are shown with their real in-game color and texture.
     */
    public static void drawBedPlate(BedTracker.BedEntry bed,
                                    List<BedTracker.ProtectionEntry> protection,
                                    float scale) {
        if (bed == null) return;
        int iconCount = Math.max(1, protection == null ? 0 : protection.size());
        int width = iconCount * 18;
        double x = (bed.head().getX() + bed.foot().getX()) * 0.5D + 0.5D
                - mc.getRenderManager().viewerPosX;
        double y = bed.head().getY() + 1.25D - mc.getRenderManager().viewerPosY;
        double z = (bed.head().getZ() + bed.foot().getZ()) * 0.5D + 0.5D
                - mc.getRenderManager().viewerPosZ;

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        GlStateManager.rotate(-mc.getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(mc.getRenderManager().playerViewX, 1.0F, 0.0F, 0.0F);
        GlStateManager.scale(-scale, -scale, scale);
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        GlStateManager.depthMask(false);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        int left = -width / 2;
        int top = -8;

        RenderItem renderItem = mc.getRenderItem();
        float previousZLevel = renderItem.zLevel;
        // GUI item rendering normally adds +100 Z. Inside a world billboard that
        // moves the model out of the plate while its text overlay stays visible.
        renderItem.zLevel = -100.0F;
        RenderHelper.enableGUIStandardItemLighting();
        GlStateManager.enableRescaleNormal();
        GlStateManager.enableTexture2D();
        if (protection == null || protection.isEmpty()) {
            renderStack(renderItem, new ItemStack(Items.bed), left + 1, top);
        } else {
            int drawX = left + 1;
            for (BedTracker.ProtectionEntry entry : protection) {
                ItemStack display = entry.stack().copy();
                // BedPlates is visual: quantities never belong inside the plate.
                display.stackSize = 1;
                renderStack(renderItem, display, drawX, top + 4);
                drawX += 18;
            }
        }

        renderItem.zLevel = previousZLevel;
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableRescaleNormal();
        GlStateManager.depthMask(true);
        GlStateManager.enableDepth();
        GlStateManager.disableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
    }

    private static void renderStack(RenderItem renderItem, ItemStack stack, int x, int y) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        renderItem.renderItemIntoGUI(stack, x, y);
    }

    private static float clampOpacity(float opacity) {
        return Math.max(0.0F, Math.min(1.0F, opacity));
    }

    private static void drawFilledBox(AxisAlignedBB box, float red, float green, float blue, float alpha) {
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer renderer = tessellator.getWorldRenderer();
        renderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);

        // Bottom and top.
        quad(renderer, box.minX, box.minY, box.minZ, box.maxX, box.minY, box.minZ,
                box.maxX, box.minY, box.maxZ, box.minX, box.minY, box.maxZ, red, green, blue, alpha);
        quad(renderer, box.minX, box.maxY, box.maxZ, box.maxX, box.maxY, box.maxZ,
                box.maxX, box.maxY, box.minZ, box.minX, box.maxY, box.minZ, red, green, blue, alpha);
        // North and south.
        quad(renderer, box.minX, box.minY, box.minZ, box.minX, box.maxY, box.minZ,
                box.maxX, box.maxY, box.minZ, box.maxX, box.minY, box.minZ, red, green, blue, alpha);
        quad(renderer, box.maxX, box.minY, box.maxZ, box.maxX, box.maxY, box.maxZ,
                box.minX, box.maxY, box.maxZ, box.minX, box.minY, box.maxZ, red, green, blue, alpha);
        // West and east.
        quad(renderer, box.minX, box.minY, box.maxZ, box.minX, box.maxY, box.maxZ,
                box.minX, box.maxY, box.minZ, box.minX, box.minY, box.minZ, red, green, blue, alpha);
        quad(renderer, box.maxX, box.minY, box.minZ, box.maxX, box.maxY, box.minZ,
                box.maxX, box.maxY, box.maxZ, box.maxX, box.minY, box.maxZ, red, green, blue, alpha);
        tessellator.draw();
    }

    /**
     * Emits all twelve AABB edges explicitly. The vanilla selection-box helper
     * relies on inherited OpenGL color state and can disappear when called from
     * a custom world overlay pipeline.
     */
    private static void drawOutlinedBox(AxisAlignedBB box, float red, float green, float blue, float alpha) {
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer renderer = tessellator.getWorldRenderer();
        renderer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);

        // Bottom face.
        line(renderer, box.minX, box.minY, box.minZ, box.maxX, box.minY, box.minZ, red, green, blue, alpha);
        line(renderer, box.maxX, box.minY, box.minZ, box.maxX, box.minY, box.maxZ, red, green, blue, alpha);
        line(renderer, box.maxX, box.minY, box.maxZ, box.minX, box.minY, box.maxZ, red, green, blue, alpha);
        line(renderer, box.minX, box.minY, box.maxZ, box.minX, box.minY, box.minZ, red, green, blue, alpha);
        // Top face.
        line(renderer, box.minX, box.maxY, box.minZ, box.maxX, box.maxY, box.minZ, red, green, blue, alpha);
        line(renderer, box.maxX, box.maxY, box.minZ, box.maxX, box.maxY, box.maxZ, red, green, blue, alpha);
        line(renderer, box.maxX, box.maxY, box.maxZ, box.minX, box.maxY, box.maxZ, red, green, blue, alpha);
        line(renderer, box.minX, box.maxY, box.maxZ, box.minX, box.maxY, box.minZ, red, green, blue, alpha);
        // Vertical edges.
        line(renderer, box.minX, box.minY, box.minZ, box.minX, box.maxY, box.minZ, red, green, blue, alpha);
        line(renderer, box.maxX, box.minY, box.minZ, box.maxX, box.maxY, box.minZ, red, green, blue, alpha);
        line(renderer, box.maxX, box.minY, box.maxZ, box.maxX, box.maxY, box.maxZ, red, green, blue, alpha);
        line(renderer, box.minX, box.minY, box.maxZ, box.minX, box.maxY, box.maxZ, red, green, blue, alpha);
        tessellator.draw();
    }

    private static void line(WorldRenderer renderer,
                             double x1, double y1, double z1, double x2, double y2, double z2,
                             float red, float green, float blue, float alpha) {
        renderer.pos(x1, y1, z1).color(red, green, blue, alpha).endVertex();
        renderer.pos(x2, y2, z2).color(red, green, blue, alpha).endVertex();
    }

    private static void quad(WorldRenderer renderer,
                             double x1, double y1, double z1, double x2, double y2, double z2,
                             double x3, double y3, double z3, double x4, double y4, double z4,
                             float red, float green, float blue, float alpha) {
        renderer.pos(x1, y1, z1).color(red, green, blue, alpha).endVertex();
        renderer.pos(x2, y2, z2).color(red, green, blue, alpha).endVertex();
        renderer.pos(x3, y3, z3).color(red, green, blue, alpha).endVertex();
        renderer.pos(x4, y4, z4).color(red, green, blue, alpha).endVertex();
    }

    private static void drawBackground(double left, double top, double right, double bottom, int color) {
        float a = (color >>> 24 & 255) / 255.0F;
        float r = (color >>> 16 & 255) / 255.0F;
        float g = (color >>> 8 & 255) / 255.0F;
        float b = (color & 255) / 255.0F;
        GlStateManager.disableTexture2D();
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer renderer = tessellator.getWorldRenderer();
        renderer.begin(7, DefaultVertexFormats.POSITION_COLOR);
        renderer.pos(left, bottom, 0.0D).color(r, g, b, a).endVertex();
        renderer.pos(right, bottom, 0.0D).color(r, g, b, a).endVertex();
        renderer.pos(right, top, 0.0D).color(r, g, b, a).endVertex();
        renderer.pos(left, top, 0.0D).color(r, g, b, a).endVertex();
        tessellator.draw();
        GlStateManager.enableTexture2D();
    }
}
