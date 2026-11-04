package com.guillaume.a3cockpit;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Liste des applis installées, lancement des raccourcis et sélecteur d'appli. */
final class Apps {
    private Apps() {}

    static final class App {
        String label;
        ComponentName cn;
        Drawable icon;
    }

    interface Picked { void on(App app); }

    static final String NAV = "app_nav", MUSIC = "app_music", PHONE = "app_phone", RADIO = "app_radio", SOUND = "app_sound",
            CAM = "app_cam";

    static String role(String key) {
        switch (key) {
            case NAV: return "Navigation";
            case MUSIC: return "Musique";
            case PHONE: return "Téléphone";
            case RADIO: return "Radio";
            case CAM: return "Appli caméra de l'autoradio";
            default: return "Son / Égaliseur";
        }
    }

    private static List<App> cache;

    static List<App> list(Context c, boolean reload) {
        if (cache != null && !reload) return cache;
        PackageManager pm = c.getPackageManager();
        Intent i = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> ris = pm.queryIntentActivities(i, 0);
        List<App> out = new ArrayList<>();
        for (ResolveInfo ri : ris) {
            if (ri.activityInfo.packageName.equals(c.getPackageName())) continue;
            App a = new App();
            a.label = ri.loadLabel(pm).toString();
            a.cn = new ComponentName(ri.activityInfo.packageName, ri.activityInfo.name);
            a.icon = ri.loadIcon(pm);
            out.add(a);
        }
        final Collator col = Collator.getInstance(Locale.FRANCE);
        Collections.sort(out, new Comparator<App>() {
            @Override public int compare(App x, App y) { return col.compare(x.label, y.label); }
        });
        cache = out;
        return out;
    }

    static boolean launch(Context c, ComponentName cn) {
        try {
            Intent i = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                    .setComponent(cn).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            c.startActivity(i);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    static boolean start(Context c, Intent i) {
        try {
            c.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            return true;
        } catch (ActivityNotFoundException | SecurityException e) {
            return false;
        }
    }

    static App byKeywords(Context c, String[] packages, String[] labels) {
        List<App> all = list(c, false);
        if (packages != null) for (String pk : packages)
            for (App a : all) if (a.cn.getPackageName().toLowerCase(Locale.ROOT).contains(pk)) return a;
        if (labels != null) for (String kw : labels) {
            String k = kw.toLowerCase(Locale.FRANCE);
            for (App a : all) {
                String l = a.label.toLowerCase(Locale.FRANCE);
                if (l.equals(k) || l.startsWith(k + " ") || l.contains(" " + k) || (k.length() >= 4 && l.startsWith(k))) return a;
            }
        }
        return null;
    }

    /** Lance l'appli associée à un rôle : réglage utilisateur, sinon détection automatique, sinon sélecteur. */
    static void open(final Activity a, final String key) {
        String saved = Prefs.str(key, null);
        if (saved != null) {
            ComponentName cn = ComponentName.unflattenFromString(saved);
            if (cn != null && launch(a, cn)) return;
        }
        App found = null;
        Intent fallback = null;
        switch (key) {
            case NAV:
                found = byKeywords(a, new String[]{"com.waze", "com.google.android.apps.maps", "net.osmand", "com.sygic", "com.here."},
                        new String[]{"Waze", "Maps", "Navigation", "Navi", "GPS"});
                fallback = new Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q="));
                break;
            case MUSIC:
                found = byKeywords(a, new String[]{"com.spotify", "deezer", "com.google.android.apps.youtube.music"},
                        new String[]{"Musique", "Music", "Lecteur"});
                fallback = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MUSIC);
                break;
            case PHONE:
                found = byKeywords(a, null, new String[]{"Téléphone", "Phone", "Bluetooth", "BT", "Appels", "Dialer"});
                fallback = new Intent(Intent.ACTION_DIAL);
                break;
            case RADIO:
                found = byKeywords(a, new String[]{"radio", "fmradio"}, new String[]{"Radio", "FM", "Tuner"});
                break;
            case CAM:
                found = byKeywords(a, new String[]{"backcar", "reverse", "avin", "camera"},
                        new String[]{"Caméra", "Camera", "AVIN", "Recul", "Rear"});
                break;
            case SOUND:
                found = byKeywords(a, new String[]{"equalizer", "eqset", ".eq", "dsp"},
                        new String[]{"Égaliseur", "Egaliseur", "Equalizer", "EQ", "DSP", "Audio", "Son", "Sound"});
                fallback = new Intent(Settings.ACTION_SOUND_SETTINGS);
                break;
            default:
        }
        if (found != null && launch(a, found.cn)) return;
        if (fallback != null && start(a, fallback)) return;
        Toast.makeText(a, "Choisissez l'appli « " + role(key) + " »", Toast.LENGTH_SHORT).show();
        pick(a, role(key), new Picked() {
            @Override public void on(App app) {
                Prefs.put(key, app.cn.flattenToString());
                launch(a, app.cn);
            }
        });
    }

    static String currentLabel(Context c, String key) {
        String saved = Prefs.str(key, null);
        if (saved == null) return "Automatique";
        ComponentName cn = ComponentName.unflattenFromString(saved);
        if (cn == null) return "Automatique";
        for (App a : list(c, false)) if (a.cn.equals(cn)) return a.label;
        return cn.getPackageName();
    }

    static void pick(final Activity act, String title, final Picked cb) {
        final List<App> apps = list(act, true);
        BaseAdapter ad = new BaseAdapter() {
            @Override public int getCount() { return apps.size(); }
            @Override public Object getItem(int i) { return apps.get(i); }
            @Override public long getItemId(int i) { return i; }
            @Override public View getView(int i, View v, ViewGroup parent) {
                LinearLayout row;
                if (v instanceof LinearLayout) row = (LinearLayout) v;
                else {
                    row = new LinearLayout(act);
                    row.setOrientation(LinearLayout.HORIZONTAL);
                    row.setGravity(Gravity.CENTER_VERTICAL);
                    row.setPadding(Ui.s(20), Ui.s(10), Ui.s(20), Ui.s(10));
                    ImageView iv = new ImageView(act);
                    row.addView(iv, new LinearLayout.LayoutParams(Ui.s(40), Ui.s(40)));
                    TextView tv = Ui.text(act, "", 17, Ui.TEXT, Ui.REG);
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
                    lp.leftMargin = Ui.s(16);
                    row.addView(tv, lp);
                }
                App a = apps.get(i);
                ((ImageView) row.getChildAt(0)).setImageDrawable(a.icon);
                ((TextView) row.getChildAt(1)).setText(a.label);
                return row;
            }
        };
        new AlertDialog.Builder(act, AlertDialog.THEME_DEVICE_DEFAULT_DARK)
                .setTitle(title)
                .setAdapter(ad, new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int which) { cb.on(apps.get(which)); }
                })
                .setNegativeButton("Annuler", null)
                .show();
    }
}
