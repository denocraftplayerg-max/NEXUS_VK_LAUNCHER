/*
 * Copyright (c) 2026 DNA Mobile Applications.
 * All rights reserved.
 */

package ca.dnamobile.javalauncher.skin;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

/**
 * Placeholder view that renders a simple 2-D representation of the player
 * skin/head while the full 3-D model renderer is not yet available.
 */
public class PlayerModelPreviewView extends View {

    private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public PlayerModelPreviewView(Context context) {
        super(context);
    }

    public PlayerModelPreviewView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public PlayerModelPreviewView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;

        mPaint.setColor(Color.parseColor("#FF1976D2"));
        float headSize = Math.min(w, h) * 0.35f;
        float cx = w / 2f;
        float headTop = h * 0.05f;
        canvas.drawRoundRect(cx - headSize / 2f, headTop,
                cx + headSize / 2f, headTop + headSize,
                headSize * 0.15f, headSize * 0.15f, mPaint);

        mPaint.setColor(Color.parseColor("#FF1565C0"));
        float bodyW = headSize * 0.8f;
        float bodyH = headSize * 1.1f;
        float bodyTop = headTop + headSize + h * 0.02f;
        canvas.drawRoundRect(cx - bodyW / 2f, bodyTop,
                cx + bodyW / 2f, bodyTop + bodyH,
                6f, 6f, mPaint);
    }

    /**
     * Update the preview with a skin bitmap. No-op in the stub implementation.
     */
    public void setSkinBitmap(@Nullable android.graphics.Bitmap skin) {
    }

    /**
     * Set the model type (slim / classic). No-op in the stub implementation.
     */
    public void setSkinModelType(@Nullable SkinModelType modelType) {
    }
}
