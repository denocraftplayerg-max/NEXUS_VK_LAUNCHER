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

package ca.dnamobile.javalauncher.renderer;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Environment;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Locale;

/**
 * MobileGlues optional configuration helper.
 *
 * Safer approach than MANAGE_EXTERNAL_STORAGE:
 * - Prefer opening the MobileGlues plugin app for users who want to edit settings there.
 * - Optionally allow the user to pick the MG folder with SAF and persist that tree Uri.
 * - Fall back to /sdcard/MG/config.json when readable.
 */
public final class MobileGluesConfigHelper {
    private static final String CONFIG_DIR_NAME = "MG";
    private static final String CONFIG_FILE_NAME = "config.json";

    private static final String PREFS_NAME = "mobile_glues_config";
    private static final String PREF_TREE_URI = "mg_tree_uri";

    private static final String[] MOBILE_GLUES_PACKAGES = new String[]{
            "com.fcl.plugin.mobileglues",
            "com.fcl.plugin.renderer.mobileglues",
            "com.mio.plugin.renderer.mobileglues"
    };

    private MobileGluesConfigHelper() {
    }

    public static boolean isMobileGluesRenderer(@Nullable RendererInterface renderer) {
        if (renderer == null) return false;
        String combined = (safe(renderer.getUniqueIdentifier()) + " "
                + safe(renderer.getRendererName()) + " "
                + safe(renderer.getRendererId()) + " "
                + safe(renderer.getRendererLibrary()))
                .toLowerCase(Locale.ROOT);
        return combined.contains("mobileglues") || combined.contains("mobile glues");
    }

    @NonNull
    public static File getConfigDirectory() {
        return new File(Environment.getExternalStorageDirectory(), CONFIG_DIR_NAME);
    }

    @NonNull
    public static File getConfigFile() {
        return new File(getConfigDirectory(), CONFIG_FILE_NAME);
    }

    public static boolean hasStorageAccess(@NonNull Context context) {
        return canReadConfigFile() || hasSelectedConfigTree(context);
    }

    public static boolean shouldShowStorageAccessPrompt(@NonNull Context context,
                                                        @Nullable RendererInterface renderer) {
        return isMobileGluesRenderer(renderer) && !hasStorageAccess(context);
    }

    public static boolean canReadConfigFile() {
        File configFile = getConfigFile();
        return configFile.isFile() && configFile.canRead();
    }

    public static boolean hasSelectedConfigTree(@NonNull Context context) {
        return getSelectedConfigTreeUri(context) != null;
    }

    public static void setSelectedConfigTreeUri(@NonNull Context context, @Nullable Uri treeUri) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(PREF_TREE_URI, treeUri != null ? treeUri.toString() : null).apply();
    }

    @Nullable
    public static Uri getSelectedConfigTreeUri(@NonNull Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String value = prefs.getString(PREF_TREE_URI, null);
        if (value == null || value.trim().isEmpty()) return null;
        try {
            return Uri.parse(value);
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Nullable
    public static DocumentFile getSelectedConfigTree(@NonNull Context context) {
        Uri treeUri = getSelectedConfigTreeUri(context);
        if (treeUri == null) return null;
        try {
            return DocumentFile.fromTreeUri(context, treeUri);
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Nullable
    private static DocumentFile findSafConfigFile(@NonNull Context context) {
        DocumentFile root = getSelectedConfigTree(context);
        if (root == null || !root.exists()) return null;

        DocumentFile direct = root.findFile(CONFIG_FILE_NAME);
        if (direct != null && direct.isFile()) return direct;

        DocumentFile mgDir = root.findFile(CONFIG_DIR_NAME);
        if (mgDir != null && mgDir.isDirectory()) {
            DocumentFile nested = mgDir.findFile(CONFIG_FILE_NAME);
            if (nested != null && nested.isFile()) return nested;
        }

        return null;
    }

    @NonNull
    public static Intent buildStorageAccessIntent(@NonNull Context context) {
        Intent pluginIntent = buildOpenPluginIntent(context);
        if (pluginIntent != null) return pluginIntent;

        Intent fallback = new Intent(Intent.ACTION_MAIN);
        fallback.addCategory(Intent.CATEGORY_LAUNCHER);
        fallback.setPackage(MOBILE_GLUES_PACKAGES[0]);
        fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return fallback;
    }

    @Nullable
    public static Intent buildOpenPluginIntent(@NonNull Context context) {
        PackageManager packageManager = context.getPackageManager();
        for (String packageName : MOBILE_GLUES_PACKAGES) {
            try {
                Intent intent = packageManager.getLaunchIntentForPackage(packageName);
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    return intent;
                }
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    @NonNull
    public static String buildSettingsSummary(@NonNull Context context, @Nullable RendererInterface renderer) {
        if (!isMobileGluesRenderer(renderer)) {
            return "No external MobileGlues configuration is used by this renderer.";
        }

        File configFile = getConfigFile();
        String sourceLabel = "Direct path: " + configFile.getAbsolutePath();
        String jsonText = null;

        if (canReadConfigFile()) {
            try {
                jsonText = readFile(configFile);
            } catch (Throwable throwable) {
                return "MobileGlues config path: " + configFile.getAbsolutePath()
                        + "\nThe config exists, but JavaLauncher could not parse it: " + throwable.getMessage();
            }
        } else {
            DocumentFile safFile = findSafConfigFile(context);
            if (safFile != null) {
                try {
                    jsonText = readDocumentFile(context, safFile);
                    sourceLabel = "Selected SAF config: " + safe(safFile.getUri() != null ? safFile.getUri().toString() : "");
                } catch (Throwable throwable) {
                    return sourceLabel
                            + "\nThe SAF config exists, but JavaLauncher could not parse it: " + throwable.getMessage();
                }
            }
        }

        if (jsonText == null) {
            return "MobileGlues config path: " + configFile.getAbsolutePath()
                    + "\nSelected SAF folder: " + safe(getSelectedConfigTreeUri(context) != null ? getSelectedConfigTreeUri(context).toString() : "None")
                    + "\nJavaLauncher cannot currently read a MobileGlues config from either location."
                    + "\nUse Open MobileGlues App or Choose MobileGlues Folder to test settings parity.";
        }

        try {
            JSONObject json = new JSONObject(jsonText);
            StringBuilder out = new StringBuilder();
            out.append(sourceLabel).append('\n');
            appendKnown(out, json, "enableANGLE", "ANGLE");
            appendKnown(out, json, "enableNoError", "Ignore GL errors");
            appendKnown(out, json, "enableExtComputeShader", "ARB_compute_shader");
            appendKnown(out, json, "enableExtTimerQuery", "timer_query");
            appendKnown(out, json, "enableExtDirectStateAccess", "direct_state_access");
            appendKnown(out, json, "maxGlslCacheSize", "GLSL cache MB");
            appendKnown(out, json, "multidrawMode", "MultiDraw mode");
            appendKnown(out, json, "angleDepthClearFixMode", "ANGLE depth clear fix");
            appendKnown(out, json, "bufferCoherentAsFlush", "Coherent buffer as flush");
            appendKnown(out, json, "customGLVersion", "Custom GL version");
            appendKnown(out, json, "fsr1Setting", "FSR1");
            appendKnown(out, json, "hideMGEnvLevel", "Hide environment level");

            boolean hasUnknown = false;
            Iterator<String> keys = json.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                if (isKnownKey(key)) continue;
                if (!hasUnknown) {
                    out.append("Other values:\n");
                    hasUnknown = true;
                }
                out.append("• ").append(key).append(" = ").append(json.opt(key)).append('\n');
            }
            return out.toString().trim();
        } catch (Throwable throwable) {
            return sourceLabel
                    + "\nThe config exists, but JavaLauncher could not parse it: " + throwable.getMessage();
        }
    }

    private static void appendKnown(@NonNull StringBuilder out,
                                    @NonNull JSONObject json,
                                    @NonNull String key,
                                    @NonNull String label) {
        if (!json.has(key)) return;
        out.append("• ").append(label).append(" = ").append(json.opt(key)).append('\n');
    }

    private static boolean isKnownKey(@NonNull String key) {
        return "enableANGLE".equals(key)
                || "enableNoError".equals(key)
                || "enableExtGL43".equals(key)
                || "enableExtComputeShader".equals(key)
                || "enableExtTimerQuery".equals(key)
                || "enableExtDirectStateAccess".equals(key)
                || "maxGlslCacheSize".equals(key)
                || "multidrawMode".equals(key)
                || "angleDepthClearFixMode".equals(key)
                || "bufferCoherentAsFlush".equals(key)
                || "customGLVersion".equals(key)
                || "fsr1Setting".equals(key)
                || "hideMGEnvLevel".equals(key);
    }

    @NonNull
    private static String readFile(@NonNull File file) throws Exception {
        try (FileInputStream input = new FileInputStream(file)) {
            byte[] buffer = new byte[(int) file.length()];
            int offset = 0;
            while (offset < buffer.length) {
                int read = input.read(buffer, offset, buffer.length - offset);
                if (read < 0) break;
                offset += read;
            }
            return new String(buffer, 0, offset, StandardCharsets.UTF_8);
        }
    }

    @NonNull
    private static String readDocumentFile(@NonNull Context context, @NonNull DocumentFile file) throws Exception {
        ContentResolver resolver = context.getContentResolver();
        try (InputStream input = resolver.openInputStream(file.getUri());
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            if (input == null) throw new IllegalStateException("Could not open selected MobileGlues config.");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
            return output.toString(StandardCharsets.UTF_8.name());
        }
    }

    @NonNull
    private static String safe(@Nullable String value) {
        return value == null ? "" : value;
    }
}
