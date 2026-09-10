package com.aiden.folddisplay;

import android.app.Activity;
import android.app.ActivityOptions;
import android.app.Presentation;
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
import android.widget.FrameLayout;
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

    private Presentation externalShell;
    private int shellRotation = 90;

    private final ArrayList<AppEntry> apps = new ArrayList<>();

    static class AppEntry {
        final String label;
        final String packageName;

        AppEntry(String label, String packageName) {
            this.label = label;
            this.packageName = packageName;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        displayManager =
                (DisplayManager) getSystemService(Context.DISPLAY_SERVICE);

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
        title.setTextSize(28);
        title.setTextColor(Color.BLACK);
        body.addView(title);

        TextView sub = new TextView(this);
        sub.setText(
                "Frankenstein Fold shell\n\n" +
                "The external touchscreen runs as its own persistent Android Presentation."
        );
        sub.setTextSize(15);
        sub.setPadding(0, dp(6), 0, dp(12));
        body.addView(sub);

        displayInfo = new TextView(this);
        displayInfo.setTextSize(15);
        displayInfo.setPadding(0, dp(8), 0, dp(12));
        body.addView(displayInfo);

        Button refresh = new Button(this);
        refresh.setText("Refresh displays");
        refresh.setOnClickListener(v -> refreshDisplays());
        body.addView(refresh);

        Button openShell = new Button(this);
        openShell.setText("OPEN FOLD SHELL");
        openShell.setOnClickListener(v -> openExternalShell());
        body.addView(openShell);

        Button closeShell = new Button(this);
        closeShell.setText("Close fold shell");
        closeShell.setOnClickListener(v -> closeExternalShell());
        body.addView(closeShell);

        TextView rotLabel = new TextView(this);
        rotLabel.setText("External software rotation");
        rotLabel.setTextSize(17);
        rotLabel.setPadding(0, dp(20), 0, dp(5));
        body.addView(rotLabel);

        Button rot0 = new Button(this);
        rot0.setText("0°");
        rot0.setOnClickListener(v -> {
            shellRotation = 0;
            reopenShell();
        });
        body.addView(rot0);

        Button rot90 = new Button(this);
        rot90.setText("90°");
        rot90.setOnClickListener(v -> {
            shellRotation = 90;
            reopenShell();
        });
        body.addView(rot90);

        Button rot270 = new Button(this);
        rot270.setText("270°");
        rot270.setOnClickListener(v -> {
            shellRotation = 270;
            reopenShell();
        });
        body.addView(rot270);

        TextView appLabel = new TextView(this);
        appLabel.setText("Phone-side app launch test");
        appLabel.setTextSize(17);
        appLabel.setPadding(0, dp(20), 0, dp(5));
        body.addView(appLabel);

        appSpinner = new Spinner(this);
        body.addView(appSpinner);

        Button launch = new Button(this);
        launch.setText("Try selected app on external");
        launch.setOnClickListener(v -> {
            if (apps.isEmpty()) return;

            AppEntry entry =
                    (AppEntry) appSpinner.getSelectedItem();

            launchApp(entry);
        });
        body.addView(launch);

        TextView note = new TextView(this);
        note.setText(
                "\nExternal shell behavior:\n" +
                "• Runs without Shizuku.\n" +
                "• HDMI timing is never rotated.\n" +
                "• Whole external UI rotates in software.\n" +
                "• Touch works directly on the second panel.\n" +
                "• App buttons try display 2 first.\n" +
                "• If Samsung blocks that, the app opens on the phone.\n"
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

        List<ResolveInfo> infos =
                pm.queryIntentActivities(intent, 0);

        apps.clear();

        for (ResolveInfo ri : infos) {
            String pkg = ri.activityInfo.packageName;

            if (pkg.equals(getPackageName())) continue;

            String label =
                    ri.loadLabel(pm).toString();

            apps.add(new AppEntry(label, pkg));
        }

        Collections.sort(
                apps,
                Comparator.comparing(
                        a -> a.label.toLowerCase()
                )
        );

        ArrayAdapter<AppEntry> adapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_dropdown_item,
                        apps
                );

        appSpinner.setAdapter(adapter);
    }

    private void refreshDisplays() {
        Display[] displays =
                displayManager.getDisplays();

        StringBuilder s =
                new StringBuilder("Displays Android sees:\n");

        for (Display d : displays) {
            android.util.DisplayMetrics m =
                    new android.util.DisplayMetrics();

            d.getRealMetrics(m);

            s.append("• ID ")
                    .append(d.getDisplayId())
                    .append(
                            d.getDisplayId()
                                    == Display.DEFAULT_DISPLAY
                                    ? " (phone)"
                                    : " (secondary)"
                    )
                    .append(" — ")
                    .append(m.widthPixels)
                    .append("×")
                    .append(m.heightPixels)
                    .append(" rot=")
                    .append(d.getRotation())
                    .append("\n");
        }

        Display ext = findExternalDisplay();

        if (ext == null) {
            s.append(
                    "\nNo usable secondary display found."
            );
        } else {
            s.append(
                    "\nExternal display ID: "
            ).append(ext.getDisplayId());

            s.append(
                    "\nShell rotation: "
            ).append(shellRotation).append("°");
        }

        displayInfo.setText(s.toString());
    }

    private Display findExternalDisplay() {
        Display[] presentation =
                displayManager.getDisplays(
                        DisplayManager.DISPLAY_CATEGORY_PRESENTATION
                );

        for (Display d : presentation) {
            if (
                    d.getDisplayId()
                            != Display.DEFAULT_DISPLAY
                            &&
                    d.getState()
                            != Display.STATE_OFF
            ) {
                return d;
            }
        }

        for (Display d :
                displayManager.getDisplays()) {

            if (
                    d.getDisplayId()
                            != Display.DEFAULT_DISPLAY
                            &&
                    d.getState()
                            != Display.STATE_OFF
            ) {
                return d;
            }
        }

        return null;
    }

    private void openExternalShell() {
        Display ext = findExternalDisplay();

        if (ext == null) {
            toast(
                    "Connect the external screen first."
            );
            return;
        }

        closeExternalShell();

        try {
            externalShell =
                    new Presentation(this, ext);

            Context pc =
                    externalShell.getContext();

            FrameLayout root =
                    new FrameLayout(pc);

            root.setBackgroundColor(
                    Color.rgb(12, 12, 12)
            );

            ScrollView scroll =
                    new ScrollView(pc);

            LinearLayout panel =
                    new LinearLayout(pc);

            panel.setOrientation(
                    LinearLayout.VERTICAL
            );

            panel.setPadding(
                    dpFor(pc, 22),
                    dpFor(pc, 22),
                    dpFor(pc, 22),
                    dpFor(pc, 22)
            );

            TextView title =
                    new TextView(pc);

            title.setText(
                    "FOLD DISPLAY"
            );

            title.setTextSize(30);
            title.setTextColor(Color.WHITE);
            title.setGravity(Gravity.CENTER);

            panel.addView(title);

            TextView info =
                    new TextView(pc);

            info.setText(
                    "External display " +
                    ext.getDisplayId() +
                    "\nRotation " +
                    shellRotation +
                    "°\n\nTap an app:"
            );

            info.setTextColor(
                    Color.LTGRAY
            );

            info.setTextSize(16);
            info.setGravity(Gravity.CENTER);

            info.setPadding(
                    0,
                    dpFor(pc, 8),
                    0,
                    dpFor(pc, 15)
            );

            panel.addView(info);

            for (AppEntry entry : apps) {
                Button b =
                        new Button(pc);

                b.setText(entry.label);

                b.setAllCaps(false);

                b.setOnClickListener(
                        v -> launchApp(entry)
                );

                panel.addView(b);
            }

            TextView footer =
                    new TextView(pc);

            footer.setText(
                    "\nFrankenstein Fold v0.2\n" +
                    "Touchscreen shell active"
            );

            footer.setTextColor(
                    Color.GRAY
            );

            footer.setGravity(
                    Gravity.CENTER
            );

            footer.setPadding(
                    0,
                    dpFor(pc, 20),
                    0,
                    dpFor(pc, 30)
            );

            panel.addView(footer);

            scroll.addView(panel);

            root.addView(
                    scroll,
                    new FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT
                    )
            );

            externalShell.setContentView(root);
            externalShell.show();

            applySoftwareRotation(
                    scroll,
                    shellRotation
            );

            toast(
                    "Fold shell opened on display " +
                    ext.getDisplayId()
            );

            refreshDisplays();

        } catch (Throwable t) {
            toast(
                    "Fold shell failed: " +
                    t.getClass().getSimpleName() +
                    ": " +
                    String.valueOf(t.getMessage())
            );
        }
    }

    private void applySoftwareRotation(
            View view,
            int degrees
    ) {
        view.setRotation(
                (float) degrees
        );

        if (
                degrees == 90 ||
                degrees == 270
        ) {
            view.post(() -> {
                float w = view.getWidth();
                float h = view.getHeight();

                if (w <= 0 || h <= 0) return;

                float scale =
                        Math.min(
                                h / w,
                                w / h
                        );

                view.setScaleX(scale);
                view.setScaleY(scale);
            });
        } else {
            view.setScaleX(1f);
            view.setScaleY(1f);
        }
    }

    private void reopenShell() {
        if (findExternalDisplay() == null) {
            toast(
                    "No external display."
            );
            return;
        }

        openExternalShell();
    }

    private void closeExternalShell() {
        if (externalShell != null) {
            try {
                externalShell.dismiss();
            } catch (Throwable ignored) {
            }

            externalShell = null;
        }
    }

    private void launchApp(AppEntry entry) {
        Intent launch =
                getPackageManager()
                        .getLaunchIntentForPackage(
                                entry.packageName
                        );

        if (launch == null) {
            toast(
                    "Couldn't launch " +
                    entry.label
            );
            return;
        }

        launch.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
        );

        Display ext =
                findExternalDisplay();

        if (ext != null) {
            try {
                ActivityOptions options =
                        ActivityOptions.makeBasic();

                options.setLaunchDisplayId(
                        ext.getDisplayId()
                );

                startActivity(
                        launch,
                        options.toBundle()
                );

                toast(
                        "Launching " +
                        entry.label +
                        " on external display"
                );

                return;

            } catch (Throwable ignored) {
            }
        }

        try {
            startActivity(launch);

            toast(
                    "Samsung blocked external launch — opened " +
                    entry.label +
                    " on phone"
            );

        } catch (Throwable t) {
            toast(
                    "Couldn't launch " +
                    entry.label
            );
        }
    }

    @Override
    protected void onDestroy() {
        closeExternalShell();
        super.onDestroy();
    }

    private void toast(String text) {
        Toast.makeText(
                this,
                text,
                Toast.LENGTH_LONG
        ).show();
    }

    private int dp(int value) {
        return Math.round(
                value *
                getResources()
                        .getDisplayMetrics()
                        .density
        );
    }

    private int dpFor(
            Context context,
            int value
    ) {
        return Math.round(
                value *
                context.getResources()
                        .getDisplayMetrics()
                        .density
        );
    }
}
