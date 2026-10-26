"""Create real, local PNG assets for every article; never hotlink community images."""
import hashlib
from pathlib import Path


def font_path(cfg):
    candidates = [cfg.get("font_file"), "C:/Windows/Fonts/msyh.ttc", "C:/Windows/Fonts/simhei.ttf",
                  "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc",
                  "/usr/share/fonts/truetype/wqy/wqy-microhei.ttc"]
    for value in candidates:
        if value and Path(value).is_file():
            return value
    raise RuntimeError("install a CJK font or configure font_file before publishing illustrated articles")


def save_png(image, path):
    temp = path.with_suffix(".tmp")
    image.save(temp, format="PNG", optimize=True)
    temp.replace(path)


def wrap(draw, text, font, width, lines=3):
    rows, row = [], ""
    for char in text:
        if draw.textlength(row + char, font=font) > width:
            rows.append(row)
            row = char
        else:
            row += char
    if row:
        rows.append(row)
    if len(rows) > lines:
        rows = rows[:lines]
        rows[-1] = rows[-1][:-1] + "…"
    return rows


def make_images(cfg, root, ident, article, sources):
    from PIL import Image, ImageDraw, ImageFont
    root = Path(root) / "media"
    root.mkdir(parents=True, exist_ok=True)
    font = font_path(cfg)
    title_font = ImageFont.truetype(font, 48)
    small_font = ImageFont.truetype(font, 23)
    palette = [("#102f30", "#79d7b8", "#f2fff8"), ("#28294b", "#b3a5ee", "#fff9ee"),
               ("#393047", "#edb29d", "#fff7e9")]
    bg, accent, ink = palette[int(hashlib.sha256(article["title"].encode()).hexdigest()[:2], 16) % 3]
    image = Image.new("RGB", (1280, 720), bg)
    draw = ImageDraw.Draw(image)
    draw.ellipse((720, -180, 1450, 550), fill=accent)
    draw.ellipse((970, -70, 1320, 280), fill=ink)
    draw.polygon([(790, 540), (1150, 450), (1370, 740), (880, 790)], fill=accent)
    # An original book/gamepad illustration provides a visual, not only a title card.
    game = any(word in article["title"] for word in ("游戏", "玩家", "Steam", "主机"))
    if game:
        draw.rounded_rectangle((820, 220, 1160, 430), radius=80, fill=bg, outline=ink, width=8)
        draw.rounded_rectangle((870, 270, 900, 370), radius=8, fill=accent)
        draw.rounded_rectangle((835, 305, 935, 335), radius=8, fill=accent)
        for x, y in ((1080, 280), (1120, 320), (1040, 320), (1080, 360)):
            draw.ellipse((x - 13, y - 13, x + 13, y + 13), fill=ink)
    else:
        draw.polygon([(835, 245), (975, 280), (975, 470), (835, 430)], fill=ink)
        draw.polygon([(985, 280), (1130, 245), (1130, 430), (985, 470)], fill=bg)
        for y in (300, 340, 380):
            draw.line((1010, y, 1100, y - 23), fill=accent, width=7)
        draw.line((980, 280, 980, 470), fill=accent, width=7)
    draw.text((64, 62), "0721 ACGN  /  " + {"discussion": "社区观察", "news": "新消息", "rumor": "未证实消息"}.get(article["kind"], "资讯"), font=small_font, fill=accent)
    for index, line in enumerate(wrap(draw, article["title"], title_font, 610, 4)):
        draw.text((64, 220 + index * 75), line, font=title_font, fill=ink)
    draw.text((64, 624), article.get("event_date") or "持续关注中的新鲜事", font=small_font, fill=accent)
    credit = "0721ACGN 原创程序插画"
    # User-supplied, licensed local artwork is automatically matched by article topic.
    for entry in cfg.get("image_library", []):
        if any(word in article["title"] + article["summary"] for word in entry.get("topics", [])):
            with Image.open(entry["file"]) as original:
                if original.width * original.height > 40_000_000:
                    raise ValueError("licensed image too large")
                original.load()
                from PIL import ImageOps
                image = ImageOps.fit(original.convert("RGB"), (1280, 720))
            credit = entry["credit"] + " · " + entry["license"]
            break
    save_png(image, root / (ident + "-cover.png"))
    result = [{"role": "cover", "url": "/api/news/image?id=" + ident + "&role=cover",
               "caption": "封面", "credit": credit}]
    metrics_source = next((s for s in sources if any(v is not None for v in (s.get("metrics") or {}).values())), None)
    if metrics_source:
        chart = Image.new("RGB", (1280, 560), "#f1f7f4")
        pen = ImageDraw.Draw(chart)
        pen.text((64, 45), "讨论热度 · 采集时记录", font=ImageFont.truetype(font, 36), fill=bg)
        labels = {"views": "播放 / 浏览", "comments": "评论", "likes": "点赞"}
        for index, (key, label) in enumerate(labels.items()):
            x = 64 + index * 400
            value = metrics_source["metrics"].get(key)
            pen.text((x, 155), label, font=small_font, fill=bg)
            pen.text((x, 210), str(value) if value is not None else "未获取", font=title_font, fill=bg)
            growth = metrics_source.get("growth_per_hour") or {}
            pen.text((x, 300), f"每小时 +{growth[key]:g}" if key in growth else "增长：样本不足", font=small_font, fill=bg)
        pen.text((64, 435), "样本来自单条内容，不代表全平台；数据随时间变化。", font=small_font, fill=bg)
        save_png(chart, root / (ident + "-stats.png"))
        result.append({"role": "stats", "url": "/api/news/image?id=" + ident + "&role=stats",
                       "caption": "原内容的采集时统计", "credit": "0721ACGN 制图；来源：" + metrics_source["url"]})
    return result
