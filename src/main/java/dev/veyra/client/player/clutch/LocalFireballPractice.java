package dev.veyra.client.player.clutch;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityLargeFireball;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Vec3;

/** Local integrated-server simulation. Never sends custom packets or touches remote projectiles. */
public final class LocalFireballPractice {
    private record Session(WorldClient clientWorld, IntegratedServer server, double speed,
                           float explosionPower, boolean destroyBlocks, boolean debug, double knockback) {}
    private static volatile Session session;
    public static final String TAG = "VeyraLocalFireball";
    public static final String POWER_TAG = "VeyraLocalFireballPower";
    public static final String BLOCKS_TAG = "VeyraLocalFireballBlocks";

    public static void publish(Minecraft mc, double speed, float power, boolean blocks, boolean debug, double knockback) {
        if (!mc.isSingleplayer() || mc.getIntegratedServer() == null || mc.getIntegratedServer().getPublic() || mc.theWorld == null) {
            clear(); return;
        }
        session = new Session(mc.theWorld, mc.getIntegratedServer(), speed, power, blocks, debug, knockback);
    }
    public static void clear() { session = null; }
    public static boolean acceptsWorld(World world) {
        Session s = session;
        return s != null && !s.server.getPublic() && (world.isRemote ? world == s.clientWorld
                : world instanceof WorldServer && ((WorldServer) world).getMinecraftServer() == s.server);
    }
    public static boolean accepts(World world, ItemStack stack) {
        return acceptsWorld(world) && stack != null && stack.stackSize > 0 && stack.getItem() == Items.fire_charge;
    }
    public static void trace(World world, String action, EntityPlayer player) {
        Session s=session;
        if(s!=null && s.debug && !world.isRemote)
            System.out.printf("FireballPractice serverTick=%d action=%s slot=%d pitch=%.3f%n",
                world.getTotalWorldTime(),action,player.inventory.currentItem,player.rotationPitch);
    }
    public static void traceEvent(World world, String action) {
        Session s=session;
        if(s!=null && s.debug && !world.isRemote && acceptsWorld(world))
            System.out.printf("FireballPractice serverTick=%d action=%s%n",world.getTotalWorldTime(),action);
    }
    public static ItemStack usePearl(World world, EntityPlayer player, ItemStack stack) {
        if(world.isRemote || !acceptsWorld(world))return stack;
        boolean spawned=world.spawnEntityInWorld(new net.minecraft.entity.item.EntityEnderPearl(world,player));
        if(spawned && !player.capabilities.isCreativeMode)--stack.stackSize;
        trace(world,spawned?"spawn-pearl":"pearl-spawn-failed",player);
        return stack;
    }
    public static ItemStack use(World world, EntityPlayer player, ItemStack stack) {
        Session s = session;
        if (s == null || !accepts(world, stack) || world.isRemote) return stack;
        Vec3 look = player.getLookVec();
        // Vanilla entity type preserves networking, rendering, deflection and collision behavior.
        EntityLargeFireball ball = new EntityLargeFireball(world, player, look.xCoord, look.yCoord, look.zCoord);
        ball.setPosition(player.posX + look.xCoord * 0.75,
                player.posY + player.getEyeHeight() + look.yCoord * 0.75,
                player.posZ + look.zCoord * 0.75);
        ball.motionX = look.xCoord * s.speed;
        ball.motionY = look.yCoord * s.speed;
        ball.motionZ = look.zCoord * s.speed;
        double acceleration = FireballPracticePhysics.accelerationForSpeed(s.speed);
        ball.accelerationX = look.xCoord * acceleration;
        ball.accelerationY = look.yCoord * acceleration;
        ball.accelerationZ = look.zCoord * acceleration;
        ((LocalFireballState) ball).veyra$configurePractice(s.explosionPower, s.destroyBlocks, s.knockback);
        boolean spawned=world.spawnEntityInWorld(ball);
        if (spawned && !player.capabilities.isCreativeMode) --stack.stackSize;
        trace(world,spawned?"spawn-fireball":"fireball-spawn-failed",player);
        return stack;
    }
}
