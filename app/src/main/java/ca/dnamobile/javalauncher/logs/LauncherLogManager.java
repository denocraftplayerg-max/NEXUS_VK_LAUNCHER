package ca.dnamobile.javalauncher.logs;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class LauncherLogManager {

    private static final String LOG_DIR = "launcher_logs";
    private static volatile LauncherLogManager instance;

    private final File logFile;

    private LauncherLogManager(@NonNull Context context) {
        File dir = new File(context.getFilesDir(), LOG_DIR);
        dir.mkdirs();
        logFile = new File(dir, "launcher.log");
    }

    @NonNull public static LauncherLogManager get(@NonNull Context context) {
        if (instance == null) {
            synchronized (LauncherLogManager.class) {
                if (instance == null) instance = new LauncherLogManager(context);
            }
        }
        return instance;
    }

    public void log(@NonNull String tag, @NonNull String message) {
        String ts = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date());
        String line = ts + " [" + tag + "] " + message + "\n";
        try (FileWriter fw = new FileWriter(logFile, true)) { fw.write(line); } catch (IOException ignored) {}
    }

    public void clear() { logFile.delete(); }

    @Nullable public File getLogFile() { return logFile.exists() ? logFile : null; }
}
