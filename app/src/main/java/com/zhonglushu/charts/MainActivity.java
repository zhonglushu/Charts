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

    private BarChart barChart;
    private Chart.PointD[] coordiates = null;
    private int[] scrollMonthIndex = new int[2];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        barChart = (BarChart) findViewById(R.id.bar_chart);
        //间距margin
        barChart.setyStartMarginRadio(0.21f);
        barChart.setyEndMarginRadio(0.21f);
        barChart.setxEndMarginRadio(0.0f);
        barChart.setxStartMarginRadio(0.074f);
        //内补padding
        barChart.getCoordinate().setCxStartSpaceRadio(0.0065f);
        barChart.getCoordinate().setCxEndSpaceRadio(0.0065f);//0.0417f
        barChart.getCoordinate().setCyStartSpaceRadio(0.0232f);
        barChart.getCoordinate().setCyEndSpaceRadio(0.0f);
        //x轴刻度距离原点距离
        barChart.getCoordinate().setCyTextSpaceRadio(0.07f);
        //barChart.getCoordinate().setCyUnitValue(new double[]{1.0f, 2.0f, 3.0f}, 0.0f, 3.0f);
        barChart.getCoordinate().setCxDirection(BarChart.Coordinate.DIRECTION.NEGATIVE);
        barChart.getCoordinate().setCyDirection(BarChart.Coordinate.DIRECTION.POSITIVE);
        barChart.getCoordinate().setCxReverse(true);
        barChart.getCoordinate().setCxyTextSize(getResources().getDimensionPixelSize(R.dimen.battery_chart_unit_textsize));
        barChart.getCoordinate().setCyFormat(new Chart.Coordinate.UnitFormat() {
            @Override
            public String format(String unit) {
                double value = Double.valueOf(unit);
                return "" + ((long)(value * 100) / 100);
            }
        });
        barChart.getStatus().setFormat(new BarChart.Status.StatusFormat() {
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
        barChart.getCoordinate().setCxFormat(new BarChart.Coordinate.UnitFormat() {
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
        barChart.setEmphFunc(new Chart.EmphasisFunc() {
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
        barChart.getCoordinate().setCyUnitValueFunc(new Chart.Coordinate.CustomUnitValueFunc() {
            @Override
            public String[] unitValues(Chart.PointD[] pointDs, double[] range) {
                return caluCyUnitValues(pointDs, range);
            }
        });
        coordiates = CommonUtil.createAveDayCoorPoints(scrollMonthIndex);
        if (coordiates != null && coordiates.length > 0) {
            //设置滑动模式
            barChart.setScrollMode(Chart.ScrollMode.DEFAULT);
            barChart.setTotalCoordPoints(coordiates);
            barChart.setScrollIndex(scrollMonthIndex);
            barChart.invalidate();
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
