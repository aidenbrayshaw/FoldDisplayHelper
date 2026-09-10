FOLD DISPLAY HELPER — MVP 0.1

Purpose
-------
This is a standalone Android helper app, NOT a Cemu patch.

It:
1. Detects your external Android/DeX/presentation display.
2. Shows its display ID, resolution, and Android rotation.
3. Lets you choose a launcher app and asks Android to launch it directly on the external display.
4. Has 0°, 90°, and 270° SOFTWARE-rotation test screens.

Important limitation
--------------------
The 90°/270° test rotates only Fold Display's own pixels. A normal Android app cannot
arbitrarily rotate another app's surface system-wide.

If the software-rotation test stays connected while Samsung DeX's 90° rotation disconnects
the panel, that's useful: it proves we should leave HDMI timing alone and do any future
rotation through a shell/Shizuku/privileged path instead.

Build in Codespaces / Linux
---------------------------
From this folder:

  gradle wrapper --gradle-version 8.9
  ./gradlew assembleDebug

APK:
  app/build/outputs/apk/debug/app-debug.apk

If "gradle" isn't installed, open this folder in Android Studio and build the app there,
or copy these files into your existing Android build environment.

Test order
----------
1. Leave DeX/external display rotation at the setting that DOES NOT disconnect.
2. Connect the screen.
3. Open Fold Display.
4. Verify it lists a secondary display ID.
5. Tap "Test SOFTWARE 90° rotation".
6. If orientation is backwards, try 270°.
7. Tap the external screen and verify the test changes to "TOUCH WORKS".
8. Then try "Launch selected app on external".

Next planned stage
------------------
If stage 1 works, add optional ADB/Shizuku-backed per-display rotation for arbitrary apps,
but only after checking the actual `wm help` output on your Samsung Android build.
