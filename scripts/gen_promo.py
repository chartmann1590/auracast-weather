from PIL import Image, ImageDraw, ImageFont, ImageFilter, ImageOps
import os, math

W, H = 1920, 1080
# Colors
def load_font(size, bold=False):
    paths = [r"C:\Windows\Fonts\arialbd.ttf" if bold else r"C:\Windows\Fonts\Arial.ttf", r"C:\Windows\Fonts\segoeuib.ttf" if bold else r"C:\Windows\Fonts\segoeui.ttf"]
    for p in paths:
        if os.path.exists(p):
            try:
                return ImageFont.truetype(p, size)
            except: pass
    return ImageFont.load_default()

def gradient_bg(w,h, top=(79,168,255), bot=(11,30,61)):
    bg = Image.new("RGB", (w,h), top)
    d = ImageDraw.Draw(bg)
    for y in range(h):
        t = y/(h-1)
        r = int(top[0]*(1-t)+bot[0]*t)
        g = int(top[1]*(1-t)+bot[1]*t)
        b = int(top[2]*(1-t)+bot[2]*t)
        d.line([(0,y),(w,y)], fill=(r,g,b))
    # subtle bokeh
    overlay = Image.new("RGBA", (w,h), (0,0,0,0))
    od = ImageDraw.Draw(overlay)
    for (cx,cy,rx,ry,alpha) in [(1650,180,420,220,16),(260,900,380,220,12),(960,540,520,320,6)]:
        od.ellipse([cx-rx, cy-ry, cx+rx, cy+ry], fill=(255,255,255,alpha))
    bg = Image.alpha_composite(bg.convert("RGBA"), overlay).convert("RGB")
    return bg

def draw_phone_mock(bg, screenshot_path, x_center, y_center=540, target_h=920, caption=None, title=None, sub=None, align="right"):
    # bg is PIL image 1920x1080
    img = bg.copy()
    # Load screenshot and scale to target_h keeping aspect (original 1080x2400)
    shot = Image.open(screenshot_path).convert("RGBA")
    # scale so height = target_h
    scale = target_h / shot.height
    new_w = int(shot.width * scale)
    new_h = target_h
    shot_scaled = shot.resize((new_w, new_h), Image.LANCZOS)
    # add device shadow and rounded border
    # create phone frame with rounded corners and shadow
    # shadow
    shadow = Image.new("RGBA", (W,H), (0,0,0,0))
    sd = ImageDraw.Draw(shadow)
    # shadow ellipse under phone
    sx0, sx1 = x_center - new_w//2 -8, x_center + new_w//2 +8
    sy0, sy1 = y_center - new_h//2 -8, y_center + new_h//2 +8
    # blurred shadow via rectangle with alpha
    sd.rounded_rectangle([sx0+6, sy0+8, sx1+6, sy1+8], radius=28, fill=(0,0,0,55))
    img_rgba = img.convert("RGBA")
    img_rgba = Image.alpha_composite(img_rgba, shadow)
    # phone body: white border 12px with rounded 34
    # Create phone card
    phone = Image.new("RGBA", (new_w+24, new_h+24), (0,0,0,0))
    pd = ImageDraw.Draw(phone)
    pd.rounded_rectangle([0,0, new_w+24, new_h+24], radius=36, fill=(22,30,45))
    pd.rounded_rectangle([4,4, new_w+20, new_h+20], radius=32, fill=(0,0,0,255))
    # notch
    pd.rounded_rectangle([ (new_w+24)//2 - 44, 4, (new_w+24)//2 +44, 18], radius=6, fill=(22,30,45))
    # paste screenshot centered
    phone.paste(shot_scaled, (12,12), shot_scaled if shot_scaled.mode=="RGBA" else None)
    # clip? Already rounded via phone bg but screenshot corners not rounded; mask screenshot corners
    mask = Image.new("L", (new_w+24, new_h+24), 0)
    md = ImageDraw.Draw(mask)
    md.rounded_rectangle([12,12, new_w+12, new_h+12], radius=26, fill=255)
    # Apply mask to phone's screenshot area? Simpler: create alpha for phone
    # Use phone as is, just composite
    img_rgba.paste(phone, (x_center - (new_w+24)//2, y_center - (new_h+24)//2), phone)
    draw = ImageDraw.Draw(img_rgba)
    # Titles
    if title or caption:
        # Determine text area opposite phone
        if align=="right":
            tx0 = x_center + new_w//2 + 60
            tx1 = W - 80
        else:
            tx0 = 80
            tx1 = x_center - new_w//2 - 60
        # vertical center
        if title:
            font_title = load_font(54, bold=True)
            font_sub = load_font(28, bold=False)
            # wrap title? Simple split
            # measure
            # Draw title with shadow
            ty = y_center - 80
            # shadow
            draw.text((tx0+2, ty+2), title, font=font_title, fill=(0,0,0,60))
            draw.text((tx0, ty), title, font=font_title, fill=(255,255,255))
            if sub:
                # sub can be multiline (split by \n)
                lines = sub.split("\n")
                ly = ty + 78
                for line in lines:
                    draw.text((tx0, ly), line, font=font_sub, fill=(230,238,247))
                    # measure height
                    try:
                        l,t,r,b = font_sub.getbbox(line)
                        lh = b-t+10
                    except:
                        lh = 32
                    ly += lh
        if caption:
            font_cap = load_font(22, bold=False)
            # pill at bottom of text area
            draw.text((tx0, y_center+140), caption, font=font_cap, fill=(255,216,115))
    return img_rgba.convert("RGB")

# Generate slide images
os.makedirs(r"C:\WINDOWS\TEMP\opencode\promo_slides", exist_ok=True)
slides_dir = r"C:\WINDOWS\TEMP\opencode\promo_slides"

# Slide 1: Feature graphic centered with hero text (use feature graphic as bg? Actually create gradient with large icon left and text right like feature graphic)
def slide1():
    bg = gradient_bg(W,H)
    draw = ImageDraw.Draw(bg.convert("RGBA"))
    # Use large icon
    icon = Image.open(r"H:\weather-app\store\icon-512.png").convert("RGBA")
    icon = icon.resize((420,420), Image.LANCZOS)
    # shadow
    shadow = Image.new("RGBA", (W,H), (0,0,0,0))
    sd = ImageDraw.Draw(shadow)
    sd.ellipse([240, 780, 240+420+20, 820], fill=(0,0,0,45))
    bg_rgba = bg.convert("RGBA")
    bg_rgba = Image.alpha_composite(bg_rgba, shadow)
    bg_rgba.paste(icon, (240, 300), icon)
    draw = ImageDraw.Draw(bg_rgba)
    font_title = load_font(74, bold=True)
    font_sub = load_font(32, bold=False)
    font_feat = load_font(22, bold=False)
    # Title
    draw.text((742, 340), "AuraCast Weather", font=font_title, fill=(255,255,255))
    draw.text((744, 342), "AuraCast Weather", font=font_title, fill=(0,0,0,35))  # shadow underneath? Actually draw shadow first then white - but we already did white, so redo
    # Correct order: shadow then white
    # We'll just draw again with correct stacking
    # Instead recreate cleanly
    bg_rgba2 = bg.convert("RGBA")
    bg_rgba2 = Image.alpha_composite(bg_rgba2, shadow)
    bg_rgba2.paste(icon, (240, 300), icon)
    d2 = ImageDraw.Draw(bg_rgba2)
    d2.text((744, 342), "AuraCast Weather", font=font_title, fill=(0,0,0,55))
    d2.text((742, 340), "AuraCast Weather", font=font_title, fill=(255,255,255))
    d2.text((742, 430), "Your weather, narrated — fully on-device.", font=font_sub, fill=(255,235,160))
    d2.text((742, 480), "No account • Offline AI • 58 languages", font=font_feat, fill=(230,238,247))
    # badges
    badges = ["Gemma 4 + neural TTS", "RainViewer • NWS", "Open-Meteo"]
    bx = 742
    by = 540
    for b in badges:
        font_b = load_font(18, bold=False)
        try:
            l,t,r,b2 = font_b.getbbox(b)
            tw, th = r-l, b2-t
        except:
            tw, th = font_b.getsize(b)
        pad_x, pad_y = 14, 7
        bw, bh = tw+pad_x*2, th+pad_y*2
        d2.rounded_rectangle([bx, by, bx+bw, by+bh], radius=bh//2, fill=(255,255,255,230))
        d2.text((bx+pad_x, by+pad_y-1), b, font=font_b, fill=(11,30,61))
        bx += bw+12
    # Play badge bottom
    d2.rounded_rectangle([742, 620, 742+260, 620+54], radius=12, fill=(255,255,255))
    d2.text((762, 632), "GET IT ON  Google Play", font=load_font(20, bold=True), fill=(11,30,61))
    # small notice coming soon
    d2.text((1016, 638), "Coming soon", font=load_font(18, bold=False), fill=(255,255,255,200))
    return bg_rgba2.convert("RGB")

def slide_for_screenshot(screenshot_path, title, sub, caption, align="right"):
    bg = gradient_bg(W,H)
    return draw_phone_mock(bg, screenshot_path, x_center= 520 if align=="right" else 1400, y_center=540, target_h=880, title=title, sub=sub, caption=caption, align=align)

# Create slides
slides = []

# Slide 1
s1 = slide1()
s1_path = os.path.join(slides_dir, "slide1.png")
s1.save(s1_path, "PNG", optimize=True)
slides.append(s1_path)
print("slide1", s1_path)

# Slide 2: Home
s2 = slide_for_screenshot(r"H:\weather-app\store\screenshots\phone\01_home.png", "Stay ahead of the day", "Current • 48-hour hourly • 5-day\nOpen-Meteo + NWS • Offline cache\nBeautiful Meteocons + Material 3", "Home • Hero + hourly strip + 5-day", align="right")
s2_path = os.path.join(slides_dir, "slide2.png")
s2.save(s2_path, "PNG", optimize=True)
slides.append(s2_path)
print("slide2")

# Slide 3: Radar
s3 = slide_for_screenshot(r"H:\weather-app\store\screenshots\phone\02_radar.png", "Live precipitation radar", "RainViewer + NWS tiles on osmdroid\nPlay/pause & scrub — 40 min loop\nPulsing location • Attribution", "Radar • Animated tiles • No API key", align="left")
s3_path = os.path.join(slides_dir, "slide3.png")
s3.save(s3_path, "PNG", optimize=True)
slides.append(s3_path)
print("slide3")

# Slide 4: Report
s4 = slide_for_screenshot(r"H:\weather-app\store\screenshots\phone\03_report.png", "Your weather, narrated", "On-device Gemma 4 • LiteRT-LM\nReal neural TTS • Voice picker\n Karaoke transcript + 58 languages via ML Kit", "Report • Podcast player • On-device AI", align="right")
s4_path = os.path.join(slides_dir, "slide4.png")
s4.save(s4_path, "PNG", optimize=True)
slides.append(s4_path)
print("slide4")

# Slide 5: Multi-device + closing
def slide5():
    bg = gradient_bg(W,H)
    # Show 3 phones side by side: phone, 7inch, 10inch scaled
    # Load screenshots
    phone = Image.open(r"H:\weather-app\store\screenshots\phone\01_home.png").convert("RGBA")
    tab7 = Image.open(r"H:\weather-app\store\screenshots\7inch\01_home_7in.png").convert("RGBA")
    tab10 = Image.open(r"H:\weather-app\store\screenshots\10inch\01_home_10in.png").convert("RGBA")
    # Scale heights
    def phone_mock_small(sh, h, x):
        scale = h / sh.height
        nw = int(sh.width * scale)
        scaled = sh.resize((nw, h), Image.LANCZOS)
        # shadow
        shadow = Image.new("RGBA", (W,H), (0,0,0,0))
        sd = ImageDraw.Draw(shadow)
        sd.rounded_rectangle([x - nw//2 -10 +4, 540 - h//2 -10+6, x + nw//2 +10+4, 540 + h//2 +10+6], radius=28, fill=(0,0,0,35))
        bg_rgba = bg.convert("RGBA")
        bg_rgba = Image.alpha_composite(bg_rgba, shadow)
        # phone frame
        frame = Image.new("RGBA", (nw+20, h+20), (0,0,0,0))
        fd = ImageDraw.Draw(frame)
        fd.rounded_rectangle([0,0, nw+20, h+20], radius=28, fill=(22,30,45))
        frame.paste(scaled, (10,10), scaled)
        # mask corners
        mask = Image.new("L", (nw+20, h+20), 0)
        md = ImageDraw.Draw(mask)
        md.rounded_rectangle([10,10, nw+10, h+10], radius=20, fill=255)
        # Apply via paste with mask? Just use frame alpha already rounded via bg; we'll keep as is
        bg_rgba.paste(frame, (x - (nw+20)//2, 540 - (h+20)//2), frame)
        return bg_rgba
    bg_rgba = bg.convert("RGBA")
    # Composite sequentially
    # First do 7inch at left
    tmp = phone_mock_small(phone, 820, 520)
    # need to recompose: phone_mock_small created fresh from bg, not incremental. So do incremental manually
    # Let's do all three in one canvas
    canvas = bg.convert("RGBA")
    for (sh, h, x) in [(phone, 720, 380), (tab7, 800, 960), (phone, 680, 1560)]:  # Actually use phone for all but vary? Use tab10 for right? Use _use correct_
        pass
    # Simpler: just paste three phones with offsets using helper that composites onto canvas
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
        # shadow
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
    # Title bottom
    font_title = load_font(42, bold=True)
    font_sub = load_font(24, bold=False)
    # center text at bottom
    title = "One app. Every screen. Truly yours."
    sub = "Privacy-first • No account • Light & dark • Ad-free option via Play Billing"
    # measure title
    try:
        l,t,r,b = font_title.getbbox(title)
        tw = r-l
    except:
        tw = len(title)*20
    draw.text((W//2 - tw//2 +2, 880+2), title, font=font_title, fill=(0,0,0,50))
    draw.text((W//2 - tw//2, 880), title, font=font_title, fill=(255,255,255))
    try:
        l,t,r,b = font_sub.getbbox(sub)
        tw2 = r-l
    except:
        tw2 = len(sub)*10
    draw.text((W//2 - tw2//2, 940), sub, font=font_sub, fill=(255,255,255,230))
    # small url
    draw.text((W//2 - 140, 985), "auracast-weather.web.app  •  support@auracast.app", font=load_font(18, bold=False), fill=(255,216,115))
    return canvas.convert("RGB")

s5 = slide5()
s5_path = os.path.join(slides_dir, "slide5.png")
s5.save(s5_path, "PNG", optimize=True)
slides.append(s5_path)
print("slide5")

# Also generate poster from slide1
poster_path = os.path.join(slides_dir, "poster.png")
s1.save(poster_path, "PNG", optimize=True)
print("poster", poster_path)

# Now generate video via ffmpeg: 30 seconds total, 6 sec per slide, 30fps
# Use ffmpeg with concat and xfade or simple concat with no transition
# We'll create a concat file list with each slide duplicated for duration
import subprocess, textwrap

# Create ffmpeg concat list
list_path = os.path.join(slides_dir, "concat.txt")
with open(list_path, "w") as f:
    for p in slides:
        # Use forward slashes for ffmpeg
        pf = p.replace("\\", "/")
        f.write(f"file '{pf}'\n")
        f.write(f"duration 6\n")
    # need last file again without duration per concat demuxer spec
    f.write(f"file '{slides[-1].replace(chr(92),'/')}'\n")

print(open(list_path).read())

# Build video with crossfade using xfade filter is more complex with 5 inputs. Simpler: use zoompan or just hard cuts via concat then encode.
# We'll use concat demuxer with framerate
# First encode hard-cut version
out_mp4 = r"H:\weather-app\store\promo.mp4"
out_webm = r"H:\weather-app\store\promo.webm"
out_poster = r"H:\weather-app\store\promoPoster.png"
# Ensure store dir
os.makedirs(r"H:\weather-app\store", exist_ok=True)

# ffmpeg command for 30s video at 30fps, each image 6 sec
# Use: ffmpeg -f concat -safe 0 -i concat.txt -vf "scale=1920:1080:flags=lanczos,format=yuv420p" -r 30 -c:v libx264 -pix_fmt yuv420p -movflags +faststart out
cmd = [
    "ffmpeg", "-y",
    "-f", "concat", "-safe", "0", "-i", list_path,
    "-vf", "scale=1920:1080:flags=lanczos,format=yuv420p",
    "-r", "30",
    "-c:v", "libx264", "-profile:v", "high", "-crf", "18", "-preset", "medium",
    "-pix_fmt", "yuv420p",
    "-movflags", "+faststart",
    out_mp4
]
print(" ".join(cmd))
result = subprocess.run(cmd, capture_output=True, text=True)
print(result.stdout[-2000:])
print(result.stderr[-4000:])
if result.returncode != 0:
    print("ffmpeg failed, trying alternative with loop")
    # Alternative: use each slide with -loop 1
    # Build alternative via filter_complex with xfade
    # For now try simpler: generate video per slide then concat
    raise SystemExit(result.stderr)

print("mp4 done", os.path.getsize(out_mp4))

# Add audio? No audio silent is fine, but add silent audio track for compatibility (some players expect audio)
# We'll add silent aac if missing
# Check if video has audio
import json
probe = subprocess.run(["ffprobe","-v","error","-show_streams","-of","json", out_mp4], capture_output=True, text=True)
print(probe.stdout[:1000])
if "codec_type\" : \"audio\"" not in probe.stdout and '"codec_type": "audio"' not in probe.stdout:
    # add silent audio 30s
    tmp_aac = out_mp4 + ".withaudio.mp4"
    cmd2 = [
        "ffmpeg","-y",
        "-i", out_mp4,
        "-f","lavfi","-i","anullsrc=channel_layout=stereo:sample_rate=44100",
        "-shortest",
        "-c:v","copy",
        "-c:a","aac","-b:a","128k",
        tmp_aac
    ]
    r2 = subprocess.run(cmd2, capture_output=True, text=True)
    print(r2.stderr[-2000:])
    if r2.returncode==0 and os.path.exists(tmp_aac):
        os.replace(tmp_aac, out_mp4)
        print("added silent audio")

# Generate webm version via ffmpeg
cmd_webm = [
    "ffmpeg","-y","-i", out_mp4,
    "-c:v","libvpx-vp9","-b:v","0","-crf","32",
    "-pix_fmt","yuv420p",
    out_webm
]
r3 = subprocess.run(cmd_webm, capture_output=True, text=True)
print(r3.stderr[-2000:])
print("webm", os.path.exists(out_webm), os.path.getsize(out_webm) if os.path.exists(out_webm) else "missing")

# Poster is slide1
import shutil
shutil.copy(s1_path, out_poster)
print("poster copy done", os.path.getsize(out_poster))

# Copy to other locations
for dst in [
    r"H:\weather-app\fastlane\metadata\android\en-US\images\promo.mp4",
    r"H:\weather-app\website\public\promo.mp4",
    r"H:\weather-app\website\public\assets\promo.mp4",
]:
    os.makedirs(os.path.dirname(dst), exist_ok=True)
    shutil.copy(out_mp4, dst)
    print("copy mp4", dst, os.path.getsize(dst))

for dst in [
    r"H:\weather-app\fastlane\metadata\android\en-US\images\promoPoster.png",
    r"H:\weather-app\website\public\promoPoster.png",
    r"H:\weather-app\website\public\assets\promoPoster.png",
]:
    os.makedirs(os.path.dirname(dst), exist_ok=True)
    shutil.copy(out_poster, dst)
    print("copy poster", dst)

# Also copy webm to website
shutil.copy(out_webm, r"H:\weather-app\website\public\promo.webm")
print("all promo done")
