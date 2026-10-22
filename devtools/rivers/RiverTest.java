import net.enderwish.TerraForma_Subpack.core.current.CurrentField;
import net.enderwish.TerraForma_Subpack.core.worldgen.noise.GHNoiseRouter;
import net.enderwish.TerraForma_Subpack.core.worldgen.river.RiverNetwork;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * A stand-alone look at the rivers and the current.
 */
public class RiverTest {

    public static void main(String[] args) {
        GHNoiseRouter.INSTANCE.setSeed(12345L);

        System.out.println("=== the ground along x=0 ===");
        for (int x = 0; x <= 3000; x += 300) {
            System.out.println("  x=" + x + "  height=" + GHNoiseRouter.INSTANCE.getSurfaceHeight(x, 0)
                    + "  sea=" + GHNoiseRouter.SEA_LEVEL);
        }

        System.out.println("=== the rivers ===");
        int reached = 0;
        int started = 0;
        long start = System.nanoTime();
        for (int gx = -2; gx <= 3; gx++) {
            for (int gz = -2; gz <= 3; gz++) {
                List<RiverNetwork.PathPoint> path = RiverNetwork.pathOf(gx, gz);
                if (path.isEmpty()) continue;
                started++;
                var last = path.get(path.size() - 1);
                boolean atSea = last.bedY() <= GHNoiseRouter.SEA_LEVEL;
                if (atSea) reached++;
                System.out.println("  source " + gx + "," + gz
                        + "  points=" + path.size()
                        + "  from y=" + path.get(0).bedY()
                        + " to y=" + last.bedY()
                        + "  reachedSea=" + atSea);
            }
        }
        long took = (System.nanoTime() - start) / 1_000_000;
        System.out.println("=== " + started + " rivers traced, " + reached
                + " of them reached the sea, in " + took + " ms ===");

        System.out.println("=== the current in the rivers ===");
        for (int gx = -2; gx <= 3; gx++) {
            for (int gz = -2; gz <= 3; gz++) {
                List<RiverNetwork.PathPoint> path = RiverNetwork.pathOf(gx, gz);
                if (path.isEmpty()) continue;
                for (int i = 0; i < path.size(); i += Math.max(1, path.size() / 2)) {
                    var p = path.get(i);
                    Vec3 cur = CurrentField.riverCurrent(p.x(), p.z());
                    if (cur == null) continue;
                    System.out.println("  at " + p.x() + "," + p.z()
                            + "  speed=" + String.format("%.2f", cur.length() * 20) + " blocks/s"
                            + "  running " + String.format("(%.2f, %.2f)", cur.x() * 20, cur.z() * 20));
                }
            }
        }

        System.out.println("=== the channel it cuts ===");
        for (int gx = -2; gx <= 3; gx++) {
            for (int gz = -2; gz <= 3; gz++) {
                List<RiverNetwork.PathPoint> path = RiverNetwork.pathOf(gx, gz);
                if (path.isEmpty()) continue;
                for (int i = 0; i < path.size(); i += Math.max(1, path.size() / 3)) {
                    var p = path.get(i);
                    RiverNetwork.RiverColumn col = RiverNetwork.columnAt(p.x(), p.z());
                    int ground = GHNoiseRouter.INSTANCE.getSurfaceHeight(p.x(), p.z());
                    if (col == null) continue;
                    System.out.println("  at " + p.x() + "," + p.z()
                            + "  ground=" + ground
                            + "  bed=" + col.bedY()
                            + "  water=" + col.waterY()
                            + "  cut=" + (ground - col.bedY()));
                }
            }
        }
        System.out.println("=== how long a current lookup takes ===");
        start = System.nanoTime();
        int found = 0;
        float fastest = 0;
        for (int i = 0; i < 50000; i++) {
            Vec3 cur = CurrentField.riverCurrent(i * 7, i * 13);
            if (cur != null) {
                found++;
                fastest = Math.max(fastest, (float) (cur.length() * 20));
            }
        }
        took = (System.nanoTime() - start) / 1_000_000;
        System.out.println("  50000 lookups in " + took + " ms, " + found
                + " of them with a current, the fastest " + String.format("%.2f", fastest) + " blocks/s");
    }
}
