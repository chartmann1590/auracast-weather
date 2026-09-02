from PIL import Image, ImageDraw, ImageFont, ImageFilter
import os, math

# Common colors
COLORS = {
    "bg": (248,250,255),
    "surface": (255,255,255),
    "primary": (79,168,255),
    "primary_dark": (11,30,61),
    "gold": (255,216,115),
    "text_primary": (27,39,51),
    "text_secondary": (103,116,137),
    "text_on_primary": (255,255,255),
    "nav_selected": (79,168,255),
    "card_shadow": (0,0,0,25),
}

def load_font(size, bold=False):
    # try arial
    paths = [
        r"C:\Windows\Fonts\arialbd.ttf" if bold else r"C:\Windows\Fonts\Arial.ttf",
        r"C:\Windows\Fonts\segoeuib.ttf" if bold else r"C:\Windows\Fonts\segoeui.ttf",
    ]
    for p in paths:
        if os.path.exists(p):
            try:
                return ImageFont.truetype(p, size)
            except: pass
    return ImageFont.load_default()

def rounded_rect(draw, box, radius, fill, outline=None, width=1):
    try:
        draw.rounded_rectangle(box, radius=radius, fill=fill, outline=outline, width=width)
    except:
        draw.rectangle(box, fill=fill, outline=outline)

def draw_gradient(draw, box, top_color, bottom_color):
    x0,y0,x1,y1 = box
    h = y1-y0
    for y in range(y0, y1):
        t = (y-y0)/max(1,h-1)
        r = int(top_color[0]*(1-t)+bottom_color[0]*t)
        g = int(top_color[1]*(1-t)+bottom_color[1]*t)
        b = int(top_color[2]*(1-t)+bottom_color[2]*t)
        draw.line([(x0,y),(x1,y)], fill=(r,g,b))

def draw_status_bar(img, w):
    draw = ImageDraw.Draw(img)
    # status bar height ~ 60 for 1080 width
    h = int(w * 0.055)  # ~60
    draw.rectangle([0,0,w,h], fill=(248,250,255))
    # time 9:41
    font = load_font(int(w*0.038), bold=False)
    draw.text((int(w*0.06), int(h*0.18)), "9:41", font=font, fill=COLORS["text_primary"])
    # right icons: signal wifi battery
    # simple rectangles
    draw.rectangle([w-int(w*0.18), int(h*0.35), w-int(w*0.15), int(h*0.65)], fill=COLORS["text_primary"])
    draw.rectangle([w-int(w*0.13), int(h*0.30), w-int(w*0.05), int(h*0.70)], outline=COLORS["text_primary"], width=2)
    draw.rectangle([w-int(w*0.045), int(h*0.40), w-int(w*0.035), int(h*0.60)], fill=COLORS["text_primary"])
    return h

def draw_bottom_nav(draw, w, h, selected=0):
    # nav height ~ 110
    nav_h = int(w*0.10)
    nav_y0 = h - nav_h
    draw.rectangle([0, nav_y0, w, h], fill=(255,255,255))
    draw.line([(0,nav_y0),(w,nav_y0)], fill=(230,235,245), width=2)
    labels = ["Home","Radar","Report","Settings"]
    icons = ["\u2302","\u25CE","\u266B","\u2699"]  # placeholder glyphs will be drawn as shapes, not text
    n = 4
    item_w = w//n
    for i, label in enumerate(labels):
        cx = item_w*i + item_w//2
        # icon circle
        sel = (i==selected)
        icon_color = COLORS["nav_selected"] if sel else COLORS["text_secondary"]
        # draw icon as simple shape: home = house, radar=target, report=play, settings=gear
        # Instead of unicode, draw custom shapes
        iy = nav_y0 + int(nav_h*0.18)
        if i==0: # home
            # house shape
            pts = [(cx-14, iy+8),(cx, iy-8),(cx+14, iy+8),(cx+10, iy+8),(cx+10, iy+14),(cx-10, iy+14),(cx-10, iy+8)]
            draw.polygon(pts, fill=icon_color)
        elif i==1: # radar
            draw.ellipse([cx-12, iy-6, cx+12, iy+14], outline=icon_color, width=2)
            draw.ellipse([cx-6, iy-0, cx+6, iy+8], outline=icon_color, width=2)
            draw.ellipse([cx-2, iy+2, cx+2, iy+6], fill=icon_color)
        elif i==2: # report (mic/podcast)
            draw.rounded_rectangle([cx-8, iy-6, cx+8, iy+6], radius=4, fill=icon_color)
            draw.line([(cx, iy+6),(cx, iy+14)], fill=icon_color, width=2)
            draw.line([(cx-8, iy+14),(cx+8, iy+14)], fill=icon_color, width=2)
        else: # settings
            draw.ellipse([cx-10, iy-2, cx+10, iy+12], outline=icon_color, width=2)
            for a in [0,90,180,270]:
                ang = math.radians(a)
                x1 = cx + math.cos(ang)*12
                y1 = (iy+5) + math.sin(ang)*12
                x2 = cx + math.cos(ang)*6
                y2 = (iy+5) + math.sin(ang)*6
                draw.line([(x2,y2),(x1,y1)], fill=icon_color, width=2)
        # label
        font = load_font(int(w*0.028), bold=sel)
        # measure
        try:
            l,t,r,b = font.getbbox(label)
            tw, th = r-l, b-t
        except:
            tw, th = font.getsize(label)
        draw.text((cx - tw//2, nav_y0 + int(nav_h*0.60)), label, font=font, fill=icon_color)
        # indicator for selected
        if sel:
            draw.rounded_rectangle([cx-18, nav_y0+int(nav_h*0.82), cx+18, nav_y0+int(nav_h*0.86)], radius=2, fill=COLORS["nav_selected"])
    return nav_y0

# ------------------ HOME SCREEN GENERATOR ------------------
def gen_home(w, h, filename, variant="clear_day"):
    img = Image.new("RGBA", (w,h), COLORS["bg"])
    draw = ImageDraw.Draw(img)
    status_h = draw_status_bar(img, w)
    nav_y0 = h - int(w*0.10)  # placeholder for later
    # App top bar: location + search
    top_h = int(w*0.14)
    top_y0 = status_h + 6
    draw.rectangle([0, top_y0, w, top_y0+top_h], fill=COLORS["bg"])
    # location pin + text
    font_loc = load_font(int(w*0.042), bold=True)
    font_sub = load_font(int(w*0.032), bold=False)
    # location
    draw.text((int(w*0.06), top_y0+int(w*0.02)), "San Francisco, CA", font=font_loc, fill=COLORS["text_primary"])
    # pin icon
    # small dot
    draw.ellipse([int(w*0.03), top_y0+int(w*0.04), int(w*0.03)+8, top_y0+int(w*0.04)+8], fill=COLORS["primary"])
    draw.text((int(w*0.06), top_y0+int(w*0.07)), "Updated just now  •  72°  Partly Cloudy", font=font_sub, fill=COLORS["text_secondary"])
    # search icon top right
    sx = w - int(w*0.10)
    sy = top_y0 + int(w*0.04)
    draw.ellipse([sx, sy, sx+int(w*0.07), sy+int(w*0.07)], outline=COLORS["text_secondary"], width=2)
    draw.line([(sx+int(w*0.055), sy+int(w*0.055)), (sx+int(w*0.07), sy+int(w*0.07))], fill=COLORS["text_secondary"], width=2)

    # Hero card
    hero_x0, hero_y0 = int(w*0.04), top_y0+top_h+int(w*0.02)
    hero_x1, hero_y1 = int(w*0.96), hero_y0+int(w*0.78)  # aspect
    # shadow
    shadow = Image.new("RGBA", (w,h), (0,0,0,0))
    sd = ImageDraw.Draw(shadow)
    rounded_rect(sd, [hero_x0+4, hero_y0+6, hero_x1+4, hero_y1+6], radius=int(w*0.06), fill=(0,0,0,20))
    img = Image.alpha_composite(img, shadow)
    draw = ImageDraw.Draw(img)
    # gradient clipped
    # create hero image separate then clip
    hero_w, hero_h = hero_x1-hero_x0, hero_y1-hero_y0
    hero = Image.new("RGBA", (hero_w, hero_h), (0,0,0,0))
    hd = ImageDraw.Draw(hero)
    for y in range(hero_h):
        t = y/(hero_h-1)
        # clear day gradient top #4FA8FF to #FFD873 warm
        # interpolate sky blue to gold warm
        r = int(79*(1-t) + 255*t*0.9 + 79*t*0.1)  # blend to gold-ish
        g = int(168*(1-t) + 216*t)
        b = int(255*(1-t) + 115*t)
        # actually for clear day: #4FA8FF (79,168,255) to #FFD873 (255,216,115) but vertical, top blue bottom gold
        r = int(79 + (255-79)*t)
        g = int(168 + (216-168)*t)
        b = int(255 + (115-255)*t)
        hd.line([(0,y),(hero_w,y)], fill=(r,g,b,255))
    # round mask
    mask = Image.new("L", (hero_w, hero_h), 0)
    md = ImageDraw.Draw(mask)
    try:
        md.rounded_rectangle([0,0,hero_w,hero_h], radius=int(w*0.06), fill=255)
    except:
        md.rectangle([0,0,hero_w,hero_h], fill=255)
    # Add hero content on top of hero gradient
    # Big Meteocons sun/cloud icon left, temp right
    # We'll draw sun + cloud onto hero
    # Use same drawing as icon but scaled down
    # Sun
    hero_draw = ImageDraw.Draw(hero)
    # position: sun center at 28% width, 38% height
    scx, scy = int(hero_w*0.30), int(hero_h*0.42)
    sr = int(w*0.12)
    # sun glow
    hero_draw.ellipse([scx-sr-6, scy-sr-6, scx+sr+6, scy+sr+6], fill=(255,255,255,50))
    hero_draw.ellipse([scx-sr, scy-sr, scx+sr, scy+sr], fill=(255,255,255,240))
    # cloud overlapping
    cloud_y = scy + int(w*0.05)
    for (dx, dy, r) in [(-18,2,22),(8,0,28),(30,6,20)]:
        hero_draw.ellipse([scx+dx - r, cloud_y+dy - r, scx+dx + r, cloud_y+dy + r], fill=(255,255,255,245))
    hero_draw.rectangle([scx-28, cloud_y, scx+42, cloud_y+16], fill=(255,255,255,245))
    # Hero text: temp large
    font_temp = load_font(int(w*0.18), bold=True)
    font_cond = load_font(int(w*0.045), bold=False)
    font_hilo = load_font(int(w*0.032), bold=False)
    # temp 72°
    tx = int(hero_w*0.58)
    ty = int(hero_h*0.18)
    hero_draw.text((tx, ty), "72°", font=font_temp, fill=(27,39,51))
    hero_draw.text((tx, ty+int(w*0.19)), "Partly Cloudy", font=font_cond, fill=(27,39,51,230))
    hero_draw.text((tx, ty+int(w*0.26)), "H 78°  L 62°  •  Feels like 74°", font=font_hilo, fill=(27,39,51,200))
    # bottom row: wind, humidity, UV small pills
    pill_y = hero_h - int(w*0.12)
    pill_font = load_font(int(w*0.026), bold=False)
    pills = ["Wind  8 mph", "Humidity  64%", "UV  5 Moderate"]
    px = int(w*0.04)
    for pill in pills:
        # measure
        try:
            l,t,r,b = pill_font.getbbox(pill)
            tw, th = r-l, b-t
        except:
            tw, th = pill_font.getsize(pill)
        pw, ph = tw+16, th+10
        # pill bg translucent white
        hero_draw.rounded_rectangle([px, pill_y-4, px+pw, pill_y+ph-4], radius=ph//2, fill=(255,255,255,180))
        hero_draw.text((px+8, pill_y), pill, font=pill_font, fill=(27,39,51))
        px += pw + 10
    # clip hero to rounded
    hero_masked = Image.new("RGBA", (hero_w, hero_h), (0,0,0,0))
    hero_masked.paste(hero, (0,0), mask)
    img.paste(hero_masked, (hero_x0, hero_y0), hero_masked)
    draw = ImageDraw.Draw(img)

    # AI Report teaser card under hero
    teaser_y0 = hero_y1 + int(w*0.03)
    teaser_y1 = teaser_y0 + int(w*0.20)
    teaser_x0, teaser_x1 = int(w*0.04), int(w*0.96)
    # shadow
    shadow2 = Image.new("RGBA", (w,h), (0,0,0,0))
    sd2 = ImageDraw.Draw(shadow2)
    rounded_rect(sd2, [teaser_x0+3, teaser_y0+4, teaser_x1+3, teaser_y1+4], radius=int(w*0.04), fill=(0,0,0,12))
    img = Image.alpha_composite(img, shadow2)
    draw = ImageDraw.Draw(img)
    rounded_rect(draw, [teaser_x0, teaser_y0, teaser_x1, teaser_y1], radius=int(w*0.04), fill=(255,255,255))
    # left icon: small mic/play
    draw.ellipse([teaser_x0+int(w*0.04), teaser_y0+int(w*0.05), teaser_x0+int(w*0.14), teaser_y0+int(w*0.15)], fill=(79,168,255))
    # play triangle
    cx, cy = teaser_x0+int(w*0.09), teaser_y0+int(w*0.10)
    draw.polygon([(cx-6, cy-8),(cx-6, cy+8),(cx+8, cy)], fill=(255,255,255))
    # text
    font_teaser_title = load_font(int(w*0.035), bold=True)
    font_teaser_body = load_font(int(w*0.028), bold=False)
    draw.text((teaser_x0+int(w*0.18), teaser_y0+int(w*0.04)), "Your Daily Briefing — ready to play", font=font_teaser_title, fill=COLORS["text_primary"])
    draw.text((teaser_x0+int(w*0.18), teaser_y0+int(w*0.11)), "Sunny morning, late storm chance — tap to listen", font=font_teaser_body, fill=COLORS["text_secondary"])

    # Hourly strip
    hour_y0 = teaser_y1 + int(w*0.03)
    font_section = load_font(int(w*0.032), bold=True)
    draw.text((int(w*0.04), hour_y0), "Hourly  •  48 hours", font=font_section, fill=COLORS["text_primary"])
    # row of 6 hour cards
    hour_cards_x0 = int(w*0.04)
    hour_cards_y0 = hour_y0 + int(w*0.06)
    card_w, card_h = int(w*0.18), int(w*0.28)
    hours = [("Now","72°", True), ("1 PM","74°", False), ("2 PM","75°", False), ("3 PM","74°", False), ("4 PM","70°", False), ("5 PM","68°", False)]
    for i, (time,temp, sel) in enumerate(hours):
        x0 = hour_cards_x0 + i*(card_w+int(w*0.02))
        x1 = x0+card_w
        y0 = hour_cards_y0
        y1 = y0+card_h
        if sel:
            rounded_rect(draw, [x0, y0, x1, y1], radius=int(w*0.035), fill=(79,168,255))
            # white text
            font_h_time = load_font(int(w*0.028), bold=True)
            font_h_temp = load_font(int(w*0.038), bold=True)
            draw.text((x0+int(w*0.04), y0+int(w*0.04)), time, font=font_h_time, fill=(255,255,255))
            # small icon placeholder (cloud)
            draw.ellipse([x0+int(w*0.05), y0+int(w*0.11), x0+int(w*0.11), y0+int(w*0.16)], fill=(255,255,255,240))
            draw.text((x0+int(w*0.05), y0+int(w*0.19)), temp, font=font_h_temp, fill=(255,255,255))
            # precip bar
            draw.rectangle([x0+int(w*0.04), y1-int(w*0.04), x0+int(w*0.08), y1-int(w*0.03)], fill=(255,255,255,120))
        else:
            rounded_rect(draw, [x0, y0, x1, y1], radius=int(w*0.035), fill=(255,255,255), outline=(230,235,245), width=1)
            font_h_time = load_font(int(w*0.028), bold=False)
            font_h_temp = load_font(int(w*0.032), bold=True)
            draw.text((x0+int(w*0.04), y0+int(w*0.04)), time, font=font_h_time, fill=COLORS["text_secondary"])
            # icon: sun
            draw.ellipse([x0+int(w*0.06), y0+int(w*0.11), x0+int(w*0.11), y0+int(w*0.16)], fill=(255,216,115))
            draw.text((x0+int(w*0.05), y0+int(w*0.19)), temp, font=font_h_temp, fill=COLORS["text_primary"])
            # precip
            draw.text((x0+int(w*0.05), y0+int(w*0.23)), "0%", font=load_font(int(w*0.022), bold=False), fill=COLORS["text_secondary"])

    # 5-day list
    list_y0 = hour_cards_y0 + card_h + int(w*0.05)
    draw.text((int(w*0.04), list_y0), "5-day forecast", font=font_section, fill=COLORS["text_primary"])
    rows_y0 = list_y0 + int(w*0.07)
    row_h = int(w*0.13)
    days = [("Today", "78°", "62°", 0.9), ("Tue", "76°","61°",0.7), ("Wed", "70°","58°",0.5), ("Thu", "68°","57°",0.4), ("Fri", "71°","59°",0.6)]
    for idx, (day, high, low, fill_ratio) in enumerate(days):
        y0 = rows_y0 + idx*(row_h+int(w*0.015))
        y1 = y0+row_h
        rounded_rect(draw, [int(w*0.04), y0, int(w*0.96), y1], radius=int(w*0.035), fill=(255,255,255), outline=(230,235,245), width=1)
        # day
        font_day = load_font(int(w*0.032), bold=True)
        draw.text((int(w*0.07), y0+int(w*0.04)), day, font=font_day, fill=COLORS["text_primary"])
        # icon
        icon_x = int(w*0.24)
        draw.ellipse([icon_x, y0+int(w*0.03), icon_x+int(w*0.06), y0+int(w*0.09)], fill=(255,216,115) if idx<2 else (180,195,210))
        if idx==3:
            # rain
            draw.line([(icon_x+8, y0+int(w*0.10)), (icon_x+6, y0+int(w*0.12))], fill=(79,168,255), width=2)
        # bar
        bar_x0, bar_x1 = int(w*0.36), int(w*0.78)
        bar_y = y0+row_h//2
        draw.rounded_rectangle([bar_x0, bar_y-3, bar_x1, bar_y+3], radius=3, fill=(230,235,245))
        # filled portion
        fill_w = int((bar_x1-bar_x0)*fill_ratio)
        # gradient for bar?
        draw.rounded_rectangle([bar_x0, bar_y-3, bar_x0+fill_w, bar_y+3], radius=3, fill=(79,168,255))
        # temps
        font_temp_small = load_font(int(w*0.03), bold=False)
        draw.text((bar_x0- int(w*0.07), bar_y-8), low, font=font_temp_small, fill=COLORS["text_secondary"])
        draw.text((bar_x1+ int(w*0.02), bar_y-8), high, font=font_temp_small, fill=COLORS["text_primary"])

    # bottom nav
    draw_bottom_nav(draw, w, h, selected=0)
    # save
    img.convert("RGB").save(filename, "PNG", optimize=True)
    print(f"saved home {filename} {w}x{h} {os.path.getsize(filename)}")

def gen_radar(w, h, filename):
    img = Image.new("RGBA", (w,h), COLORS["bg"])
    draw = ImageDraw.Draw(img)
    status_h = draw_status_bar(img, w)
    # top bar
    top_h = int(w*0.12)
    top_y0 = status_h + 4
    draw.rectangle([0, top_y0, w, top_y0+top_h], fill=COLORS["bg"])
    font_title = load_font(int(w*0.042), bold=True)
    font_sub = load_font(int(w*0.028), bold=False)
    draw.text((int(w*0.06), top_y0+int(w*0.02)), "Radar", font=font_title, fill=COLORS["text_primary"])
    draw.text((int(w*0.06), top_y0+int(w*0.07)), "San Francisco Bay Area  •  Precipitation", font=font_sub, fill=COLORS["text_secondary"])
    # map area
    map_x0, map_y0 = int(w*0.04), top_y0+top_h+int(w*0.02)
    map_x1, map_y1 = int(w*0.96), h - int(w*0.32)  # leave space for controls + nav
    # shadow
    sh = Image.new("RGBA", (w,h), (0,0,0,0))
    sdd = ImageDraw.Draw(sh)
    rounded_rect(sdd, [map_x0+4, map_y0+4, map_x1+4, map_y1+4], radius=int(w*0.05), fill=(0,0,0,18))
    img = Image.alpha_composite(img, sh)
    draw = ImageDraw.Draw(img)
    # map bg: light gray with road lines
    map_w, map_h = map_x1-map_x0, map_y1-map_y0
    map_img = Image.new("RGBA", (map_w, map_h), (0,0,0,0))
    md = ImageDraw.Draw(map_img)
    # base map: #E6EEF7 with subtle grid
    md.rounded_rectangle([0,0,map_w,map_h], radius=int(w*0.05), fill=(230,238,247))
    # roads: light lines
    for y in [map_h*0.3, map_h*0.55, map_h*0.78]:
        md.line([(0,y),(map_w,y)], fill=(210,220,235), width=2)
    for x in [map_w*0.25, map_w*0.55, map_w*0.80]:
        md.line([(x,0),(x,map_h)], fill=(210,220,235), width=2)
    # water area bottom-right
    md.ellipse([map_w*0.45, map_h*0.6, map_w*1.1, map_h*1.2], fill=(200,222,245,180))
    # radar blobs: precipitation
    # create blobs with colors: light green to yellow to red
    # blob 1 (north)
    blob = Image.new("RGBA", (map_w, map_h), (0,0,0,0))
    bd = ImageDraw.Draw(blob)
    for (cx,cy,rx,ry,col,alpha) in [
        (map_w*0.35, map_h*0.32, map_w*0.18, map_h*0.09, (80,180,120), 160),
        (map_w*0.55, map_h*0.28, map_w*0.22, map_h*0.11, (120,200,90), 170),
        (map_w*0.72, map_h*0.38, map_w*0.14, map_h*0.07, (255,220,80), 165),
        (map_w*0.30, map_h*0.52, map_w*0.12, map_h*0.08, (255,140,60), 170),
        (map_w*0.48, map_h*0.60, map_w*0.16, map_h*0.09, (220,80,80), 175),
        (map_w*0.65, map_h*0.68, map_w*0.10, map_h*0.06, (255,190,50), 150),
    ]:
        bd.ellipse([cx-rx, cy-ry, cx+rx, cy+ry], fill=col+(alpha,))
    # blur blob
    blob = blob.filter(ImageFilter.GaussianBlur(radius=int(w*0.015)))
    map_img = Image.alpha_composite(map_img, blob)
    md = ImageDraw.Draw(map_img)
    # location dot pulsing
    dot_cx, dot_cy = int(map_w*0.48), int(map_h*0.50)
    for r, a in [(22,30),(16,50),(9,90)]:
        md.ellipse([dot_cx-r, dot_cy-r, dot_cx+r, dot_cy+r], fill=(79,168,255,a))
    md.ellipse([dot_cx-5, dot_cy-5, dot_cx+5, dot_cy+5], fill=(255,255,255), outline=(79,168,255), width=2)
    # label for city
    font_city = load_font(int(w*0.026), bold=True)
    md.rounded_rectangle([dot_cx-42, dot_cy-38, dot_cx+42, dot_cy-16], radius=8, fill=(27,39,51,230))
    md.text((dot_cx-28, dot_cy-35), "You", font=font_city, fill=(255,255,255))
    # attribution bottom-left
    font_attr = load_font(int(w*0.018), bold=False)
    md.text((12, map_h-18), "© OpenStreetMap  •  RainViewer / NWS", font=font_attr, fill=(103,116,137,180))
    # zoom buttons top-right
    for i, sym in enumerate(["+", "-"]):
        bx0 = map_w- int(w*0.11) - 6
        by0 = 14 + i*(int(w*0.09)+8)
        bx1 = map_w-14
        by1 = by0+int(w*0.09)
        md.rounded_rectangle([bx0,by0,bx1,by1], radius=10, fill=(255,255,255,240), outline=(230,235,245), width=1)
        font_z = load_font(int(w*0.04), bold=True)
        # center
        try:
            l,t,r,b = font_z.getbbox(sym)
            tw, th = r-l,b-t
        except:
            tw,th = font_z.getsize(sym)
        md.text((bx0+(bx1-bx0)//2 - tw//2, by0+(by1-by0)//2 - th//2 -2), sym, font=font_z, fill=COLORS["text_primary"])
    # layers button
    lx0 = 14
    ly0 = 14
    lx1 = lx0+int(w*0.09)
    ly1 = ly0+int(w*0.09)
    md.rounded_rectangle([lx0,ly0,lx1,ly1], radius=10, fill=(255,255,255,240), outline=(230,235,245), width=1)
    # layers icon: stacked squares
    md.rectangle([lx0+12, ly0+12, lx0+22, ly0+22], outline=COLORS["text_primary"], width=1)
    md.rectangle([lx0+16, ly0+16, lx0+26, ly0+26], fill=(255,255,255), outline=COLORS["text_primary"], width=1)
    # paste map
    mask = Image.new("L", (map_w, map_h), 0)
    mdd = ImageDraw.Draw(mask)
    try:
        mdd.rounded_rectangle([0,0,map_w,map_h], radius=int(w*0.05), fill=255)
    except:
        mdd.rectangle([0,0,map_w,map_h], fill=255)
    map_masked = Image.new("RGBA", (map_w, map_h), (0,0,0,0))
    map_masked.paste(map_img, (0,0), mask)
    img.paste(map_masked, (map_x0, map_y0), map_masked)
    draw = ImageDraw.Draw(img)

    # playback controls sheet just below map but above nav
    ctrl_y0 = map_y1 + int(w*0.02)
    ctrl_y1 = h - int(w*0.10) - int(w*0.02)
    ctrl_x0, ctrl_x1 = int(w*0.04), int(w*0.96)
    # card
    sh2 = Image.new("RGBA", (w,h), (0,0,0,0))
    sd2 = ImageDraw.Draw(sh2)
    rounded_rect(sd2, [ctrl_x0+3, ctrl_y0+4, ctrl_x1+3, ctrl_y1+4], radius=int(w*0.04), fill=(0,0,0,14))
    img = Image.alpha_composite(img, sh2)
    draw = ImageDraw.Draw(img)
    rounded_rect(draw, [ctrl_x0, ctrl_y0, ctrl_x1, ctrl_y1], radius=int(w*0.04), fill=(255,255,255))
    # play button
    btn_x0 = ctrl_x0+int(w*0.04)
    btn_y0 = ctrl_y0+int(w*0.04)
    btn_s = int(w*0.11)
    draw.ellipse([btn_x0, btn_y0, btn_x0+btn_s, btn_y0+btn_s], fill=(79,168,255))
    # play triangle
    cx = btn_x0+btn_s//2 +2
    cy = btn_y0+btn_s//2
    draw.polygon([(cx-8, cy-10),(cx-8, cy+10),(cx+10, cy)], fill=(255,255,255))
    # scrub bar
    scrub_x0 = btn_x0+btn_s+int(w*0.04)
    scrub_x1 = ctrl_x1-int(w*0.04)
    scrub_y = btn_y0+btn_s//2
    draw.rounded_rectangle([scrub_x0, scrub_y-3, scrub_x1, scrub_y+3], radius=3, fill=(230,235,245))
    # filled portion + handle
    fill_x1 = scrub_x0 + int((scrub_x1-scrub_x0)*0.42)
    draw.rounded_rectangle([scrub_x0, scrub_y-3, fill_x1, scrub_y+3], radius=3, fill=(79,168,255))
    draw.ellipse([fill_x1-8, scrub_y-8, fill_x1+8, scrub_y+8], fill=(79,168,255), outline=(255,255,255), width=2)
    # time labels
    font_t = load_font(int(w*0.024), bold=False)
    draw.text((scrub_x0, scrub_y+12), "11:20 AM", font=font_t, fill=COLORS["text_secondary"])
    draw.text((scrub_x1-78, scrub_y+12), "12:00 PM", font=font_t, fill=COLORS["text_secondary"])
    # speed / timeline text at bottom of card?
    font_ctrl_sub = load_font(int(w*0.022), bold=False)
    draw.text((btn_x0, ctrl_y1- int(w*0.035)), "Live  •  40 min loop  •  Source: RainViewer", font=font_ctrl_sub, fill=COLORS["text_secondary"])

    draw_bottom_nav(draw, w, h, selected=1)
    img.convert("RGB").save(filename, "PNG", optimize=True)
    print(f"saved radar {filename} {w}x{h} {os.path.getsize(filename)}")

def gen_report(w, h, filename):
    img = Image.new("RGBA", (w,h), COLORS["bg"])
    draw = ImageDraw.Draw(img)
    status_h = draw_status_bar(img, w)
    top_h = int(w*0.12)
    top_y0 = status_h+4
    draw.rectangle([0, top_y0, w, top_y0+top_h], fill=COLORS["bg"])
    font_title = load_font(int(w*0.042), bold=True)
    font_sub = load_font(int(w*0.028), bold=False)
    draw.text((int(w*0.06), top_y0+int(w*0.02)), "Daily Report", font=font_title, fill=COLORS["text_primary"])
    draw.text((int(w*0.06), top_y0+int(w*0.07)), "AI-narrated  •  On-device  •  Gemma 4", font=font_sub, fill=COLORS["text_secondary"])
    # voice picker top-right
    vx0 = w - int(w*0.28)
    vy0 = top_y0+int(w*0.03)
    vx1 = w - int(w*0.04)
    vy1 = vy0+int(w*0.08)
    draw.rounded_rectangle([vx0,vy0,vx1,vy1], radius=int(w*0.04), fill=(255,255,255), outline=(230,235,245), width=1)
    font_voice = load_font(int(w*0.026), bold=False)
    draw.text((vx0+int(w*0.03), vy0+int(w*0.022)), "Voice: Ava", font=font_voice, fill=COLORS["text_primary"])
    # album art card
    art_x0, art_y0 = int(w*0.08), top_y0+top_h+int(w*0.04)
    art_x1, art_y1 = int(w*0.92), art_y0+int(w*0.78)
    sh = Image.new("RGBA", (w,h), (0,0,0,0))
    sdd = ImageDraw.Draw(sh)
    rounded_rect(sdd, [art_x0+4, art_y0+6, art_x1+4, art_y1+6], radius=int(w*0.06), fill=(0,0,0,18))
    img = Image.alpha_composite(img, sh)
    draw = ImageDraw.Draw(img)
    # gradient for art: thunderstorm vibe #3A3A5E to #6E4B8A but for this report it's sunny so use gold/blue
    art_w, art_h = art_x1-art_x0, art_y1-art_y0
    art = Image.new("RGBA", (art_w, art_h), (0,0,0,0))
    ad = ImageDraw.Draw(art)
    for y in range(art_h):
        t = y/(art_h-1)
        r = int(79 + (110-79)*t)  # blue to violet?
        g = int(168 + (75-168)*t)
        b = int(255 + (138-255)*t)
        # use #4FA8FF to #FFD873 again but slightly different
        r = int(79 + (255-79)*t*0.6)
        g = int(168 + (216-168)*t*0.6)
        b = int(255 + (115-255)*t*0.6)
        # actually just do blue to gold
        r = int(79 + (255-79)*t)
        g = int(168 + (216-168)*t)
        b = int(255 + (115-255)*t)
        ad.line([(0,y),(art_w,y)], fill=(r,g,b,255))
    # mask
    mask = Image.new("L", (art_w, art_h), 0)
    mdd = ImageDraw.Draw(mask)
    try:
        mdd.rounded_rectangle([0,0,art_w,art_h], radius=int(w*0.06), fill=255)
    except:
        mdd.rectangle([0,0,art_w,art_h], fill=255)
    # draw large sun/cloud same as home but centered
    ad = ImageDraw.Draw(art)
    scx, scy = art_w//2, int(art_h*0.42)
    sr = int(w*0.14)
    ad.ellipse([scx-sr-10, scy-sr-10, scx+sr+10, scy+sr+10], fill=(255,255,255,35))
    ad.ellipse([scx-sr, scy-sr, scx+sr, scy+sr], fill=(255,255,255,245))
    # cloud
    cloud_y = scy+int(w*0.06)
    for (dx, dy, rr) in [(-22,4,26),(10,0,32),(34,8,22)]:
        ad.ellipse([scx+dx - rr, cloud_y+dy - rr, scx+dx + rr, cloud_y+dy + rr], fill=(255,255,255,250))
    ad.rectangle([scx-32, cloud_y, scx+46, cloud_y+18], fill=(255,255,255,250))
    # small waveform at bottom of art
    wave_y = int(art_h*0.78)
    bars = [12, 28, 18, 36, 22, 30, 14, 26, 20, 34, 16, 24, 30, 18, 26]
    bar_w = int(w*0.015)
    gap = int(w*0.018)
    total_w = len(bars)*bar_w + (len(bars)-1)*gap
    start_x = (art_w - total_w)//2
    for i, bh in enumerate(bars):
        bh_px = int(bh * (w/1080) * 1.2)
        x0 = start_x + i*(bar_w+gap)
        y0 = wave_y - bh_px//2
        y1 = wave_y + bh_px//2
        # active bars brighter
        alpha = 255 if i in [3,4,5,9,10] else 180
        ad.rounded_rectangle([x0,y0, x0+bar_w, y1], radius=bar_w//2, fill=(255,255,255, alpha))
    # time indicator under waveform
    font_wave = load_font(int(w*0.024), bold=False)
    # need to draw after mask? We'll draw after composite
    art_masked = Image.new("RGBA", (art_w, art_h), (0,0,0,0))
    art_masked.paste(art, (0,0), mask)
    img.paste(art_masked, (art_x0, art_y0), art_masked)
    draw = ImageDraw.Draw(img)
    # script card below art
    script_y0 = art_y1 + int(w*0.03)
    script_y1 = h - int(w*0.32)  # leave space for controls + nav
    script_x0, script_x1 = int(w*0.04), int(w*0.96)
    sh2 = Image.new("RGBA", (w,h), (0,0,0,0))
    sd2 = ImageDraw.Draw(sh2)
    rounded_rect(sd2, [script_x0+3, script_y0+4, script_x1+3, script_y1+4], radius=int(w*0.04), fill=(0,0,0,12))
    img = Image.alpha_composite(img, sh2)
    draw = ImageDraw.Draw(img)
    rounded_rect(draw, [script_x0, script_y0, script_x1, script_y1], radius=int(w*0.04), fill=(255,255,255))
    # script title
    font_script_title = load_font(int(w*0.032), bold=True)
    font_script_body = load_font(int(w*0.030), bold=False)
    font_script_highlight = load_font(int(w*0.030), bold=True)
    draw.text((script_x0+int(w*0.04), script_y0+int(w*0.04)), "Transcript — Today 8:00 AM", font=font_script_title, fill=COLORS["text_primary"])
    # body lines
    lines = [
        ("Good morning, San Francisco! Today starts sunny at 72°", False),
        ("with a light breeze from the west.", False),
        ("By afternoon, clouds build and a brief shower", True),
        ("is possible around 4 PM — 40% chance.", True),
        ("High 78°, low 62°. Perfect for a morning walk.", False),
    ]
    ly = script_y0+int(w*0.12)
    for text, highlight in lines:
        col = (79,168,255) if highlight else COLORS["text_primary"] if not highlight else COLORS["primary"]
        f = font_script_highlight if highlight else font_script_body
        # karaoke highlight: if highlight, draw bg
        if highlight:
            # measure
            try:
                l,t,r,b = f.getbbox(text)
                tw, th = r-l, b-t
            except:
                tw, th = f.getsize(text)
            draw.rounded_rectangle([script_x0+int(w*0.04)-6, ly-4, script_x0+int(w*0.04)+tw+6, ly+th+4], radius=6, fill=(79,168,255,22))
            draw.text((script_x0+int(w*0.04), ly), text, font=f, fill=(79,168,255))
        else:
            draw.text((script_x0+int(w*0.04), ly), text, font=f, fill=(27,39,51, 220 if not highlight else 255))
        ly += int(w*0.07)
    # translate badge at bottom of script
    badge_y0 = script_y1 - int(w*0.09)
    draw.rounded_rectangle([script_x0+int(w*0.04), badge_y0, script_x0+int(w*0.40), badge_y0+int(w*0.06)], radius=int(w*0.03), fill=(11,30,61))
    font_badge = load_font(int(w*0.022), bold=True)
    draw.text((script_x0+int(w*0.07), badge_y0+int(w*0.015)), "Translated • ES", font=font_badge, fill=(255,255,255))
    # offline badge
    draw.rounded_rectangle([script_x1-int(w*0.30), badge_y0, script_x1-int(w*0.04), badge_y0+int(w*0.06)], radius=int(w*0.03), fill=(255,216,115))
    draw.text((script_x1-int(w*0.27), badge_y0+int(w*0.015)), "On-device", font=font_badge, fill=(27,39,51))

    # playback controls bar (similar to radar but audio style)
    ctrl_y0 = h - int(w*0.28)
    ctrl_y1 = h - int(w*0.10) - int(w*0.02)
    ctrl_x0, ctrl_x1 = int(w*0.04), int(w*0.96)
    sh3 = Image.new("RGBA", (w,h), (0,0,0,0))
    sd3 = ImageDraw.Draw(sh3)
    rounded_rect(sd3, [ctrl_x0+3, ctrl_y0+4, ctrl_x1+3, ctrl_y1+4], radius=int(w*0.04), fill=(0,0,0,14))
    img = Image.alpha_composite(img, sh3)
    draw = ImageDraw.Draw(img)
    rounded_rect(draw, [ctrl_x0, ctrl_y0, ctrl_x1, ctrl_y1], radius=int(w*0.04), fill=(255,255,255))
    # previous, play, next
    btn_y = ctrl_y0 + (ctrl_y1-ctrl_y0)//2
    # prev
    px = ctrl_x0+int(w*0.10)
    draw.polygon([(px-8, btn_y),(px+8, btn_y-10),(px+8, btn_y+10)], fill=COLORS["text_secondary"])
    draw.rectangle([px-12, btn_y-10, px-8, btn_y+10], fill=COLORS["text_secondary"])
    # play central
    cx = w//2
    draw.ellipse([cx- int(w*0.07), btn_y- int(w*0.07), cx+ int(w*0.07), btn_y+ int(w*0.07)], fill=(79,168,255))
    draw.polygon([(cx-10, btn_y-14),(cx-10, btn_y+14),(cx+14, btn_y)], fill=(255,255,255))
    # next
    nx = ctrl_x1-int(w*0.10)
    draw.polygon([(nx+8, btn_y),(nx-8, btn_y-10),(nx-8, btn_y+10)], fill=COLORS["text_secondary"])
    draw.rectangle([nx+8, btn_y-10, nx+12, btn_y+10], fill=COLORS["text_secondary"])
    # scrub bar at top of controls
    scrub_y = ctrl_y0+int(w*0.03)
    scrub_x0, scrub_x1 = int(w*0.08), int(w*0.92)
    draw.rounded_rectangle([scrub_x0, scrub_y, scrub_x1, scrub_y+4], radius=2, fill=(230,235,245))
    draw.rounded_rectangle([scrub_x0, scrub_y, scrub_x0+int((scrub_x1-scrub_x0)*0.38), scrub_y+4], radius=2, fill=(79,168,255))
    draw.ellipse([scrub_x0+int((scrub_x1-scrub_x0)*0.38)-6, scrub_y-5, scrub_x0+int((scrub_x1-scrub_x0)*0.38)+6, scrub_y+9], fill=(79,168,255), outline=(255,255,255), width=2)
    font_time = load_font(int(w*0.022), bold=False)
    draw.text((scrub_x0, scrub_y+10), "0:24", font=font_time, fill=COLORS["text_secondary"])
    draw.text((scrub_x1-34, scrub_y+10), "1:42", font=font_time, fill=COLORS["text_secondary"])

    draw_bottom_nav(draw, w, h, selected=2)
    img.convert("RGB").save(filename, "PNG", optimize=True)
    print(f"saved report {filename} {w}x{h} {os.path.getsize(filename)}")

# Main generation
import pathlib
base_phone = r"H:\weather-app\store\screenshots\phone"
base_7 = r"H:\weather-app\store\screenshots\7inch"
base_10 = r"H:\weather-app\store\screenshots\10inch"
fast_phone = r"H:\weather-app\fastlane\metadata\android\en-US\images\phoneScreenshots"
fast_7 = r"H:\weather-app\fastlane\metadata\android\en-US\images\sevenInchScreenshots"
fast_10 = r"H:\weather-app\fastlane\metadata\android\en-US\images\tenInchScreenshots"
web_base = r"H:\weather-app\website\public\screenshots"

for p in [base_phone, base_7, base_10, fast_phone, fast_7, fast_10, web_base]:
    os.makedirs(p, exist_ok=True)

# Phone 1080x2400
phone_w, phone_h = 1080, 2400
gen_home(phone_w, phone_h, os.path.join(base_phone, "01_home.png"))
gen_radar(phone_w, phone_h, os.path.join(base_phone, "02_radar.png"))
gen_report(phone_w, phone_h, os.path.join(base_phone, "03_report.png"))
# also copy to fastlane and web
for i in ["01_home.png","02_radar.png","03_report.png"]:
    for dst in [fast_phone, web_base+"/phone"]:
        os.makedirs(dst, exist_ok=True)
        Image.open(os.path.join(base_phone, i)).save(os.path.join(dst, i))

# 7-inch 1200x1920
w7, h7 = 1200, 1920
# For tablet, we can reuse same generators but with different w/h they will stretch slightly; that's okay but we should generate at those dimensions
gen_home(w7, h7, os.path.join(base_7, "01_home_7in.png"))
gen_radar(w7, h7, os.path.join(base_7, "02_radar_7in.png"))
gen_report(w7, h7, os.path.join(base_7, "03_report_7in.png"))
for i in ["01_home_7in.png","02_radar_7in.png","03_report_7in.png"]:
    # copy to fastlane with expected names without suffix? Keep as is
    Image.open(os.path.join(base_7, i)).save(os.path.join(fast_7, i.replace("_7in","")))
    os.makedirs(web_base+"/7inch", exist_ok=True)
    Image.open(os.path.join(base_7, i)).save(os.path.join(web_base+"/7inch", i))

# 10-inch 1600x2560
w10, h10 = 1600, 2560
gen_home(w10, h10, os.path.join(base_10, "01_home_10in.png"))
gen_radar(w10, h10, os.path.join(base_10, "02_radar_10in.png"))
gen_report(w10, h10, os.path.join(base_10, "03_report_10in.png"))
for i in ["01_home_10in.png","02_radar_10in.png","03_report_10in.png"]:
    Image.open(os.path.join(base_10, i)).save(os.path.join(fast_10, i.replace("_10in","")))
    os.makedirs(web_base+"/10inch", exist_ok=True)
    Image.open(os.path.join(base_10, i)).save(os.path.join(web_base+"/10inch", i))

print("all screenshots done")
