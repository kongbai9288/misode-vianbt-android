package net.querz.mcaselector.android

/**
 * Minecraft 世界坐标 与 Android Canvas / Bitmap 坐标 的对照与换算。
 *
 * # 为什么需要这个
 *
 * MCA Selector 的核心是把 Minecraft 存档渲染成一张俯视地图。
 * 两套坐标系的轴向定义完全不同，直接照搬坐标会导致：
 * 地图南北颠倒、朝向箭头偏 90°、区块选区整体偏移。
 *
 * # 轴向对照
 *
 * | 轴 | Minecraft | Android Canvas / Bitmap |
 * |---|---|---|
 * | X | 东为 +X，西为 -X | 右为 +X，左为 -X |
 * | Y | 上为 +Y，下为 -Y（3D 高度） | 下为 +Y，上为 -Y（屏幕垂直） |
 * | Z | 南为 +Z，北为 -Z | 无直接对应；俯视图把 Z 映射成屏幕 Y |
 * | 坐标系 | 右手坐标系 | 屏幕坐标系，Y 向下 |
 * | 角度 | yaw 0=南，90=西，180=北，270=东；pitch 正向下 | rotate 正角度=顺时针，0=右 |
 *
 * # 俯视图映射规则
 *
 * ```
 * screenX = X      （东 = 右，直接映射）
 * screenY = Z      （南 = 下；北在上、南在下，与直觉一致）
 * ```
 *
 * **注意**：Minecraft 的 Y 是 3D 高度，俯视图里不参与平面定位，
 * 只在高度图着色或伪 3D 时才有意义。
 * 常见错误是把 MC 的 Y 当成屏幕 Y 用，结果整张图沿垂直轴镜像。
 *
 * # 关于 Mojang 的坐标语义差异
 *
 * 历史上 Minecraft 在不同版本 / 不同维度下，同一份 NBT 字段的坐标语义
 * 发生过变化（例如区块内相对坐标与绝对坐标的混用、结构方块旋转时的
 * 坐标变换顺序差异）。跨版本读取存档时不要假定坐标语义一致，
 * 应以该版本的 VersionHandler 为准。
 * 本文件只负责「已知语义后的纯数学换算」，不做版本判定。
 */
object MinecraftCoordinates {

	// ------------------------------------------------------------------ 常量

	/** 一个区块的边长（方块数）。 */
	const val CHUNK_SIZE = 16

	/** 一个区域文件（.mca）横跨的区块数。 */
	const val REGION_SIZE = 32

	/** 一个区域文件横跨的方块数 = 32 * 16 = 512。 */
	const val REGION_BLOCK_SIZE = REGION_SIZE * CHUNK_SIZE

	// --------------------------------------------------------- yaw / 朝向换算

	/**
	 * Minecraft yaw → Canvas 旋转角（度）。
	 *
	 * MC yaw：0=南(+Z)，90=西(-X)，180=北(-Z)，270=东(+X)。
	 * Canvas：0=指向右(+X)，正角度顺时针。
	 *
	 * 俯视图下南 = 屏幕下方(+Y)，相对「右」顺时针 90°，故偏移量为 +90：
	 *
	 * | yaw | 朝向 | Canvas 角 |
	 * |---|---|---|
	 * | 0   | 南  | 90°  |
	 * | 90  | 西  | 180° |
	 * | 180 | 北  | 270° |
	 * | 270 | 东  | 360° → 0° |
	 */
	@JvmStatic
	fun yawToCanvasAngle(yaw: Float): Float = normalizeDegrees(yaw + 90f)

	/** [yawToCanvasAngle] 的逆运算。 */
	@JvmStatic
	fun canvasAngleToYaw(angle: Float): Float = normalizeDegrees(angle - 90f)

	/**
	 * yaw 单位向量在屏幕上的方向，返回 (dx, dy)，dy 为正表示朝屏幕下方。
	 */
	@JvmStatic
	fun yawToScreenDirection(yaw: Float): Pair<Float, Float> {
		val rad = Math.toRadians(yaw.toDouble())
		// MC: yaw=0 指向 +Z(南)。x = -sin(yaw), z = cos(yaw)
		val dx = -Math.sin(rad).toFloat()
		val dz = Math.cos(rad).toFloat()
		// MC (X, Z) -> 屏幕 (X, Y)
		return dx to dz
	}

	/** 把角度规范到 [0, 360)。 */
	@JvmStatic
	fun normalizeDegrees(deg: Float): Float {
		val d = deg % 360f
		return if (d < 0f) d + 360f else d
	}

	// ------------------------------------------------------- 坐标 → 屏幕像素

	/**
	 * 方块坐标 → 屏幕像素（左上角为原点）。
	 *
	 * @param blockX 方块 X（东为正）
	 * @param blockZ 方块 Z（南为正）
	 * @param originX 视口左上角的方块 X
	 * @param originZ 视口左上角的方块 Z
	 * @param scale 每方块占多少像素
	 */
	@JvmStatic
	fun blockToPixel(
		blockX: Int,
		blockZ: Int,
		originX: Int,
		originZ: Int,
		scale: Float,
	): Pair<Float, Float> =
		(blockX - originX) * scale to (blockZ - originZ) * scale

	/** 屏幕像素 → 方块坐标，[blockToPixel] 的逆运算。 */
	@JvmStatic
	fun pixelToBlock(
		pixelX: Float,
		pixelY: Float,
		originX: Int,
		originZ: Int,
		scale: Float,
	): Pair<Int, Int> =
		Math.floor((pixelX / scale).toDouble()).toInt() + originX to
			Math.floor((pixelY / scale).toDouble()).toInt() + originZ

	// ------------------------------------------------------- 区块 / 区域换算

	/** 方块坐标 → 所属区块坐标。 */
	@JvmStatic
	fun blockToChunk(block: Int): Int = block shr 4

	/** 区块坐标 → 所在区域文件坐标。 */
	@JvmStatic
	fun chunkToRegion(chunk: Int): Int = chunk shr 5

	/** 区块在所属区域内的下标，范围 0..31。 */
	@JvmStatic
	fun chunkIndexInRegion(chunk: Int): Int = chunk and (REGION_SIZE - 1)

	/** 区块内局部方块下标，范围 0..15。 */
	@JvmStatic
	fun blockIndexInChunk(block: Int): Int = block and (CHUNK_SIZE - 1)

	/** (区域坐标, 区域内下标) → 绝对区块坐标。 */
	@JvmStatic
	fun regionToChunk(regionX: Int, regionZ: Int, indexX: Int, indexZ: Int): Pair<Int, Int> =
		(regionX shl 5) + indexX to (regionZ shl 5) + indexZ

	/**
	 * 区域文件内偏移量 → 区块下标对。
	 * .mca 头部每个区块占 4 字节，偏移量 = (indexZ * 32 + indexX) * 4。
	 */
	@JvmStatic
	fun chunkOffsetToIndex(offset: Int): Pair<Int, Int> {
		val i = offset / 4
		return (i % REGION_SIZE) to (i / REGION_SIZE)
	}

	/** 区块下标对 → 区域文件内偏移量。 */
	@JvmStatic
	fun chunkIndexToOffset(indexX: Int, indexZ: Int): Int =
		(indexZ * REGION_SIZE + indexX) * 4

	// ------------------------------------------------------------------ 距离

	/** 平面距离（忽略高度 Y），单位：方块。 */
	@JvmStatic
	fun planarDistance(x1: Int, z1: Int, x2: Int, z2: Int): Double {
		val dx = (x2 - x1).toDouble()
		val dz = (z2 - z1).toDouble()
		return Math.sqrt(dx * dx + dz * dz)
	}

	/** 3D 距离（含高度 Y），单位：方块。 */
	@JvmStatic
	fun distance3D(x1: Int, y1: Int, z1: Int, x2: Int, y2: Int, z2: Int): Double {
		val dx = (x2 - x1).toDouble()
		val dy = (y2 - y1).toDouble()
		val dz = (z2 - z1).toDouble()
		return Math.sqrt(dx * dx + dy * dy + dz * dz)
	}
}
