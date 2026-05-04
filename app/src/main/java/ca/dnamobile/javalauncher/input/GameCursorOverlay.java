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

package ca.dnamobile.javalauncher.input;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.view.Choreographer;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import ca.dnamobile.javalauncher.controls.ControlsPreferences;

import org.lwjgl.glfw.CallbackBridge;

/**
 * Visible software cursor used for Minecraft menus.
 *
 * Important touch rule:
 * This class must never be a touch target. Earlier versions used a full-screen
 * View and then a small moving View. Both can still break Android hit testing:
 * when a child View is above the game surface, Android does not keep searching
 * lower siblings after that child rejects ACTION_DOWN.
 *
 * The Zalith-style safe approach is to keep this View gone/1x1 and draw the
 * cursor through the parent ViewGroupOverlay. ViewGroupOverlay is visual only,
 * so touchscreen taps, right-side camera swipes, Touch Controller buttons, and
 * Minecraft menu clicks continue to reach the game surface underneath.
 */
public final class GameCursorOverlay extends View {
    private static final float CURSOR_CANVAS_DP = 42f;

    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path cursorPath = new Path();
    private final GamepadMappingStore mappingStore;

    @Nullable private ViewGroup overlayParent;
    private boolean drawableAdded;
    private boolean removed;
    private boolean cursorVisible;

    private final Drawable cursorDrawable = new Drawable() {
        @Override
        public void draw(@NonNull Canvas canvas) {
            if (!cursorVisible) return;

            canvas.save();
            canvas.translate(getBounds().left, getBounds().top);
            canvas.drawPath(cursorPath, fillPaint);
            canvas.drawPath(cursorPath, strokePaint);
            canvas.restore();
        }

        @Override
        public void setAlpha(int alpha) {
            fillPaint.setAlpha(alpha);
            strokePaint.setAlpha(alpha);
            invalidateSelf();
        }

        @Override
        public void setColorFilter(@Nullable ColorFilter colorFilter) {
            fillPaint.setColorFilter(colorFilter);
            strokePaint.setColorFilter(colorFilter);
            invalidateSelf();
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    };

    private final Choreographer.FrameCallback frameCallback = new Choreographer.FrameCallback() {
        @Override
        public void doFrame(long frameTimeNanos) {
            updateFromBridge();
            if (!removed) {
                Choreographer.getInstance().postFrameCallback(this);
            }
        }
    };

    public GameCursorOverlay(@NonNull Context context) {
        super(context);
        mappingStore = GamepadMappingStore.get(context);

        // This view is only a lifecycle owner for the overlay drawable.
        // Keep it out of layout hit testing completely.
        setVisibility(GONE);
        setWillNotDraw(true);
        setClickable(false);
        setLongClickable(false);
        setFocusable(false);
        setFocusableInTouchMode(false);
        setHapticFeedbackEnabled(false);
        setSoundEffectsEnabled(false);
        setEnabled(false);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);

        fillPaint.setColor(Color.WHITE);
        fillPaint.setStyle(Paint.Style.FILL);

        strokePaint.setColor(Color.BLACK);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(dp(1.5f));

        buildPath();

        Choreographer.getInstance().postFrameCallback(frameCallback);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        setVisibility(GONE);
        attachDrawableToParent();
    }

    @Override
    protected void onDetachedFromWindow() {
        detachDrawableFromParent();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Stay effectively non-existent for hit testing/layout.
        setMeasuredDimension(1, 1);
    }

    public void removeSelf() {
        removed = true;
        cursorVisible = false;
        cursorDrawable.setBounds(0, 0, 0, 0);
        cursorDrawable.invalidateSelf();
        Choreographer.getInstance().removeFrameCallback(frameCallback);
        detachDrawableFromParent();
    }

    private void attachDrawableToParent() {
        if (drawableAdded) return;
        if (!(getParent() instanceof ViewGroup)) return;

        overlayParent = (ViewGroup) getParent();
        overlayParent.getOverlay().add(cursorDrawable);
        drawableAdded = true;
    }

    private void detachDrawableFromParent() {
        if (!drawableAdded || overlayParent == null) return;

        overlayParent.getOverlay().remove(cursorDrawable);
        drawableAdded = false;
        overlayParent = null;
    }

    private void buildPath() {
        float s = dp(1f);
        cursorPath.reset();

        // Classic simple arrow pointer.
        cursorPath.moveTo(0f, 0f);
        cursorPath.lineTo(0f, 22f * s);
        cursorPath.lineTo(6f * s, 16f * s);
        cursorPath.lineTo(10f * s, 27f * s);
        cursorPath.lineTo(15f * s, 25f * s);
        cursorPath.lineTo(11f * s, 14f * s);
        cursorPath.lineTo(19f * s, 14f * s);
        cursorPath.close();
    }

    private void updateFromBridge() {
        attachDrawableToParent();

        boolean menuMode = !mappingStore.isForceGameMode() && !CallbackBridge.isGrabbing();
        boolean physicalPointerConnected = hasPhysicalPointerDevice();

        // The virtual mouse preference only means "draw a cursor when Minecraft is
        // in a GUI/menu". It must not force a cursor while the game is grabbing
        // mouse input, because grabbed mode means the pointer is being used as
        // camera/attack input inside the world. It also hides when a real mouse,
        // trackpad, or trackball is connected because the hardware pointer should
        // become the only visible cursor.
        boolean showTouchVirtualCursor = ControlsPreferences.isVirtualMouseEnabled(getContext())
                && menuMode
                && !physicalPointerConnected;
        boolean showControllerMenuCursor = mappingStore.isShowCursorOverlay()
                && menuMode
                && !physicalPointerConnected;
        boolean shouldShow = showTouchVirtualCursor || showControllerMenuCursor;

        cursorVisible = shouldShow;
        if (!shouldShow || overlayParent == null) {
            cursorDrawable.setBounds(0, 0, 0, 0);
            cursorDrawable.invalidateSelf();
            return;
        }

        int rootWidth = Math.max(1, overlayParent.getWidth());
        int rootHeight = Math.max(1, overlayParent.getHeight());
        int cursorSize = Math.max(1, Math.round(dp(CURSOR_CANVAS_DP)));

        float bridgeWidth = Math.max(1f, CallbackBridge.windowWidth > 0
                ? CallbackBridge.windowWidth : CallbackBridge.physicalWidth);
        float bridgeHeight = Math.max(1f, CallbackBridge.windowHeight > 0
                ? CallbackBridge.windowHeight : CallbackBridge.physicalHeight);

        float drawX = CallbackBridge.mouseX * rootWidth / bridgeWidth;
        float drawY = CallbackBridge.mouseY * rootHeight / bridgeHeight;

        drawX = clamp(drawX, 0f, rootWidth - cursorSize);
        drawY = clamp(drawY, 0f, rootHeight - cursorSize);

        int left = Math.round(drawX);
        int top = Math.round(drawY);
        cursorDrawable.setBounds(left, top, left + cursorSize, top + cursorSize);
        cursorDrawable.invalidateSelf();
    }

    private static boolean hasPhysicalPointerDevice() {
        try {
            for (int id : android.view.InputDevice.getDeviceIds()) {
                android.view.InputDevice device = android.view.InputDevice.getDevice(id);
                if (isRealExternalPointerDevice(device)) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    /**
     * Real USB/Bluetooth mice should hide the launcher software cursor.
     * Controllers must not. Some Android handhelds and Bluetooth pads expose
     * misleading pointer-like sources, especially TOUCHPAD/MOUSE, even though
     * they are still the game controller. The previous detector trusted those
     * sources too much, so attaching a controller could hide the virtual cursor.
     *
     * Keep this intentionally conservative: only a confident external mouse
     * source hides the virtual cursor. A controller/touchpad-like device should
     * not stop the user from toggling the on-screen cursor with the touch button.
     */
    private static boolean isRealExternalPointerDevice(@Nullable android.view.InputDevice device) {
        if (device == null) return false;

        int sources = device.getSources();
        if (isControllerLikeDevice(device, sources)) return false;

        boolean hasMouseSource = (sources & android.view.InputDevice.SOURCE_MOUSE) == android.view.InputDevice.SOURCE_MOUSE
                || (sources & android.view.InputDevice.SOURCE_MOUSE_RELATIVE) == android.view.InputDevice.SOURCE_MOUSE_RELATIVE;
        if (!hasMouseSource) return false;

        String name = safeLower(device.getName());
        if (looksLikeControllerName(name) || looksLikeVirtualTouchName(name)) return false;

        // isExternal filters out Android's built-in/virtual pointer helpers.
        // Some dongles are named as generic receivers, so accept either an
        // external device or a strongly mouse-like name.
        return device.isExternal() || looksLikeMouseName(name);
    }

    private static boolean isControllerLikeDevice(@NonNull android.view.InputDevice device, int sources) {
        if ((sources & android.view.InputDevice.SOURCE_GAMEPAD) == android.view.InputDevice.SOURCE_GAMEPAD
                || (sources & android.view.InputDevice.SOURCE_JOYSTICK) == android.view.InputDevice.SOURCE_JOYSTICK) {
            return true;
        }
        return looksLikeControllerName(safeLower(device.getName()));
    }

    private static boolean looksLikeControllerName(@NonNull String name) {
        return name.contains("controller")
                || name.contains("gamepad")
                || name.contains("joystick")
                || name.contains("xbox")
                || name.contains("dualshock")
                || name.contains("dualsense")
                || name.contains("playstation")
                || name.contains("8bitdo")
                || name.contains("gamesir")
                || name.contains("ipega")
                || name.contains("backbone")
                || name.contains("kishi")
                || name.contains("odin")
                || name.contains("retroid")
                || name.contains("anbernic")
                || name.contains("aya")
                || name.contains("gpd")
                || name.contains("legion go")
                || name.contains("steam deck")
                || name.contains("razer raiju")
                || name.contains("moga");
    }

    private static boolean looksLikeVirtualTouchName(@NonNull String name) {
        return name.contains("virtual")
                || name.contains("touch")
                || name.contains("touchpad")
                || name.contains("touchscreen")
                || name.contains("touch mapping")
                || name.contains("touchmapping")
                || name.contains("uinput")
                || name.contains("gpio")
                || name.contains("keypad");
    }

    private static boolean looksLikeMouseName(@NonNull String name) {
        return name.contains("mouse")
                || name.contains("trackball")
                || name.contains("trackpad")
                || name.contains("receiver")
                || name.contains("logitech")
                || name.contains("razer")
                || name.contains("microsoft")
                || name.contains("hid-compliant");
    }

    @NonNull
    private static String safeLower(@Nullable String value) {
        return value == null ? "" : value.toLowerCase(java.util.Locale.US);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return false;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // Drawing happens through the parent ViewGroupOverlay instead.
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
