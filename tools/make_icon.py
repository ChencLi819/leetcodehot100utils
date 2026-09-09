"""一次性脚本：生成 icon.ico / icon.png —— 与 GUI 内 drawBrandLogo 完全一致的原创 Logo
（绿色渐变圆角 + 玻璃高光 + 白色代码符 </> + 细内描边），不复用任何第三方标志。
"""
from PIL import Image, ImageDraw, ImageFont
SIZE = 256
A = (0x0C, 0xC8, 0xA4)   # drawBrandLogo GRAD_A
B = (0x65, 0xD2, 0x75)   # drawBrandLogo GRAD_B

base = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
grad = Image.new("RGBA", (SIZE, SIZE))
gd = ImageDraw.Draw(grad)
for y in range(SIZE):
    t = y / (SIZE - 1)
    row = tuple(int(A[i] + (B[i] - A[i]) * t) for i in range(3)) + (255,)
    gd.line([(0, y), (SIZE, y)], fill=row)
grad = grad.rotate(-30, expand=False)
mask = Image.new("L", (SIZE, SIZE), 0)
ImageDraw.Draw(mask).rounded_rectangle([6, 6, SIZE - 6, SIZE - 6], radius=64, fill=255)
base.paste(grad, (0, 0), mask)
# 顶部玻璃高光
gloss = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
ImageDraw.Draw(gloss).rounded_rectangle([6, 6, SIZE - 6, SIZE // 2], radius=64, fill=(255, 255, 255, 85))
alpha = gloss.split()[3].load()
for y in range(SIZE // 2, SIZE):
    fade = max(0, 255 - int(255 * (y - SIZE // 2) / (SIZE // 2)))
    for x in range(SIZE):
        alpha[x, y] = int(alpha[x, y] * fade / 255)
base = Image.alpha_composite(base, gloss)
d = ImageDraw.Draw(base)
# 中心代码符 </>（Consolas Bold，带柔和投影）
font = None
for cand in ["C:/Windows/Fonts/consolab.ttf", "C:/Windows/Fonts/arialbd.ttf"]:
    try:
        font = ImageFont.truetype(cand, 112)
        break
    except OSError:
        continue
if font is None:
    font = ImageFont.load_default()
bbox = d.textbbox((0, 0), "</>", font=font)
w, h = bbox[2] - bbox[0], bbox[3] - bbox[1]
tx, ty = (SIZE - w) / 2 - bbox[0], (SIZE - h) / 2 - bbox[1]
d.text((tx + 2, ty + 3), "</>", font=font, fill=(0, 0, 0, 55))
d.text((tx, ty), "</>", font=font, fill=(255, 255, 255, 255))
# 细内描边
d.rounded_rectangle([6, 6, SIZE - 7, SIZE - 7], radius=64, outline=(255, 255, 255, 60), width=2)
base.save("icon.ico", sizes=[(16,16),(24,24),(32,32),(48,48),(64,64),(128,128),(256,256)])
base.save("icon.png")
print("icon regenerated: </> logo (matches GUI drawBrandLogo)")
