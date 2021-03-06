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
import android.util.AttributeSet;
import android.view.MotionEvent;

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
    private int emphRadius;
    private int selectRadius;
    private int innerRadius;
    private int outerRadius;
    private Paint selectPaint = new Paint();
    private boolean drawGradientArea = false;
    private Paint cyValuePaint = new Paint();
    private LineStatus status;
    private int selectColor;
    private int innerColor;
    private int outerColor;

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
        status = new LineStatus(getContext());
        emphRadius = getResources().getDimensionPixelSize(R.dimen.line_chart_emphis_circle_radius);
        selectRadius = getResources().getDimensionPixelSize(R.dimen.line_chart_select_circle_radius);
        innerRadius = getResources().getDimensionPixelSize(R.dimen.line_chart_inner_circle_radius);
        outerRadius = getResources().getDimensionPixelSize(R.dimen.line_chart_outer_circle_radius);
        selectColor = getResources().getColor(R.color.line_chart_emph_color);
        innerColor = getResources().getColor(R.color.line_chart_inner_color);
        outerColor = getResources().getColor(R.color.line_chart_outer_color);

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

        selectPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        selectPaint.setAntiAlias(true);

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
                x = getChartScrollX() + transXToChartViewPosition(getCoordPoints()[i].x);
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
                if (isDrawCyValue()) {
                    String text = getCoordinate().getCyFormat().format("" + getCoordPoints()[i].y);
                    getTextRound(text, rect, cyValuePaint);
                    float cyValue = (float)y - rect.height() < 0.0f ? 0.0f : (float)y - rect.height();
                    canvas.drawText(text, (float)x - rect.centerX(), cyValue, cyValuePaint);
                }
                lastPoint = getCoordPoints()[i];
            }
            //绘制渐变
            if (drawGradientArea && firstPoint != null && lastPoint != null) {
                gradientPath.lineTo(getChartScrollX() + (float)transXToChartViewPosition(lastPoint.x), getCoordinate().coord.y);
                float firstPointX = getChartScrollX() + (float)transXToChartViewPosition(firstPoint.x);
                float firstPointY = (float)transYToPosition(firstPoint.y);
                gradientPath.lineTo(firstPointX, getCoordinate().coord.y);
                gradientPath.lineTo(firstPointX, firstPointY);
                float maxPointX = (float)transXToChartViewPosition(cyMaxPoint.x);
                float maxPointY = (float)transYToPosition(cyMaxPoint.y);
                gradientPaint.setShader(new LinearGradient(getChartScrollX() + maxPointX, maxPointY, getChartScrollX() + maxPointX, getCoordinate().coord.y,
                        gradientColors, null, Shader.TileMode.CLAMP));
                canvas.drawPath(gradientPath, gradientPaint);
            }
            //绘制折线
            paint.setPathEffect(new CornerPathEffect(5));
            canvas.drawPath(linePath, paint);
        }
    }

    @Override
    protected boolean showStatusView(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        int count = getCoordPoints().length;
        double curPointX, curPointY;
        for (int i = 0; i < count; i++) {
            if (isDoubleMinValue(getCoordPoints()[i].y)) {
                continue;
            }
            curPointX = transXToPosition(getCoordPoints()[i].x);
            curPointY = transYToPosition(getCoordPoints()[i].y);
            if (Math.sqrt((x - curPointX)*(x - curPointX) + (y - curPointY)*(y - curPointY)) < status.radioRadius) {
                if (i == status.getIndex()) {
                    hideStatusView(event);
                } else {
                    status.setIndex(i);
                    invalidateStatusView();
                }
                return true;
            }
        }
        hideStatusView(event);
        return false;
    }

    @Override
    protected boolean hideStatusView(MotionEvent event) {
        if (status != null && status.index >= 0) {
            status.setIndex(-1);
            invalidateStatusView();
        }
        return true;
    }

    @Override
    protected void onDrawStatus(Canvas canvas) {
        super.onDrawStatus(canvas);
        if (status.index >= 0) {
            PointD pointD = getCoordPoints()[status.index];
            status.text = status.getFormat().format(String.valueOf(pointD.x), String.valueOf(pointD.y));
            Rect rect = new Rect();
            getTextRound(status.text, rect, status.textPaint);
            float curPointX = (float)transXToPosition(pointD.x);
            float curPointY = (float)transYToPosition(pointD.y);

            //outer circle
            selectPaint.setColor(outerColor);
            canvas.drawCircle(curPointX, curPointY, outerRadius, selectPaint);
            //inner circle
            selectPaint.setColor(innerColor);
            canvas.drawCircle(curPointX, curPointY, innerRadius, selectPaint);
            //select circle
            canvas.drawCircle(curPointX, curPointY, selectRadius, selectPaint);
            selectPaint.setColor(selectColor);

            status.textRect.left = (int)curPointX - rect.centerX() > 0 ? (int)curPointX - rect.centerX() : 0;
            status.textRect.top = (int)curPointY - rect.height() > 0 ? (int)curPointY - rect.height() : 0;
            canvas.drawText(status.text, status.textRect.left, status.textRect.top, status.textPaint);
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

    public LineStatus getStatus() {
        return status;
    }

    public static class LineStatus extends Status {

        int radioRadius;

        public LineStatus(Context context) {
            super(context);
            radioRadius = context.getResources().getDimensionPixelSize(R.dimen.line_chart_default_touch_radius);
        }

        public void setRadioRadius(int radioRadius) {
            this.radioRadius = radioRadius;
        }
    }
}
