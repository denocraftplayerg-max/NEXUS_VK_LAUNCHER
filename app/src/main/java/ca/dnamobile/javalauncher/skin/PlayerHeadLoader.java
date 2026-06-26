package ca.dnamobile.javalauncher.skin;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;

import ca.dnamobile.javalauncher.data.AccountStore;

public final class PlayerHeadLoader {

    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private PlayerHeadLoader() {}

    public static void loadInto(@NonNull Context context, @NonNull ImageView target,
            @NonNull AccountStore.Account account, @Nullable Runnable onLoaded) {
        if (account.offlineSkinPath == null) {
            if (onLoaded != null) onLoaded.run();
            return;
        }
        String path = account.offlineSkinPath;
        new Thread(() -> {
            Bitmap head = extractHead(path);
            MAIN.post(() -> {
                if (head != null) target.setImageBitmap(head);
                if (onLoaded != null) onLoaded.run();
            });
        }).start();
    }

    @Nullable
    private static Bitmap extractHead(String path) {
        try {
            Bitmap skin = BitmapFactory.decodeFile(path);
            if (skin == null || skin.getWidth() < 64) return null;
            int scale = skin.getWidth() / 64;
            int size = 8 * scale;
            Bitmap head = Bitmap.createBitmap(skin, 8 * scale, 8 * scale, size, size);
            skin.recycle();
            return head;
        } catch (Throwable ignored) { return null; }
    }
}
