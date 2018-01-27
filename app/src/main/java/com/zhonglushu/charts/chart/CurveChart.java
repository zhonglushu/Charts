package com.zhonglushu.charts.chart;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.os.Build;
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
            double x, y;
            int length = getCoordPoints().length;
            for (int i = 0; i < length; i++) {
                x = getCoordPoints()[i].x;
                y = getCoordPoints()[i].y;

                if (i == 0) {
                    x = transXToPosition(x);
                    y = transYToPosition(y);
                    curvePath.moveTo((float)x, (float)y);
                    gradientPath.moveTo((float)x, (float)y);
                } else {
                    lastX = getCoordPoints()[i - 1].x;
                    lastY = getCoordPoints()[i - 1].y;

                    lastX = transXToPosition(lastX);
                    lastY = transYToPosition(lastY);
                    x = transXToPosition(x);
                    y = transYToPosition(y);

                    float controlX = (float)((lastX + x) / 2);
                    curvePath.cubicTo(controlX, (float)lastY, controlX, (float)y, (float)x, (float)y);
                    gradientPath.cubicTo(controlX, (float)lastY, controlX, (float)y, (float)x, (float)y);
                }
            }
            //绘制渐变
            gradientPath.lineTo((float)transXToPosition(getCoordPoints()[length - 1].x), getCoordinate().coord.y);
            float firstPointX = (float)transXToPosition(getCoordPoints()[0].x);
            float firstPointY = (float)transYToPosition(getCoordPoints()[0].y);
            gradientPath.lineTo(firstPointX, getCoordinate().coord.y);
            gradientPath.lineTo(firstPointX, firstPointY);
            gradientPaint.setShader(new LinearGradient(firstPointX, firstPointY, firstPointX, getCoordinate().coord.y,
                    gradientColors, new float[]{0.75f, 1.0f}, Shader.TileMode.MIRROR));
            canvas.drawPath(gradientPath, gradientPaint);
            //绘制曲线
            canvas.drawPath(curvePath, paint);
        }
    }
}