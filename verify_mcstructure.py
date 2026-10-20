"""Independent little-endian NBT parser to validate .mcstructure output."""
import struct, sys
def rd(b, p, t):
    if t == 1: return b[p], p+1
    if t == 2: return struct.unpack_from('<h', b, p)[0], p+2
    if t == 3: return struct.unpack_from('<i', b, p)[0], p+4
    if t == 4: return struct.unpack_from('<q', b, p)[0], p+8
    if t == 5: return struct.unpack_from('<f', b, p)[0], p+4
    if t == 6: return struct.unpack_from('<d', b, p)[0], p+8
    if t == 8:
        n = struct.unpack_from('<H', b, p)[0]; return b[p+2:p+2+n].decode(), p+2+n
    if t == 9:
        et = b[p]; n = struct.unpack_from('<i', b, p+1)[0]; p += 5; out = []
        for _ in range(n):
            v, p = rd(b, p, et); out.append(v)
        return out, p
    if t == 10:
        d = {}
        while True:
            tt = b[p]; p += 1
            if tt == 0: return d, p
            n = struct.unpack_from('<H', b, p)[0]; name = b[p+2:p+2+n].decode(); p += 2+n
            d[name], p = rd(b, p, tt)
    raise ValueError(t)
b = open(sys.argv[1], 'rb').read()
assert b[0] == 10 and b[1:3] == b'\0\0', "root must be unnamed compound"
root, end = rd(b, 3, 10)
assert end == len(b), "trailing bytes"
sx, sy, sz = root['size']; st = root['structure']
l0, l1 = st['block_indices']
assert len(l0) == len(l1) == sx*sy*sz
pal = st['palette']['default']['block_palette']
print("keys:", list(root.keys()), "size:", (sx, sy, sz), "palette:", [(p['name'], p['states'], p['version']) for p in pal])
# index = (x*sy + y)*sz + z
def at(x, y, z): return l0[(x*sy+y)*sz+z]
print("stairs idx:", pal[at(2,1,3)]['name'], "| corner:", pal[at(4,0,5)]['name'], "| empty:", at(0,1,0))
assert pal[at(2,1,3)]['name'] == 'minecraft:oak_stairs' and at(0,1,0) == -1
print("PYTHON VERIFY OK")
