package com.zhonglushu.charts.chart;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

import com.zhonglushu.charts.R;

/**
 * Created by rambo.huang on 17/11/9.
 */

public class BarChart extends Chart {

    private Paint paint = new Paint();
    private Paint touchPaint = new Paint();
    private float barWidth = 10.0f;
    private float[] roundRadius = new float[8];
    private Path barPath = new Path();
    private int selectColor;
    private int normalColor;
    private Status status;
    private int statusMarginBottom = 4;
    private int statusRoundRadius = 4;
    private int angleHeight = 8;
    private int angleWidth = 12;
    private Path statusPath = new Path();
    private int[] gradientColors = new int[2];
    private InvalidateType currentType = new InvalidateType();

    private class InvalidateType {
        INVALIDATE type;
        int index;

        public InvalidateType() {
            this.type = INVALIDATE.ALL;
            this.index = -1;
        }
    }

    private enum INVALIDATE {
        ALL, BAR
    }

    public BarChart(Context context) {
        this(context, null);
    }

    public BarChart(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BarChart(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public BarChart(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    private void init(Context context) {
        barWidth = getResources().getDimensionPixelSize(R.dimen.bar_chart_bar_width);
        updateRoundRadius();

        selectColor = getResources().getColor(R.color.bar_chart_bar_select_color);
        normalColor = getResources().getColor(R.color.bar_chart_bar_unselect_color);

        gradientColors[0] = getResources().getColor(R.color.barchart_click_start_color);
        gradientColors[1] = getResources().getColor(R.color.barchart_click_end_color);

        paint.setFlags(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(normalColor);
        paint.setAntiAlias(true);

        touchPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        touchPaint.setAntiAlias(true);

        status = new Status(context);
    }

    private void updateRoundRadius() {
        for (int i = 0; i < roundRadius.length; i++) {
            roundRadius[i] = barWidth / 2.0f;
        }
    }

    public Status getStatus() {
        return status;
    }

    public void onPause() {
        Log.i("Rambo", "BarChart onPause()");
        if (status != null) {
            status.setIndex(-1);
        }
        resetInvalidateType();
    }

    @Override
    public void invalidate() {
        Log.i("Rambo", "BarChart invalidate()");
        resetInvalidateType();
        super.invalidate();
    }

    public void updateCoordPoint(PointD pointD, int index) {
        Log.i("Rambo", "BarChart updateCoordPoint() index = " + index);
        if (getCoordPoints().length <= index) {
            return;
        }
        hideStatusView(null);
        getCoordPoints()[index].y = pointD.y;
        if (getCoordinate().cyRange[1] > pointD.y && getCoordinate().cyRange[0] < pointD.y) {
            invalidateBar(index);
        } else {
            updateRange();
            invalidate();
        }
    }

    public void invalidateBar(int index) {
        Log.i("Rambo", "invalidateBar BAR start");
        RectF rectF = getPointRect(index);
        Rect rect = new Rect();
        rect.left = (int)rectF.left - 1;
        rect.right = (int)rectF.right + 1;
        if (getCoordinate().cyDirection == Coordinate.DIRECTION.POSITIVE) {
            rect.top = (int)getStatusBottom();
            rect.bottom = (int)getCoordinate().coord.y;
        } else {
            rect.top = (int)getCoordinate().coord.y - 1;
            rect.bottom = (int)getStatusBottom() + 1;
        }
        currentType.type = INVALIDATE.BAR;
        currentType.index = index;
        invalidateChartView(rect);
    }

    @Override
    protected void onDrawStatus(Canvas canvas) {
        super.onDrawStatus(canvas);
        Log.i("Rambo", "onDrawStatus STATUS start status.index = " + status.index);
        if (status.index >= 0) {
            Rect rect = new Rect();
            PointD pointD = getCoordPoints()[status.index];
            float curPointX = (float)transXToPosition(pointD.x);
            float curPointY = (float)transYToPosition(pointD.y);
            status.text = status.getFormat().format(String.valueOf(pointD.x), String.valueOf(pointD.y));
            getTextRound(status.text, rect, status.textPaint);
            int centerX = rect.width() / 2;

            status.rect.left = (int)curPointX - centerX - status.paddingLeft;
            status.rect.right = (int)curPointX + centerX + status.paddingLeft + status.paddingRight;
            if (status.rect.left < 0 || status.rect.right > width) {
                if (status.rect.left < 0) {
                    status.rect.left = 0;
                    status.textRect.left = status.rect.left + status.paddingLeft;
                    status.rect.right = status.rect.left + rect.width() + status.paddingLeft + status.paddingRight;
                    status.textRect.right = status.rect.right - status.paddingRight;
                }
                if (status.rect.right > width) {
                    status.rect.right = width;
                    status.textRect.right = width - status.paddingRight;
                    status.rect.left = width - rect.width() - status.paddingRight - status.paddingLeft;
                    status.textRect.left = status.rect.left + status.paddingLeft;
                }
            } else {
                status.textRect.left = status.rect.left + status.paddingLeft;
                status.textRect.right = status.rect.right - status.paddingRight;
            }

            //绘制竖线
            status.linePaint.setColor(getResources().getColor(R.color.colorWhite_60));
            float statusBottom = getStatusBottom() - 4 * statusMarginBottom;
            canvas.drawLine(curPointX, curPointY - statusMarginBottom, curPointX, statusBottom + statusMarginBottom, status.linePaint);
            //绘制矩形和文字
            statusPath.reset();
            statusPath.moveTo(curPointX, statusBottom);
            statusPath.lineTo(curPointX - angleWidth, statusBottom - angleHeight);
            statusPath.lineTo(status.rect.left + statusRoundRadius, statusBottom - angleHeight);
            //left bottom
            RectF rectF = new RectF();
            rectF.left = status.rect.left;
            rectF.right = status.rect.left + 2 * statusRoundRadius;
            rectF.bottom = statusBottom - angleHeight;
            rectF.top = rectF.bottom - 2 * statusRoundRadius;
            statusPath.addArc(rectF, 90, 90);

            float lineHeight = rect.height() + status.paddingTop + status.paddingBottom;
            float lineWidth = rect.width() + status.paddingLeft + status.paddingRight;
            //left top
            rectF.top = statusBottom - angleHeight - lineHeight;
            rectF.bottom = rectF.top + 2 * statusRoundRadius;
            statusPath.lineTo(rectF.left, rectF.bottom - statusRoundRadius);
            statusPath.addArc(rectF, 180, 90);

            //right top
            rectF.right = rectF.left + lineWidth;
            rectF.left = rectF.right - 2 * statusRoundRadius;
            statusPath.lineTo(rectF.left + statusRoundRadius, rectF.top);
            statusPath.addArc(rectF, 270, 90);

            //right bottom
            rectF.bottom = rectF.top + lineHeight;
            rectF.top = rectF.bottom - 2 * statusRoundRadius;
            statusPath.lineTo(rectF.right, rectF.bottom - statusRoundRadius);
            statusPath.addArc(rectF, 0, 90);

            statusPath.lineTo(curPointX + angleWidth, rectF.bottom);
            statusPath.lineTo(curPointX, rectF.bottom + angleHeight);

            canvas.drawPath(statusPath, status.linePaint);
            canvas.drawText(status.text, status.textRect.left, statusBottom - angleHeight - status.paddingBottom, status.textPaint);
            Log.i("Rambo", "onDrawStatus STATUS finish");
        }
    }

    @Override
    protected boolean showStatusView(MotionEvent event) {
        if (getCoordPoints() == null || getCoordPoints().length <= 0) {
            return false;
        }
        float x = event.getX();
        float y = event.getY();
        int len = getCoordPoints().length;
        RectF rectF = new RectF();
        for (int i = 0; i < len; i++) {
            if (isDoubleMinValue(getCoordPoints()[i].y)){
                continue;
            }
            double curPointX = transXToPosition(getCoordPoints()[i].x);
            double prePointX = curPointX;
            double nextPointX = curPointX;
            if (getCoordinate().cxReverse) {
                if (i > 0) {
                    prePointX = transXToPosition(getCoordPoints()[i - 1].x);
                } else if (i == 0) {
                    prePointX = curPointX - 4 * barWidth < 0 ? 0 : curPointX - 4 * barWidth;
                }
                if (i < len - 1) {
                    nextPointX = transXToPosition(getCoordPoints()[i + 1].x);
                } else if (i == len - 1) {
                    nextPointX = curPointX + 4 * barWidth > width ? width : curPointX + 4 * barWidth;
                }
            } else {
                if (i < len - 1) {
                    prePointX = transXToPosition(getCoordPoints()[i + 1].x);
                } else if (i == len - 1) {
                    prePointX = curPointX - 4 * barWidth < 0 ? 0 : curPointX - 4 * barWidth;
                }
                if (i > 0) {
                    nextPointX = transXToPosition(getCoordPoints()[i - 1].x);
                } else if (i == 0) {
                    nextPointX = curPointX + 4 * barWidth > width ? width : curPointX + 4 * barWidth;
                }
            }
            double minX = Math.min(prePointX, nextPointX);
            double maxX = Math.max(prePointX, nextPointX);
            rectF.left = (float)(curPointX - (curPointX - minX) / 2);
            rectF.right = (float)(curPointX + (maxX - curPointX) / 2);
            if (getCoordinate().cyDirection == Coordinate.DIRECTION.POSITIVE) {
                rectF.top = getStatusBottom();
                rectF.bottom = getCoordinate().coord.y;
            } else {
                rectF.top = getCoordinate().coord.y;
                rectF.bottom = getStatusBottom();
            }
            if (rectF.contains(x, y)) {
                status.setIndex(i);
                invalidateStatusView(new Rect(0, 0, width, height));
                return true;
            }
        }
        status.setIndex(-1);
        invalidateStatusView(new Rect(0, 0, width, height));
        return true;
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
    protected boolean customDrawChart() {
        return currentType.type == INVALIDATE.BAR;
    }

    @Override
    protected void onDrawChart(Canvas canvas) {
        Log.i("Rambo", "BarChart onDrawChart() currentType = " + currentType);
        //画柱状图
        if (getCoordPoints() != null && getCoordPoints().length > 0) {
            if (currentType.type == INVALIDATE.BAR) {
                drawRoundRect(canvas, currentType.index);
                Log.i("Rambo", "onDrawChart BAR finish");
                resetInvalidateType();

            } else {
                super.onDrawChart(canvas);
                int length = getCoordPoints().length;
                for (int i = 0; i < length; i++) {
                    drawRoundRect(canvas, i);
                }
            }
        } else {
            super.onDrawChart(canvas);
        }
    }

    private void resetInvalidateType() {
        currentType.type = INVALIDATE.ALL;
        currentType.index = -1;
    }

    private void drawRoundRect(Canvas canvas, int index) {
        if (isDoubleMinValue(getCoordPoints()[index].y)) {
            return;
        }
        barPath.reset();
        RectF rectF = getPointRect(index);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            barPath.addRoundRect(rectF.left + getChartScrollX(), rectF.top, rectF.right + getChartScrollX(), rectF.bottom, roundRadius, Path.Direction.CW);
        }
        if (status.index >= 0 && status.index == index) {
            touchPaint.setShader(new LinearGradient(rectF.centerX(), rectF.top, rectF.centerX(), rectF.bottom, gradientColors, new float[]{0.20f, 1.0f}, Shader.TileMode.CLAMP));
            canvas.drawPath(barPath, touchPaint);
        } else {
            if (getEmphFunc() != null && getEmphFunc().emphasis(getCoordPoints()[index])) {
                paint.setColor(selectColor);
            } else {
                paint.setColor(normalColor);
            }
            canvas.drawPath(barPath, paint);
        }
    }

    private RectF getPointRect(int index) {
        double x, y;
        double startX, endX;
        double min, max;
        x = transXToChartViewPosition(getCoordPoints()[index].x);
        y = transYToPosition(getCoordPoints()[index].y);
        startX = x - barWidth / 2.0f;
        endX = x + barWidth / 2.0f;
        if (y > getCoordinate().coord.y) {
            min = getCoordinate().coord.y + getCoordinate().cyStartPadding;
            max = y;
        } else {
            min = y;
            max = getCoordinate().coord.y - getCoordinate().cyStartPadding;
        }
        return new RectF((float)startX, (float)min, (float)endX, (float)max);
    }

    public float getStatusBottom() {
        return getCoordEndY();
    }

    public void setBarWidth(float barWidth) {
        this.barWidth = barWidth;
        updateRoundRadius();
    }

    public static class Status {
        Rect rect = new Rect();
        int index = -1;
        int textSize;
        int textColor;
        String text;
        Rect textRect = new Rect();
        Paint linePaint = new Paint();
        Paint textPaint = new Paint();
        int paddingLeft = 20;
        int paddingTop = 10;
        int paddingRight = 20;
        int paddingBottom = 10;
        StatusFormat format = new StatusFormat();

        public static class StatusFormat {
            public String format(String cxValue, String cyValue) {
                double d = Double.valueOf(cyValue);
                return "" + (int)d;
            }
        }

        public Status(Context context) {
            linePaint.setFlags(Paint.ANTI_ALIAS_FLAG);
            linePaint.setStyle(Paint.Style.STROKE);
            linePaint.setAntiAlias(true);
            linePaint.setColor(context.getResources().getColor(R.color.line_chart_line_color));

            textPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
            textPaint.setAntiAlias(true);
            Typeface typeface = Typeface.create("sans-serif-light", Typeface.NORMAL);
            textPaint.setTypeface(typeface);
            textPaint.setTextSize(context.getResources().getDimensionPixelSize(R.dimen.bar_chart_tipbar_textsize));
            textPaint.setColor(context.getResources().getColor(R.color.line_chart_emph_color));
        }

        public int getTextSize() {
            return textSize;
        }

        public void setTextSize(int textSize) {
            this.textSize = textSize;
            textPaint.setTextSize(textSize);
        }

        public int getTextColor() {
            return textColor;
        }

        public void setTextColor(int textColor) {
            this.textColor = textColor;
            textPaint.setColor(textColor);
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public void setIndex(int index) {
            this.index = index;
        }

        public StatusFormat getFormat() {
            return format;
        }

        public void setFormat(StatusFormat format) {
            this.format = format;
        }
    }
}
