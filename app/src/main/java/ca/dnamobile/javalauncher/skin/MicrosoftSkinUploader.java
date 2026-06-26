package ca.dnamobile.javalauncher.skin;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;

public final class MicrosoftSkinUploader {

    public interface Callback {
        void onSuccess();
        void onError(@NonNull String message);
    }

    private static final String NOT_AVAILABLE = "Microsoft skin upload is not available in this offline build.";

    private MicrosoftSkinUploader() {}

    public static void upload(@NonNull File skinFile, @NonNull String model,
            @Nullable String accessToken, @NonNull Callback callback) {
        callback.onError(NOT_AVAILABLE);
    }
}
