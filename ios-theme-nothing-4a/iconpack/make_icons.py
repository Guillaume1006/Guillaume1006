"""Dessine les icônes style iOS (192x192, squircle) et génère appfilter.xml / drawable.xml."""
import math
import sys
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont, ImageChops

S = 192          # taille finale
K = 4            # suréchantillonnage
N = S * K
OUT = Path(sys.argv[1])
FONT_B = "/usr/share/fonts/opentype/inter/Inter-Bold.otf"
FONT_X = "/usr/share/fonts/opentype/inter/Inter-ExtraBold.otf"
FONT_SYM = "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"
WHITE = (255, 255, 255, 255)


def hexc(h):
    h = h.lstrip("#")
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4)) + (255,)


def squircle_mask(size=N, n=5.0):
    """Superellipse |x|^n + |y|^n = 1 (forme iOS)."""
    m = Image.new("L", (size, size), 0)
    px = m.load()
    c = (size - 1) / 2
    r = size / 2
    for y in range(size):
        dy = abs((y - c) / r) ** n
        for x in range(size):
            if abs((x - c) / r) ** n + dy <= 1:
                px[x, y] = 255
    return m


MASK = squircle_mask()


def gradient(c1, c2, angle=90):
    """Dégradé linéaire c1 (haut) -> c2 (bas) ; angle en degrés."""
    c1, c2 = hexc(c1), hexc(c2)
    g = Image.linear_gradient("L").resize((N, N))   # noir->blanc vers le bas
    g = g.rotate(angle - 90, resample=Image.BICUBIC, expand=False) if angle != 90 else g
    return Image.composite(Image.new("RGBA", (N, N), c2), Image.new("RGBA", (N, N), c1), g)


def solid(c):
    return Image.new("RGBA", (N, N), hexc(c))


def sc(v):
    return int(v * K)


def font(path, size):
    return ImageFont.truetype(path, sc(size))


def text(d, xy, s, f, fill=WHITE):
    d.text((sc(xy[0]), sc(xy[1])), s, font=f, fill=fill, anchor="mm")


def line(d, pts, w, fill=WHITE):
    d.line([(sc(x), sc(y)) for x, y in pts], fill=fill, width=sc(w), joint="curve")
    for x, y in (pts[0], pts[-1]):
        d.ellipse([sc(x - w / 2), sc(y - w / 2), sc(x + w / 2), sc(y + w / 2)], fill=fill)


def circ(d, cx, cy, r, fill=WHITE, outline=None, width=0):
    d.ellipse([sc(cx - r), sc(cy - r), sc(cx + r), sc(cy + r)], fill=fill, outline=outline, width=sc(width))


def rrect(d, box, r, fill=WHITE, outline=None, width=0):
    d.rounded_rectangle([sc(v) for v in box], radius=sc(r), fill=fill, outline=outline, width=sc(width))


def poly(d, pts, fill=WHITE):
    d.polygon([(sc(x), sc(y)) for x, y in pts], fill=fill)


# ---------- glyphes ----------
def g_phone(d):
    text(d, (96, 100), "✆", font(FONT_SYM, 110))


def g_messages(d):
    d.ellipse([sc(32), sc(40), sc(160), sc(130)], fill=WHITE)
    poly(d, [(50, 112), (42, 148), (84, 124)])


def g_chrome(d):
    cx = cy = 96
    box = [sc(cx - 62), sc(cy - 62), sc(cx + 62), sc(cy + 62)]
    d.pieslice(box, 210, 330, fill=hexc("#EA4335"))
    d.pieslice(box, 330, 90, fill=hexc("#FBBC05"))
    d.pieslice(box, 90, 210, fill=hexc("#34A853"))
    circ(d, cx, cy, 30, fill=WHITE)
    circ(d, cx, cy, 24, fill=hexc("#4285F4"))


def g_settings(d):
    cx = cy = 96
    for i in range(8):
        a = math.radians(i * 45)
        x, y = cx + 52 * math.cos(a), cy + 52 * math.sin(a)
        circ(d, x, y, 13, fill=hexc("#D1D1D6"))
    circ(d, cx, cy, 50, fill=hexc("#D1D1D6"))
    circ(d, cx, cy, 20, fill=hexc("#5B5F66"))


def g_camera(d):
    rrect(d, (30, 58, 162, 146), 16, fill=hexc("#E5E5EA"))
    rrect(d, (62, 44, 96, 64), 6, fill=hexc("#E5E5EA"))
    circ(d, 96, 102, 30, fill=hexc("#3A3A3C"))
    circ(d, 96, 102, 20, fill=hexc("#1C1C1E"))
    circ(d, 88, 94, 6, fill=hexc("#8E8E93"))


def g_play(d):
    A, B, C, D = hexc("#00D2FF"), hexc("#00E676"), hexc("#FFC107"), hexc("#F44336")
    poly(d, [(58, 40), (58, 152), (112, 96)], fill=A)
    poly(d, [(58, 40), (112, 96), (132, 76)], fill=B)
    poly(d, [(58, 152), (112, 96), (132, 116)], fill=D)
    poly(d, [(132, 76), (112, 96), (132, 116), (150, 96)], fill=C)


def g_youtube(d):
    rrect(d, (28, 56, 164, 136), 24, fill=hexc("#FF0000"))
    poly(d, [(82, 76), (82, 116), (118, 96)])


def g_gmail(d):
    red = hexc("#EA4335")
    line(d, [(42, 138), (42, 58), (96, 104), (150, 58), (150, 138)], 18, fill=red)


def g_maps(d):
    circ(d, 96, 80, 38, fill=hexc("#EA4335"))
    poly(d, [(64, 98), (128, 98), (96, 150)], fill=hexc("#EA4335"))
    circ(d, 96, 80, 14, fill=WHITE)


def g_photos(d):
    cols = ["#EA4335", "#FBBC05", "#34A853", "#4285F4"]
    for i, c in enumerate(cols):
        a = math.radians(i * 90 - 45)
        circ(d, 96 + 28 * math.cos(a), 96 + 28 * math.sin(a), 30, fill=hexc(c) )


def g_calendar(d):
    text(d, (96, 52), "SAM", font(FONT_B, 24), fill=hexc("#FF3B30"))
    text(d, (96, 118), "24", font(FONT_X, 88), fill=hexc("#1C1C1E"))


def g_clock(d):
    circ(d, 96, 96, 70, fill=WHITE)
    for i in range(12):
        a = math.radians(i * 30)
        line(d, [(96 + 58 * math.cos(a), 96 + 58 * math.sin(a)),
                 (96 + 64 * math.cos(a), 96 + 64 * math.sin(a))], 3, fill=hexc("#1C1C1E"))
    line(d, [(96, 96), (96, 52)], 6, fill=hexc("#1C1C1E"))
    line(d, [(96, 96), (124, 108)], 6, fill=hexc("#1C1C1E"))
    line(d, [(96, 106), (96, 44)], 3, fill=hexc("#FF9500"))
    circ(d, 96, 96, 5, fill=hexc("#FF9500"))


def g_calc(d):
    cols = [("#5B5B60", "#5B5B60", "#5B5B60", "#FF9F0A")] * 3
    for r in range(3):
        for c in range(4):
            x, y = 24 + c * 38, 38 + r * 38
            rrect(d, (x, y, x + 32, y + 32), 12, fill=hexc(cols[r][c]))
    rrect(d, (24, 38 + 3 * 38 - 0, 24 + 70, 38 + 3 * 38 + 32), 12, fill=hexc("#5B5B60"))
    rrect(d, (24 + 76, 38 + 3 * 38, 24 + 108, 38 + 3 * 38 + 32), 12, fill=hexc("#5B5B60"))
    rrect(d, (24 + 114, 38 + 3 * 38, 24 + 146, 38 + 3 * 38 + 32), 12, fill=hexc("#FF9F0A"))


def g_contacts(d):
    circ(d, 96, 72, 28, fill=hexc("#C7C7CC"))
    d.pieslice([sc(44), sc(104), sc(148), sc(208)], 180, 360, fill=hexc("#C7C7CC"))


def g_files(d):
    rrect(d, (34, 54, 158, 146), 14, fill=hexc("#FFFFFF"))
    rrect(d, (34, 44, 92, 70), 10, fill=hexc("#FFFFFF"))
    rrect(d, (34, 70, 158, 146), 14, fill=hexc("#E3EEFF"))


def g_whatsapp(d):
    circ(d, 96, 92, 56, fill=None, outline=WHITE, width=11)
    poly(d, [(44, 150), (50, 112), (78, 140)])
    text(d, (96, 94), "✆", font(FONT_SYM, 64))


def g_instagram(d):
    rrect(d, (36, 36, 156, 156), 36, fill=None, outline=WHITE, width=11)
    circ(d, 96, 96, 28, fill=None, outline=WHITE, width=11)
    circ(d, 130, 62, 7, fill=WHITE)


def g_spotify(d):
    blk = (20, 20, 20, 255)
    for i, (w, y) in enumerate([(92, 70), (76, 96), (58, 120)]):
        d.arc([sc(96 - w / 2 - 6), sc(y - 40), sc(96 + w / 2 + 6), sc(y + 40)], 215, 325, fill=blk, width=sc(11 - i))


def g_telegram(d):
    poly(d, [(36, 92), (156, 44), (130, 150), (96, 124), (74, 146), (74, 112), (140, 62), (84, 104)])


def g_drive(d):
    poly(d, [(72, 44), (122, 44), (170, 128), (146, 128), (98, 44)], fill=hexc("#FBBC05"))
    poly(d, [(72, 44), (24, 128), (48, 168), (96, 84)], fill=hexc("#34A853"))
    poly(d, [(48, 168), (146, 168), (170, 128), (72, 128)], fill=hexc("#4285F4"))


def g_google(d):
    text(d, (96, 100), "G", font(FONT_X, 128), fill=hexc("#4285F4"))


def g_netflix(d):
    text(d, (96, 100), "N", font(FONT_X, 130), fill=hexc("#E50914"))


def g_tiktok(d):
    f = font(FONT_SYM, 130)
    text(d, (92, 96), "♪", f, fill=hexc("#25F4EE"))
    text(d, (100, 100), "♪", f, fill=hexc("#FE2C55"))
    text(d, (96, 98), "♪", f, fill=WHITE)


def g_x(d):
    line(d, [(50, 46), (142, 146)], 16)
    line(d, [(142, 46), (50, 146)], 7)


def g_facebook(d):
    text(d, (110, 118), "f", font(FONT_X, 190))


def g_messenger(d):
    d.ellipse([sc(32), sc(30), sc(160), sc(150)], fill=WHITE)
    poly(d, [(58, 152), (60, 120), (90, 134)])
    poly(d, [(54, 108), (92, 64), (96, 90), (138, 84), (100, 128), (94, 102)], fill=hexc("#7A3BFF"))


def g_discord(d):
    rrect(d, (30, 48, 162, 140), 40, fill=WHITE)
    circ(d, 68, 98, 13, fill=hexc("#5865F2"))
    circ(d, 124, 98, 13, fill=hexc("#5865F2"))


def g_snapchat(d):
    c = WHITE
    circ(d, 96, 76, 36, fill=c)
    rrect(d, (60, 76, 132, 140), 8, fill=c)
    for i in range(3):
        poly(d, [(60 + i * 24, 138), (72 + i * 24, 158), (84 + i * 24, 138)], fill=c)
    circ(d, 82, 78, 6, fill=hexc("#1C1C1E"))
    circ(d, 110, 78, 6, fill=hexc("#1C1C1E"))


def g_notes(d):
    rrect(d, (44, 36, 148, 156), 14, fill=WHITE)
    for y in (68, 92, 116):
        line(d, [(60, y), (132, y)], 7, fill=hexc("#E5D58A"))
    line(d, [(60, 140), (96, 140)], 7, fill=hexc("#E5D58A"))


def g_weather(d):
    circ(d, 120, 70, 30, fill=hexc("#FFD60A"))
    for cx, cy, r in ((72, 118, 28), (104, 108, 34), (136, 124, 24)):
        circ(d, cx, cy, r)
    rrect(d, (72, 118, 136, 148), 12)


ICONS = {
    # nom: (fond, glyphe, [composants Android])
    "phone": (("#5CE27A", "#14B83B"), g_phone, [
        "com.google.android.dialer/com.google.android.dialer.extensions.GoogleDialtactsActivity",
        "com.android.dialer/com.android.dialer.main.impl.MainActivity",
        "com.nothing.dialer/com.android.dialer.DialtactsActivity"]),
    "messages": (("#5CE27A", "#14B83B"), g_messages, [
        "com.google.android.apps.messaging/com.google.android.apps.messaging.ui.ConversationListActivity",
        "com.google.android.apps.messaging/com.google.android.apps.messaging.home.HomeActivity"]),
    "chrome": (("#FFFFFF", "#F1F3F4"), g_chrome, [
        "com.android.chrome/com.google.android.apps.chrome.Main"]),
    "settings": (("#8E8E93", "#48484A"), g_settings, [
        "com.android.settings/com.android.settings.Settings"]),
    "camera": (("#6E6E73", "#2C2C2E"), g_camera, [
        "com.nothing.camera/com.nothing.camera.CameraLauncher",
        "com.nothing.camera/com.android.camera.Camera",
        "com.google.android.GoogleCamera/com.android.camera.CameraLauncher"]),
    "playstore": (("#FFFFFF", "#F1F3F4"), g_play, [
        "com.android.vending/com.google.android.finsky.activities.MainActivity",
        "com.android.vending/com.android.vending.AssetBrowserActivity"]),
    "youtube": (("#FFFFFF", "#F1F3F4"), g_youtube, [
        "com.google.android.youtube/com.google.android.apps.youtube.app.WatchWhileActivity"]),
    "gmail": (("#FFFFFF", "#F1F3F4"), g_gmail, [
        "com.google.android.gm/com.google.android.gm.ConversationListActivityGmail"]),
    "maps": (("#E8F5E9", "#B9E6C1"), g_maps, [
        "com.google.android.apps.maps/com.google.android.maps.MapsActivity"]),
    "photos": (("#FFFFFF", "#F1F3F4"), g_photos, [
        "com.google.android.apps.photos/com.google.android.apps.photos.home.HomeActivity"]),
    "calendar": (("#FFFFFF", "#F1F3F4"), g_calendar, [
        "com.google.android.calendar/com.android.calendar.AllInOneActivity"]),
    "clock": (("#2C2C2E", "#000000"), g_clock, [
        "com.google.android.deskclock/com.android.deskclock.DeskClock"]),
    "calculator": (("#3A3A3C", "#000000"), g_calc, [
        "com.google.android.calculator/com.android.calculator2.Calculator"]),
    "contacts": (("#A5A5AA", "#6E6E73"), g_contacts, [
        "com.google.android.contacts/com.android.contacts.activities.PeopleActivity"]),
    "files": (("#4DA3FF", "#0A60FF"), g_files, [
        "com.google.android.apps.nbu.files/com.google.android.apps.nbu.files.home.HomeActivity"]),
    "whatsapp": (("#5CE27A", "#14B83B"), g_whatsapp, [
        "com.whatsapp/com.whatsapp.Main", "com.whatsapp/com.whatsapp.home.ui.HomeActivity"]),
    "instagram": (("#7A3BFF", "#FF3B7A", "#FFB300"), g_instagram, [
        "com.instagram.android/com.instagram.android.activity.MainTabActivity"]),
    "spotify": (("#2EE06B", "#17A34A"), g_spotify, [
        "com.spotify.music/com.spotify.music.MainActivity"]),
    "telegram": (("#5AC8FA", "#1C8EE0"), g_telegram, [
        "org.telegram.messenger/org.telegram.ui.LaunchActivity"]),
    "drive": (("#FFFFFF", "#F1F3F4"), g_drive, [
        "com.google.android.apps.docs/com.google.android.apps.docs.app.NewMainProxyActivity"]),
    "google": (("#FFFFFF", "#F1F3F4"), g_google, [
        "com.google.android.googlequicksearchbox/com.google.android.googlequicksearchbox.SearchActivity"]),
    "netflix": (("#1C1C1E", "#000000"), g_netflix, [
        "com.netflix.mediaclient/com.netflix.mediaclient.ui.launch.UIWebViewActivity"]),
    "tiktok": (("#2C2C2E", "#000000"), g_tiktok, [
        "com.zhiliaoapp.musically/com.ss.android.ugc.aweme.splash.SplashActivity"]),
    "x": (("#2C2C2E", "#000000"), g_x, [
        "com.twitter.android/com.twitter.android.StartActivity"]),
    "facebook": (("#4C8DFF", "#0A5CE0"), g_facebook, [
        "com.facebook.katana/com.facebook.katana.LoginActivity"]),
    "messenger": (("#2AA3FF", "#A033FF"), g_messenger, [
        "com.facebook.orca/com.facebook.orca.auth.StartScreenActivity"]),
    "discord": (("#7983F5", "#4752C4"), g_discord, [
        "com.discord/com.discord.app.AppActivity$Main"]),
    "snapchat": (("#FFFC4D", "#FFD500"), g_snapchat, [
        "com.snapchat.android/com.snap.mushroom.MainActivity"]),
    "notes": (("#FFE066", "#FFC107"), g_notes, [
        "com.nothing.notes/com.nothing.notes.MainActivity",
        "com.google.android.keep/com.google.android.keep.activities.BrowseActivity"]),
    "weather": (("#5AB8FF", "#1E6FE0"), g_weather, [
        "com.nothing.weather/com.nothing.weather.MainActivity",
        "com.google.android.googlequicksearchbox/com.google.android.apps.search.weather.WeatherActivity"]),
}


def render(name, bg, glyph):
    if len(bg) == 3:     # dégradé 3 couleurs (Instagram)
        ramp = Image.linear_gradient("L").resize((N, N))
        m1 = ramp.point(lambda v: min(v * 2, 255))
        m2 = ramp.point(lambda v: max(v * 2 - 255, 0))
        base = Image.composite(solid(bg[1]), solid(bg[0]), m1)
        base = Image.composite(solid(bg[2]), base, m2)
    else:
        base = gradient(bg[0], bg[1], 90)
    art = Image.new("RGBA", (N, N), (0, 0, 0, 0))
    d = ImageDraw.Draw(art)
    glyph(d)
    base = Image.alpha_composite(base, art)
    base.putalpha(MASK)
    return base.resize((S, S), Image.LANCZOS)


def main():
    drawables = OUT / "res" / "drawable-nodpi"
    drawables.mkdir(parents=True, exist_ok=True)
    (OUT / "assets").mkdir(parents=True, exist_ok=True)
    (OUT / "res" / "xml").mkdir(parents=True, exist_ok=True)

    items = ['<?xml version="1.0" encoding="utf-8"?>', "<resources>",
             '    <iconback img1="iconback" />', '    <iconmask img1="iconmask" />',
             '    <iconupon img1="iconupon" />', '    <scale factor="0.7" />']
    cat = ['<?xml version="1.0" encoding="utf-8"?>', "<resources>", '    <version>1</version>']
    for name, (bg, glyph, comps) in ICONS.items():
        render(name, bg, glyph).save(drawables / f"{name}.png", optimize=True)
        for c in comps:
            items.append(f'    <item component="ComponentInfo{{{c}}}" drawable="{name}" />')
        cat.append(f'    <item drawable="{name}" />')
    items.append("</resources>")
    cat.append("</resources>")

    # icônes de repli (apps sans icône dédiée) : fond + masque squircle
    back = Image.new("RGBA", (N, N), hexc("#F2F2F7"))
    back.putalpha(MASK)
    back.resize((S, S), Image.LANCZOS).save(drawables / "iconback.png")
    mask = Image.new("RGBA", (N, N), (255, 255, 255, 255))
    mask.putalpha(MASK)
    mask.resize((S, S), Image.LANCZOS).save(drawables / "iconmask.png")
    Image.new("RGBA", (S, S), (0, 0, 0, 0)).save(drawables / "iconupon.png")
    render("launcher", ("#4DA3FF", "#0A60FF"), g_settings).save(drawables / "ic_launcher.png")

    for target in (OUT / "assets", OUT / "res" / "xml"):
        (target / "appfilter.xml").write_text("\n".join(items) + "\n", encoding="utf-8")
        (target / "drawable.xml").write_text("\n".join(cat) + "\n", encoding="utf-8")
    print(len(ICONS), "icônes générées")


main()
