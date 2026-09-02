from PIL import Image, ImageDraw, ImageFont
import os, subprocess, pathlib

# Reuse existing slides from previous generation - they are already good at C:\WINDOWS\TEMP\opencode\promo_slides
# But we will regenerate slides with slightly more polished text and ensure 1920x1080

W,H = 1920,1080

def load_font(size, bold=False):
    paths = [r"C:\Windows\Fonts\arialbd.ttf" if bold else r"C:\Windows\Fonts\Arial.ttf"]
    for p in paths:
        if os.path.exists(p):
            try:
                return ImageFont.truetype(p, size)
            except: pass
    return ImageFont.load_default()

def gradient_bg():
    bg = Image.new("RGB", (W,H), (79,168,255))
    d = ImageDraw.Draw(bg)
    for y in range(H):
        t=y/(H-1)
        r=int(79*(1-t)+11*t)
        g=int(168*(1-t)+30*t)
        b=int(255*(1-t)+61*t)
        d.line([(0,y),(W,y)], fill=(r,g,b))
    overlay = Image.new("RGBA", (W,H), (0,0,0,0))
    od = ImageDraw.Draw(overlay)
    for (cx,cy,rx,ry,a) in [(1650,180,420,220,16),(260,900,380,220,12)]:
        od.ellipse([cx-rx,cy-ry,cx+rx,cy+ry], fill=(255,255,255,a))
    bg = Image.alpha_composite(bg.convert("RGBA"), overlay).convert("RGB")
    return bg

slides_dir = r"C:\WINDOWS\TEMP\opencode\promo_slides"
os.makedirs(slides_dir, exist_ok=True)

# Check if slides exist, if not we need to generate them - but we already did, reuse
# Ensure slides are present
existing = [os.path.join(slides_dir, f"slide{i}.png") for i in range(1,6)]
missing = [p for p in existing if not os.path.exists(p)]
if missing:
    print("Missing slides, regenerating via previous script...")
    # fallback to run previous gen_promo.py slides generation? Instead just copy from store promo slides? 
    raise SystemExit(f"Missing {missing}, run gen_promo.py first")

# Now create polished promo with xfade
# Inputs: 5 slides, each 6 sec, fade 0.8 sec
# Build ffmpeg command with xfade

# Create temp list for debugging, but we will use filter_complex

# Build command
# Each input: -loop 1 -t 6 -i slide - but for xfade we need to set framerate
# Use -r 30 before inputs? Better to set -framerate 30 -loop 1 -t 6

cmd = ["ffmpeg","-y"]
for i, slide in enumerate(existing):
    cmd += ["-loop","1","-t","6","-i", slide]
# filter_complex
# Chain xfade: [0][1]xfade=transition=fade:duration=0.8:offset=5.2[v01]; [v01][2]xfade=...
filter_parts = []
# First, scale and format each input to 1920x1080 yuv420p - but slides already 1920x1080, so just format
# We'll apply scale/format in filter before xfade
# Actually we can just set inputs as is, but need to ensure same resolution and pix_fmt
# We'll do: [0]scale=1920:1080:flags=lanczos,format=yuv420p[0s]; etc? Simpler to include in filter

# Build scaled streams
for i in range(5):
    filter_parts.append(f"[{i}:v]scale=1920:1080:flags=lanczos,format=yuv420p,setsar=1,fps=30[v{i}s]")

# Now xfade chain
# v0s + v1s -> v01
filter_parts.append(f"[v0s][v1s]xfade=transition=fade:duration=0.8:offset=5.2[v01]")
filter_parts.append(f"[v01][v2s]xfade=transition=fade:duration=0.8:offset=10.4[v02]")
filter_parts.append(f"[v02][v3s]xfade=transition=fade:duration=0.8:offset=15.6[v03]")
filter_parts.append(f"[v03][v4s]xfade=transition=fade:duration=0.8:offset=20.8,format=yuv420p[v]")

filter_complex = ";".join(filter_parts)

out_mp4 = r"H:\weather-app\store\promo.mp4"
out_webm = r"H:\weather-app\store\promo.webm"
out_poster = r"H:\weather-app\store\promoPoster.png"

# Poster is slide1
Image.open(existing[0]).save(out_poster, "PNG", optimize=True)
print(f"poster {out_poster} {os.path.getsize(out_poster)}")

# Build final ffmpeg command with audio (silent)
# We need to add silent audio for compatibility: use anullsrc
# Duration total = 5*6 -4*0.8 = 26.8 sec
duration = 5*6 - 4*0.8
print(f"expected duration {duration}")

# Use filter_complex for video, and add audio
full_cmd = cmd + ["-f","lavfi","-t",str(duration),"-i","anullsrc=channel_layout=stereo:sample_rate=44100",
                  "-filter_complex", filter_complex,
                  "-map","[v]","-map","5:a",
                  "-c:v","libx264","-profile:v","high","-crf","18","-preset","medium","-pix_fmt","yuv420p","-r","30","-movflags","+faststart",
                  "-c:a","aac","-b:a","128k","-shortest",
                  out_mp4]

print(" ".join(full_cmd))
result = subprocess.run(full_cmd, capture_output=True, text=True)
print(result.stdout[-1000:])
print(result.stderr[-5000:])
if result.returncode !=0:
    print("FAILED")
    raise SystemExit(result.stderr)

print("mp4 done", os.path.getsize(out_mp4))
# Verify
import subprocess as sp
probe = sp.run(["ffprobe","-v","error","-select_streams","v:0","-show_entries","stream=width,height,codec_name,avg_frame_rate,duration","-of","default=nw=1", out_mp4], capture_output=True, text=True)
print(probe.stdout)
probe2 = sp.run(["ffprobe","-v","error","-show_entries","format=duration","-of","default=nw=1", out_mp4], capture_output=True, text=True)
print(probe2.stdout)

# Generate webm from mp4
cmd_webm = ["ffmpeg","-y","-i", out_mp4, "-c:v","libvpx-vp9","-b:v","0","-crf","32","-pix_fmt","yuv420p","-c:a","libopus","-b:a","96k", out_webm]
r3 = subprocess.run(cmd_webm, capture_output=True, text=True)
print(r3.stderr[-2000:])
print("webm", os.path.getsize(out_webm) if os.path.exists(out_webm) else "missing")

# Copy to fastlane and website
import shutil
for dst in [r"H:\weather-app\fastlane\metadata\android\en-US\images\promo.mp4", r"H:\weather-app\website\public\assets\promo.mp4"]:
    os.makedirs(os.path.dirname(dst), exist_ok=True)
    shutil.copy(out_mp4, dst)
    print("copy mp4", dst, os.path.getsize(dst))
for dst in [r"H:\weather-app\fastlane\metadata\android\en-US\images\promoPoster.png", r"H:\weather-app\website\public\assets\promo-poster.png", r"H:\weather-app\website\public\assets\promoPoster.png"]:
    os.makedirs(os.path.dirname(dst), exist_ok=True)
    shutil.copy(out_poster, dst)
    print("copy poster", dst)
shutil.copy(out_webm, r"H:\weather-app\website\public\assets\promo.webm")
print("all done")
