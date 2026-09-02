from PIL import Image, ImageDraw, ImageFont, ImageFilter
import math, os

W, H = 1024, 500
# Create gradient background #4FA8FF to #0B1E3D with gold highlight
bg = Image.new("RGB", (W,H), "#4FA8FF")
draw = ImageDraw.Draw(bg)
for y in range(H):
    t = y/(H-1)
    # gradient top #4FA8FF (79,168,255) to bottom #0B1E3D (11,30,61)
    r = int(79*(1-t) + 11*t)
    g = int(168*(1-t) + 30*t)
    b = int(255*(1-t) + 61*t)
    draw.line([(0,y),(W,y)], fill=(r,g,b))

# Add abstract cloud shapes / subtle pattern overlay
overlay = Image.new("RGBA", (W,H), (0,0,0,0))
od = ImageDraw.Draw(overlay)
# large soft white ellipses for cloud bokeh
for (cx,cy,rx,ry,alpha) in [(860,90,220,120,18),(180,420,280,160,14),(520,240,360,180,8)]:
    od.ellipse([cx-rx, cy-ry, cx+rx, cy+ry], fill=(255,255,255, alpha))
bg = Image.alpha_composite(bg.convert("RGBA"), overlay).convert("RGB")

# Load icon master 1024 and composite on left
icon_path = r"H:\weather-app\store\icon-512.png"
icon = Image.open(icon_path).convert("RGBA")
# Create feature's icon: use master 1024 original with rounded rect? We'll use the 512 and scale to 360x360 with shadow
icon_size = 320
icon_resized = icon.resize((icon_size, icon_size), Image.LANCZOS)
# shadow
shadow = Image.new("RGBA", (W,H), (0,0,0,0))
sd = ImageDraw.Draw(shadow)
# ellipse shadow under icon
sd.ellipse([58, 380, 58+icon_size+20, 420], fill=(0,0,0,45))
bg_rgba = bg.convert("RGBA")
bg_rgba = Image.alpha_composite(bg_rgba, shadow)
# paste icon at (60, 90)
bg_rgba.paste(icon_resized, (64, 78), icon_resized)

# Need fonts: try to use DejaVu or Arial. Check available
import pathlib
# Try to find Inter / Lexend via system? Fallback to DejaVu
def find_font():
    candidates = [
        r"C:\Windows\Fonts\Inter-Regular.ttf",
        r"C:\Windows\Fonts\Arial.ttf",
        r"C:\Windows\Fonts\segoeui.ttf",
        r"C:\Windows\Fonts\calibri.ttf",
    ]
    for p in candidates:
        if os.path.exists(p):
            return p
    return None

font_path = find_font()
print("font", font_path)

# Use PIL's default if not found but we want better. We'll try to use a bundled approach: use DejaVuSans via PIL's resource? Let's attempt to load via ImageFont.truetype
try:
    # Try using a free Google font if not present, we can use PIL's load_default but need nice
    # We'll attempt to download nothing; just use arial
    title_font = ImageFont.truetype(font_path, 66) if font_path else ImageFont.load_default()
    sub_font = ImageFont.truetype(font_path, 22) if font_path else ImageFont.load_default()
    feat_font = ImageFont.truetype(font_path, 17) if font_path else ImageFont.load_default()
    badge_font = ImageFont.truetype(font_path, 15) if font_path else ImageFont.load_default()
except Exception as e:
    print("font load failed", e)
    title_font = ImageFont.load_default()
    sub_font = ImageFont.load_default()
    feat_font = ImageFont.load_default()
    badge_font = ImageFont.load_default()

draw2 = ImageDraw.Draw(bg_rgba)

# Title: AuraCast Weather
# Use bold effect by drawing twice with offset? We'll just draw with stronger color
title = "AuraCast Weather"
subtitle = "Your weather, narrated — fully on-device."
# Try to get bold variant if exists
bold_path = font_path.replace("Regular","Bold") if font_path and "Regular" in font_path else font_path
try:
    if os.path.exists(bold_path):
        title_font_bold = ImageFont.truetype(bold_path, 68)
    else:
        # try Arial Bold
        arial_bold = r"C:\Windows\Fonts\arialbd.ttf"
        if os.path.exists(arial_bold):
            title_font_bold = ImageFont.truetype(arial_bold, 66)
        else:
            title_font_bold = title_font
except:
    title_font_bold = title_font

# Measure title
def text_size(font, text):
    try:
        l,t,r,b = font.getbbox(text)
        return r-l, b-t
    except:
        return font.getsize(text)

tw, th = text_size(title_font_bold, title)
# Position: right of icon, x=420, y=110
tx = 420
ty = 112
# Draw subtle text shadow
draw2.text((tx+2, ty+2), title, font=title_font_bold, fill=(0,0,0,70))
draw2.text((tx, ty), title, font=title_font_bold, fill=(255,255,255,255))

# Subtitle
st_w, st_h = text_size(sub_font, subtitle)
draw2.text((tx+1, ty+th+18+1), subtitle, font=sub_font, fill=(0,0,0,60))
draw2.text((tx, ty+th+18), subtitle, font=sub_font, fill=(255,235,160,255))  # gold tint
# Actually make subtitle white with slight opacity
draw2.text((tx, ty+th+18), subtitle, font=sub_font, fill=(255,255,255, 225))

# Feature pills row: 3 pills with icons (text only)
features = [
    "AI weather podcast  • Gemma 4 on-device",
    "Animated radar  • RainViewer + NWS",
    "58 languages  • ML Kit offline",
]
pill_y = ty+th+70
for i, feat in enumerate(features):
    # pill bg: translucent white rounded
    fw, fh = text_size(feat_font, feat)
    pad_x, pad_y = 14, 7
    pw, ph = fw + pad_x*2, fh + pad_y*2
    px = tx + (i % 2)* (pw+12) if i<2 else tx
    # For 3 pills, layout as 2 on first row? Actually do 2 on first row, 1 on second? Simplify: single column vertical list with bullet dots?
    # Let's do vertical list with dot + text on translucent bg full width
    pass

# Instead do clean vertical list with check/dots
list_y = ty+th+68
for feat in features:
    # dot
    dot_r = 4
    dot_x = tx + 6
    dot_y = list_y + 9
    draw2.ellipse([dot_x-dot_r, dot_y-dot_r, dot_x+dot_r, dot_y+dot_r], fill=(255,216,115,255))
    draw2.text((tx+18, list_y), feat, font=feat_font, fill=(255,255,255,255))
    # Add subtle bg behind each line? Not needed
    list_y += 28

# Bottom badge row: Google Play, On-device, No account
badge_y = H - 62
badges = ["No account needed", "Works offline", "Light & dark"]
bx = tx
for badge in badges:
    bw, bh = text_size(badge_font, badge)
    pad_x, pad_y = 12, 6
    bw2, bh2 = bw+pad_x*2, bh+pad_y*2
    # pill
    # Use rounded_rectangle if available
    try:
        draw2.rounded_rectangle([bx, badge_y, bx+bw2, badge_y+bh2], radius= bh2//2, fill=(255,255,255,230), outline=(255,255,255,255))
    except:
        draw2.rectangle([bx, badge_y, bx+bw2, badge_y+bh2], fill=(255,255,255,230))
    # text in dark blue
    draw2.text((bx+pad_x, badge_y+pad_y-1), badge, font=badge_font, fill=(11,30,61,255))
    bx += bw2 + 10

# Top-right small "GET IT ON" style? We'll add a subtle Play badge mock
# Draw a small fake Google Play badge on far right? Instead keep clean.

# Add subtle vignette border
# Save
out_feature = r"H:\weather-app\store\featureGraphic-1024x500.png"
bg_rgba.convert("RGB").save(out_feature, "PNG", optimize=True)
print("saved feature", out_feature, os.path.getsize(out_feature))

# Also save copy for fastlane / website
for p in [
    r"H:\weather-app\fastlane\metadata\android\en-US\images\featureGraphic.png",
    r"H:\weather-app\website\public\featureGraphic.png",
    r"H:\weather-app\website\public\featureGraphic-1024x500.png",
]:
    os.makedirs(os.path.dirname(p), exist_ok=True)
    bg_rgba.convert("RGB").save(p, "PNG", optimize=True)
    print("copy", p)

# Also create 2x version for website high dpi? Already 1024
# Verify
im = Image.open(out_feature)
print("feature size", im.size)
