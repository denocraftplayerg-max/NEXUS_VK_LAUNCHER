/*
 * Copyright (c) 2026 DNA Mobile Applications.
 * All rights reserved.
 *
 * This file is DroidBridge project code.
 * It is not part of Minecraft and does not grant rights to Minecraft,
 * Mojang, Microsoft, PojavLauncher, Zalith Launcher, or any third-party project.
 *
 * Files written entirely by DNA Mobile Applications are proprietary unless
 * a file header or separate license notice states otherwise.
 */

package ca.dnamobile.javalauncher.instance;

import android.content.Context;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import ca.dnamobile.javalauncher.feature.log.Logging;

// Copy default options.txt file for phone compatbilty on install this is copied automatically
public final class DefaultMinecraftOptionsInstaller {
    private static final String TAG = "DefaultOptions";
    private static final String DEFAULT_OPTIONS_ASSET = "minecraft_defaults/options.txt";

    private DefaultMinecraftOptionsInstaller() {
    }
    public static void installIfMissingForNewInstance(
            @NonNull Context context,
            @NonNull File gameDirectory
    ) {
        File optionsFile = new File(gameDirectory, "options.txt");
        if (optionsFile.exists()) {
            Logging.i(TAG, "options.txt already exists, not overwriting: " + optionsFile.getAbsolutePath());
            return;
        }

        File parent = optionsFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            Logging.i(TAG, "Unable to create game directory for default options: " + parent.getAbsolutePath());
            return;
        }

        try (InputStream input = context.getAssets().open(DEFAULT_OPTIONS_ASSET);
             FileOutputStream output = new FileOutputStream(optionsFile, false)) {

            byte[] buffer = new byte[16 * 1024];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }

            Logging.i(TAG, "Seeded default options.txt: " + optionsFile.getAbsolutePath());
        } catch (Throwable throwable) {
            Logging.e(TAG, "Failed to seed default options.txt: " + optionsFile.getAbsolutePath(), throwable);
        }
    }
}