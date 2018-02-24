package com.zhonglushu.charts.chart;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;

import com.zhonglushu.charts.R;

/**
 * Created by rambo.huang on 17/11/10.
 */

public class Chart extends FrameLayout {

    //坐标系
    private Coordinate coordinate;
    //坐标点
    private PointD[] coordPoints;
    protected float xStartMarginRadio = 0.12f;
    protected float xEndMarginRadio = 0.12f;
    protected float yStartMarginRadio = 0.12f;
    protected float yEndMarginRadio = 0.12f;
    protected int width;
    protected int height;
    private float xStartSpace = 0.0f;
    private Path dashPath = new Path();
    private DashPathEffect dashPathEffect = null;
    //保存最高y轴的点，用于绘制渐变的颜色
    protected PointD cyMaxPoint;
    protected CoordinateView coordinateView = null;
    protected ChartView chartView = null;
    protected StatusView statusView = null;
    private ScrollCallback scrollCallback = null;
    private float chartScrollX = 0.0f;
    protected PointD[] totalCoordPoints = null;
    protected int[] scrollIndex = new int[2];
    private ScrollMode scrollMode = ScrollMode.NONE;
    public enum ScrollMode {
        NONE, DEFAULT, CUSTOM
    }

    private EmphasisFunc emphFunc = new EmphasisFunc();
    public static class EmphasisFunc {
        public boolean emphasis(PointD point) {
            return false;
        }
    }

    public Chart(Context context) {
        super(context);
    }

    public Chart(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public Chart(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public Chart(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        coordinate = new Coordinate(getContext());
        dashPathEffect = new DashPathEffect(new float[]{20, 4}, 0);
    }

    @Override
    public void invalidate() {
        super.invalidate();
        invalidateCooridateView();
        invalidateChartView();
    }

    public void invalidateChartView(Rect rect) {
        if (chartView != null) {
            chartView.invalidate(rect);
        }
    }

    public void invalidateChartView() {
        if (chartView != null) {
            chartView.invalidate();
        }
    }

    public void invalidateCooridateView() {
        if (coordinateView != null) {
            coordinateView.invalidate();
        }
    }

    public void invalidateStatusView() {
        if (statusView != null) {
            statusView.invalidate();
        }
    }

    public void invalidateStatusView(Rect rect) {
        if (statusView != null) {
            statusView.invalidate(rect);
        }
    }

    public Coordinate getCoordinate() {
        return coordinate;
    }

    public void setCoordPoints(PointD[] coordPoints) {
        this.coordPoints = coordPoints;
        updateRange();
    }

    public void updateCoordPoints(PointD[] coordPoints) {
        this.coordPoints = coordPoints;
    }

    public PointD[] getCoordPoints() {
        return coordPoints;
    }

    public EmphasisFunc getEmphFunc() {
        return emphFunc;
    }

    public void setEmphFunc(EmphasisFunc emphFunc) {
        this.emphFunc = emphFunc;
    }

    public float getxStartMarginRadio() {
        return xStartMarginRadio;
    }

    public void setxStartMarginRadio(float xStartMarginRadio) {
        this.xStartMarginRadio = xStartMarginRadio;
    }

    public float getxEndMarginRadio() {
        return xEndMarginRadio;
    }

    public void setxEndMarginRadio(float xEndMarginRadio) {
        this.xEndMarginRadio = xEndMarginRadio;
    }

    public float getyStartMarginRadio() {
        return yStartMarginRadio;
    }

    public void setyStartMarginRadio(float yStartMarginRadio) {
        this.yStartMarginRadio = yStartMarginRadio;
    }

    public float getyEndMarginRadio() {
        return yEndMarginRadio;
    }

    public void setyEndMarginRadio(float yEndMarginRadio) {
        this.yEndMarginRadio = yEndMarginRadio;
    }

    public float getChartScrollX() {
        return chartScrollX;
    }

    public void setChartScrollX(float chartScrollX) {
        this.chartScrollX = chartScrollX;
    }

    private void updateRange() {
        int count = getCoordPoints().length;
        double minCx = Double.MAX_VALUE, maxCx = Double.MIN_VALUE;
        double minCy = Double.MAX_VALUE, maxCy = Double.MIN_VALUE;
        int index = 0;
        for (int i = 0; i < count; i++) {
            minCx = Math.min(minCx, getCoordPoints()[i].x);
            maxCx = Math.max(maxCx, getCoordPoints()[i].x);
            minCy = Math.min(minCy, getCoordPoints()[i].y);
            if (getCoordPoints()[i].y > maxCy) {
                index = i;
                maxCy = getCoordPoints()[i].y;
            }
        }
        cyMaxPoint = getCoordPoints()[index];
        Log.i("Rambo", "updateRange minCx = " + minCx + ", maxCx = " + maxCx);
        Log.i("Rambo", "updateRange minCy = " + minCy + ", maxCy = " + maxCy);
        getCoordinate().setCxRange(minCx, maxCx);
        getCoordinate().setCyRange(minCy, maxCy);
        getCoordinate().updateUnitValues(coordPoints);
        updateCoord();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        Log.i("Rambo", "Chart onFinishInflate()");
        ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        coordinateView = new CoordinateView(getContext());
        coordinateView.setCoordinate(coordinate);
        this.addView(coordinateView, layoutParams);
        chartView = new ChartView(getContext());
        this.addView(chartView, layoutParams);
        statusView = new StatusView(getContext());
        this.addView(statusView, layoutParams);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        width = MeasureSpec.getSize(widthMeasureSpec);
        height = MeasureSpec.getSize(heightMeasureSpec);
        setMeasuredDimension(width, height);
        updateCoord();
        Log.i("Rambo", "Chart onMeasure()");
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        Log.i("Rambo", "Chart onLayout()");
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            if(getChildAt(i) instanceof ChartView) {
                Log.i("Rambo", "Chart onLayout() getChildAt(i) instanceof ChartView");
                float chartLeft = 0.0f;
                float chartRight = 0.0f;
                if (getCoordinate().cxDirection == Coordinate.DIRECTION.POSITIVE) {
                    chartLeft = getMaginStartX();
                    chartRight = getMaginEndX();
                } else {
                    chartLeft = getMaginEndX();
                    chartRight = getMaginStartX();
                }
                getChildAt(i).layout((int)chartLeft, top, (int)chartRight, bottom);
                break;
            }
        }
    }

    private void updateCoord() {
        xStartSpace = xStartMarginRadio * width;
        getCoordinate().updateCoord(width, height, xStartMarginRadio, xEndMarginRadio, yStartMarginRadio, yEndMarginRadio);
    }

    public float getCoordStartX() {
        if (getCoordinate().cxDirection == Coordinate.DIRECTION.POSITIVE) {
            return (xStartMarginRadio + getCoordinate().cxStartSpaceRadio)*width;
        } else {
            return (1.0f - xStartMarginRadio - getCoordinate().cxStartSpaceRadio)*width;
        }
    }

    public float getCoordEndX() {
        if (getCoordinate().cxDirection == Coordinate.DIRECTION.POSITIVE) {
            return (1.0f - xEndMarginRadio - getCoordinate().cxEndSpaceRadio)*width;
        } else {
            return (xEndMarginRadio + getCoordinate().cxEndSpaceRadio)*width;
        }
    }

    public float getCoordStartY() {
        if (getCoordinate().cyDirection == Coordinate.DIRECTION.POSITIVE) {
            return (1.0f - yStartMarginRadio - getCoordinate().cyStartSpaceRadio)*height;
        } else {
            return (yStartMarginRadio + getCoordinate().cyStartSpaceRadio)*height;
        }
    }

    public float getCoordEndY() {
        if (getCoordinate().cyDirection == Coordinate.DIRECTION.POSITIVE) {
            return (yEndMarginRadio + getCoordinate().cyEndSpaceRadio)*height;
        } else {
            return (1.0f - yEndMarginRadio - getCoordinate().cyEndSpaceRadio)*height;
        }
    }

    public float getMaginStartX() {
        if (getCoordinate().cxDirection == Coordinate.DIRECTION.POSITIVE) {
            return xStartMarginRadio * width;
        } else {
            return (1.0f - xStartMarginRadio)*width;
        }
    }

    public float getMaginEndX() {
        if (getCoordinate().cxDirection == Coordinate.DIRECTION.POSITIVE) {
            return (1.0f - xEndMarginRadio)*width;
        } else {
            return xEndMarginRadio * width;
        }
    }

    public double transXToPosition(double x) {
        float startX = getCoordStartX();
        if (getCoordinate().cxDirection == Coordinate.DIRECTION.POSITIVE) {
            if (getCoordinate().cxReverse) {
                return startX + (getCoordinate().cxRange[1] - x)*getCoordinate().cxRealUnit;
            } else {
                return startX + (x - getCoordinate().cxRange[0])*getCoordinate().cxRealUnit;
            }
        } else {
            if (getCoordinate().cxReverse) {
                return startX  - (getCoordinate().cxRange[1] - x)*getCoordinate().cxRealUnit;
            } else {
                return startX  - (x - getCoordinate().cxRange[0])*getCoordinate().cxRealUnit;
            }
        }
    }

    public double transXToTouch(double x) {
        if (getCoordinate().cxDirection == Coordinate.DIRECTION.POSITIVE) {
            if (getCoordinate().cxReverse) {
                return width - transXToPosition(x);
            } else {
                return transXToPosition(x);
            }
        } else {
            if (getCoordinate().cxReverse) {
                return transXToPosition(x);
            } else {
                return width - transXToPosition(x);
            }
        }
    }

    public double transYToPosition(double y) {
        float startY = getCoordStartY();
        if (getCoordinate().cyDirection == Coordinate.DIRECTION.POSITIVE) {
            return startY - (y - getCoordinate().cyRange[0])*getCoordinate().cyRealUnit;
        } else {
            return startY + (y - getCoordinate().cyRange[0])*getCoordinate().cyRealUnit;
        }
    }

    protected void getTextRound(String text, Rect rect, Paint temPaint) {
        temPaint.getTextBounds(text, 0, text.length(), rect);
    }

    protected boolean isDoubleMinValue(double d) {
        if (Double.compare(d, Double.MIN_VALUE) == 0) {
            return true;
        }
        return false;
    }

    public ScrollMode getScrollMode() {
        return scrollMode;
    }

    public void setScrollMode(ScrollMode scrollMode) {
        this.scrollMode = scrollMode;
        if (this.scrollMode == ScrollMode.DEFAULT) {
            scrollCallback = null;
            scrollCallback = new DefaultScrollCallback();
        }
    }

    /**
     * 默认滚动模式需要设置所有所标点的数组
     * @return
     */
    public boolean inDefaultScrollMode() {
        if (coordPoints != null && coordPoints.length > 0) {
            if (totalCoordPoints != null && totalCoordPoints.length > coordPoints.length) {
                if (this.scrollMode == ScrollMode.DEFAULT) {
                    return true;
                }
            } else {
                Log.e("Rambo", "Points array has not enough points, totalCoordPoints = " + totalCoordPoints);
            }
        } else {
            Log.e("Rambo", "Points array is empty, coordPoints = " + coordPoints);
        }
        return false;
    }

    public boolean inScrollMode() {
        if (coordPoints != null && coordPoints.length > 0) {
            if (totalCoordPoints != null && totalCoordPoints.length > coordPoints.length) {
                if (this.scrollMode == ScrollMode.DEFAULT || this.scrollMode == ScrollMode.CUSTOM) {
                    return true;
                }
            } else {
                Log.e("Rambo", "Points array has not enough points, totalCoordPoints = " + totalCoordPoints);
            }
        } else {
            Log.e("Rambo", "Points array is empty, coordPoints = " + coordPoints);
        }
        return false;
    }

    public void setTotalCoordPoints(PointD[] totalCoordPoints) {
        this.totalCoordPoints = totalCoordPoints;
    }

    public PointD[] getTotalCoordPoints() {
        return totalCoordPoints;
    }

    public static class Coordinate {
        //坐标原点
        PointF coord = new PointF();
        //X轴长度
        double[] cxRange = new double[2];
        //Y轴长度
        double[] cyRange = new double[2];
        //x轴单位刻度
        double cxUnit;
        double cxRealUnit;
        //y轴单位刻度
        double cyUnit;
        double cyRealUnit;
        //自定义刻度值
        String[] cxUnitValue = null;
        String[] cyUnitValue = null;
        //是否需要刻度虚线
        boolean unitDashLine = true;
        //是否绘制x, y轴坐标
        boolean drawXCoord = true;
        boolean drawYCoord = false;
        //是否绘制x，y轴坐标的刻度文本
        boolean drawXCoordText = true;
        boolean drawYCoordText = true;
        //第一个刻度值与坐标原点（x,y）的距离
        float cxStartSpaceRadio = 0.0f;
        float cyStartSpaceRadio = 0.0f;
        float cxEndSpaceRadio = 0.0f;
        float cyEndSpaceRadio = 0.0f;
        //刻度文本与坐标轴的距离
        float cyTextSpaceRadio = 0.03f;
        float cyTextSpace = 0.0f;
        int cxyTextSize = 30;
        //x轴的坐标值是否从大到小排列
        boolean cxReverse = false;
        Paint unitTextPaint = new Paint();
        Paint unitPaint = new Paint();
        float lenCx = 0.0f;
        float lenCy = 0.0f;
        //x、y轴方向
        DIRECTION cxDirection = DIRECTION.POSITIVE;
        DIRECTION cyDirection = DIRECTION.POSITIVE;

        public enum DIRECTION {
            POSITIVE, NEGATIVE
        }

        private UnitFormat cxFormat = new UnitFormat();
        private UnitFormat cyFormat = new UnitFormat();
        public static class UnitFormat {
            public String format(String unit) {
                return unit;
            }
        }

        //自定义x、y轴的刻度值函数
        private CustomUnitValueFunc cxUnitValueFunc = null;
        private CustomUnitValueFunc cyUnitValueFunc = null;
        public static class CustomUnitValueFunc {
            public String[] unitValues(PointD[] pointDs, double[] range) {
                return null;
            }
        }

        public Coordinate(Context context) {
            unitTextPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
            Typeface typeface = Typeface.create("sans-serif-light", Typeface.NORMAL);
            unitTextPaint.setTypeface(typeface);
            unitTextPaint.setTextSize(cxyTextSize);
            unitTextPaint.setAntiAlias(true);
            unitTextPaint.setColor(context.getResources().getColor(R.color.curve_chart_unit_color));

            unitPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
            unitPaint.setStyle(Paint.Style.STROKE);
            unitPaint.setAntiAlias(true);
            unitPaint.setStrokeCap(Paint.Cap.ROUND);
            unitPaint.setDither(true);
            unitPaint.setStrokeWidth(1);
            unitPaint.setColor(context.getResources().getColor(R.color.curve_chart_coordinate_color));
        }

        public void updateUnitValues(PointD[] pointDs) {
            if (pointDs != null) {
                if (cxUnitValueFunc != null) {
                    cxUnitValue = cxUnitValueFunc.unitValues(pointDs, cxRange);
                }
                if (cyUnitValueFunc != null) {
                    cyUnitValue = cyUnitValueFunc.unitValues(pointDs, cyRange);
                }
            }
        }

        public void updateCoord(int width, int height, float xStartRadio, float xEndRadio,
                                float yStartRadio, float yEndRadio) {
            if (cxDirection == DIRECTION.POSITIVE) {
                coord.x = width * xStartRadio;
            } else {
                coord.x = (1.0f - xStartRadio)*width;
            }
            if(cyDirection == DIRECTION.POSITIVE) {
                coord.y = (1.0f - yStartRadio)*height;
            } else {
                coord.y = height*yStartRadio;
            }

            lenCx = (1.0f - xStartRadio - xEndRadio - cxStartSpaceRadio - cxEndSpaceRadio)*width;
            lenCy = (1.0f - yStartRadio - yEndRadio - cyStartSpaceRadio - cyEndSpaceRadio)*height;
            cxRealUnit = lenCx / (cxRange[1] - cxRange[0]);
            cyRealUnit = lenCy / (cyRange[1] - cyRange[0]);
            if (isCustomCxUnit()) {
                cxUnit = lenCx / cxUnitValue.length;
            } else {
                cxUnit = cxRealUnit;
            }
            if (isCustomCyUnit()) {
                cyUnit = lenCy / cyUnitValue.length;
            } else {
                cyUnit = cyRealUnit;
            }

            cyTextSpace = cyTextSpaceRadio * height;
        }

        protected void setCxRange(double min, double max) {
            this.cxRange[0] = min;
            this.cxRange[1] = max;
        }

        protected void setCyRange(double min, double max) {
            this.cyRange[0] = min;
            this.cyRange[1] = max;
        }

        public boolean isCustomCxUnit() {
            if (cxUnitValue != null && cxUnitValue.length >= 0) {
                return true;
            }
            return false;
        }

        public boolean isCustomCyUnit() {
            if (cyUnitValue != null && cyUnitValue.length >= 0) {
                return true;
            }
            return false;
        }

        public UnitFormat getCxFormat() {
            return cxFormat;
        }

        public void setCxFormat(UnitFormat cxFormat) {
            this.cxFormat = cxFormat;
        }

        public UnitFormat getCyFormat() {
            return cyFormat;
        }

        public void setCyFormat(UnitFormat cyFormat) {
            this.cyFormat = cyFormat;
        }

        public DIRECTION getCxDirection() {
            return cxDirection;
        }

        public void setCxDirection(DIRECTION cxDirection) {
            this.cxDirection = cxDirection;
        }

        public DIRECTION getCyDirection() {
            return cyDirection;
        }

        public void setCyDirection(DIRECTION cyDirection) {
            this.cyDirection = cyDirection;
        }

        public boolean isCxReverse() {
            return cxReverse;
        }

        public void setCxReverse(boolean cxReverse) {
            this.cxReverse = cxReverse;
        }

        public float getCyTextSpaceRadio() {
            return cyTextSpaceRadio;
        }

        public void setCyTextSpaceRadio(float cyTextSpaceRadio) {
            this.cyTextSpaceRadio = cyTextSpaceRadio;
        }

        public boolean isDrawXCoordText() {
            return drawXCoordText;
        }

        public void setDrawXCoordText(boolean drawXCoordText) {
            this.drawXCoordText = drawXCoordText;
        }

        public boolean isDrawYCoordText() {
            return drawYCoordText;
        }

        public void setDrawYCoordText(boolean drawYCoordText) {
            this.drawYCoordText = drawYCoordText;
        }

        public boolean isDrawXCoord() {
            return drawXCoord;
        }

        public void setDrawXCoord(boolean drawXCoord) {
            this.drawXCoord = drawXCoord;
        }

        public boolean isDrawYCoord() {
            return drawYCoord;
        }

        public void setDrawYCoord(boolean drawYCoord) {
            this.drawYCoord = drawYCoord;
        }

        public int getCxyTextSize() {
            return cxyTextSize;
        }

        public void setCxyTextSize(int cxyTextSize) {
            this.cxyTextSize = cxyTextSize;
            unitTextPaint.setTextSize(cxyTextSize);
        }

        public void setCxStartSpaceRadio(float cxStartSpaceRadio) {
            this.cxStartSpaceRadio = cxStartSpaceRadio;
        }

        public void setCyStartSpaceRadio(float cyStartSpaceRadio) {
            this.cyStartSpaceRadio = cyStartSpaceRadio;
        }

        public void setCxEndSpaceRadio(float cxEndSpaceRadio) {
            this.cxEndSpaceRadio = cxEndSpaceRadio;
        }

        public void setCyEndSpaceRadio(float cyEndSpaceRadio) {
            this.cyEndSpaceRadio = cyEndSpaceRadio;
        }

        public float getLenCx() {
            return lenCx;
        }

        public float getLenCy() {
            return lenCy;
        }

        public double getCxRealUnit() {
            return cxRealUnit;
        }

        public void setCyUnitValueFunc(CustomUnitValueFunc cyUnitValueFunc) {
            this.cyUnitValueFunc = cyUnitValueFunc;
        }

        public void setCxUnitValueFunc(CustomUnitValueFunc cxUnitValueFunc) {
            this.cxUnitValueFunc = cxUnitValueFunc;
        }
    }

    public static class PointD {

        public PointD() {
        }

        public PointD(double x, double y) {
            this.x = x;
            this.y = y;
        }

        public double x;
        public double y;
    }

    protected class CoordinateView extends View {

        private Coordinate coordinate;

        public CoordinateView(Context context) {
            super(context);
        }

        public void setCoordinate(Coordinate coordinate) {
            this.coordinate = coordinate;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            Log.i("Rambo", "CoordinateView onDraw()");
            if (getCoordPoints() == null || getCoordPoints().length <= 0) {
                return;
            }
            //画x，y轴
            //Coordinate coordinate = getCoordinate();
            coordinate.unitPaint.setPathEffect(null);
            double x, y;
            double cx, cy;
            String text;
            Rect cxRect = new Rect();
            Rect cyRect = new Rect();
            float startX = getCoordStartX();
            float startY = getCoordStartY();
            if (coordinate.cxDirection == Coordinate.DIRECTION.POSITIVE &&
                    coordinate.cyDirection == Coordinate.DIRECTION.POSITIVE) {
                if (coordinate.drawXCoord) {
                    canvas.drawLine(coordinate.coord.x, coordinate.coord.y, (1.0f - xEndMarginRadio)*width, coordinate.coord.y, coordinate.unitPaint);
                }
                if (coordinate.drawYCoord) {
                    canvas.drawLine(coordinate.coord.x, coordinate.coord.y, coordinate.coord.x, yEndMarginRadio*height, coordinate.unitPaint);
                }
                //绘制刻度
                if (coordinate.drawYCoordText) {
                    if (coordinate.isCustomCyUnit()) {
                        for (int i = 0; i < getCoordinate().cyUnitValue.length; i++) {
                            cy = startY - (i + 1)*coordinate.cyUnit;
                            text = coordinate.getCyFormat().format(coordinate.cyUnitValue[i]);
                            if (!TextUtils.isEmpty(text)) {
                                getTextRound(text, cyRect, coordinate.unitTextPaint);
                                canvas.drawText(text, coordinate.coord.x - xStartSpace + 5, (float)cy + cyRect.centerY(), coordinate.unitTextPaint);
                            }
                        }
                    } else {
                        for (int i= 0; i < getCoordPoints().length; i++) {
                            y = getCoordPoints()[i].y;
                            cy = startY - (y - coordinate.cyRange[0])*coordinate.cyUnit;
                            text = coordinate.getCyFormat().format("" + y);
                            if (!TextUtils.isEmpty(text)) {
                                getTextRound(text, cyRect, coordinate.unitTextPaint);
                                canvas.drawText(text, coordinate.coord.x - xStartSpace + 5, (float)cy + cyRect.centerY(), coordinate.unitTextPaint);
                            }
                        }
                    }
                }

            } else if (coordinate.cxDirection == Coordinate.DIRECTION.POSITIVE &&
                    coordinate.cyDirection == Coordinate.DIRECTION.NEGATIVE) {
                if (coordinate.drawXCoord) {
                    canvas.drawLine(coordinate.coord.x, coordinate.coord.y, (1.0f - xEndMarginRadio)*width, coordinate.coord.y, coordinate.unitPaint);
                }
                if (coordinate.drawYCoord) {
                    canvas.drawLine(coordinate.coord.x, coordinate.coord.y, coordinate.coord.x, (1.0f - yEndMarginRadio)*height, coordinate.unitPaint);
                }
                //绘制刻度
                if (coordinate.drawYCoordText) {
                    if (coordinate.isCustomCyUnit()) {
                        for (int i = 0; i < getCoordinate().cyUnitValue.length; i++) {
                            cy = startY + (i + 1)*coordinate.cyUnit;
                            text = coordinate.getCyFormat().format(coordinate.cyUnitValue[i]);
                            if (!TextUtils.isEmpty(text)) {
                                getTextRound(text, cyRect, coordinate.unitTextPaint);
                                canvas.drawText(text, coordinate.coord.x - xStartSpace + 5, (float)cy + cyRect.centerY(), coordinate.unitTextPaint);
                            }
                        }
                    } else {
                        for (int i= 0; i < getCoordPoints().length; i++) {
                            y = getCoordPoints()[i].y;
                            cy = startY + (y - coordinate.cyRange[0])*coordinate.cyUnit;
                            text = coordinate.getCyFormat().format("" + y);
                            if (!TextUtils.isEmpty(text)) {
                                getTextRound(text, cyRect, coordinate.unitTextPaint);
                                canvas.drawText(text, coordinate.coord.x - xStartSpace + 5, (float)cy + cyRect.centerY(), coordinate.unitTextPaint);
                            }
                        }
                    }
                }

            } else if (coordinate.cxDirection == Coordinate.DIRECTION.NEGATIVE &&
                    coordinate.cyDirection == Coordinate.DIRECTION.NEGATIVE) {
                if (coordinate.drawXCoord) {
                    canvas.drawLine(coordinate.coord.x, coordinate.coord.y, xEndMarginRadio*width, coordinate.coord.y, coordinate.unitPaint);
                }
                if (coordinate.drawYCoord) {
                    canvas.drawLine(coordinate.coord.x, coordinate.coord.y, coordinate.coord.x, (1.0f - xEndMarginRadio)*height, coordinate.unitPaint);
                }
                //绘制刻度
                if (coordinate.drawYCoordText) {
                    if (coordinate.isCustomCyUnit()) {
                        for (int i = 0; i < getCoordinate().cyUnitValue.length; i++) {
                            cy = startY + (i + 1)*coordinate.cyUnit;
                            text = coordinate.getCyFormat().format(coordinate.cyUnitValue[i]);
                            if (!TextUtils.isEmpty(text)) {
                                getTextRound(text, cyRect, coordinate.unitTextPaint);
                                canvas.drawText(text, coordinate.coord.x + (xStartSpace - cyRect.width() - 5), (float)cy + cyRect.centerY(), coordinate.unitTextPaint);
                            }
                        }
                    } else {
                        for (int i= 0; i < getCoordPoints().length; i++) {
                            y = getCoordPoints()[i].y;
                            cy = startY + (y - coordinate.cyRange[0])*coordinate.cyUnit;
                            text = coordinate.getCyFormat().format("" + y);
                            if (!TextUtils.isEmpty(text)) {
                                getTextRound(text, cyRect, coordinate.unitTextPaint);
                                canvas.drawText(text, coordinate.coord.x + (xStartSpace - cyRect.width() - 5), (float)cy + cyRect.centerY(), coordinate.unitTextPaint);
                            }
                        }
                    }
                }
            } else if (coordinate.cxDirection == Coordinate.DIRECTION.NEGATIVE &&
                    coordinate.cyDirection == Coordinate.DIRECTION.POSITIVE) {
                if (coordinate.drawXCoord) {
                    canvas.drawLine(coordinate.coord.x, coordinate.coord.y, xEndMarginRadio*width, coordinate.coord.y, coordinate.unitPaint);
                }
                if (coordinate.drawYCoord) {
                    canvas.drawLine(coordinate.coord.x, coordinate.coord.y, coordinate.coord.x, yEndMarginRadio*height, coordinate.unitPaint);
                }
                //绘制刻度
                if (coordinate.drawYCoordText) {
                    if (getCoordinate().isCustomCyUnit()) {
                        for (int i = 0; i < getCoordinate().cyUnitValue.length; i++) {
                            cy = startY - (i + 1)*coordinate.cyUnit;
                            text = coordinate.getCyFormat().format(getCoordinate().cyUnitValue[i]);
                            if (!TextUtils.isEmpty(text)) {
                                getTextRound(text, cyRect, coordinate.unitTextPaint);
                                canvas.drawText(text, coordinate.coord.x + (xStartSpace - cyRect.width() - 5), (float)cy - cyRect.centerY(), coordinate.unitTextPaint);
                            }

                            if (coordinate.unitDashLine) {
                                coordinate.unitPaint.setPathEffect(dashPathEffect);
                                dashPath.reset();
                                dashPath.moveTo(coordinate.coord.x, (float)cy);
                                dashPath.lineTo(xEndMarginRadio*width, (float)cy);
                                canvas.drawPath(dashPath, coordinate.unitPaint);
                            }
                        }

                    } else {
                        for (int i= 0; i < getCoordPoints().length; i++) {
                            y = getCoordPoints()[i].y;
                            cy = startY - (y - coordinate.cyRange[0])*coordinate.cyUnit;
                            text = coordinate.getCyFormat().format("" + y);
                            if (!TextUtils.isEmpty(text)) {
                                getTextRound(text, cyRect, coordinate.unitTextPaint);
                                canvas.drawText(text, coordinate.coord.x + (xStartSpace - cyRect.width() - 5), (float)cy - cyRect.centerY(), coordinate.unitTextPaint);
                            }
                        }
                    }
                }
            }
        }
    }

    protected void onDrawChart(Canvas canvas) {

    }

    protected boolean showStatusView(MotionEvent event) {
        return true;
    }

    protected boolean hideStatusView(MotionEvent event) {
        return true;
    }

    protected class ChartView extends View {

        private GestureDetector gestureDetector = null;

        public ChartView(Context context) {
            super(context);
            gestureDetector = new GestureDetector(context, simpleOnGestureListener);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            drawXCoord(canvas);
            onDrawChart(canvas);
        }

        private void drawXCoord(Canvas canvas) {
            double x;
            double cx;
            String text;
            Rect cxRect = new Rect();
            float startX = getCoordStartX();
            if (coordinate.cxDirection == Coordinate.DIRECTION.POSITIVE &&
                    coordinate.cyDirection == Coordinate.DIRECTION.POSITIVE) {
                //绘制刻度
                if (coordinate.drawXCoordText) {
                    if (coordinate.isCustomCxUnit()) {
                        int count = getCoordinate().cxUnitValue.length;
                        for (int i = 0; i < count; i++) {
                            if (coordinate.cxReverse) {
                                cx = startX + (count - i)*coordinate.cxUnit;
                            } else {
                                cx = startX + (i + 1)*coordinate.cxUnit;
                            }
                            text = coordinate.getCxFormat().format(coordinate.cxUnitValue[i]);
                            if (!TextUtils.isEmpty(text)) {
                                getTextRound(text, cxRect, coordinate.unitTextPaint);
                                canvas.drawText(text, (float)cx - cxRect.centerX(), coordinate.coord.y + coordinate.cyTextSpace + cxRect.height(), coordinate.unitTextPaint);
                            }
                        }
                    } else {
                        for (int i= 0; i < getCoordPoints().length; i++) {
                            x = getCoordPoints()[i].x;
                            if (coordinate.cxReverse) {
                                cx = startX + (coordinate.cxRange[1] - x)*coordinate.cxUnit;
                            } else {
                                cx = startX + (x - coordinate.cxRange[0])*coordinate.cxUnit;
                            }
                            text = coordinate.getCxFormat().format("" + x);
                            if (!TextUtils.isEmpty(text)) {
                                getTextRound(text, cxRect, coordinate.unitTextPaint);
                                canvas.drawText(text, (float)cx - cxRect.centerX(), coordinate.coord.y + coordinate.cyTextSpace + cxRect.height(), coordinate.unitTextPaint);
                            }
                        }
                    }
                }

            } else if (coordinate.cxDirection == Coordinate.DIRECTION.POSITIVE &&
                    coordinate.cyDirection == Coordinate.DIRECTION.NEGATIVE) {
                //绘制刻度
                if (coordinate.drawXCoordText) {
                    if (coordinate.isCustomCxUnit()) {
                        int count = getCoordinate().cxUnitValue.length;
                        for (int i = 0; i < count; i++) {
                            if (coordinate.cxReverse) {
                                cx = startX + (count - i)*coordinate.cxUnit;
                            } else {
                                cx = startX + (i + 1)*coordinate.cxUnit;
                            }
                            text = coordinate.getCxFormat().format(coordinate.cxUnitValue[i]);
                            if (!TextUtils.isEmpty(text)) {
                                getTextRound(text, cxRect, coordinate.unitTextPaint);
                                canvas.drawText(text, (float)cx - cxRect.centerX(), coordinate.coord.y - coordinate.cyTextSpace, coordinate.unitTextPaint);
                            }
                        }
                    } else {
                        for (int i= 0; i < getCoordPoints().length; i++) {
                            x = getCoordPoints()[i].x;
                            if (coordinate.cxReverse) {
                                cx = startX + (coordinate.cxRange[1] - x)*coordinate.cxUnit;
                            } else {
                                cx = startX + (x - coordinate.cxRange[0])*coordinate.cxUnit;
                            }
                            text = coordinate.getCxFormat().format("" + x);
                            if (!TextUtils.isEmpty(text)) {
                                getTextRound(text, cxRect, coordinate.unitTextPaint);
                                canvas.drawText(text, (float)cx - cxRect.centerX(), coordinate.coord.y - coordinate.cyTextSpace, coordinate.unitTextPaint);
                            }
                        }
                    }
                }

            } else if (coordinate.cxDirection == Coordinate.DIRECTION.NEGATIVE &&
                    coordinate.cyDirection == Coordinate.DIRECTION.NEGATIVE) {
                //绘制刻度
                if (coordinate.drawXCoordText) {
                    if (coordinate.isCustomCxUnit()) {
                        int count = getCoordinate().cxUnitValue.length;
                        for (int i = 0; i < count; i++) {
                            if (coordinate.cxReverse) {
                                cx = startX - (count - i)*coordinate.cxUnit;
                            } else {
                                cx = startX - (i + 1)*coordinate.cxUnit;
                            }
                            text = coordinate.getCxFormat().format(coordinate.cxUnitValue[i]);
                            if (!TextUtils.isEmpty(text)) {
                                getTextRound(text, cxRect, coordinate.unitTextPaint);
                                canvas.drawText(text, (float)cx - cxRect.centerX(), coordinate.coord.y - coordinate.cyTextSpace, coordinate.unitTextPaint);
                            }
                        }
                    } else {
                        for (int i= 0; i < getCoordPoints().length; i++) {
                            x = getCoordPoints()[i].x;
                            if (coordinate.cxReverse) {
                                cx = startX - (coordinate.cxRange[1] - x)*coordinate.cxUnit;
                            } else {
                                cx = startX - (x - coordinate.cxRange[0])*coordinate.cxUnit;
                            }
                            text = coordinate.getCxFormat().format("" + x);
                            if (!TextUtils.isEmpty(text)) {
                                getTextRound(text, cxRect, coordinate.unitTextPaint);
                                canvas.drawText(text, (float)cx - cxRect.centerX(), coordinate.coord.y - coordinate.cyTextSpace, coordinate.unitTextPaint);
                            }
                        }
                    }
                }
            } else if (coordinate.cxDirection == Coordinate.DIRECTION.NEGATIVE &&
                    coordinate.cyDirection == Coordinate.DIRECTION.POSITIVE) {
                //绘制刻度
                if (coordinate.drawXCoordText) {
                    if (getCoordinate().isCustomCxUnit()) {
                        int count = coordinate.cxUnitValue.length;
                        for (int i = 0; i < count; i++) {
                            if (coordinate.cxReverse) {
                                cx = startX - (count - i)*coordinate.cxUnit;
                            } else {
                                cx = startX - (i + 1)*coordinate.cxUnit;
                            }
                            cx += getChartScrollX();
                            text = coordinate.getCxFormat().format(getCoordinate().cxUnitValue[i]);
                            if (!TextUtils.isEmpty(text)) {
                                getTextRound(text, cxRect, coordinate.unitTextPaint);
                                canvas.drawText(text, (float)cx - cxRect.centerX(), coordinate.coord.y + coordinate.cyTextSpace + cxRect.height(), coordinate.unitTextPaint);
                            }
                        }
                    } else {
                        for (int i= 0; i < getCoordPoints().length; i++) {
                            x = getCoordPoints()[i].x;
                            if (coordinate.cxReverse) {
                                cx = startX - (coordinate.cxRange[1] - x)*coordinate.cxUnit;
                            } else {
                                cx = startX - (x - coordinate.cxRange[0])*coordinate.cxUnit;
                            }
                            cx += getChartScrollX();
                            text = coordinate.getCxFormat().format("" + x);
                            if (!TextUtils.isEmpty(text)) {
                                getTextRound(text, cxRect, coordinate.unitTextPaint);
                                canvas.drawText(text, (float)cx - cxRect.centerX(), coordinate.coord.y + coordinate.cyTextSpace + cxRect.height(), coordinate.unitTextPaint);
                            }
                        }
                    }
                }
            }
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (gestureDetector.onTouchEvent(event)) {

            } else {
                int action = event.getAction();
                if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                    Log.i("Rambo", "Charts onTouchEvent action = " + action);
                    if (scrollCallback != null) {
                        scrollCallback.onUpCallback(event);
                    }
                }
            }
            return true;
        }

        GestureDetector.SimpleOnGestureListener simpleOnGestureListener = new GestureDetector.SimpleOnGestureListener() {

            boolean inScrollMode = false;

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                Log.i("Rambo", "SimpleOnGestureListener onSingleTapUp");
                showStatusView(e);
                return true;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                Log.i("Rambo", "SimpleOnGestureListener onScroll");
                if (inScrollMode) {
                    Chart.this.hideStatusView(e2);
                }
                if (scrollCallback != null) {
                    scrollCallback.onScrollCallback(e1, e2, distanceX, distanceY);
                }
                return true;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                Log.i("Rambo", "SimpleOnGestureListener onFling velocityX = " + velocityX);
                if (scrollCallback != null) {
                    scrollCallback.onFlingCallback(e1, e2, velocityX, velocityY);
                }
                return true;
            }

            @Override
            public boolean onDown(MotionEvent e) {
                Log.i("Rambo", "SimpleOnGestureListener onDown downX = " + e.getX() + ", downY = " + e.getY());
                inScrollMode = Chart.this.inScrollMode();
                if (scrollCallback != null) {
                    scrollCallback.onDownCallback(e);
                }
                return super.onDown(e);
            }

            @Override
            public void onLongPress(MotionEvent e) {
                super.onLongPress(e);
                Log.i("Rambo", "SimpleOnGestureListener onLongPress");
                showStatusView(e);
            }
        };
    }

    public void setScrollCallback(ScrollCallback scrollCallback) {
        this.scrollCallback = scrollCallback;
    }

    public int[] getScrollIndex() {
        return scrollIndex;
    }

    public void setScrollIndex(int[] scrollIndex) {
        this.scrollIndex = scrollIndex;
        if (totalCoordPoints != null && totalCoordPoints.length > 0) {
            int count = scrollIndex[1] - scrollIndex[0] + 1;
            PointD[] pointDs = new PointD[count];
            System.arraycopy(totalCoordPoints, scrollIndex[0], pointDs, 0, count);
            setCoordPoints(pointDs);
        }
    }

    public interface ScrollCallback {
        void onDownCallback(MotionEvent e);
        void onScrollCallback(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY);
        void onFlingCallback(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY);
        void onUpCallback(MotionEvent e);
    }

    private class DefaultScrollCallback implements ScrollCallback {

        int startIndex = 0;
        int endIndex = 0;
        int pointsCount = 0;
        float downX = 0.0f;
        float downY = 0.0f;
        float scrollX = 0.0f;
        ValueAnimator valueAnimator = null;
        float MAX_VELX = 2500;
        boolean inDefaultScrollMode = false;
        float perUnitLenX = 0.0f;

        @Override
        public void onDownCallback(MotionEvent e) {
            inDefaultScrollMode = inDefaultScrollMode();
            cancelFlingAnim();
            startIndex = scrollIndex[0];
            endIndex = scrollIndex[1];
            pointsCount = scrollIndex[1] - scrollIndex[0] + 1;
            downX = e.getX();
            downY = e.getY();
            perUnitLenX = getCoordinate().getLenCx() / (pointsCount - 1);
        }

        @Override
        public void onScrollCallback(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if (inDefaultScrollMode && pointsCount > 1) {
                if (scrollCallback != null) {
                    handleScrollEvent(e2.getX());
                }
            }
        }

        @Override
        public void onFlingCallback(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (inDefaultScrollMode && pointsCount > 0) {
                float tempVelocityX = velocityX;
                if (Math.abs(velocityX) > MAX_VELX) {
                    if (velocityX < 0) {
                        tempVelocityX = -MAX_VELX;
                    } else {
                        tempVelocityX = MAX_VELX;
                    }
                }
                float percentVx = tempVelocityX / MAX_VELX;
                float startX = e2.getX();

                float lenX = getCoordinate().getLenCx() * 2 / 3;
                float deltaX = percentVx * lenX;
                float endDeltaX = startX + deltaX - downX;
                float endDeltaPercent = endDeltaX / getCoordinate().getLenCx();
                float endPointCount = (pointsCount - 1) * Math.abs(endDeltaPercent);
                float extra = (float)((endPointCount - Math.floor(endPointCount)) * perUnitLenX);

                float endX = startX + deltaX;
                if (startX - downX < 0) {
                    endX += extra;
                } else {
                    endX -= extra;
                }
                Log.i("Rambo222", "onFlingCallback endPointCount = " + endPointCount + ", extra = " + extra + ", downX = " + downX + ", startX = " + startX + ", endX = " + endX + ", perUnitLenX = " + perUnitLenX);
                startFlingAnim(startX, endX);
            }
        }

        @Override
        public void onUpCallback(MotionEvent e) {
            if (inDefaultScrollMode && pointsCount > 0) {
                float x = e.getX();
                float percent = (x - downX) / getCoordinate().getLenCx();
                float originDelta = (pointsCount - 1) * Math.abs(percent);
                float extra = (float)((originDelta - Math.floor(originDelta)) * perUnitLenX);
                float endX = x;
                if (percent < 0) {
                    endX += extra;
                } else {
                    endX -= extra;
                }
                Log.i("Rambo222", "onUpCallback originDelta = " + originDelta + ", x = " + x + ", downX = " + downX + ", endX = " + endX + ", extra = " + extra);
                startFlingAnim(x, endX);
            }
        }

        private void handleScrollUpEvent(float x) {
            float percent = (x - downX) / getCoordinate().getLenCx();
            int delta = Math.round((pointsCount - 1) * Math.abs(percent));
            if (percent < 0) {
                if (scrollIndex[1] + delta > totalCoordPoints.length - 1) {
                    scrollIndex[1] = totalCoordPoints.length - 1;
                    scrollIndex[0] = scrollIndex[1] - pointsCount + 1;
                } else {
                    scrollIndex[0] += delta;
                    scrollIndex[1] += delta;
                }
            } else {
                if (scrollIndex[0] - delta < 0) {
                    scrollIndex[0] = 0;
                    scrollIndex[1] = scrollIndex[0] + pointsCount - 1;
                } else {
                    scrollIndex[0] -= delta;
                    scrollIndex[1] -= delta;
                }
            }
            Log.i("Rambo222", "handleScrollUpEvent startIndex = " + startIndex + ", endIndex = " + endIndex + ", scrollIndex[0] = " + scrollIndex[0] + ", scrollIndex[1] = " + scrollIndex[1]);
            int count = scrollIndex[1] - scrollIndex[0] + 1;
            Chart.PointD[] points = new Chart.PointD[count];
            System.arraycopy(totalCoordPoints, scrollIndex[0], points, 0, count);
            if (points != null && points.length > 0) {
                scrollX = 0.0f;
                setChartScrollX(scrollX);
                setCoordPoints(points);
                invalidate();
            }
        }

        private void handleScrollEvent(float x) {
            //Log.i("Rambo111", "handleScrollEvent() downX = " + downX + ", x = " + x);
            float percent = (x - downX) / getCoordinate().getLenCx();
            int delta = (int)Math.ceil((pointsCount - 1) * Math.abs(percent));
            if (percent < 0) {
                if (scrollIndex[1] + delta > totalCoordPoints.length - 1) {
                    endIndex = totalCoordPoints.length - 1;
                    startIndex = scrollIndex[0];
                    scrollX = (scrollIndex[1]- endIndex) * perUnitLenX;
                } else {
                    startIndex = scrollIndex[0];
                    endIndex = scrollIndex[1] + delta;
                    scrollX = x - downX;
                }
            } else {
                if (scrollIndex[0] - delta < 0) {
                    startIndex = 0;
                    endIndex = scrollIndex[1];
                    scrollX = (scrollIndex[0] - startIndex) * perUnitLenX;
                } else {
                    startIndex = scrollIndex[0] - delta;
                    endIndex = scrollIndex[1];
                    scrollX = x - downX;
                }
            }
            Log.i("Rambo222", "handleScrollEvent startIndex = " + startIndex + ", endIndex = " + endIndex + ", scrollIndex[0] = " + scrollIndex[0] + ", scrollIndex[1] = " + scrollIndex[1]);
            int count = endIndex - startIndex + 1;
            Chart.PointD[] pointDs = new Chart.PointD[count];
            System.arraycopy(totalCoordPoints, startIndex, pointDs, 0, count);
            if (pointDs != null && pointDs.length > 0) {
                updateCoordPoints(pointDs);
                setChartScrollX(scrollX);
                invalidate();
            }
        }

        private void startFlingAnim(float startX, final float endX) {
            Log.i("Rambo222", "cancelFlingAnim start");
            cancelFlingAnim();
            Log.i("Rambo222", "cancelFlingAnim finish");
            valueAnimator = new ValueAnimator();
            valueAnimator.setDuration(500);
            valueAnimator.setInterpolator(new ScrollInterpolator());
            valueAnimator.setFloatValues(startX, endX);
            valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float value = (float) animation.getAnimatedValue();
                    handleScrollEvent(value);
                }
            });
            valueAnimator.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {

                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    Log.i("Rambo222", "valueAnimator onAnimationEnd");
                    handleScrollUpEvent(endX);
                }

                @Override
                public void onAnimationCancel(Animator animation) {

                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            });
            Log.i("Rambo222", "valueAnimator start");
            valueAnimator.start();
            Log.i("Rambo222", "valueAnimator start finish");
        }

        private void cancelFlingAnim() {
            if (valueAnimator != null && valueAnimator.isRunning()) {
                valueAnimator.cancel();
            }
            valueAnimator = null;
        }
    }

    public static class ScrollInterpolator implements Interpolator {
        public ScrollInterpolator() {
        }

        public float getInterpolation(float t) {
            t -= 1.0f;
            return t * t * t * t * t + 1;
        }
    }

    protected void onDrawStatus(Canvas canvas) {

    }

    protected class StatusView extends View {

        public StatusView(Context context) {
            super(context);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            onDrawStatus(canvas);
        }
    }
}
