"""Generate exact Code 128 labels; requires Pillow and reportlab (development tools only)."""
from pathlib import Path
import html
import json
from PIL import Image, ImageDraw, ImageFont
from reportlab.graphics.barcode.code128 import Code128

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "apps/web/public/demo-barcodes"
SAMPLES = [
    ("api-rice", "Gạo gói 1 kg", "8930000000001", "API / PostgreSQL"),
    ("api-apple", "Táo cân ký", "ST-APPLE-01", "API / PostgreSQL"),
    ("demo-milk", "Sữa tươi ít đường", "8934673120010", "Demo trình duyệt"),
    ("demo-cookies", "Bánh quy bơ", "8934673120027", "Demo trình duyệt"),
]
def font(size):
    for candidate in ("C:/Windows/Fonts/segoeui.ttf", "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"):
        if Path(candidate).exists():
            return ImageFont.truetype(candidate, size)
    return ImageFont.load_default(size=size)

def bars(value):
    code = Code128(value)
    code.validate()
    code.encode()
    pattern = code.decompose()
    x = 0
    result = []
    for letter in pattern:
        width = ord(letter.lower()) - ord("a") + 1
        if letter.isupper():
            result.append((x, width))
        x += width
    return x, result

OUT.mkdir(parents=True, exist_ok=True)
metadata = []
for filename, name, value, mode in SAMPLES:
    width, pattern = bars(value)
    module = 4
    left = (960 - width * module) // 2
    image = Image.new("RGB", (960, 420), "white")
    draw = ImageDraw.Draw(image)
    draw.text((480, 34), name, font=font(34), fill="#20192f", anchor="mt")
    draw.text((480, 88), mode + " · CODE 128", font=font(21), fill="#594a70", anchor="mt")
    for x, count in pattern:
        draw.rectangle((left + x * module, 142, left + (x + count) * module - 1, 301), fill="black")
    draw.text((480, 326), value, font=font(29), fill="black", anchor="mt")
    image.save(OUT / (filename + ".png"))
    rects = "".join(f'<rect x="{left+x*module}" y="142" width="{count*module}" height="160"/>' for x, count in pattern)
    svg = f'<svg xmlns="http://www.w3.org/2000/svg" width="960" height="420" viewBox="0 0 960 420"><rect width="960" height="420" fill="white"/><g text-anchor="middle" font-family="sans-serif" fill="#20192f"><text x="480" y="65" font-size="34">{html.escape(name)}</text><text x="480" y="110" font-size="21">{html.escape(mode)} · CODE 128</text><text x="480" y="355" font-size="29">{value}</text></g><g fill="black" shape-rendering="crispEdges">{rects}</g></svg>'
    (OUT / (filename + ".svg")).write_text(svg, encoding="utf-8")
    metadata.append(dict(file=filename, name=name, value=value, mode=mode))
sheet = Image.new("RGB", (1920, 840), "#ddd")
for i, (filename, *_rest) in enumerate(SAMPLES):
    sheet.paste(Image.open(OUT / (filename + ".png")), ((i % 2) * 960, (i // 2) * 420))
sheet.save(OUT / "sample-sheet.png")
(OUT / "samples.json").write_text(json.dumps(metadata, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
cards = "".join(f'<article><img src="{item["file"]}.png" alt="{html.escape(item["name"])} — {item["value"]}"><a download href="{item["file"]}.png">Tải ảnh {html.escape(item["name"])}</a></article>' for item in metadata)
page = '<!doctype html><html lang="vi"><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1"><title>Mã vạch mẫu · Sim Tím</title><style>body{font-family:system-ui;max-width:1200px;margin:32px auto;padding:0 16px;background:#f6f3fa;color:#241b35}main{display:grid;grid-template-columns:repeat(auto-fit,minmax(min(100%,420px),1fr));gap:20px}article{background:white;border-radius:16px;overflow:hidden;padding:12px}img{width:100%;height:auto}a{display:inline-block;padding:12px;color:#653d9b}</style><h1>Mã vạch mẫu</h1><p>Mở ảnh trên máy tính hoặc in rõ trên giấy rồi quét bằng điện thoại. Chọn ảnh đúng chế độ ứng dụng đang dùng; mã mẫu chỉ tìm được khi sản phẩm tương ứng có trong danh mục.</p><p>Hai mã API dùng danh mục gạo và táo của bản demo server. Hai mã demo dùng dữ liệu riêng trong trình duyệt. Giữ nguyên toàn bộ chuỗi, kể cả số 0 ở đầu. Đây là mã CODE 128; không đổi sang EAN-13.</p><main>' + cards + '</main></html>'
(OUT / "index.html").write_text(page, encoding="utf-8")
print(f"Generated {len(SAMPLES)} PNG/SVG labels and one sheet at {OUT}")
