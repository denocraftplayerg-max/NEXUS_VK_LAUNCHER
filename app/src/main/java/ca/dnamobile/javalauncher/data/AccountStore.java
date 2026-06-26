package ca.dnamobile.javalauncher.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class AccountStore {

    private static final String PREFS_NAME = "nexusvk_accounts";
    private static final String KEY_ACTIVE_ACCOUNT_ID = "active_account_id";
    private static final String KEY_OFFLINE_ACCOUNTS = "offline_accounts";

    public static final class Account {
        public final String accountId;
        public final String username;
        @Nullable public final String minecraftAccessToken;
        @Nullable public final String skinUrl;
        @Nullable public final String offlineSkinPath;
        @Nullable public final String offlineSkinModel;
        private final boolean microsoftAccount;
        private final boolean hasMinecraftSession;

        public Account(@NonNull String accountId, @NonNull String username,
                @Nullable String minecraftAccessToken, @Nullable String skinUrl,
                @Nullable String offlineSkinPath, @Nullable String offlineSkinModel,
                boolean microsoftAccount, boolean hasMinecraftSession) {
            this.accountId = accountId; this.username = username;
            this.minecraftAccessToken = minecraftAccessToken; this.skinUrl = skinUrl;
            this.offlineSkinPath = offlineSkinPath; this.offlineSkinModel = offlineSkinModel;
            this.microsoftAccount = microsoftAccount; this.hasMinecraftSession = hasMinecraftSession;
        }

        public static Account offlineAccount(@NonNull String username) {
            return new Account(
                "offline-" + UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes()),
                username, null, null, null, null, false, false);
        }

        @NonNull public String getBestDisplayName() { return username; }
        public boolean isMicrosoftAccount() { return microsoftAccount; }
        public boolean isOfflineAccount() { return !microsoftAccount; }
        public boolean hasMinecraftSession() { return hasMinecraftSession; }
        public boolean hasOfflineSkin() { return isOfflineAccount() && offlineSkinPath != null && !offlineSkinPath.isEmpty(); }

        @NonNull JSONObject toJson() throws Exception {
            JSONObject obj = new JSONObject();
            obj.put("accountId", accountId); obj.put("username", username);
            if (minecraftAccessToken != null) obj.put("minecraftAccessToken", minecraftAccessToken);
            if (skinUrl != null) obj.put("skinUrl", skinUrl);
            if (offlineSkinPath != null) obj.put("offlineSkinPath", offlineSkinPath);
            if (offlineSkinModel != null) obj.put("offlineSkinModel", offlineSkinModel);
            obj.put("microsoftAccount", microsoftAccount);
            obj.put("hasMinecraftSession", hasMinecraftSession);
            return obj;
        }

        @Nullable static Account fromJson(@NonNull JSONObject obj) {
            try {
                return new Account(obj.getString("accountId"), obj.getString("username"),
                    obj.optString("minecraftAccessToken", null), obj.optString("skinUrl", null),
                    obj.optString("offlineSkinPath", null), obj.optString("offlineSkinModel", null),
                    obj.optBoolean("microsoftAccount", false), obj.optBoolean("hasMinecraftSession", false));
            } catch (Throwable ignored) { return null; }
        }
    }

    private final Context context;
    private final SharedPreferences prefs;

    public AccountStore(@NonNull Context context) {
        this.context = context.getApplicationContext();
        this.prefs = this.context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /** Always true — offline accounts are unlocked without Microsoft login. */
    public boolean hasMicrosoftLoginCompletedOnce() { return true; }

    /** Always false — Microsoft accounts are not supported in this build. */
    public boolean hasStoredMicrosoftAccount() { return false; }

    @Nullable public Account load() {
        String activeId = prefs.getString(KEY_ACTIVE_ACCOUNT_ID, null);
        ArrayList<Account> offline = listOfflineAccounts();
        if (activeId != null) { for (Account a : offline) { if (activeId.equals(a.accountId)) return a; } }
        if (!offline.isEmpty()) return offline.get(0);
        Account def = Account.offlineAccount("Player");
        saveOfflineAccount(def); setActiveAccount(def);
        return def;
    }

    @Nullable public Account loadLastMicrosoftAccount() { return null; }

    public void setActiveAccount(@NonNull Account account) {
        prefs.edit().putString(KEY_ACTIVE_ACCOUNT_ID, account.accountId).apply();
    }

    public void activateOfflineAccount(@NonNull String accountId) {
        for (Account a : listOfflineAccounts()) {
            if (a.accountId.equals(accountId)) { setActiveAccount(a); return; }
        }
    }

    public void useLastMicrosoftAccount() {
        throw new IllegalStateException("Microsoft accounts are not available in this build.");
    }

    @NonNull public ArrayList<Account> listOfflineAccounts() {
        ArrayList<Account> list = new ArrayList<>();
        String json = prefs.getString(KEY_OFFLINE_ACCOUNTS, "[]");
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                Account a = Account.fromJson(arr.getJSONObject(i));
                if (a != null) list.add(a);
            }
        } catch (Throwable ignored) {}
        return list;
    }

    public void saveOfflineAccount(@NonNull Account account) {
        ArrayList<Account> accounts = listOfflineAccounts();
        boolean found = false;
        for (int i = 0; i < accounts.size(); i++) {
            if (accounts.get(i).accountId.equals(account.accountId)) { accounts.set(i, account); found = true; break; }
        }
        if (!found) accounts.add(account);
        persistOfflineAccounts(accounts);
    }

    public void deleteOfflineAccount(@NonNull String accountId) {
        for (Account a : listOfflineAccounts()) {
            if (a.accountId.equals(accountId) && a.offlineSkinPath != null) new File(a.offlineSkinPath).delete();
        }
        ArrayList<Account> accounts = listOfflineAccounts();
        accounts.removeIf(a -> a.accountId.equals(accountId));
        persistOfflineAccounts(accounts);
        String activeId = prefs.getString(KEY_ACTIVE_ACCOUNT_ID, null);
        if (accountId.equals(activeId)) prefs.edit().remove(KEY_ACTIVE_ACCOUNT_ID).apply();
    }

    @NonNull public Account saveOrUpdateOfflineAccount(@Nullable String existingId, @NonNull String name,
            @Nullable Uri skinUri, boolean clearSkin) throws Exception {
        String id = existingId != null ? existingId
                : "offline-" + UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes());
        Account existing = null;
        for (Account a : listOfflineAccounts()) { if (a.accountId.equals(id)) { existing = a; break; } }
        String skinPath = existing != null ? existing.offlineSkinPath : null;
        String skinModel = existing != null ? existing.offlineSkinModel : null;
        if (clearSkin) {
            if (skinPath != null) new File(skinPath).delete();
            skinPath = null; skinModel = null;
        }
        if (skinUri != null) {
            File skinDir = new File(context.getFilesDir(), "skins");
            skinDir.mkdirs();
            File skinFile = new File(skinDir, "offline_" + id + ".png");
            try (InputStream in = context.getContentResolver().openInputStream(skinUri);
                 FileOutputStream out = new FileOutputStream(skinFile)) {
                if (in == null) throw new IllegalStateException("Cannot read skin file.");
                byte[] buf = new byte[8192]; int n;
                while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
            }
            skinPath = skinFile.getAbsolutePath();
            skinModel = detectSkinModel(skinFile);
        }
        Account account = new Account(id, name, null, null, skinPath, skinModel, false, false);
        saveOfflineAccount(account); setActiveAccount(account);
        return account;
    }

    private void persistOfflineAccounts(@NonNull List<Account> accounts) {
        JSONArray arr = new JSONArray();
        for (Account a : accounts) { try { arr.put(a.toJson()); } catch (Throwable ignored) {} }
        prefs.edit().putString(KEY_OFFLINE_ACCOUNTS, arr.toString()).apply();
    }

    @NonNull private static String detectSkinModel(@NonNull File skinFile) {
        try {
            android.graphics.Bitmap bmp = android.graphics.BitmapFactory.decodeFile(skinFile.getAbsolutePath());
            if (bmp == null || bmp.getWidth() < 64 || bmp.getHeight() < 64) { if (bmp != null) bmp.recycle(); return "classic"; }
            int pixel = bmp.getPixel(50, 16); bmp.recycle();
            return ((pixel >> 24) & 0xFF) == 0 ? "slim" : "classic";
        } catch (Throwable ignored) { return "classic"; }
    }
}
