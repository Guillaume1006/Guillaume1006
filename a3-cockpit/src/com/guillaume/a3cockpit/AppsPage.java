package com.guillaume.a3cockpit;

import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

/** Tiroir d'applications. */
final class AppsPage extends Page {
    private List<Apps.App> apps;
    private final BaseAdapter adapter;

    AppsPage(final MainActivity a, float w, float h) {
        super(a, w, h);
        setBackground(Ui.card());
        Ui.place(this, Ui.text(a, "Applications", 26, Ui.TEXT, Ui.BOLD), 24, 20, w - 48, 32);
        apps = Apps.list(a, true);
        GridView g = Ui.place(this, new GridView(a), 12, 70, w - 24, h - 82);
        g.setColumnWidth(Ui.s(112));
        g.setNumColumns(GridView.AUTO_FIT);
        g.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        g.setVerticalSpacing(Ui.s(10));
        g.setSelector(new android.graphics.drawable.ColorDrawable(0));
        g.setVerticalScrollBarEnabled(false);
        adapter = new BaseAdapter() {
            @Override public int getCount() { return apps.size(); }
            @Override public Object getItem(int i) { return apps.get(i); }
            @Override public long getItemId(int i) { return i; }
            @Override public View getView(int i, View v, ViewGroup parent) {
                LinearLayout cell;
                if (v instanceof LinearLayout) cell = (LinearLayout) v;
                else {
                    cell = new LinearLayout(a);
                    cell.setOrientation(LinearLayout.VERTICAL);
                    cell.setGravity(Gravity.CENTER_HORIZONTAL);
                    cell.setPadding(0, Ui.s(8), 0, Ui.s(4));
                    ImageView iv = new ImageView(a);
                    cell.addView(iv, new LinearLayout.LayoutParams(Ui.s(58), Ui.s(58)));
                    TextView tv = Ui.text(a, "", 13, Ui.TEXT, Ui.REG);
                    tv.setGravity(Gravity.CENTER);
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
                    lp.topMargin = Ui.s(8);
                    lp.leftMargin = lp.rightMargin = Ui.s(4);
                    cell.addView(tv, lp);
                }
                Apps.App app = apps.get(i);
                ((ImageView) cell.getChildAt(0)).setImageDrawable(app.icon);
                ((TextView) cell.getChildAt(1)).setText(app.label);
                return cell;
            }
        };
        g.setAdapter(adapter);
        g.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
                Apps.launch(a, apps.get(pos).cn);
            }
        });
        g.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override public boolean onItemLongClick(AdapterView<?> parent, View view, int pos, long id) {
                try {
                    a.startActivity(new android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            android.net.Uri.parse("package:" + apps.get(pos).cn.getPackageName())));
                } catch (Exception ignored) { }
                return true;
            }
        });
    }

    @Override void onShow() {
        apps = Apps.list(a, true);
        adapter.notifyDataSetChanged();
    }
}
