package com.aiden.folddisplay;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.hardware.display.DisplayManager;
import android.os.Bundle;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends Activity {

    private DisplayManager displayManager;
    private TextView displayInfo;
    private Spinner appSpinner;
    private final ArrayList<AppEntry> apps = new ArrayList<>();

    static class AppEntry {
        final String label;
        final String packageName;
        AppEntry(String label, String packageName) {
            this.label = label;
            this.packageName = packageName;
        }
        @Override public String toString() { return label; }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        displayManager = (DisplayManager) getSystemService(Context.DISPLAY_SERVICE);
        setContentView(buildUi());
        loadApps();
        refreshDisplays();
    }

    private View buildUi() {
        int pad = dp(18);

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(this);
        title.setText("Fold Display");
        title.setTextSize(26);
        title.setTextColor(Color.BLACK);
        title.setPadding(0, 0, 0, dp(8));
        body.addView(title);

        TextView sub = new TextView(this);
        sub.setText(
            "MVP: keep the monitor in its stable HDMI mode, then launch apps directly " +
            "onto Android's secondary display. This does NOT change Samsung/DeX rotation."
        );
        sub.setTextSize(15);
        body.addView(sub);

        displayInfo = new TextView(this);
        displayInfo.setTextSize(15);
        displayInfo.setPadding(0, dp(16), 0, dp(12));
        body.addView(displayInfo);

        Button refresh = new Button(this);
        refresh.setText("Refresh external display");
        refresh.setOnClickListener(v -> refreshDisplays());
        body.addView(refresh);

        TextView appLabel = new TextView(this);
        appLabel.setText("App to launch on external screen");
        appLabel.setTextSize(16);
        appLabel.setPadding(0, dp(18), 0, dp(6));
        body.addView(appLabel);

        appSpinner = new Spinner(this);
        body.addView(appSpinner);

        Button launch = new Button(this);
        launch.setText("Launch selected app on external");
        launch.setOnClickListener(v -> launchSelectedExternal());
        body.addView(launch);

        Button testNormal = new Button(this);
        testNormal.setText("Test external canvas");
        testNormal.setOnClickListener(v -> launchTestExternal(0));
        body.addView(testNormal);

        Button test90 = new Button(this);
        test90.setText("Test SOFTWARE 90° rotation");
        test90.setOnClickListener(v -> launchTestExternal(90));
        body.addView(test90);

        Button test270 = new Button(this);
        test270.setText("Test SOFTWARE 270° rotation");
        test270.setOnClickListener(v -> launchTestExternal(270));
        body.addView(test270);

        TextView note = new TextView(this);
        note.setText(
            "\nWhat the 90°/270° test proves:\n" +
            "• HDMI timing stays untouched.\n" +
            "• Only our own rendered content rotates.\n" +
            "• If the panel stays connected, we know DeX's display-mode change was the problem.\n\n" +
            "Next step after this test: add a Shizuku/ADB-powered per-display rotation option " +
            "for arbitrary apps if your Samsung build exposes the needed display command."
        );
        note.setTextSize(14);
        body.addView(note);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(body);
        return scroll;
    }

    private void loadApps() {
        PackageManager pm = getPackageManager();
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> infos = pm.queryIntentActivities(intent, 0);
        apps.clear();

        for (ResolveInfo ri : infos) {
            String pkg = ri.activityInfo.packageName;
            if (pkg.equals(getPackageName())) continue;
            String label = ri.loadLabel(pm).toString();
            apps.add(new AppEntry(label, pkg));
        }

        Collections.sort(apps, Comparator.comparing(a -> a.label.toLowerCase()));
        ArrayAdapter<AppEntry> adapter =
            new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, apps);
        appSpinner.setAdapter(adapter);
    }

    private void refreshDisplays() {
        Display[] displays = displayManager.getDisplays();
        StringBuilder s = new StringBuilder("Displays Android sees:\n");

        for (Display d : displays) {
            android.util.DisplayMetrics m = new android.util.DisplayMetrics();
            d.getRealMetrics(m);
            s.append("• ID ").append(d.getDisplayId())
             .append(d.getDisplayId() == Display.DEFAULT_DISPLAY ? " (phone)" : " (secondary)")
             .append(" — ")
             .append(m.widthPixels).append("×").append(m.heightPixels)
             .append(" rot=").append(d.getRotation())
             .append("\n");
        }

        Display ext = findExternalDisplay();
        if (ext == null) {
            s.append("\nNo usable secondary display found.");
        } else {
            s.append("\nUsing external display ID ").append(ext.getDisplayId()).append(".");
        }

        displayInfo.setText(s.toString());
    }

    private Display findExternalDisplay() {
        Display[] presentation =
            displayManager.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION);

        for (Display d : presentation) {
            if (d.getDisplayId() != Display.DEFAULT_DISPLAY && d.getState() != Display.STATE_OFF) {
                return d;
            }
        }

        for (Display d : displayManager.getDisplays()) {
            if (d.getDisplayId() != Display.DEFAULT_DISPLAY && d.getState() != Display.STATE_OFF) {
                return d;
            }
        }
        return null;
    }

    private void launchSelectedExternal() {
        if (apps.isEmpty()) {
            toast("No launcher apps found.");
            return;
        }

        Display ext = findExternalDisplay();
        if (ext == null) {
            toast("Connect the external screen first.");
            return;
        }

        AppEntry entry = (AppEntry) appSpinner.getSelectedItem();
        Intent launch = getPackageManager().getLaunchIntentForPackage(entry.packageName);
        if (launch == null) {
            toast("Couldn't get launch intent for " + entry.label);
            return;
        }

        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        try {
            ActivityOptions options = ActivityOptions.makeBasic();
            options.setLaunchDisplayId(ext.getDisplayId());
            startActivity(launch, options.toBundle());
            toast("Launching " + entry.label + " on display " + ext.getDisplayId());
        } catch (Throwable t) {
            toast("Android/Samsung blocked that launch: " + t.getClass().getSimpleName());
        }
    }

    private void launchTestExternal(int degrees) {
        Display ext = findExternalDisplay();
        if (ext == null) {
            toast("Connect the external screen first.");
            return;
        }

        try {
            final android.app.Presentation presentation =
                    new android.app.Presentation(this, ext);

            android.widget.FrameLayout root =
                    new android.widget.FrameLayout(this);
            root.setBackgroundColor(android.graphics.Color.BLACK);

            android.widget.TextView text =
                    new android.widget.TextView(this);
            text.setText(
                    "FOLD DISPLAY TEST\n\n" +
                    "Display ID: " + ext.getDisplayId() + "\n" +
                    "Software rotation: " + degrees + "°\n\n" +
                    "TAP THIS SCREEN"
            );
            text.setTextColor(android.graphics.Color.WHITE);
            text.setTextSize(28);
            text.setGravity(android.view.Gravity.CENTER);

            android.widget.FrameLayout.LayoutParams lp =
                    new android.widget.FrameLayout.LayoutParams(
                            android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                            android.widget.FrameLayout.LayoutParams.MATCH_PARENT
                    );

            root.addView(text, lp);

            text.setRotation((float) degrees);

            if (degrees == 90 || degrees == 270) {
                text.post(() -> {
                    float sx = (float) text.getHeight() /
                            Math.max(1, text.getWidth());
                    float sy = (float) text.getWidth() /
                            Math.max(1, text.getHeight());
                    float scale = Math.min(sx, sy);

                    text.setScaleX(scale);
                    text.setScaleY(scale);
                });
            }

            root.setOnClickListener(v -> {
                text.setText(
                        "TOUCH WORKS ✓\n\n" +
                        "Display ID: " + ext.getDisplayId() + "\n" +
                        "Rotation: " + degrees + "°"
                );
            });

            presentation.setContentView(root);
            presentation.show();

            toast("Presentation opened on display " + ext.getDisplayId());

        } catch (Throwable t) {
            toast("Presentation failed: " +
                    t.getClass().getSimpleName() + ": " +
                    String.valueOf(t.getMessage()));
        }
    }

    private void toast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_LONG).show();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
