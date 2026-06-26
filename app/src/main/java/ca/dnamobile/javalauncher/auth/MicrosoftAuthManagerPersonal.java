package ca.dnamobile.javalauncher.auth;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import ca.dnamobile.javalauncher.data.AccountStore;

public final class MicrosoftAuthManagerPersonal {

    public interface Listener {
        void onSignedIn(@NonNull AccountStore.Account account);
        void onError(@NonNull String message);
    }

    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final String NOT_AVAILABLE = "Microsoft login is not available in this build.";

    @SuppressWarnings("unused") private final Context context;
    @SuppressWarnings("unused") private final AccountStore accountStore;
    @Nullable private Listener listener;

    public MicrosoftAuthManagerPersonal(@NonNull Context context, @NonNull AccountStore accountStore) {
        this.context = context.getApplicationContext();
        this.accountStore = accountStore;
    }

    public void setListener(@Nullable Listener listener) { this.listener = listener; }
    public void signIn() { deliverError(NOT_AVAILABLE); }
    public void signOut() {}
    public void refreshMicrosoftAccount() { deliverError(NOT_AVAILABLE); }
    public void dispose() { listener = null; }

    private void deliverError(@NonNull String message) {
        final Listener l = listener;
        if (l == null) return;
        if (Looper.myLooper() == Looper.getMainLooper()) l.onError(message);
        else MAIN.post(() -> l.onError(message));
    }
}
