from PIL import Image, ImageDraw, ImageFont
import os

W,H = 1920,1080

def load_font(size, bold=False):
    paths = [r"C:\Windows\Fonts\arialbd.ttf" if bold else r"C:\Windows\Fonts\Arial.ttf", r"C:\Windows\Fonts\segoeuib.ttf" if bold else r"C:\Windows\Fonts\segoeui.ttf"]
    for p in paths:
        if os.path.exists(p):
            try: return ImageFont.truetype(p, size)
            except: pass
    return ImageFont.load_default()

def gradient_bg():
    bg = Image.new("RGB", (W,H), (79,168,255))
    d = ImageDraw.Draw(bg)
    for y in range(H):
        t=y/(H-1)
        r=int(79*(1-t)+11*t); g=int(168*(1-t)+30*t); b=int(255*(1-t)+61*t)
        d.line([(0,y),(W,y)], fill=(r,g,b))
    overlay = Image.new("RGBA", (W,H), (0,0,0,0))
    od = ImageDraw.Draw(overlay)
    for (cx,cy,rx,ry,a) in [(1650,180,420,220,16),(260,900,380,220,12),(960,540,520,320,6)]:
        od.ellipse([cx-rx, cy-ry, cx+rx, cy+ry], fill=(255,255,255,a))
    bg = Image.alpha_composite(bg.convert("RGBA"), overlay).convert("RGB")
    return bg

slides_dir = r"C:\WINDOWS\TEMP\opencode\promo_slides"
os.makedirs(slides_dir, exist_ok=True)

# Load existing slide5 or regenerate
# Let's fully regenerate slide5 with GitHub end card
def slide5_with_github():
    bg = gradient_bg()
    canvas = bg.convert("RGBA")
    items = [
        (r"H:\weather-app\store\screenshots\7inch\02_radar_7in.png", 720, 430),
        (r"H:\weather-app\store\screenshots\phone\01_home.png", 820, 960),
        (r"H:\weather-app\store\screenshots\10inch\03_report_10in.png", 760, 1490),
    ]
    for path, h, cx in items:
        sh = Image.open(path).convert("RGBA")
        scale = h / sh.height
        nw = int(sh.width * scale)
        scaled = sh.resize((nw, h), Image.LANCZOS)
        shadow = Image.new("RGBA", (W,H), (0,0,0,0))
        sd = ImageDraw.Draw(shadow)
        sd.rounded_rectangle([cx - nw//2 -8 +5, 540 - h//2 -8+7, cx + nw//2 +8+5, 540 + h//2 +8+7], radius=28, fill=(0,0,0,40))
        canvas = Image.alpha_composite(canvas, shadow)
        frame = Image.new("RGBA", (nw+18, h+18), (0,0,0,0))
        fd = ImageDraw.Draw(frame)
        fd.rounded_rectangle([0,0, nw+18, h+18], radius=30, fill=(22,30,45))
        fd.rounded_rectangle([3,3, nw+15, h+15], radius=26, fill=(0,0,0,255))
        frame.paste(scaled, (9,9), scaled if scaled.mode=="RGBA" else None)
        canvas.paste(frame, (cx - (nw+18)//2, 540 - (h+18)//2), frame)
    draw = ImageDraw.Draw(canvas)
    font_title = load_font(44, bold=True)
    font_sub = load_font(22, bold=False)
    font_github = load_font(26, bold=True)
    font_small = load_font(18, bold=False)
    # Title top? Keep center bottom area
    title = "One app. Every screen. Truly yours."
    sub = "Privacy-first  •  No account  •  Light & dark  •  Ad-free option via Play Billing"
    # GitHub line - prominent white pill with dark text
    github_url = "github.com/chartmann1590/auracast-weather"
    website = "auracast-weather.web.app  •  support@auracast.app"
    # measure title
    try:
        l,t,r,b = font_title.getbbox(title)
        tw = r-l
    except: tw = len(title)*20
    draw.text((W//2 - tw//2 +2, 860+2), title, font=font_title, fill=(0,0,0,50))
    draw.text((W//2 - tw//2, 860), title, font=font_title, fill=(255,255,255))
    try:
        l,t,r,b = font_sub.getbbox(sub)
        tw2 = r-l
    except: tw2 = len(sub)*10
    draw.text((W//2 - tw2//2, 915), sub, font=font_sub, fill=(255,255,255,230))
    # GitHub pill - white rounded rectangle behind
    try:
        l,t,r,b = font_github.getbbox(github_url)
        gw, gh = r-l, b-t
    except:
        gw, gh = len(github_url)*14, 26
    pad_x, pad_y = 22, 10
    gw2, gh2 = gw+pad_x*2, gh+pad_y*2
    gx = W//2 - gw2//2
    gy = 958
    # subtle shadow for pill
    draw.rounded_rectangle([gx+3, gy+3, gx+gw2+3, gy+gh2+3], radius=gh2//2, fill=(0,0,0,45))
    draw.rounded_rectangle([gx, gy, gx+gw2, gy+gh2], radius=gh2//2, fill=(255,255,255))
    # GitHub icon: simple cat-like? we'll just do text with star
    draw.text((gx+pad_x, gy+pad_y-1), github_url, font=font_github, fill=(11,30,61))
    # Small star text
    # below github pill, website line
    try:
        l,t,r,b = font_small.getbbox(website)
        ww = r-l
    except: ww = len(website)*8
    draw.text((W//2 - ww//2, gy+gh2+10), website, font=font_small, fill=(255,216,115))
    # Top-left small "OPEN SOURCE" badge
    # Add a tiny badge top-left for open source
    # draw.text((30, 30), "OPEN SOURCE ON GITHUB", font=load_font(16,bold=True), fill=(255,255,255,210))
    return canvas.convert("RGB")

s5 = slide5_with_github()
out = os.path.join(slides_dir, "slide5.png")
s5.save(out, "PNG", optimize=True)
print(f"new slide5 saved {out} {os.path.getsize(out)} {s5.size}")
# also save poster? poster is slide1, keep
# verify
im = Image.open(out)
print(im.size)

# Also create an explicit end card (slide6) that is full-screen ending card with feature graphic + github
# But we will keep 5 slides; the last is now the end card with github prominently
# Ensure slides 1-4 still exist; if not, regenerate them via gen_promo? Check
for i in range(1,6):
    p=os.path.join(slides_dir, f"slide{i}.png")
    exists=os.path.exists(p)
    print(f"slide{i} exists:{exists} size:{os.path.getsize(p) if exists else 0}")

# Also update the promoPoster's underlying? keep slide1
