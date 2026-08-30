package mccfk.DeltaForce.java.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Sable子世界坐标投影兼容工具
 * <p>
 * 当Sable模组加载时，其物理结构(sublevel)中的方块位置位于独立的plotgrid坐标系中。
 * 标点系统需要通过Sable的API将子世界局部坐标投影回主世界全局坐标。
 * 该工具通过反射安全调用Sable API，避免编译期硬依赖。
 * </p>
 */
public class SubLevelProjectionHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger("DeltaForce-SubLevelCompat");

    private static boolean initialized = false;
    private static boolean sableDetected = false;
    private static Object sableCompanionInstance = null;
    private static Method projectOutOfSubLevelMethod = null;
    private static Method getContainingMethod = null;

    private static Method subLevelGetPlotMethod = null;
    private static Method plotToLocalMethod = null;
    private static Method plotGetChunkMethod = null;
    private static Method chunkGetBlockEntityMethod = null;
    private static Constructor<?> chunkPosCtor = null;

    private static void init() {
        if (initialized) return;
        initialized = true;

        try {
            Class<?> sableClass = Class.forName("dev.ryanhcode.sable.Sable");
            java.lang.reflect.Field helperField = sableClass.getField("HELPER");
            sableCompanionInstance = helperField.get(null);

            if (sableCompanionInstance != null) {
                Class<?> companionClass = sableCompanionInstance.getClass();
                projectOutOfSubLevelMethod = companionClass.getMethod("projectOutOfSubLevel", Level.class, Position.class);
                getContainingMethod = companionClass.getMethod("getContaining", Level.class, Position.class);

                try {
                    Class<?> subLevelClass = Class.forName("dev.ryanhcode.sable.sublevel.SubLevel");
                    subLevelGetPlotMethod = subLevelClass.getMethod("getPlot");

                    Class<?> levelPlotClass = Class.forName("dev.ryanhcode.sable.sublevel.plot.LevelPlot");
                    plotToLocalMethod = levelPlotClass.getMethod("toLocal", net.minecraft.world.level.ChunkPos.class);
                    plotGetChunkMethod = levelPlotClass.getMethod("getChunk", net.minecraft.world.level.ChunkPos.class);

                    chunkGetBlockEntityMethod = LevelChunk.class.getMethod("getBlockEntity", BlockPos.class);
                    chunkPosCtor = net.minecraft.world.level.ChunkPos.class.getConstructor(int.class, int.class);
                } catch (Exception ignored) {
                }

                sableDetected = true;
                LOGGER.info("Sable模组已检测到，子世界坐标投影功能已启用");
            }
        } catch (Exception e) {
            LOGGER.debug("Sable模组未检测到，子世界坐标投影功能未启用");
            sableCompanionInstance = null;
            projectOutOfSubLevelMethod = null;
            getContainingMethod = null;
        }
    }

    public static Vec3 projectToGlobal(Level level, Vec3 localPos) {
        init();
        if (!sableDetected || projectOutOfSubLevelMethod == null) {
            return localPos;
        }

        try {
            Object result = projectOutOfSubLevelMethod.invoke(sableCompanionInstance, level, (Position) localPos);
            if (result instanceof Vec3 vec3) {
                return vec3;
            }
        } catch (Exception e) {
            LOGGER.debug("坐标投影反射调用失败", e);
        }
        return localPos;
    }

    public static BlockPos projectToGlobal(Level level, BlockPos localPos) {
        Vec3 global = projectToGlobal(level, Vec3.atCenterOf(localPos));
        return BlockPos.containing(global);
    }

    public static AABB projectBlockAABBToGlobal(Level level, BlockPos localPos) {
        init();
        if (!sableDetected || projectOutOfSubLevelMethod == null) {
            return new AABB(localPos);
        }

        try {
            double x = localPos.getX();
            double y = localPos.getY();
            double z = localPos.getZ();

            Vec3 c000 = projectToGlobal(level, new Vec3(x, y, z));
            Vec3 c001 = projectToGlobal(level, new Vec3(x, y, z + 1));
            Vec3 c010 = projectToGlobal(level, new Vec3(x, y + 1, z));
            Vec3 c011 = projectToGlobal(level, new Vec3(x, y + 1, z + 1));
            Vec3 c100 = projectToGlobal(level, new Vec3(x + 1, y, z));
            Vec3 c101 = projectToGlobal(level, new Vec3(x + 1, y, z + 1));
            Vec3 c110 = projectToGlobal(level, new Vec3(x + 1, y + 1, z));
            Vec3 c111 = projectToGlobal(level, new Vec3(x + 1, y + 1, z + 1));

            double minX = Math.min(Math.min(c000.x, c001.x), Math.min(c010.x, c011.x));
            minX = Math.min(minX, Math.min(Math.min(c100.x, c101.x), Math.min(c110.x, c111.x)));
            double minY = Math.min(Math.min(c000.y, c001.y), Math.min(c010.y, c011.y));
            minY = Math.min(minY, Math.min(Math.min(c100.y, c101.y), Math.min(c110.y, c111.y)));
            double minZ = Math.min(Math.min(c000.z, c001.z), Math.min(c010.z, c011.z));
            minZ = Math.min(minZ, Math.min(Math.min(c100.z, c101.z), Math.min(c110.z, c111.z)));

            double maxX = Math.max(Math.max(c000.x, c001.x), Math.max(c010.x, c011.x));
            maxX = Math.max(maxX, Math.max(Math.max(c100.x, c101.x), Math.max(c110.x, c111.x)));
            double maxY = Math.max(Math.max(c000.y, c001.y), Math.max(c010.y, c011.y));
            maxY = Math.max(maxY, Math.max(Math.max(c100.y, c101.y), Math.max(c110.y, c111.y)));
            double maxZ = Math.max(Math.max(c000.z, c001.z), Math.max(c010.z, c011.z));
            maxZ = Math.max(maxZ, Math.max(Math.max(c100.z, c101.z), Math.max(c110.z, c111.z)));

            return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
        } catch (Exception e) {
            LOGGER.debug("计算子世界方块AABB失败", e);
            return new AABB(localPos);
        }
    }

    public static boolean isInSubLevel(Level level, Vec3 pos) {
        init();
        if (!sableDetected || getContainingMethod == null) {
            return false;
        }

        try {
            Object subLevel = getContainingMethod.invoke(sableCompanionInstance, level, (Position) pos);
            return subLevel != null;
        } catch (Exception e) {
            return false;
        }
    }

    public static BlockEntity getBlockEntityFromSubLevel(Level level, BlockPos pos) {
        init();
        if (!sableDetected || getContainingMethod == null || subLevelGetPlotMethod == null) {
            return null;
        }

        try {
            Object subLevel = getContainingMethod.invoke(sableCompanionInstance, level, (Position) Vec3.atCenterOf(pos));
            if (subLevel == null) return null;

            Object plot = subLevelGetPlotMethod.invoke(subLevel);
            if (plot == null) return null;

            Object globalChunkPos = chunkPosCtor.newInstance(
                net.minecraft.core.SectionPos.blockToSectionCoord(pos.getX()),
                net.minecraft.core.SectionPos.blockToSectionCoord(pos.getZ())
            );

            Object localChunkPos = plotToLocalMethod.invoke(plot, globalChunkPos);
            if (localChunkPos == null) return null;

            Object chunk = plotGetChunkMethod.invoke(plot, localChunkPos);
            if (chunk == null) return null;

            Object blockEntity = chunkGetBlockEntityMethod.invoke(chunk, pos);
            if (blockEntity instanceof BlockEntity be) {
                return be;
            }
        } catch (Exception e) {
            LOGGER.debug("通过SubLevel直接查找BlockEntity失败", e);
        }
        return null;
    }
}