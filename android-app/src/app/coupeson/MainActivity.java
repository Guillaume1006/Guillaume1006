package app.coupeson;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.view.Window;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/** Coque native de Coupe-Son : affiche la page locale et fournit l'enregistrement et le partage. */
public class MainActivity extends Activity {
    private static final int REQ_PICK = 1;
    private static final String FOLDER = "Music/Coupe-Son";

    private WebView web;
    private ValueCallback<Uri[]> chooser;
    private ByteArrayOutputStream pending;
    private String pendingName;
    private String pendingMime;
    private Uri lastUri;
    private String lastMime;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        boolean night = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
        int bg = night ? 0xFF121211 : 0xFFF6F6F4;
        Window w = getWindow();
        w.setStatusBarColor(bg);
        w.setNavigationBarColor(bg);
        if (!night) {
            // 0x2000 = SYSTEM_UI_FLAG_LIGHT_STATUS_BAR ; 0x10 = SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            w.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | 0x10);
        }

        web = new WebView(this);
        web.setBackgroundColor(bg);
        setContentView(web);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setSupportZoom(false);
        s.setBuiltInZoomControls(false);

        web.addJavascriptInterface(new Bridge(), "AndroidBridge");
        web.setWebViewClient(new WebViewClient());
        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> cb, FileChooserParams params) {
                if (chooser != null) chooser.onReceiveValue(null);
                chooser = cb;
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("audio/*");
                try {
                    startActivityForResult(Intent.createChooser(i, "Choisir un son"), REQ_PICK);
                } catch (Exception e) {
                    chooser = null;
                    cb.onReceiveValue(null);
                    return false;
                }
                return true;
            }
        });
        web.loadUrl("file:///android_asset/index.html");
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        if (req == REQ_PICK && chooser != null) {
            chooser.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(res, data));
            chooser = null;
        }
        super.onActivityResult(req, res, data);
    }

    @Override
    protected void onPause() {
        super.onPause();
        web.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        web.onResume();
    }

    @Override
    protected void onDestroy() {
        web.destroy();
        super.onDestroy();
    }

    /** Pont appelé depuis la page : begin, chunk (base64), finish, share. */
    private class Bridge {
        @JavascriptInterface
        public void begin(String name, String mime) {
            pending = new ByteArrayOutputStream();
            pendingName = name;
            pendingMime = mime;
        }

        @JavascriptInterface
        public void chunk(String b64) {
            try {
                pending.write(Base64.decode(b64, Base64.DEFAULT));
            } catch (IOException ignored) {
            }
        }

        @JavascriptInterface
        public String finish() {
            try {
                ContentValues v = new ContentValues();
                v.put("_display_name", pendingName);
                v.put("mime_type", pendingMime);
                v.put("relative_path", FOLDER);
                v.put("is_pending", 1);
                Uri uri = getContentResolver().insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, v);
                if (uri == null) return "ERREUR : dossier inaccessible.";
                OutputStream out = getContentResolver().openOutputStream(uri);
                out.write(pending.toByteArray());
                out.close();
                ContentValues done = new ContentValues();
                done.put("is_pending", 0);
                getContentResolver().update(uri, done, null, null);
                lastUri = uri;
                lastMime = pendingMime;
                pending = null;
                return "Musique/Coupe-Son";
            } catch (Exception e) {
                return "ERREUR : " + e.getMessage();
            }
        }

        @JavascriptInterface
        public void share() {
            if (lastUri == null) return;
            final Intent send = new Intent(Intent.ACTION_SEND);
            send.setType(lastMime);
            send.putExtra(Intent.EXTRA_STREAM, lastUri);
            send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        startActivity(Intent.createChooser(send, "Partager le son"));
                    } catch (Exception e) {
                        Toast.makeText(MainActivity.this, "Partage impossible.", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }
}
