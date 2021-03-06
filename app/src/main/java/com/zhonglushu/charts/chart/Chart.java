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
import android.view.ViewParent;
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
    protected float xStartMargin = 0.0f;
    protected float xEndMargin = 0.0f;
    protected float yStartMargin = 0.0f;
    protected float yEndMargin = 0.0f;
    protected int width;
    protected int height;
    private Path dashPath = new Path();
    private DashPathEffect dashPathEffect = null;
    //保存最高y轴的点，用于绘制渐变的颜色
    protected PointD cyMaxPoint;
    protected XCoordinateView xCoordinateView = null;
    protected YCoordinateView yCoordinateView = null;
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
    private GestureDetector gestureDetector = null;
    private boolean drawCyValue = false;
    private boolean showStatus = false;

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
        gestureDetector = new GestureDetector(getContext(), simpleOnGestureListener);
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
        if (xCoordinateView != null) {
            xCoordinateView.invalidate();
        }
    }

    public void invalidateCooridateView() {
        if (xCoordinateView != null) {
            xCoordinateView.invalidate();
        }
        if (yCoordinateView != null) {
            yCoordinateView.invalidate();
        }
    }

    public void invalidateStatusView() {
        if (statusView != null && isShowStatus()) {
            statusView.invalidate();
        }
    }

    public void invalidateStatusView(Rect rect) {
        if (statusView != null && isShowStatus()) {
            statusView.invalidate(rect);
        }
    }

    public Coordinate getCoordinate() {
        return coordinate;
    }

    public float getXCoordinateLen() {
        return coordinate.getLenCx();
    }

    public float getYCoordinateLen() {
        return coordinate.getLenCy();
    }

    public void setCoordPoints(PointD[] coordPoints) {
        this.coordPoints = coordPoints;
        updateRange();
    }

    public void setScrollIndex(int[] index) {
        this.scrollIndex = index;
        if (totalCoordPoints != null && totalCoordPoints.length > 0) {
            int count = scrollIndex[1] - scrollIndex[0] + 1;
            PointD[] pointDs = new PointD[count];
            System.arraycopy(totalCoordPoints, scrollIndex[0], pointDs, 0, count);
            setCoordPoints(pointDs);
        }
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

    public float getxStartMargin() {
        return xStartMargin;
    }

    public void setxStartMargin(float xStartMargin) {
        this.xStartMargin = xStartMargin;
    }

    public float getxEndMargin() {
        return xEndMargin;
    }

    public void setxEndMargin(float xEndMargin) {
        this.xEndMargin = xEndMargin;
    }

    public float getyStartMargin() {
        return yStartMargin;
    }

    public void setyStartMargin(float yStartMargin) {
        this.yStartMargin = yStartMargin;
    }

    public float getyEndMargin() {
        return yEndMargin;
    }

    public void setyEndMargin(float yEndMargin) {
        this.yEndMargin = yEndMargin;
    }

    public float getChartScrollX() {
        return chartScrollX;
    }

    public void setChartScrollX(float chartScrollX) {
        this.chartScrollX = chartScrollX;
    }

    protected void updateRange() {
        int count = getCoordPoints().length;
        double minCx = Double.MAX_VALUE, maxCx = 0.0f;
        double minCy = Double.MAX_VALUE, maxCy = 0.0f;
        int index = 0;
        for (int i = 0; i < count; i++) {
            double tempX = getCoordPoints()[i].x;
            minCx = Math.min(minCx, tempX);
            maxCx = Math.max(maxCx, tempX);
            double tempY = getCoordPoints()[i].y;
            minCy = Math.min(minCy, tempY);
            if (tempY > maxCy) {
                index = i;
                maxCy = tempY;
            }
        }
        cyMaxPoint = getCoordPoints()[index];
        if (getCoordinate().getCxUnitValueFunc() != null) {
            getCoordinate().updateCxUnitValues(coordPoints);
        } else {
            getCoordinate().setCxRange(minCx, maxCx);
        }
        if (getCoordinate().getCyUnitValueFunc() != null) {
            getCoordinate().updateCyUnitValues(coordPoints);
        } else {
            getCoordinate().setCyRange(minCy, maxCy);
        }
        updateCoord();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        Log.i("Rambo", "Chart onFinishInflate()");
        ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        xCoordinateView = new XCoordinateView(getContext());
        xCoordinateView.setCoordinate(coordinate);
        this.addView(xCoordinateView, layoutParams);
        yCoordinateView = new YCoordinateView(getContext());
        yCoordinateView.setCoordinate(coordinate);
        this.addView(yCoordinateView, layoutParams);
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
                float chartLeft = 0.0f;
                float chartRight = 0.0f;
                if (getCoordinate().cxDirection == Coordinate.DIRECTION.POSITIVE) {
                    chartLeft = getMarginStartX();
                    chartRight = getMarginEndX();
                } else {
                    chartLeft = getMarginEndX();
                    chartRight = getMarginStartX();
                }
                Log.i("Rambo222", "Chart onLayout() chartLeft = " + chartLeft + ", top = " + top + ", chartRight = " + chartRight + ", bottom = " + bottom);
                getChildAt(i).layout((int)chartLeft, 0, (int)chartRight, bottom - top);
                break;
            }
        }
    }

    private void updateCoord() {
        getCoordinate().updateCoord(width, height, xStartMargin, xEndMargin, yStartMargin, yEndMargin);
    }

    public float getCoordStartX() {
        if (getCoordinate().cxDirection == Coordinate.DIRECTION.POSITIVE) {
            return xStartMargin + getCoordinate().cxStartPadding;
        } else {
            return width - xStartMargin - getCoordinate().cxStartPadding;
        }
    }

    public float getCoordEndX() {
        if (getCoordinate().cxDirection == Coordinate.DIRECTION.POSITIVE) {
            return width - xEndMargin - getCoordinate().cxEndPadding;
        } else {
            return xEndMargin + getCoordinate().cxEndPadding;
        }
    }

    public float getCoordStartY() {
        if (getCoordinate().cyDirection == Coordinate.DIRECTION.POSITIVE) {
            return height - yStartMargin - getCoordinate().cyStartPadding;
        } else {
            return yStartMargin + getCoordinate().cyStartPadding;
        }
    }

    public float getCoordEndY() {
        if (getCoordinate().cyDirection == Coordinate.DIRECTION.POSITIVE) {
            return yEndMargin + getCoordinate().cyEndPadding;
        } else {
            return height - yEndMargin - getCoordinate().cyEndPadding;
        }
    }

    public float getMarginStartX() {
        if (getCoordinate().cxDirection == Coordinate.DIRECTION.POSITIVE) {
            return xStartMargin;
        } else {
            return width - xStartMargin;
        }
    }

    public float getMarginEndX() {
        if (getCoordinate().cxDirection == Coordinate.DIRECTION.POSITIVE) {
            return width - xEndMargin;
        } else {
            return xEndMargin;
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

    public double transYToPosition(double y) {
        float startY = getCoordStartY();
        if (getCoordinate().cyDirection == Coordinate.DIRECTION.POSITIVE) {
            return startY - (y - getCoordinate().cyRange[0])*getCoordinate().cyRealUnit;
        } else {
            return startY + (y - getCoordinate().cyRange[0])*getCoordinate().cyRealUnit;
        }
    }

    public double transXToChartViewPosition(double x) {
        float startX = getCoordStartX();
        if (getCoordinate().cxDirection == Coordinate.DIRECTION.POSITIVE) {
            startX -= xStartMargin;
            if (getCoordinate().cxReverse) {
                return startX + (getCoordinate().cxRange[1] - x)*getCoordinate().cxRealUnit;
            } else {
                return startX + (x - getCoordinate().cxRange[0])*getCoordinate().cxRealUnit;
            }
        } else {
            startX -= xEndMargin;
            if (getCoordinate().cxReverse) {
                return startX  - (getCoordinate().cxRange[1] - x)*getCoordinate().cxRealUnit;
            } else {
                return startX  - (x - getCoordinate().cxRange[0])*getCoordinate().cxRealUnit;
            }
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
            scrollCallback = new DefaultScrollCallback();
        }
    }

    /**
     * 滚动模式需要设置所有所标点的数组
     * @return
     */
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

    public int getTotalPointCount() {
        if (totalCoordPoints == null) {
            return 0;
        } else {
            return totalCoordPoints.length;
        }
    }

    public boolean isDrawCyValue() {
        return drawCyValue;
    }

    public void setDrawCyValue(boolean drawCyValue) {
        this.drawCyValue = drawCyValue;
    }

    public boolean isShowStatus() {
        return showStatus;
    }

    public void setShowStatus(boolean showStatus) {
        this.showStatus = showStatus;
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
        float cxStartPadding = 0.0f;
        float cyStartPadding = 0.0f;
        float cxEndPadding = 0.0f;
        float cyEndPadding = 0.0f;
        //刻度文本与坐标轴的距离
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

        public void updateCxUnitValues(PointD[] pointDs) {
            if (pointDs != null) {
                if (cxUnitValueFunc != null) {
                    String[] temp = cxUnitValueFunc.unitValues(pointDs, cxRange);
                    if (temp != null) {
                        cxUnitValue = temp;
                    }
                }
            }
        }

        public void updateCyUnitValues(PointD[] pointDs) {
            if (pointDs != null) {
                if (cyUnitValueFunc != null) {
                    String[] temp = cyUnitValueFunc.unitValues(pointDs, cyRange);
                    if (temp != null) {
                        cyUnitValue = temp;
                    }
                }
            }
        }

        public void updateCoord(int width, int height, float xStart, float xEnd,
                                float yStart, float yEnd) {
            if (cxDirection == DIRECTION.POSITIVE) {
                coord.x = xStart;
            } else {
                coord.x = width - xStart;
            }
            if(cyDirection == DIRECTION.POSITIVE) {
                coord.y = height - yStart;
            } else {
                coord.y = yStart;
            }

            lenCx = width - xStart - xEnd - cxStartPadding - cxEndPadding;
            lenCy = height - yStart - yEnd - cyStartPadding - cyEndPadding;
            if (Double.compare(cxRange[1], cxRange[0]) == 0) {
                cxRealUnit = 0.0f;
            } else {
                cxRealUnit = lenCx / (cxRange[1] - cxRange[0]);
            }
            if (Double.compare(cyRange[1], cyRange[0]) == 0) {
                cyRealUnit = 0.0f;
            } else {
                cyRealUnit = lenCy / (cyRange[1] - cyRange[0]);
            }
            if (isCustomCxUnit()) {
                if (cxUnitValue.length <= 1) {
                    cxUnit = 0.0f;
                } else {
                    cxUnit = lenCx / (cxUnitValue.length - 1);
                }
            } else {
                cxUnit = cxRealUnit;
            }
            if (isCustomCyUnit()) {
                if (cyUnitValue.length <= 1) {
                    cyUnit = 0.0f;
                } else {
                    cyUnit = lenCy / (cyUnitValue.length - 1);
                }
            } else {
                cyUnit = cyRealUnit;
            }
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

        public void setCyTextSpace(float cyTextSpace) {
            this.cyTextSpace = cyTextSpace;
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

        public void setCxStartPadding(float cxStartPadding) {
            this.cxStartPadding = cxStartPadding;
        }

        public void setCyStartPadding(float cyStartPadding) {
            this.cyStartPadding = cyStartPadding;
        }

        public void setCxEndPadding(float cxEndPadding) {
            this.cxEndPadding = cxEndPadding;
        }

        public void setCyEndPadding(float cyEndPadding) {
            this.cyEndPadding = cyEndPadding;
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

        public CustomUnitValueFunc getCxUnitValueFunc() {
            return cxUnitValueFunc;
        }

        public CustomUnitValueFunc getCyUnitValueFunc() {
            return cyUnitValueFunc;
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

    protected class XCoordinateView extends View {

        private Coordinate coordinate;

        public XCoordinateView(Context context) {
            super(context);
        }

        public void setCoordinate(Coordinate coordinate) {
            this.coordinate = coordinate;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (getCoordPoints() == null || getCoordPoints().length <= 0) {
                return;
            }
            coordinate.unitPaint.setPathEffect(null);
            drawXCoord(canvas);
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
                                cx = startX + (count - 1 - i)*coordinate.cxUnit;
                            } else {
                                cx = startX + i * coordinate.cxUnit;
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
                                cx = startX + (count - 1 - i)*coordinate.cxUnit;
                            } else {
                                cx = startX + i * coordinate.cxUnit;
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
                                cx = startX - (count - 1 - i)*coordinate.cxUnit;
                            } else {
                                cx = startX - i * coordinate.cxUnit;
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
                                cx = startX - (count - 1 - i)*coordinate.cxUnit;
                            } else {
                                cx = startX - i * coordinate.cxUnit;
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
    }

    protected class YCoordinateView extends View {

        private Coordinate coordinate;

        public YCoordinateView(Context context) {
            super(context);
        }

        public void setCoordinate(Coordinate coordinate) {
            this.coordinate = coordinate;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (getCoordPoints() == null || getCoordPoints().length <= 0) {
                return;
            }
            coordinate.unitPaint.setPathEffect(null);
            drawYCoord(canvas);
        }

        private void drawYCoord(Canvas canvas) {
            //画y轴
            double y, cy;
            String text;
            Rect cyRect = new Rect();
            float startY = getCoordStartY();
            if (coordinate.cxDirection == Coordinate.DIRECTION.POSITIVE &&
                    coordinate.cyDirection == Coordinate.DIRECTION.POSITIVE) {
                if (coordinate.drawXCoord) {
                    canvas.drawLine(coordinate.coord.x, coordinate.coord.y, width - xEndMargin, coordinate.coord.y, coordinate.unitPaint);
                }
                if (coordinate.drawYCoord) {
                    canvas.drawLine(coordinate.coord.x, coordinate.coord.y, coordinate.coord.x, yEndMargin, coordinate.unitPaint);
                }
                //绘制刻度
                if (coordinate.drawYCoordText) {
                    if (coordinate.isCustomCyUnit()) {
                        for (int i = 0; i < getCoordinate().cyUnitValue.length; i++) {
                            cy = startY - i * coordinate.cyUnit;
                            text = coordinate.getCyFormat().format(coordinate.cyUnitValue[i]);
                            if (!TextUtils.isEmpty(text)) {
                                getTextRound(text, cyRect, coordinate.unitTextPaint);
                                canvas.drawText(text, coordinate.coord.x - xStartMargin + 5, (float)cy - cyRect.centerY(), coordinate.unitTextPaint);
                            }
                        }
                    } else {
                        for (int i= 0; i < getCoordPoints().length; i++) {
                            y = getCoordPoints()[i].y;
                            cy = startY - (y - coordinate.cyRange[0])*coordinate.cyUnit;
                            text = coordinate.getCyFormat().format("" + y);
                            if (!TextUtils.isEmpty(text)) {
                                getTextRound(text, cyRect, coordinate.unitTextPaint);
                                canvas.drawText(text, coordinate.coord.x - xStartMargin + 5, (float)cy - cyRect.centerY(), coordinate.unitTextPaint);
                            }
                        }
                    }
                }

            } else if (coordinate.cxDirection == Coordinate.DIRECTION.POSITIVE &&
                    coordinate.cyDirection == Coordinate.DIRECTION.NEGATIVE) {
                if (coordinate.drawXCoord) {
                    canvas.drawLine(coordinate.coord.x, coordinate.coord.y, width - xEndMargin, coordinate.coord.y, coordinate.unitPaint);
                }
                if (coordinate.drawYCoord) {
                    canvas.drawLine(coordinate.coord.x, coordinate.coord.y, coordinate.coord.x, height - yEndMargin, coordinate.unitPaint);
                }
                //绘制刻度
                if (coordinate.drawYCoordText) {
                    if (coordinate.isCustomCyUnit()) {
                        for (int i = 0; i < getCoordinate().cyUnitValue.length; i++) {
                            cy = startY + i * coordinate.cyUnit;
                            text = coordinate.getCyFormat().format(coordinate.cyUnitValue[i]);
                            if (!TextUtils.isEmpty(text)) {
                                getTextRound(text, cyRect, coordinate.unitTextPaint);
                                canvas.drawText(text, coordinate.coord.x - xStartMargin + 5, (float)cy + cyRect.centerY(), coordinate.unitTextPaint);
                            }
                        }
                    } else {
                        for (int i= 0; i < getCoordPoints().length; i++) {
                            y = getCoordPoints()[i].y;
                            cy = startY + (y - coordinate.cyRange[0])*coordinate.cyUnit;
                            text = coordinate.getCyFormat().format("" + y);
                            if (!TextUtils.isEmpty(text)) {
                                getTextRound(text, cyRect, coordinate.unitTextPaint);
                                canvas.drawText(text, coordinate.coord.x - xStartMargin + 5, (float)cy + cyRect.centerY(), coordinate.unitTextPaint);
                            }
                        }
                    }
                }

            } else if (coordinate.cxDirection == Coordinate.DIRECTION.NEGATIVE &&
                    coordinate.cyDirection == Coordinate.DIRECTION.NEGATIVE) {
                if (coordinate.drawXCoord) {
                    canvas.drawLine(coordinate.coord.x, coordinate.coord.y, xEndMargin, coordinate.coord.y, coordinate.unitPaint);
                }
                if (coordinate.drawYCoord) {
                    canvas.drawLine(coordinate.coord.x, coordinate.coord.y, coordinate.coord.x, height - xEndMargin, coordinate.unitPaint);
                }
                //绘制刻度
                if (coordinate.drawYCoordText) {
                    if (coordinate.isCustomCyUnit()) {
                        for (int i = 0; i < getCoordinate().cyUnitValue.length; i++) {
                            cy = startY + i * coordinate.cyUnit;
                            text = coordinate.getCyFormat().format(coordinate.cyUnitValue[i]);
                            if (!TextUtils.isEmpty(text)) {
                                getTextRound(text, cyRect, coordinate.unitTextPaint);
                                canvas.drawText(text, coordinate.coord.x + (xStartMargin - cyRect.width() - 5), (float)cy + cyRect.centerY(), coordinate.unitTextPaint);
                            }
                        }
                    } else {
                        for (int i= 0; i < getCoordPoints().length; i++) {
                            y = getCoordPoints()[i].y;
                            cy = startY + (y - coordinate.cyRange[0])*coordinate.cyUnit;
                            text = coordinate.getCyFormat().format("" + y);
                            if (!TextUtils.isEmpty(text)) {
                                getTextRound(text, cyRect, coordinate.unitTextPaint);
                                canvas.drawText(text, coordinate.coord.x + (xStartMargin - cyRect.width() - 5), (float)cy + cyRect.centerY(), coordinate.unitTextPaint);
                            }
                        }
                    }
                }
            } else if (coordinate.cxDirection == Coordinate.DIRECTION.NEGATIVE &&
                    coordinate.cyDirection == Coordinate.DIRECTION.POSITIVE) {
                if (coordinate.drawXCoord) {
                    canvas.drawLine(coordinate.coord.x, coordinate.coord.y, xEndMargin, coordinate.coord.y, coordinate.unitPaint);
                }
                if (coordinate.drawYCoord) {
                    canvas.drawLine(coordinate.coord.x, coordinate.coord.y, coordinate.coord.x, yEndMargin, coordinate.unitPaint);
                }
                //绘制刻度
                if (coordinate.drawYCoordText) {
                    if (getCoordinate().isCustomCyUnit()) {
                        for (int i = 0; i < getCoordinate().cyUnitValue.length; i++) {
                            cy = startY - i * coordinate.cyUnit;
                            text = coordinate.getCyFormat().format(getCoordinate().cyUnitValue[i]);
                            if (!TextUtils.isEmpty(text)) {
                                getTextRound(text, cyRect, coordinate.unitTextPaint);
                                canvas.drawText(text, coordinate.coord.x + (xStartMargin - cyRect.width() - 5), (float)cy - cyRect.centerY(), coordinate.unitTextPaint);
                            }

                            if (coordinate.unitDashLine && i != 0) {
                                coordinate.unitPaint.setPathEffect(dashPathEffect);
                                dashPath.reset();
                                dashPath.moveTo(coordinate.coord.x, (float)cy);
                                dashPath.lineTo(xEndMargin, (float)cy);
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
                                canvas.drawText(text, coordinate.coord.x + (xStartMargin - cyRect.width() - 5), (float)cy - cyRect.centerY(), coordinate.unitTextPaint);
                            }
                        }
                    }
                }
            }
        }
    }

    protected void onDrawChart(Canvas canvas) {

    }

    protected boolean customDrawChart() {
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (getCoordPoints() == null || getCoordPoints().length <= 0) {
            return true;
        }
        if (gestureDetector.onTouchEvent(event)) {
            ViewParent parent = getParent();
            while (parent != null) {
                parent.requestDisallowInterceptTouchEvent(true);
                parent = parent.getParent();
            }
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

    protected class ChartView extends View {

        public ChartView(Context context) {
            super(context);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            if (getCoordPoints() == null || getCoordPoints().length <= 0) {
                return;
            }
            if (!customDrawChart()) {
                super.onDraw(canvas);
            }
            onDrawChart(canvas);
        }
    }

    GestureDetector.SimpleOnGestureListener simpleOnGestureListener = new GestureDetector.SimpleOnGestureListener() {

        boolean inScrollMode = false;

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            Log.i("Rambo", "SimpleOnGestureListener onSingleTapUp");
            if (isShowStatus()) {
                showStatusView(e);
            }
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            Log.i("Rambo", "SimpleOnGestureListener onScroll");
            if (inScrollMode && isShowStatus()) {
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
            if (isShowStatus()) {
                showStatusView(e);
            }
        }
    };

    public void setScrollCallback(ScrollCallback scrollCallback) {
        this.scrollCallback = scrollCallback;
    }

    public int[] getScrollIndex() {
        return scrollIndex;
    }

    public void reset() {
        if (this.scrollCallback != null) {
            this.scrollCallback.reset();
        }
    }

    public interface ScrollCallback {
        void onDownCallback(MotionEvent e);
        void onScrollCallback(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY);
        void onFlingCallback(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY);
        void onUpCallback(MotionEvent e);
        void reset();
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
        float perUnitLenX = 0.0f;

        @Override
        public void onDownCallback(MotionEvent e) {
            cancelFlingAnim();
            startIndex = scrollIndex[0];
            endIndex = scrollIndex[1];
            pointsCount = scrollIndex[1] - scrollIndex[0] + 1;
            downX = e.getX();
            downY = e.getY();
            perUnitLenX = getXCoordinateLen() / (pointsCount - 1);
        }

        @Override
        public void onScrollCallback(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if (pointsCount > 1) {
                handleScrollEvent(e2.getX());
            }
        }

        @Override
        public void onFlingCallback(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (pointsCount > 0) {
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

                float lenX = getXCoordinateLen() * 2 / 3;
                float deltaX = percentVx * lenX;
                float endDeltaX = startX + deltaX - downX;
                float endDeltaPercent = endDeltaX / getXCoordinateLen();
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
            if (pointsCount > 0) {
                float x = e.getX();
                float percent = (x - downX) / getXCoordinateLen();
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

        @Override
        public void reset() {
            cancelFlingAnim();
        }

        private void handleScrollUpEvent(float x) {
            float temp = x - downX;
            if (!getCoordinate().cxReverse) {
                temp = -temp;
            }
            float percent = temp / getXCoordinateLen();
            int delta = Math.round((pointsCount - 1) * Math.abs(percent));
            if (percent < 0) {
                int count = getTotalPointCount();
                if (scrollIndex[1] + delta > count - 1) {
                    scrollIndex[1] = count - 1;
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
            PointD[] points = new PointD[count];
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
            float temp = x - downX;
            if (!getCoordinate().cxReverse) {
                temp = -temp;
            }
            float percent = temp / getXCoordinateLen();
            int delta = (int)Math.ceil((pointsCount - 1) * Math.abs(percent));
            if (percent < 0) {
                int count = getTotalPointCount();
                if (scrollIndex[1] + delta > count - 1) {
                    endIndex = count - 1;
                    startIndex = scrollIndex[0];
                    if (getCoordinate().cxReverse) {
                        scrollX = (scrollIndex[1] - endIndex) * perUnitLenX;
                    } else {
                        scrollX = (endIndex - scrollIndex[1]) * perUnitLenX;
                    }
                } else {
                    startIndex = scrollIndex[0];
                    endIndex = scrollIndex[1] + delta;
                    scrollX = x - downX;
                }
            } else {
                if (scrollIndex[0] - delta < 0) {
                    startIndex = 0;
                    endIndex = scrollIndex[1];
                    if (getCoordinate().cxReverse) {
                        scrollX = (scrollIndex[0] - startIndex) * perUnitLenX;
                    } else {
                        scrollX = (startIndex - scrollIndex[0]) * perUnitLenX;
                    }
                } else {
                    startIndex = scrollIndex[0] - delta;
                    endIndex = scrollIndex[1];
                    scrollX = x - downX;
                }
            }
            Log.i("Rambo222", "handleScrollEvent startIndex = " + startIndex + ", endIndex = " + endIndex + ", scrollIndex[0] = " + scrollIndex[0] + ", scrollIndex[1] = " + scrollIndex[1]);
            int count = endIndex - startIndex + 1;
            PointD[] pointDs = new PointD[count];
            System.arraycopy(totalCoordPoints, startIndex, pointDs, 0, count);
            if (pointDs != null && pointDs.length > 0) {
                updateCoordPoints(pointDs);
                setChartScrollX(scrollX);
                invalidate();
            }
        }

        private void startFlingAnim(float startX, final float endX) {
            cancelFlingAnim();
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
                    handleScrollUpEvent(endX);
                }

                @Override
                public void onAnimationCancel(Animator animation) {

                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            });
            valueAnimator.start();
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

    protected class StatusView extends View {

        public StatusView(Context context) {
            super(context);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (isShowStatus()) {
                onDrawStatus(canvas);
            }
        }
    }

    protected boolean showStatusView(MotionEvent event) {
        return false;
    }

    protected boolean hideStatusView(MotionEvent event) {
        return false;
    }

    protected void onDrawStatus(Canvas canvas) {

    }

    public static class Status {
        int textSize;
        int textColor;
        String text;
        int index = -1;
        Rect textRect = new Rect();
        Paint textPaint = new Paint();
        StatusFormat format = new StatusFormat();

        public static class StatusFormat {
            public String format(String cxValue, String cyValue) {
                double d = Double.valueOf(cyValue);
                return "" + (int)d;
            }
        }

        public Status(Context context) {
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

        public StatusFormat getFormat() {
            return format;
        }

        public void setFormat(StatusFormat format) {
            this.format = format;
        }

        public void setIndex(int index) {
            this.index = index;
        }

        public int getIndex() {
            return index;
        }
    }
}
