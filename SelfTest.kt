import com.bedrockbuilder.core.*
import java.io.File

var failures = 0
fun check(c: Boolean, msg: String) { if (!c) { failures++; println("FAIL: $msg") } else println("ok:   $msg") }

fun main(args: Array<String>) {
    val out = args.getOrElse(0) { "/tmp/out" }
    // --- build a structure and round trip ---
    val m = BuildModel.create(5, 4, 6)
    val s = EditSession(m)
    val stone = BlockState("minecraft:stone")
    val stairs = BlockState("minecraft:oak_stairs", mapOf("weirdo_direction" to 2, "upside_down_bit" to true))
    s.apply(Op.PLACE, ShapeKind.BOX, intArrayOf(0, 0, 0), intArrayOf(4, 0, 5), false, stone, false)
    s.apply(Op.PLACE, ShapeKind.SINGLE, intArrayOf(2, 1, 3), intArrayOf(2, 1, 3), false, stairs, false)
    check(m.blockCount == 5 * 6 + 1, "box+single count ${m.blockCount}")
    val bytes = McStructure.write(m, false)
    File("$out/test.mcstructure").writeBytes(bytes)
    val r = McStructure.read(bytes)
    check(r.sizeX == 5 && r.sizeY == 4 && r.sizeZ == 6, "size roundtrip")
    check(r.blockCount == m.blockCount, "blocks roundtrip")
    check(r.get(2, 1, 3) == stairs, "stairs state roundtrip: ${r.get(2, 1, 3)}")
    check(r.get(4, 0, 5) == stone, "corner stone")
    check(r.get(0, 1, 0) == null, "empty stays empty")
    val air = McStructure.write(m, true)
    check(McStructure.read(air).emptyAsAir, "air mode detected")
    check(McStructure.read(air).blockCount == m.blockCount, "air not counted as block")

    // --- negative expansion & export normalisation ---
    val m2 = BuildModel.create(2, 2, 2)
    val s2 = EditSession(m2)
    s2.apply(Op.PLACE, ShapeKind.SINGLE, intArrayOf(-3, 0, 0), intArrayOf(-3, 0, 0), false, stone, true)
    check(m2.minX == -3 && m2.sizeX == 5, "grid expanded west: ${m2.minX}..${m2.maxX}")
    val r2 = McStructure.read(McStructure.write(m2, false))
    check(r2.get(0, 0, 0) == stone, "shifted to origin on export")
    check(s2.undo() && m2.blockCount == 0, "undo")
    check(s2.redo() && m2.blockCount == 1, "redo")

    // --- shapes ---
    val buf = LongBuf()
    Shapes.generate(ShapeKind.SPHERE, intArrayOf(10, 10, 10), intArrayOf(15, 10, 10), false, buf)
    check(buf.size in 700..780, "filled sphere r5 cells=${buf.size}")
    val hb = LongBuf()
    Shapes.generate(ShapeKind.SPHERE, intArrayOf(10, 10, 10), intArrayOf(15, 10, 10), true, hb)
    check(hb.size in 150..350 && hb.size < buf.size, "hollow sphere cells=${hb.size}")
    val box = LongBuf(); Shapes.generate(ShapeKind.BOX, intArrayOf(0, 0, 0), intArrayOf(4, 4, 4), true, box)
    check(box.size == 125 - 27, "hollow box 5^3 = 98, got ${box.size}")
    val ln = LongBuf(); Shapes.generate(ShapeKind.LINE, intArrayOf(0, 0, 0), intArrayOf(10, 5, 0), false, ln)
    check(ln.size == 11, "line cells ${ln.size}")
    val pyr = LongBuf(); Shapes.generate(ShapeKind.PYRAMID, intArrayOf(0, 0, 0), intArrayOf(8, 4, 8), false, pyr)
    check(pyr.size == 81 + 49 + 25 + 9 + 1, "pyramid cells ${pyr.size}")
    val cyl = LongBuf(); Shapes.generate(ShapeKind.CYLINDER, intArrayOf(5, 0, 5), intArrayOf(8, 2, 5), false, cyl)
    check(cyl.size in 100..150, "cylinder r3 h3 cells=${cyl.size}")
    val dome = LongBuf(); Shapes.generate(ShapeKind.DOME, intArrayOf(0, 0, 0), intArrayOf(5, 0, 0), false, dome)
    check(dome.size < buf.size && dome.size > 200, "dome cells ${dome.size}")

    // --- flood fill / destroy / replace ---
    val m3 = BuildModel.create(6, 6, 6); val s3 = EditSession(m3)
    s3.apply(Op.PLACE, ShapeKind.FLOOD, intArrayOf(0, 0, 0), intArrayOf(0, 0, 0), false, stone, false)
    check(m3.blockCount == 216, "flood fill whole grid ${m3.blockCount}")
    val dirt = BlockState("minecraft:dirt")
    s3.apply(Op.REPLACE, ShapeKind.BOX, intArrayOf(0, 0, 0), intArrayOf(5, 1, 5), false, dirt, false)
    check(m3.get(0, 1, 0) == dirt && m3.get(0, 2, 0) == stone, "replace box")
    s3.apply(Op.DESTROY, ShapeKind.SPHERE, intArrayOf(3, 3, 3), intArrayOf(5, 3, 3), false, dirt, false)
    check(m3.blockCount < 216 - 50, "destroy sphere ${m3.blockCount}")
    check(s3.undo() && s3.undo() && s3.undo() && m3.blockCount == 0, "undo x3")

    // --- camera / picking ---
    val m4 = BuildModel.create(10, 10, 10); val s4 = EditSession(m4)
    s4.apply(Op.PLACE, ShapeKind.BOX, intArrayOf(0, 0, 0), intArrayOf(9, 2, 9), false, stone, false)
    s4.apply(Op.PLACE, ShapeKind.SINGLE, intArrayOf(4, 3, 4), intArrayOf(4, 3, 4), false, dirt, false)
    for ((yaw, pitch) in listOf(35f to 30f, 200f to 50f, -70f to 20f, 0f to 89f)) {
        val cam = Camera.fit(m4, yaw, pitch)
        val w = 1080f; val h = 1800f
        val pj = Projector(cam, w, h)
        val o = FloatArray(2)
        pj.project(4.5f, 3.5f, 4.5f, o, 0)
        val p = Picker.pick(m4, cam, w, h, o[0], o[1], 0, false, 0)
        check(p != null && p.hit && p.bx == 4 && p.by == 3 && p.bz == 4, "pick single block yaw=$yaw pitch=$pitch -> ${p?.let { "${it.bx},${it.by},${it.bz}" }}")
        pj.project(1.5f, 3.0f, 1.5f, o, 0)  // top surface of the slab layer
        val q = Picker.pick(m4, cam, w, h, o[0], o[1], 0, false, 0)
        check(q != null && q.hit && q.by == 2 && q.ny == 1 && q.nx == 0 && q.nz == 0, "pick top face normal yaw=$yaw: ${q?.let { "${it.bx},${it.by},${it.bz} n=${it.nx},${it.ny},${it.nz}" }}")
    }
    // plane pick on empty cell
    val cam = Camera.fit(m4, 35f, 40f); val pj = Projector(cam, 1080f, 1800f); val o = FloatArray(2)
    pj.project(8.5f, 6.0f, 8.5f, o, 0)
    val pl = Picker.pick(m4, cam, 1080f, 1800f, o[0], o[1], 6, false, 0)
    check(pl != null && !pl.hit && pl.hasPlane && pl.px == 8 && pl.py == 6 && pl.pz == 8, "plane pick")

    // --- mesh + render ---
    val mesh = MeshBuilder.build(m4, false, 0)
    // 10x10x3 slab: top 100 + 4 sides (10*3 each) = 100+120 +1 bottom 100 = 320, plus single block 5 faces (bottom hidden)
    check(mesh.count == 99 + 100 + 120 + 5, "mesh faces ${mesh.count}")
    val fr = FrameRenderer(); var quads = 0
    val drawn = fr.render(mesh, Camera.fit(m4), 1080f, 1800f, true) { quads += it }
    check(drawn in 100..250 && quads == drawn * 2, "render drawn=$drawn quads=$quads")

    // --- state parsing ---
    check(BlockState.parseStates("a=1, b=true,c=foo") == mapOf("a" to 1, "b" to true, "c" to "foo"), "parse states")
    check(BlockState.parseStates("bad") == null, "reject bad states")
    check(BlockRegistry.all.size > 900, "registry size ${BlockRegistry.all.size}")
    check(BlockRegistry.all.toSet().size == BlockRegistry.all.size, "no duplicate ids")
    println("registry: ${BlockRegistry.all.size} ids")
    println(if (failures == 0) "ALL PASSED" else "$failures FAILED")
    if (failures > 0) System.exit(1)
}
