package com.aiden.folddisplay;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

public class ExternalCanvasActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN |
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );

        int degrees = getIntent().getIntExtra("rotation", 0);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.rgb(15, 15, 15));

        FrameLayout canvas = new FrameLayout(this);
        canvas.setBackgroundColor(Color.rgb(35, 35, 35));

        TextView t = new TextView(this);
        t.setText("FOLD DISPLAY\n\nSoftware rotation: " + degrees + "°\n\nTap anywhere to prove touch still works.");
        t.setTextColor(Color.WHITE);
        t.setTextSize(26);
        t.setGravity(Gravity.CENTER);

        canvas.addView(t, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ));

        root.addView(canvas, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ));

        root.setOnClickListener(v -> {
            t.setText("TOUCH WORKS ✓\n\nSoftware rotation: " + degrees + "°");
        });

        setContentView(root);

        // Rotate pixels only. We deliberately DO NOT request a new display orientation.
        // The HDMI/USB-C display mode therefore remains exactly as Android negotiated it.
        root.post(() -> {
            if (degrees == 90 || degrees == 270) {
                float w = root.getWidth();
                float h = root.getHeight();

                canvas.setPivotX(w / 2f);
                canvas.setPivotY(h / 2f);
                canvas.setRotation(degrees);

                // After rotating a landscape-sized View into portrait, swap its scale so
                // the rotated content fills as much of the physical panel as possible.
                if (w > 0 && h > 0) {
                    float scaleX = h / w;
                    float scaleY = w / h;
                    canvas.setScaleX(scaleX);
                    canvas.setScaleY(scaleY);
                }
            } else {
                canvas.setRotation(0);
                canvas.setScaleX(1f);
                canvas.setScaleY(1f);
            }
        });
    }
}
