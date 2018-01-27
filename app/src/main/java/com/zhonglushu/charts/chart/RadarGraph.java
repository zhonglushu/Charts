package com.zhonglushu.charts.chart;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.zhonglushu.charts.R;

/**
 * Created by rambo.huang on 17/11/9.
 */

public class RadarGraph extends View {

    private int circleCount = 4;
    private float innerRadius = 68;
    private float radiusGap = 30;
    private Aspect[] aspects;
    private float[] scoreRange = new float[2];
    private float score = 5.6f;
    private float hlRadius = 5;
    private Paint paint = new Paint();
    private Paint scorePaint = new Paint();
    private Paint textPaint = new Paint();
    private int width;
    private int height;
    private float centerX;
    private float centerY;
    private Rect rect = new Rect();
    private float aspectDegree;
    private static int defaultTextColor;
    private int textSpace = 30;

    public RadarGraph(Context context) {
        this(context, null);
    }

    public RadarGraph(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RadarGraph(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public RadarGraph(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        paint.setFlags(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(getResources().getColor(R.color.radar_graph_circle_color));
        paint.setStyle(Paint.Style.STROKE);
        paint.setAntiAlias(true);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setDither(true);
        paint.setStrokeWidth(2);

        scorePaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        scorePaint.setAntiAlias(true);
        scorePaint.setColor(getResources().getColor(R.color.radar_graph_score_color));
        scorePaint.setTextSize(40);

        textPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        textPaint.setAntiAlias(true);
        Typeface typeface = Typeface.create("sans-serif-light", Typeface.NORMAL);
        textPaint.setTypeface(typeface);
        textPaint.setTextSize(getResources().getDimensionPixelSize(R.dimen.radar_graph_text_size));

        defaultTextColor = getResources().getColor(R.color.radar_graph_default_text_color);
        textSpace = getResources().getDimensionPixelOffset(R.dimen.radar_graph_text_margin);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        width = MeasureSpec.getSize(widthMeasureSpec);
        height = MeasureSpec.getSize(heightMeasureSpec);
        setMeasuredDimension(width, height);
        centerX = 1.0f * width / 2;
        centerY = 1.0f * height / 2;
        updateApsectsPosition();
        Log.i("Rambo", "RadarGraph onMeasure()");
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (int i = 0; i < circleCount; i++) {
            canvas.drawCircle(centerX, centerY, innerRadius + i*radiusGap, paint);
        }

        String scoreText = String.valueOf(score);
        getTextRound(scorePaint, scoreText, rect);
        canvas.drawText(scoreText, centerX - rect.centerX(), centerY - rect.centerY(), scorePaint);
        int next = 1;
        int count = aspects.length;
        for (int i = 0; i < count; i++) {
            Position position = aspects[i].position;
            canvas.drawCircle(position.outerPosition.x, position.outerPosition.y, hlRadius, scorePaint);
            canvas.drawLine(position.outerPosition.x, position.outerPosition.y, position.innerPosition.x, position.innerPosition.y, paint);
            next = (i + 1) >= count?0 : i + 1;
            canvas.drawLine(position.scorePosition.x, position.scorePosition.y, aspects[next].position.scorePosition.x, aspects[next].position.scorePosition.y, paint);
            //绘制文字
            Text text = aspects[i].getText();
            textPaint.setColor(text.textColor);
            //textPaint.setTextSize(text.textSize);
            Rect rect = new Rect();
            getTextRound(textPaint, text.text, rect);
            canvas.drawText(text.text, text.getLeft(), text.getBottom(), textPaint);
        }
    }

    public int getCircleCount() {
        return circleCount;
    }

    public void setCircleCount(int circleCount) {
        this.circleCount = circleCount;
    }

    public float getInnerRadius() {
        return innerRadius;
    }

    public void setInnerRadius(float innerRadius) {
        this.innerRadius = innerRadius;
    }

    public float getRadiusGap() {
        return radiusGap;
    }

    public void setRadiusGap(float radiusGap) {
        this.radiusGap = radiusGap;
    }

    public Aspect[] getAspects() {
        return aspects;
    }

    public void setAspects(Aspect[] aspects) {
        this.aspects = aspects;
    }

    public float[] getScoreRange() {
        return scoreRange;
    }

    public void setScoreRange(float[] scoreRange) {
        this.scoreRange = scoreRange;
    }

    public void setScoreRange(float min, float max) {
        this.scoreRange[0] = min;
        this.scoreRange[1] = max;
    }

    public float getScore() {
        return score;
    }

    public void setScore(float score) {
        this.score = score;
    }

    public float getHlRadius() {
        return hlRadius;
    }

    public void setHlRadius(float hlRadius) {
        this.hlRadius = hlRadius;
    }

    private void updateApsectsPosition() {
        aspectDegree = 360 / aspects.length;
        Rect rect = new Rect();
        for (int i = 0; i < aspects.length; i++) {
            Position position = new Position();
            float degree = i*aspectDegree + 90.0f;
            getPositionWithDegree(centerX, centerY, degree, innerRadius + radiusGap*(circleCount - 1), position.outerPosition);
            getPositionWithDegree(centerX, centerY, degree, innerRadius, position.innerPosition);
            float scoreRadius = innerRadius + (aspects[i].score - scoreRange[0]) / (scoreRange[1] - scoreRange[0]) * (circleCount - 1)*radiusGap;
            getPositionWithDegree(centerX, centerY, degree, scoreRadius, position.scorePosition);
            aspects[i].position = position;
            //计算文字位置
            getTextRound(textPaint, aspects[i].text.text, rect);
            if (Float.compare(90.0f, degree) == 0) {
                aspects[i].getText().left = position.outerPosition.x - rect.centerX();
                aspects[i].getText().bottom = position.outerPosition.y - textSpace;
            } else if (degree > 90.0f && degree < 270.0f) {
                aspects[i].getText().left = position.outerPosition.x - rect.width() - textSpace;
                aspects[i].getText().bottom = position.outerPosition.y - rect.centerY();
            } else if (Float.compare(270.0f, degree) == 0) {
                aspects[i].getText().left = position.outerPosition.x - rect.centerX();
                aspects[i].getText().bottom = position.outerPosition.y + textSpace;
            } else {
                aspects[i].getText().left = position.outerPosition.x + textSpace;
                aspects[i].getText().bottom = position.outerPosition.y - rect.centerY();
            }
        }
    }

    private void getTextRound(Paint tempPaint, String text, Rect rect) {
        tempPaint.getTextBounds(text.trim(), 0, text.trim().length(), rect);
    }

    private void getPositionWithDegree(float centerX, float centerY, float degree, float radius, PointF pointF) {
        pointF.x = (float)(centerX + radius * Math.cos(degree * Math.PI / 180));
        pointF.y = (float)(centerY - radius * Math.sin(degree * Math.PI / 180));
    }

    public static class Position {
        PointF outerPosition = new PointF();
        PointF innerPosition = new PointF();
        PointF scorePosition = new PointF();
    }

    public static class Aspect {
        Text text = new Text();
        float score;
        Position position;

        public Text getText() {
            return text;
        }

        public void setText(Text text) {
            this.text = text;
        }

        public float getScore() {
            return score;
        }

        public void setScore(float score) {
            this.score = score;
        }

        public Position getPosition() {
            return position;
        }

        public void setPosition(Position position) {
            this.position = position;
        }
    }

    //文本类
    public static class Text {
        String text;
        int textColor = -1;
        int textSize = 30;
        float left;
        float bottom;

        public Text() {
            textColor = defaultTextColor;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public int getTextColor() {
            return textColor;
        }

        public void setTextColor(int textColor) {
            this.textColor = textColor;
        }

        public int getTextSize() {
            return textSize;
        }

        public void setTextSize(int textSize) {
            this.textSize = textSize;
        }

        public float getLeft() {
            return left;
        }

        public void setLeft(float left) {
            this.left = left;
        }

        public float getBottom() {
            return bottom;
        }

        public void setBottom(float bottom) {
            this.bottom = bottom;
        }
    }
}
