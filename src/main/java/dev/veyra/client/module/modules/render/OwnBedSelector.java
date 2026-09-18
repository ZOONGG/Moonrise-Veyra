package dev.veyra.client.module.modules.render;

import java.util.List;

/** Locates the team bed nearest the server-provided player spawn. */
final class OwnBedSelector {
    private static final double MAX_OWN_BED_DISTANCE_SQ = 48.0D * 48.0D;

    private OwnBedSelector() {
    }

    static int findIndex(List<BedPoint> beds, int spawnX, int spawnZ) {
        if (beds == null || beds.isEmpty()) return -1;
        int closestIndex = -1;
        double closestDistance = MAX_OWN_BED_DISTANCE_SQ;
        for (int index = 0; index < beds.size(); index++) {
            BedPoint bed = beds.get(index);
            if (bed == null) continue;
            double distance = horizontalDistanceSq(bed.x(), bed.z(), spawnX, spawnZ);
            if (distance < closestDistance) {
                closestIndex = index;
                closestDistance = distance;
            }
        }
        return closestIndex;
    }

    private static double horizontalDistanceSq(int firstX, int firstZ, int secondX, int secondZ) {
        double x = firstX - secondX;
        double z = firstZ - secondZ;
        return x * x + z * z;
    }

    record BedPoint(int x, int z) {
    }
}
