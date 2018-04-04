package com.zhonglushu.charts.chart;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.util.AttributeSet;

import com.zhonglushu.charts.R;

/**
 * Created by rambo.huang on 17/11/9.
 */

public class CurveChart extends Chart {

    private Path curvePath = new Path();
    private Path gradientPath = new Path();
    private Paint gradientPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int[] gradientColors = new int[2];
    private Paint paint = new Paint();
    private boolean drawGradientArea = false;

    public CurveChart(Context context) {
        this(context, null);
    }

    public CurveChart(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CurveChart(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public CurveChart(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    private void init(Context context) {
        paint.setFlags(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(context.getResources().getColor(R.color.curve_chart_unit_color));
        paint.setStyle(Paint.Style.STROKE);
        paint.setAntiAlias(true);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setDither(true);
        paint.setStrokeWidth(3);

        gradientColors[0] = context.getResources().getColor(R.color.curve_chart_gradient_color1);
        gradientColors[1] = context.getResources().getColor(R.color.curve_chart_gradient_color2);
        gradientPaint.setAntiAlias(true);
    }

    @Override
    protected void onDrawChart(Canvas canvas) {
        super.onDrawChart(canvas);
        //画曲线
        if (getCoordPoints() != null && getCoordPoints().length > 0) {
            curvePath.reset();
            gradientPath.reset();
            double lastX, lastY;
            boolean moveToFirst = false;
            PointD firstPoint = null;
            PointD lastPoint = null;
            double x, y;
            int length = getCoordPoints().length;
            for (int i = 0; i < length; i++) {
                if (isDoubleMinValue(getCoordPoints()[i].y)) {
                    continue;
                }
                x = getChartScrollX() + transXToChartViewPosition(getCoordPoints()[i].x);
                y = transYToPosition(getCoordPoints()[i].y);

                if (!moveToFirst) {
                    moveToFirst = true;
                    firstPoint = getCoordPoints()[i];
                    curvePath.moveTo((float)x, (float)y);
                    gradientPath.moveTo((float)x, (float)y);
                } else {
                    lastX = getChartScrollX() + transXToChartViewPosition(getCoordPoints()[i - 1].x);
                    lastY = transYToPosition(getCoordPoints()[i - 1].y);

                    float controlX = (float)((lastX + x) / 2);
                    curvePath.cubicTo(controlX, (float)lastY, controlX, (float)y, (float)x, (float)y);
                    gradientPath.cubicTo(controlX, (float)lastY, controlX, (float)y, (float)x, (float)y);
                }
                lastPoint = getCoordPoints()[i];
            }
            //绘制渐变
            if (drawGradientArea && firstPoint != null && lastPoint != null) {
                gradientPath.lineTo((float) transXToChartViewPosition(getCoordPoints()[length - 1].x), getCoordinate().coord.y);
                float firstPointX = (float) transXToChartViewPosition(getCoordPoints()[0].x);
                float firstPointY = (float) transYToPosition(getCoordPoints()[0].y);
                gradientPath.lineTo(firstPointX, getCoordinate().coord.y);
                gradientPath.lineTo(firstPointX, firstPointY);
                float maxPointX = (float)transXToChartViewPosition(cyMaxPoint.x);
                float maxPointY = (float)cyMaxPoint.y;
                gradientPaint.setShader(new LinearGradient(maxPointX, maxPointY, maxPointX, getCoordinate().coord.y,
                        gradientColors, null, Shader.TileMode.MIRROR));
                canvas.drawPath(gradientPath, gradientPaint);
            }
            //绘制曲线
            canvas.drawPath(curvePath, paint);
        }
    }

    public boolean isDrawGradientArea() {
        return drawGradientArea;
    }

    public void setDrawGradientArea(boolean drawGradientArea) {
        this.drawGradientArea = drawGradientArea;
    }
}
