package dev.proclient.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec3d;

/**
 * World-to-screen projection for first-person view, done by hand so we do not depend on
 * render-matrix internals that change between versions. Uses the FOV option, so it is
 * approximate while sprinting / under speed effects.
 */
public final class Projector {
    private Projector() {}

    /** @param out receives scaled-GUI x, y. Returns false if the point is behind the camera or off-screen. */
    public static boolean project(MinecraftClient mc, ClientPlayerEntity p, Vec3d target, float tp, float[] out) {
        Vec3d cam = p.getCameraPosVec(tp);
        double vx = target.x - cam.x, vy = target.y - cam.y, vz = target.z - cam.z;

        double yaw = Math.toRadians(p.getYaw(tp));
        double pitch = Math.toRadians(p.getPitch(tp));
        double cp = Math.cos(pitch), sp = Math.sin(pitch), cy = Math.cos(yaw), sy = Math.sin(yaw);

        double fx = -sy * cp, fy = -sp, fz = cy * cp;
        double rx = -cy, ry = 0, rz = -sy;
        double ux = ry * fz - rz * fy;
        double uy = rz * fx - rx * fz;
        double uz = rx * fy - ry * fx;

        double depth = vx * fx + vy * fy + vz * fz;
        if (depth < 0.05) return false;

        double tanHalf = Math.tan(Math.toRadians(mc.options.getFov().getValue()) / 2.0);
        double aspect = (double) mc.getWindow().getFramebufferWidth() / (double) mc.getWindow().getFramebufferHeight();

        double ndcX = ((vx * rx + vy * ry + vz * rz) / depth) / (tanHalf * aspect);
        double ndcY = ((vx * ux + vy * uy + vz * uz) / depth) / tanHalf;
        if (Math.abs(ndcX) > 1.2 || Math.abs(ndcY) > 1.2) return false;

        out[0] = (float) ((ndcX * 0.5 + 0.5) * mc.getWindow().getScaledWidth());
        out[1] = (float) ((0.5 - ndcY * 0.5) * mc.getWindow().getScaledHeight());
        return true;
    }
}
