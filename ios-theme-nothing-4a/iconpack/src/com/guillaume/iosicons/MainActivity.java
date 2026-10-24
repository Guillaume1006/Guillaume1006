package com.guillaume.iosicons;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.TextView;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Appelé par un launcher pour choisir une icône : on ne renvoie rien, il lit le pack lui-même.
        String action = getIntent().getAction();
        if ("org.adw.launcher.icons.ACTION_PICK_ICON".equals(action)
                || "com.novalauncher.THEME".equals(action)) {
            setResult(RESULT_CANCELED, new Intent());
            finish();
            return;
        }
        TextView t = new TextView(this);
        t.setText("iOS Icons\n\nPack d'icônes installé.\nOuvrez Lawnchair ou Nova Launcher → Réglages → Pack d'icônes → iOS Icons.");
        t.setGravity(Gravity.CENTER);
        t.setTextSize(18);
        t.setTextColor(Color.BLACK);
        t.setBackgroundColor(Color.WHITE);
        t.setPadding(48, 48, 48, 48);
        setContentView(t);
    }
}
