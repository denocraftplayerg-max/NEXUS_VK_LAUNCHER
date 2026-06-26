package ca.dnamobile.javalauncher.auth;

public final class MicrosoftAuthConfigPersonal {
    private MicrosoftAuthConfigPersonal() {}

    /** Always false — Microsoft auth is not configured in this offline build. */
    public static boolean isConfigured() {
        return false;
    }
}
