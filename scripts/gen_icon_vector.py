from pathlib import Path
import math

# Create adaptive background vector: shape gradient
bg_xml = '''<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <gradient android:angle="270" android:startColor="#4FA8FF" android:endColor="#0B1E3D" android:type="linear" />
</shape>'''
Path(r'H:\weather-app\app\src\main\res\drawable\ic_launcher_background.xml').write_text(bg_xml, encoding='utf-8')
print('wrote background xml')

s = 108/1024
sun_cx, sun_cy = 512*s, 360*s
sun_r = 148*s
print(f'sun {sun_cx:.2f},{sun_cy:.2f} r={sun_r:.2f}')

fg_lines = []
fg_lines.append('<?xml version="1.0" encoding="utf-8"?>')
fg_lines.append('<vector xmlns:android="http://schemas.android.com/apk/res/android"')
fg_lines.append('    android:width="108dp"')
fg_lines.append('    android:height="108dp"')
fg_lines.append('    android:viewportWidth="108"')
fg_lines.append('    android:viewportHeight="108">')
# Sun glow
r_glow = sun_r+4
fg_lines.append(f'    <path android:fillColor="#3DFFD873" android:pathData="M{sun_cx:.2f},{sun_cy:.2f} m-{r_glow:.2f},0 a{r_glow:.2f},{r_glow:.2f} 0 1,0 {2*r_glow:.2f},0 a{r_glow:.2f},{r_glow:.2f} 0 1,0 -{2*r_glow:.2f},0" />')
fg_lines.append(f'    <path android:fillColor="#FFD873" android:pathData="M{sun_cx:.2f},{sun_cy:.2f} m-{sun_r:.2f},0 a{sun_r:.2f},{sun_r:.2f} 0 1,0 {2*sun_r:.2f},0 a{sun_r:.2f},{sun_r:.2f} 0 1,0 -{2*sun_r:.2f},0" />')
r_h = sun_r*0.35
fg_lines.append(f'    <path android:fillColor="#6DFFFFFF" android:pathData="M{sun_cx-6*s:.2f},{sun_cy-6*s:.2f} m-{r_h:.2f},0 a{r_h:.2f},{r_h:.2f} 0 1,0 {2*r_h:.2f},0 a{r_h:.2f},{r_h:.2f} 0 1,0 -{2*r_h:.2f},0" />')

import math as m
for i in range(12):
    angle = m.radians(i*30 -15)
    inner = sun_r + 0.8
    outer = sun_r + 4.0
    x1 = sun_cx + m.cos(angle)*inner
    y1 = sun_cy + m.sin(angle)*inner
    x2 = sun_cx + m.cos(angle)*outer
    y2 = sun_cy + m.sin(angle)*outer
    fg_lines.append(f'    <path android:strokeColor="#FFD873" android:strokeWidth="1.8" android:strokeLineCap="round" android:pathData="M{x1:.2f},{y1:.2f} L{x2:.2f},{y2:.2f}" />')

cloud_y = 520*s
cloud_cx = 512*s
configs = [(-110,10,92), (-30,0,108), (70,12,92), (125,30,64)]
for dx, dy, r in configs:
    cx = cloud_cx + dx*s
    cy = cloud_y + dy*s
    rr = r*s
    fg_lines.append(f'    <path android:fillColor="#1A0B1E3D" android:pathData="M{cx:.2f},{cy+0.8:.2f} m-{rr:.2f},0 a{rr:.2f},{rr:.2f} 0 1,0 {2*rr:.2f},0 a{rr:.2f},{rr:.2f} 0 1,0 -{2*rr:.2f},0" />')
for dx, dy, r in configs:
    cx = cloud_cx + dx*s
    cy = cloud_y + dy*s
    rr = r*s
    fg_lines.append(f'    <path android:fillColor="#FFFFFF" android:pathData="M{cx:.2f},{cy:.2f} m-{rr:.2f},0 a{rr:.2f},{rr:.2f} 0 1,0 {2*rr:.2f},0 a{rr:.2f},{rr:.2f} 0 1,0 -{2*rr:.2f},0" />')
x1 = cloud_cx -195*s
x2 = cloud_cx +175*s
y1 = cloud_y +20*s
y2 = cloud_y +85*s
fg_lines.append(f'    <path android:fillColor="#FFFFFF" android:pathData="M{x1:.2f},{y1:.2f} L{x2:.2f},{y1:.2f} L{x2:.2f},{y2:.2f} L{x1:.2f},{y2:.2f} Z" />')

mic_cx, mic_cy = 512*s, 685*s
mw = 24*s
mh = 48*s
x1 = mic_cx - mw
x2 = mic_cx + mw
y1 = mic_cy - mh
y2 = mic_cy + 12*s
r = 2.6*s if 2.6*s>0.8 else 1.2
fg_lines.append(f'    <path android:fillColor="#FFFFFF" android:pathData="M{x1+r:.2f},{y1:.2f} L{x2-r:.2f},{y1:.2f} A{r:.2f},{r:.2f} 0 0,1 {x2:.2f},{y1+r:.2f} L{x2:.2f},{y2-r:.2f} A{r:.2f},{r:.2f} 0 0,1 {x2-r:.2f},{y2:.2f} L{x1+r:.2f},{y2:.2f} A{r:.2f},{r:.2f} 0 0,1 {x1:.2f},{y2-r:.2f} L{x1:.2f},{y1+r:.2f} A{r:.2f},{r:.2f} 0 0,1 {x1+r:.2f},{y1:.2f} Z" />')
fg_lines.append(f'    <path android:strokeColor="#FFFFFF" android:strokeWidth="0.9" android:strokeLineCap="round" android:pathData="M{mic_cx:.2f},{y2:.2f} L{mic_cx:.2f},{mic_cy+32*s:.2f}" />')
fg_lines.append(f'    <path android:strokeColor="#FFFFFF" android:strokeWidth="0.9" android:strokeLineCap="round" android:pathData="M{mic_cx-22*s:.2f},{mic_cy+32*s:.2f} L{mic_cx+22*s:.2f},{mic_cy+32*s:.2f}" />')
for side in [-1,1]:
    for i, (h, alpha) in enumerate([(28, 0.86),(46, 0.63),(62, 0.35)]):
        hh = h*s
        x = mic_cx + side*(46 + i*18)*s
        y0 = mic_cy - hh/2
        y1w = mic_cy + hh/2
        w = 1.1*s if 1.1*s>0.6 else 0.9
        # rounded bar using two arcs
        fg_lines.append(f'    <path android:fillColor="#FFFFFF" android:alpha="{alpha:.2f}" android:pathData="M{x-w:.2f},{y0+w:.2f} A{w:.2f},{w:.2f} 0 0,1 {x:.2f},{y0:.2f} L{x:.2f},{y0:.2f} A{w:.2f},{w:.2f} 0 0,1 {x+w:.2f},{y0+w:.2f} L{x+w:.2f},{y1w-w:.2f} A{w:.2f},{w:.2f} 0 0,1 {x:.2f},{y1w:.2f} L{x:.2f},{y1w:.2f} A{w:.2f},{w:.2f} 0 0,1 {x-w:.2f},{y1w-w:.2f} Z" />')

fg_lines.append('</vector>')
fg_xml = "\n".join(fg_lines)
Path(r'H:\weather-app\app\src\main\res\drawable\ic_launcher_foreground.xml').write_text(fg_xml, encoding='utf-8')
print('wrote foreground vector', len(fg_xml))

# Create adaptive icon xmls
adaptive_xml = '''<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
</adaptive-icon>'''
import os
for name in ['ic_launcher','ic_launcher_round']:
    out_dir = r'H:\weather-app\app\src\main\res\mipmap-anydpi-v26'
    os.makedirs(out_dir, exist_ok=True)
    Path(os.path.join(out_dir, f'{name}.xml')).write_text(adaptive_xml, encoding='utf-8')
    print(f'wrote {name}.xml')

# Verify
for p in [r'H:\weather-app\app\src\main\res\drawable\ic_launcher_background.xml', r'H:\weather-app\app\src\main\res\drawable\ic_launcher_foreground.xml', r'H:\weather-app\app\src\main\res\mipmap-anydpi-v26\ic_launcher.xml']:
    print(Path(p).read_text()[:400])
