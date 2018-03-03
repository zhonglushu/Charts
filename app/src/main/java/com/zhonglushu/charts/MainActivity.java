package com.zhonglushu.charts;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;

import com.zhonglushu.charts.chart.BarChart;
import com.zhonglushu.charts.chart.Chart;
import com.zhonglushu.charts.utils.CommonUtil;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity {

    //翻页的图表
    private BarChart pageBarChart;
    //默认随手势滚动的图表
    private BarChart defaultBarChart;
    private int[] scrollMonthIndex = new int[2];
    private int[] scrollMonthIndex2 = new int[2];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        pageBarChart = (BarChart) findViewById(R.id.page_bar_chart);
        //间距margin
        pageBarChart.setyStartMarginRadio(0.21f);
        pageBarChart.setyEndMarginRadio(0.21f);
        pageBarChart.setxEndMarginRadio(0.0f);
        pageBarChart.setxStartMarginRadio(0.074f);
        //内补padding
        pageBarChart.getCoordinate().setCxStartSpaceRadio(0.0065f);
        pageBarChart.getCoordinate().setCxEndSpaceRadio(0.0065f);//0.0417f
        pageBarChart.getCoordinate().setCyStartSpaceRadio(0.0232f);
        pageBarChart.getCoordinate().setCyEndSpaceRadio(0.0f);
        //x轴刻度距离原点距离
        pageBarChart.getCoordinate().setCyTextSpaceRadio(0.07f);
        //pageBarChart.getCoordinate().setCyUnitValue(new double[]{1.0f, 2.0f, 3.0f}, 0.0f, 3.0f);
        pageBarChart.getCoordinate().setCxDirection(BarChart.Coordinate.DIRECTION.NEGATIVE);
        pageBarChart.getCoordinate().setCyDirection(BarChart.Coordinate.DIRECTION.POSITIVE);
        pageBarChart.getCoordinate().setCxReverse(true);
        pageBarChart.getCoordinate().setCxyTextSize(getResources().getDimensionPixelSize(R.dimen.battery_chart_unit_textsize));
        pageBarChart.getCoordinate().setCyFormat(new Chart.Coordinate.UnitFormat() {
            @Override
            public String format(String unit) {
                double value = Double.valueOf(unit);
                return "" + ((long)(value * 100) / 100);
            }
        });
        pageBarChart.getStatus().setFormat(new BarChart.Status.StatusFormat() {
            @Override
            public String format(String cxValue, String cyValue) {
                double time = Double.valueOf(cxValue);
                Calendar c = Calendar.getInstance();
                c.setTimeInMillis((long)time * 1000);
                int month = c.get(Calendar.MONTH);
                int day = c.get(Calendar.DAY_OF_MONTH);
                double cy = Double.valueOf(cyValue);
                float y = Math.round(cy * 100) / 100.0f;
                return getResources().getString(R.string.barchart_tipbar_text, "" + (month + 1), "" + day, "" + y);
            }
        });
        pageBarChart.getCoordinate().setCxFormat(new BarChart.Coordinate.UnitFormat() {
            @Override
            public String format(String unit) {
                double value = Double.valueOf(unit);
                long time = (long)value;
                Calendar c = Calendar.getInstance();
                c.setTimeInMillis(time * 1000);
                int week = c.get(Calendar.DAY_OF_WEEK);
                if (week == Calendar.SUNDAY) {
                    int day = c.get(Calendar.DAY_OF_MONTH);
                    return "" + day + "日";
                } else {
                    return null;
                }
            }
        });
        pageBarChart.setEmphFunc(new Chart.EmphasisFunc() {
            @Override
            public boolean emphasis(Chart.PointD point) {
                long time = (long)point.x;
                Calendar c = Calendar.getInstance();
                c.setTimeInMillis(time * 1000);
                int week = c.get(Calendar.DAY_OF_WEEK);
                if (week == Calendar.SUNDAY) {
                    return true;
                } else {
                    return false;
                }
            }
        });
        pageBarChart.setScrollCallback(new Chart.ScrollCallback() {
            int startIndex = 0;
            int endIndex = 0;
            int pointsCount = 0;
            float downX = 0.0f;
            float downY = 0.0f;
            float scrollX = 0.0f;
            ValueAnimator valueAnimator = null;
            float MAX_VELX = 2500;
            float MAX_DISTANCE = 200;
            float perUnitLenX = 0.0f;
            int[] scrollIndex = new int[2];
            int[] lastMonthIndex = new int[2];
            int[] nextMonthIndex = new int[2];
            int[] tempIndex = new int[2];

            @Override
            public void onDownCallback(MotionEvent e) {
                cancelFlingAnim();
                startIndex = scrollIndex[0] = pageBarChart.getScrollIndex()[0];
                endIndex = scrollIndex[1] = pageBarChart.getScrollIndex()[1];
                pointsCount = scrollIndex[1] - scrollIndex[0] + 1;
                downX = e.getX();
                downY = e.getY();
                perUnitLenX = pageBarChart.getXCoordinateLen() / (pointsCount - 1);

                long time = (long)(pageBarChart.getCoordPoints()[pointsCount / 2].x);
                Calendar c = Calendar.getInstance();
                c.setTimeInMillis(time * 1000);
                c.add(Calendar.MONTH, -1);
                int lastMonthDaysCount = c.getActualMaximum(Calendar.DAY_OF_MONTH);
                if (startIndex - lastMonthDaysCount < 0) {
                    lastMonthIndex[0] = startIndex;
                    lastMonthIndex[1] = endIndex;
                } else {
                    lastMonthIndex[0] = startIndex - lastMonthDaysCount;
                    lastMonthIndex[1] = startIndex - 1;
                }
                c.add(Calendar.MONTH, 2);
                int nextMonthDaysCount = c.getActualMaximum(Calendar.DAY_OF_MONTH);
                int pointCount = pageBarChart.getTotalPointCount();
                if (endIndex + nextMonthDaysCount > pointCount - 1) {
                    nextMonthIndex[0] = startIndex;
                    nextMonthIndex[1] = endIndex;
                } else {
                    nextMonthIndex[0] = endIndex + 1;
                    nextMonthIndex[1] = endIndex + nextMonthDaysCount;
                }
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
//                    float tempVelocityX = velocityX;
//                    if (Math.abs(velocityX) > MAX_VELX) {
//                        if (velocityX < 0) {
//                            tempVelocityX = -MAX_VELX;
//                        } else {
//                            tempVelocityX = MAX_VELX;
//                        }
//                    }
//                    float percentVx = tempVelocityX / MAX_VELX;
//                    float startX = e2.getX();
//
//                    float lenX = pageBarChart.getXCoordinateLen() * 2 / 3;
//                    float deltaX = percentVx * lenX;
//                    float endDeltaX = startX + deltaX - downX;
//                    float endDeltaPercent = endDeltaX / pageBarChart.getXCoordinateLen();
//                    float endPointCount = (pointsCount - 1) * Math.abs(endDeltaPercent);
//                    float extra = (float)((endPointCount - Math.floor(endPointCount)) * perUnitLenX);
//
//                    float endX = startX + deltaX;
//                    if (startX - downX < 0) {
//                        endX += extra;
//                    } else {
//                        endX -= extra;
//                    }
//                    Log.i("Rambo222", "onFlingCallback endPointCount = " + endPointCount + ", extra = " + extra + ", downX = " + downX + ", startX = " + startX + ", endX = " + endX + ", perUnitLenX = " + perUnitLenX);
//                    startFlingAnim(startX, endX);

                    float startX = e2.getX();
                    float endX = downX;
                    if (Math.abs(velocityX) > MAX_VELX) {
                        if (velocityX < 0) {
                            endX += (scrollIndex[0] - nextMonthIndex[0]) * perUnitLenX;
                            tempIndex[0] = nextMonthIndex[0];
                            tempIndex[1] = nextMonthIndex[1];
                        } else {
                            endX += (scrollIndex[0] - lastMonthIndex[0]) * perUnitLenX;
                            tempIndex[0] = lastMonthIndex[0];
                            tempIndex[1] = lastMonthIndex[1];
                        }
                    } else {
                        float distance = startX - downX;
                        if (Math.abs(distance) > MAX_DISTANCE) {
                            if (distance < 0) {
                                endX += (scrollIndex[0] - nextMonthIndex[0]) * perUnitLenX;
                                tempIndex[0] = nextMonthIndex[0];
                                tempIndex[1] = nextMonthIndex[1];
                            } else {
                                endX += (scrollIndex[0] - lastMonthIndex[0]) * perUnitLenX;
                                tempIndex[0] = lastMonthIndex[0];
                                tempIndex[1] = lastMonthIndex[1];
                            }
                        } else {
                            tempIndex[0] = scrollIndex[0];
                            tempIndex[1] = scrollIndex[1];
                        }
                    }
                    startFlingAnim(startX, endX);
                }
            }

            @Override
            public void onUpCallback(MotionEvent e) {
                if (pointsCount > 0) {
                    float x = e.getX();
//                    float percent = (x - downX) / pageBarChart.getXCoordinateLen();
//                    float originDelta = (pointsCount - 1) * Math.abs(percent);
//                    float extra = (float)((originDelta - Math.floor(originDelta)) * perUnitLenX);
//                    float endX = x;
//                    if (percent < 0) {
//                        endX += extra;
//                    } else {
//                        endX -= extra;
//                    }
//                    Log.i("Rambo222", "onUpCallback originDelta = " + originDelta + ", x = " + x + ", downX = " + downX + ", endX = " + endX + ", extra = " + extra);
//                    startFlingAnim(x, endX);

                    float distance = x - downX;
                    float endX = downX;
                    if (Math.abs(distance) > MAX_DISTANCE) {
                        if (distance < 0) {
                            endX += (scrollIndex[0] - nextMonthIndex[0]) * perUnitLenX;
                            tempIndex[0] = nextMonthIndex[0];
                            tempIndex[1] = nextMonthIndex[1];
                        } else {
                            endX += (scrollIndex[0] - lastMonthIndex[0]) * perUnitLenX;
                            tempIndex[0] = lastMonthIndex[0];
                            tempIndex[1] = lastMonthIndex[1];
                        }
                    } else {
                        tempIndex[0] = scrollIndex[0];
                        tempIndex[1] = scrollIndex[1];
                    }
                    startFlingAnim(x, endX);
                }
            }

            private void handleScrollUpEvent(float x) {
//                float percent = (x - downX) / pageBarChart.getXCoordinateLen();
//                int delta = Math.round((pointsCount - 1) * Math.abs(percent));
//                if (percent < 0) {
//                    int count = pageBarChart.getTotalPointCount();
//                    if (scrollIndex[1] + delta > count - 1) {
//                        scrollIndex[1] = count - 1;
//                        scrollIndex[0] = scrollIndex[1] - pointsCount + 1;
//                    } else {
//                        scrollIndex[0] += delta;
//                        scrollIndex[1] += delta;
//                    }
//                } else {
//                    if (scrollIndex[0] - delta < 0) {
//                        scrollIndex[0] = 0;
//                        scrollIndex[1] = scrollIndex[0] + pointsCount - 1;
//                    } else {
//                        scrollIndex[0] -= delta;
//                        scrollIndex[1] -= delta;
//                    }
//                }
//                Log.i("Rambo222", "handleScrollUpEvent startIndex = " + startIndex + ", endIndex = " + endIndex + ", scrollIndex[0] = " + scrollIndex[0] + ", scrollIndex[1] = " + scrollIndex[1]);
//                int count = scrollIndex[1] - scrollIndex[0] + 1;
                int count = tempIndex[1] - tempIndex[0] + 1;
//                int pointCount = pageBarChart.getTotalPointCount();
//                if (tempIndex[1] > pointCount - 1) {
//                    tempIndex[1] = pointCount - 1;
//                    tempIndex[0] = pointCount - count;
//                }
//                if (tempIndex[0] < 0) {
//                    tempIndex[0] = 0;
//                    tempIndex[1] = count - 1;
//                }
                Chart.PointD[] points = new Chart.PointD[count];
                System.arraycopy(pageBarChart.getTotalCoordPoints(), tempIndex[0], points, 0, count);
                if (points != null && points.length > 0) {
                    scrollX = 0.0f;
                    pageBarChart.setChartScrollX(scrollX);
                    pageBarChart.setCoordPoints(points);
                    pageBarChart.setScrollIndex(tempIndex);
                    pageBarChart.invalidate();
                }
            }

            private void handleScrollEvent(float x) {
                //Log.i("Rambo111", "handleScrollEvent() downX = " + downX + ", x = " + x);
                float percent = (x - downX) / pageBarChart.getXCoordinateLen();
                int delta = (int)Math.ceil((pointsCount - 1) * Math.abs(percent));
                if (percent < 0) {
                    int count = pageBarChart.getTotalPointCount();
                    if (scrollIndex[1] + delta > count - 1) {
                        endIndex = count - 1;
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
                System.arraycopy(pageBarChart.getTotalCoordPoints(), startIndex, pointDs, 0, count);
                if (pointDs != null && pointDs.length > 0) {
                    pageBarChart.updateCoordPoints(pointDs);
                    pageBarChart.setChartScrollX(scrollX);
                    pageBarChart.invalidate();
                }
            }

            private void startFlingAnim(float startX, final float endX) {
                Log.i("Rambo222", "cancelFlingAnim start");
                cancelFlingAnim();
                Log.i("Rambo222", "cancelFlingAnim finish");
                valueAnimator = new ValueAnimator();
                valueAnimator.setDuration(500);
                valueAnimator.setInterpolator(new Chart.ScrollInterpolator());
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
        });
        pageBarChart.getCoordinate().setCyUnitValueFunc(new Chart.Coordinate.CustomUnitValueFunc() {
            @Override
            public String[] unitValues(Chart.PointD[] pointDs, double[] range) {
                return caluCyUnitValues(pointDs, range);
            }
        });
        Chart.PointD[] coordiates = CommonUtil.createAveDayCoorPoints(scrollMonthIndex);
        if (coordiates != null && coordiates.length > 0) {
            //设置滑动模式
            pageBarChart.setScrollMode(Chart.ScrollMode.CUSTOM);
            pageBarChart.setTotalCoordPoints(coordiates);
            pageBarChart.setScrollIndex(scrollMonthIndex);
            pageBarChart.invalidate();
        }


        /*
        ------------------------------------ 默认随手势滚动的图表 ----------------------------------
         */
        defaultBarChart = (BarChart) findViewById(R.id.default_bar_chart);
        //间距margin
        defaultBarChart.setyStartMarginRadio(0.21f);
        defaultBarChart.setyEndMarginRadio(0.21f);
        defaultBarChart.setxEndMarginRadio(0.0f);
        defaultBarChart.setxStartMarginRadio(0.074f);
        //内补padding
        defaultBarChart.getCoordinate().setCxStartSpaceRadio(0.0065f);
        defaultBarChart.getCoordinate().setCxEndSpaceRadio(0.0065f);//0.0417f
        defaultBarChart.getCoordinate().setCyStartSpaceRadio(0.0232f);
        defaultBarChart.getCoordinate().setCyEndSpaceRadio(0.0f);
        //x轴刻度距离原点距离
        defaultBarChart.getCoordinate().setCyTextSpaceRadio(0.07f);
        //defaultBarChart.getCoordinate().setCyUnitValue(new double[]{1.0f, 2.0f, 3.0f}, 0.0f, 3.0f);
        defaultBarChart.getCoordinate().setCxDirection(BarChart.Coordinate.DIRECTION.NEGATIVE);
        defaultBarChart.getCoordinate().setCyDirection(BarChart.Coordinate.DIRECTION.POSITIVE);
        defaultBarChart.getCoordinate().setCxReverse(true);
        defaultBarChart.getCoordinate().setCxyTextSize(getResources().getDimensionPixelSize(R.dimen.battery_chart_unit_textsize));
        defaultBarChart.getCoordinate().setCyFormat(new Chart.Coordinate.UnitFormat() {
            @Override
            public String format(String unit) {
                double value = Double.valueOf(unit);
                return "" + ((long)(value * 100) / 100);
            }
        });
        defaultBarChart.getStatus().setFormat(new BarChart.Status.StatusFormat() {
            @Override
            public String format(String cxValue, String cyValue) {
                double time = Double.valueOf(cxValue);
                Calendar c = Calendar.getInstance();
                c.setTimeInMillis((long)time * 1000);
                int month = c.get(Calendar.MONTH);
                int day = c.get(Calendar.DAY_OF_MONTH);
                double cy = Double.valueOf(cyValue);
                float y = Math.round(cy * 100) / 100.0f;
                return getResources().getString(R.string.barchart_tipbar_text, "" + (month + 1), "" + day, "" + y);
            }
        });
        defaultBarChart.getCoordinate().setCxFormat(new BarChart.Coordinate.UnitFormat() {
            @Override
            public String format(String unit) {
                double value = Double.valueOf(unit);
                long time = (long)value;
                Calendar c = Calendar.getInstance();
                c.setTimeInMillis(time * 1000);
                int week = c.get(Calendar.DAY_OF_WEEK);
                if (week == Calendar.SUNDAY) {
                    int day = c.get(Calendar.DAY_OF_MONTH);
                    return "" + day + "日";
                } else {
                    return null;
                }
            }
        });
        defaultBarChart.setEmphFunc(new Chart.EmphasisFunc() {
            @Override
            public boolean emphasis(Chart.PointD point) {
                long time = (long)point.x;
                Calendar c = Calendar.getInstance();
                c.setTimeInMillis(time * 1000);
                int week = c.get(Calendar.DAY_OF_WEEK);
                if (week == Calendar.SUNDAY) {
                    return true;
                } else {
                    return false;
                }
            }
        });
        defaultBarChart.getCoordinate().setCyUnitValueFunc(new Chart.Coordinate.CustomUnitValueFunc() {
            @Override
            public String[] unitValues(Chart.PointD[] pointDs, double[] range) {
                return caluCyUnitValues(pointDs, range);
            }
        });
        Chart.PointD[] coordiates2 = CommonUtil.createAveDayCoorPoints(scrollMonthIndex2);
        if (coordiates2 != null && coordiates2.length > 0) {
            //设置滑动模式
            defaultBarChart.setScrollMode(Chart.ScrollMode.DEFAULT);
            defaultBarChart.setTotalCoordPoints(coordiates2);
            defaultBarChart.setScrollIndex(scrollMonthIndex2);
            defaultBarChart.invalidate();
        }
    }

    private String[] caluCyUnitValues(Chart.PointD[] coordiates, double[] range) {
        int count = coordiates.length;
        double minCy = 0.0f;
        double maxCy = Double.MIN_VALUE;
        for (int i = 0; i < count; i++) {
            maxCy = Math.max(maxCy, coordiates[i].y);
        }
        double delta = (maxCy - minCy) / 3;
        int value = 0;
        if (delta < 5) {
            value = (int)Math.ceil(delta);
        } else {
            int decede = (int)delta / 10;
            int unit = (int)delta % 10;
            if (unit < 5) {
                value = decede * 10 + 5;
            } else {
                value = (decede + 1) * 10;
            }
        }
        String[] array = new String[3];
        for(int i = 0; i < array.length; i++) {
            array[i] = "" + (minCy + value*(i + 1));
        }
        range[0] = minCy;
        range[1] = Double.valueOf(array[2]);
        return array;
    }
}
