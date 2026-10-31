package com.example.agronode;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class MiniGraphView extends View {

    private final ArrayList<Float> data1 = new ArrayList<>();
    private final ArrayList<Float> data2 = new ArrayList<>();

    private float minValue = 0;
    private float maxValue = 100;

    private final Paint backgroundPaint = new Paint();
    private final Paint gridPaint = new Paint();
    private final Paint linePaint1 = new Paint();
    private final Paint linePaint2 = new Paint();
    private final Paint pointPaint = new Paint();
    private final Paint textPaint = new Paint();

    public MiniGraphView(Context context) {
        super(context);
        setupPaint();
    }

    public MiniGraphView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setupPaint();
    }

    private void setupPaint() {
        backgroundPaint.setColor(Color.rgb(248, 250, 249));
        backgroundPaint.setStyle(Paint.Style.FILL);

        gridPaint.setColor(Color.rgb(224, 231, 227));
        gridPaint.setStrokeWidth(dp(1));

        linePaint1.setColor(Color.rgb(233, 131, 53));
        linePaint1.setStrokeWidth(dp(3));
        linePaint1.setStrokeCap(Paint.Cap.ROUND);
        linePaint1.setStrokeJoin(Paint.Join.ROUND);
        linePaint1.setAntiAlias(true);

        linePaint2.setColor(Color.rgb(86, 141, 69));
        linePaint2.setStrokeWidth(dp(3));
        linePaint2.setStrokeCap(Paint.Cap.ROUND);
        linePaint2.setStrokeJoin(Paint.Join.ROUND);
        linePaint2.setAntiAlias(true);

        pointPaint.setStyle(Paint.Style.FILL);
        pointPaint.setAntiAlias(true);

        textPaint.setColor(Color.rgb(108, 121, 115));
        textPaint.setTextSize(dp(10));
        textPaint.setAntiAlias(true);
    }

    public void setLineColors(int firstColor, int secondColor) {
        linePaint1.setColor(firstColor);
        linePaint2.setColor(secondColor);
        invalidate();
    }

    public void setGraphData(List<Float> firstLine, List<Float> secondLine,
                             float min, float max) {
        data1.clear();
        data1.addAll(firstLine);

        data2.clear();

        if (secondLine != null) {
            data2.addAll(secondLine);
        }

        minValue = min;
        maxValue = max;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float left = dp(34);
        float right = getWidth() - dp(8);
        float top = dp(10);
        float bottom = getHeight() - dp(25);

        RectF background = new RectF(0, 0, getWidth(), getHeight());
        canvas.drawRoundRect(background, dp(13), dp(13), backgroundPaint);

        for (int i = 0; i <= 4; i++) {
            float y = top + i * ((bottom - top) / 4f);
            canvas.drawLine(left, y, right, y, gridPaint);

            float labelValue = maxValue - i * ((maxValue - minValue) / 4f);
            canvas.drawText(String.valueOf((int) labelValue), dp(4), y + dp(4), textPaint);
        }

        if (data1.size() < 2) {
            textPaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("Esperando datos...", getWidth() / 2f,
                    getHeight() / 2f, textPaint);
            textPaint.setTextAlign(Paint.Align.LEFT);
            return;
        }

        drawLine(canvas, data1, left, right, top, bottom, linePaint1);

        if (!data2.isEmpty()) {
            drawLine(canvas, data2, left, right, top, bottom, linePaint2);
        }
    }

    private void drawLine(Canvas canvas, ArrayList<Float> data,
                          float left, float right, float top, float bottom, Paint paint) {
        if (data.size() < 2) {
            return;
        }

        float xStep = (right - left) / (data.size() - 1f);

        for (int i = 0; i < data.size() - 1; i++) {
            float x1 = left + i * xStep;
            float y1 = valueToY(data.get(i), top, bottom);
            float x2 = left + (i + 1) * xStep;
            float y2 = valueToY(data.get(i + 1), top, bottom);

            canvas.drawLine(x1, y1, x2, y2, paint);
        }

        float lastX = left + (data.size() - 1) * xStep;
        float lastY = valueToY(data.get(data.size() - 1), top, bottom);
        pointPaint.setColor(paint.getColor());
        canvas.drawCircle(lastX, lastY, dp(4), pointPaint);
    }

    private float valueToY(float value, float top, float bottom) {
        value = Math.max(minValue, Math.min(maxValue, value));
        float percent = (value - minValue) / (maxValue - minValue);
        return bottom - percent * (bottom - top);
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}
