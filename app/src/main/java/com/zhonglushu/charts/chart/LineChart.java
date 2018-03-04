package com.zhonglushu.charts.chart;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.CornerPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Build;
import android.util.AttributeSet;

import com.zhonglushu.charts.R;

/**
 * Created by rambo.huang on 17/11/9.
 */

public class LineChart extends Chart{

    private Path linePath = new Path();
    private Paint paint = new Paint();
    private Path gradientPath = new Path();
    private Paint gradientPaint = new Paint();
    private int[] gradientColors = new int[2];
    private Paint emphPaint = new Paint();
    private int emphRadius = 3;
    private boolean drawGradientArea = false;
    private boolean drawCyValue = false;
    private Paint cyValuePaint = new Paint();

    public LineChart(Context context) {
        this(context, null);
    }

    public LineChart(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LineChart(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public LineChart(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        paint.setFlags(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(getResources().getColor(R.color.line_chart_line_color));
        paint.setStyle(Paint.Style.STROKE);
        paint.setAntiAlias(true);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setDither(true);
        paint.setStrokeWidth(3);

        emphPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        emphPaint.setColor(getResources().getColor(R.color.line_chart_emph_color));
        emphPaint.setAntiAlias(true);

        gradientColors[0] = getResources().getColor(R.color.curve_chart_gradient_color1);
        gradientColors[1] = getResources().getColor(R.color.curve_chart_gradient_color2);
        gradientPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        gradientPaint.setAntiAlias(true);

        cyValuePaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        cyValuePaint.setColor(getResources().getColor(R.color.line_chart_line_color));
        cyValuePaint.setTextSize(getResources().getDimensionPixelSize(R.dimen.line_chart_cyvalue_textsize));
        Typeface typeface = Typeface.create("sans-serif-light", Typeface.NORMAL);
        cyValuePaint.setTypeface(typeface);

    }

    @Override
    protected void onDrawChart(Canvas canvas) {
        super.onDrawChart(canvas);
        if (getCoordPoints() != null && getCoordPoints().length > 0) {
            linePath.reset();
            gradientPath.reset();
            double x, y;
            Rect rect = new Rect();
            int length = getCoordPoints().length;
            boolean moveToFirst = false;
            PointD firstPoint = null;
            PointD lastPoint = null;
            for (int i = 0; i < length; i++) {
                if (isDoubleMinValue(getCoordPoints()[i].y)) {
                    continue;
                }
                x = getChartScrollX() + transXToPosition(getCoordPoints()[i].x);
                y = transYToPosition(getCoordPoints()[i].y);
                if (!moveToFirst) {
                    moveToFirst = true;
                    firstPoint = getCoordPoints()[i];
                    linePath.moveTo((float)x, (float)y);
                    if (drawGradientArea) {
                        gradientPath.moveTo((float)x, (float)y);
                    }
                } else {
                    linePath.lineTo((float)x, (float)y);
                    if (drawGradientArea) {
                        gradientPath.lineTo((float)x, (float)y);
                    }
                }
                //绘制圆点
                boolean emph = getEmphFunc() != null && getEmphFunc().emphasis(getCoordPoints()[i]);
                if (emph) {
                    canvas.drawCircle((float)x, (float)y, emphRadius, emphPaint);
                }
                //绘制y轴的值
                if (drawCyValue) {
                    String text = getCoordinate().getCyFormat().format("" + getCoordPoints()[i].y);
                    getTextRound(text, rect, cyValuePaint);
                    float cyValue = (float)y - rect.height() < 0.0f ? 0.0f : (float)y - rect.height();
                    canvas.drawText(text, (float)x - rect.centerX(), cyValue, cyValuePaint);
                }
                lastPoint = getCoordPoints()[i];
            }
            //绘制渐变
            if (drawGradientArea && firstPoint != null && lastPoint != null) {
                gradientPath.lineTo(getChartScrollX() + (float)transXToPosition(lastPoint.x), getCoordinate().coord.y);
                float firstPointX = getChartScrollX() + (float)transXToPosition(firstPoint.x);
                float firstPointY = (float)transYToPosition(firstPoint.y);
                gradientPath.lineTo(firstPointX, getCoordinate().coord.y);
                gradientPath.lineTo(firstPointX, firstPointY);
                gradientPaint.setShader(new LinearGradient(getChartScrollX() + (float)cyMaxPoint.x, (float)cyMaxPoint.y, getChartScrollX() + (float)cyMaxPoint.x, getCoordinate().coord.y,
                        gradientColors, new float[]{0.45f, 1.0f}, Shader.TileMode.CLAMP));
                canvas.drawPath(gradientPath, gradientPaint);
            }
            //绘制折线
            paint.setPathEffect(new CornerPathEffect(5));
            canvas.drawPath(linePath, paint);
        }
    }

    public int getEmphRadius() {
        return emphRadius;
    }

    public void setEmphRadius(int emphRadius) {
        this.emphRadius = emphRadius;
    }

    public boolean isDrawGradientArea() {
        return drawGradientArea;
    }

    public void setDrawGradientArea(boolean drawGradientArea) {
        this.drawGradientArea = drawGradientArea;
    }

    public boolean isDrawCyValue() {
        return drawCyValue;
    }

    public void setDrawCyValue(boolean drawCyValue) {
        this.drawCyValue = drawCyValue;
    }
}
