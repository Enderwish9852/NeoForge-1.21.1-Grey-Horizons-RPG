from PIL import Image
import random
import os

tex_dir = r"C:\Intellij code\NeoForge-1.21.1-Grey-Horizons RPG\Farming_Overhaul_Subpack\src\main\resources\assets\gh_farming_overhaul\textures\block"

trees = {
    "apple":     {"bark": (101, 67, 33),   "top": (139, 90, 43),    "leaves": (34, 139, 34)},
    "pear":      {"bark": (139, 119, 101), "top": (160, 140, 120),  "leaves": (50, 160, 50)},
    "fig":       {"bark": (101, 90, 80),   "top": (120, 110, 100),  "leaves": (40, 100, 40)},
    "guava":     {"bark": (160, 120, 80),  "top": (180, 140, 100),  "leaves": (60, 140, 60)},
    "kiwi":      {"bark": (180, 150, 100), "top": (200, 170, 120),  "leaves": (70, 150, 70)},
    "mango":     {"bark": (80, 50, 30),    "top": (100, 70, 50),    "leaves": (20, 100, 20)},
    "orange":    {"bark": (120, 100, 80),  "top": (140, 120, 100),  "leaves": (30, 120, 30)},
    "papaya":    {"bark": (200, 180, 140), "top": (220, 200, 160),  "leaves": (80, 160, 80)},
    "peach":     {"bark": (140, 80, 60),   "top": (160, 100, 80),   "leaves": (90, 140, 90)},
    "persimmon": {"bark": (100, 80, 60),   "top": (120, 100, 80),   "leaves": (40, 120, 40)},
    "plum":      {"bark": (70, 50, 80),    "top": (90, 70, 100),    "leaves": (60, 100, 60)},
    "pomegranate":{"bark": (110, 80, 60),  "top": (130, 100, 80),   "leaves": (50, 130, 50)},
}

def clamp(v):
    return max(0, min(255, int(v)))

def make_bark(base_color, w=16, h=16):
    img = Image.new("RGB", (w, h))
    px = img.load()
    for y in range(h):
        for x in range(w):
            r = clamp(base_color[0] + random.randint(-20, 20))
            g = clamp(base_color[1] + random.randint(-20, 20))
            b = clamp(base_color[2] + random.randint(-20, 20))
            px[x, y] = (r, g, b)
    return img

def make_log_top(base_color, w=16, h=16):
    img = Image.new("RGB", (w, h))
    px = img.load()
    cx, cy = 7.5, 7.5
    radius = 7
    for y in range(h):
        for x in range(w):
            dx = x - cx
            dy = y - cy
            dist = (dx*dx + dy*dy) ** 0.5
            if dist <= radius:
                ring = int(dist / 1.5) % 3
                if ring == 0:
                    px[x, y] = base_color
                elif ring == 1:
                    px[x, y] = tuple(clamp(c - 20) for c in base_color)
                else:
                    px[x, y] = tuple(clamp(c + 20) for c in base_color)
            else:
                # outside circle = bark color (use darker)
                px[x, y] = tuple(clamp(c - 10) for c in base_color)
    return img

def make_leaves(base_color, w=16, h=16):
    img = Image.new("RGB", (w, h))
    px = img.load()
    for y in range(h):
        for x in range(w):
            noise = random.randint(-30, 30)
            r = clamp(base_color[0] + noise)
            g = clamp(base_color[1] + noise)
            b = clamp(base_color[2] + noise)
            px[x, y] = (r, g, b)
    return img

count = 0
for name, colors in trees.items():
    # Bark
    img = make_bark(colors["bark"])
    img.save(os.path.join(tex_dir, f"{name}_log.png"))
    count += 1

    # Log top
    img = make_log_top(colors["top"])
    img.save(os.path.join(tex_dir, f"{name}_log_top.png"))
    count += 1

    # Leaves
    img = make_leaves(colors["leaves"])
    img.save(os.path.join(tex_dir, f"{name}_leaves.png"))
    count += 1

    print(f"Created textures for {name}")

print(f"\nTotal textures created: {count}")