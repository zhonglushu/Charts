package com.zhonglushu.charts.utils;

import com.zhonglushu.charts.chart.Chart;
import com.zhonglushu.charts.model.EnergyData;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by rambo.huang on 18/1/7.
 */

public class CommonUtil {

    public static final int DAY_UNIT = 24 * 60 * 60 * 1000;

    public static Chart.PointD[] createAveDayCoorPoints(int[] indexArray) {
        List<EnergyData> list = new ArrayList<>();
        EnergyData data = new EnergyData();
        data.begin = 1444472720000L;
        data.energy = 20;
        data.mileage = 100;
        list.add(data);
        EnergyData data1 = new EnergyData();
        data1.begin = 1515061520000L;
        data1.energy = 30;
        data1.mileage = 100;
        list.add(data1);
        //时间顺序
        Collections.sort(list, new Comparator<EnergyData>() {
            @Override
            public int compare(EnergyData o1, EnergyData o2) {
                if (o1.begin > o2.begin) {
                    return 1;
                } else {
                    return -1;
                }
            }
        });
        Calendar c = Calendar.getInstance();
        long minTime = 0L;
        int count = 0;
        if (list.size() > 1) {

            c.setTimeInMillis(list.get(0).begin);
            int startYear = c.get(Calendar.YEAR);
            int startMonth = c.get(Calendar.MONTH);

            c.set(Calendar.DAY_OF_MONTH, 1);
            minTime = c.getTimeInMillis();

            c.setTimeInMillis(list.get(list.size() - 1).begin);
            int endYear = c.get(Calendar.YEAR);
            int endMonth = c.get(Calendar.MONTH);
            int endMonthCount = c.getActualMaximum(Calendar.DAY_OF_MONTH);

            if (startYear == endYear) {
                if (startMonth == endMonth) {
                    count = c.getActualMaximum(Calendar.DAY_OF_MONTH);
                    indexArray[0] = 0;
                    indexArray[1] = count - 1;
                } else {
                    for (int i = startMonth; i <= endMonth; i++) {
                        c.set(Calendar.MONTH, i);
                        count += c.getActualMaximum(Calendar.DAY_OF_MONTH);
                    }
                    indexArray[0] = count - endMonthCount;
                    indexArray[1] = count - 1;
                }
            } else {
                int monthCount = 12 - startMonth + (endYear - startYear - 1) * 12 + (endMonth + 1);
                c.setTimeInMillis(list.get(0).begin);
                for (int i = 0; i < monthCount; i++) {
                    count += c.getActualMaximum(Calendar.DAY_OF_MONTH);
                    c.add(Calendar.MONTH, 1);
                }
                indexArray[0] = count - endMonthCount;
                indexArray[1] = count - 1;
            }

        } else {
            if (list.size() == 1) {
                c.setTimeInMillis(list.get(0).begin);
                count = c.getActualMaximum(Calendar.DAY_OF_MONTH);
                indexArray[0] = 0;
                indexArray[1] = count - 1;

                c.set(Calendar.DAY_OF_MONTH, 1);
                minTime = c.getTimeInMillis();
            }
        }

        if (count > 0) {
            Chart.PointD[] coordiates = new Chart.PointD[count];
            c.setTimeInMillis(minTime);
            for (int i = 0; i < count; i++) {
                Chart.PointD pointD = new Chart.PointD();
                pointD.x = c.getTimeInMillis() / 1000;
                pointD.y = c.get(Calendar.MONTH) + 1;
                coordiates[i] = pointD;
                c.add(Calendar.DAY_OF_MONTH, 1);
            }
            if (list != null && list.size() > 0 && count >= list.size()) {
                Calendar calendar = Calendar.getInstance();
                for (int i = 0; i < list.size(); i++) {
                    Chart.PointD point = new Chart.PointD();
                    calendar.setTimeInMillis(list.get(i).begin);
                    c.set(Calendar.YEAR, calendar.get(Calendar.YEAR));
                    c.set(Calendar.MONTH, calendar.get(Calendar.MONTH));
                    c.set(Calendar.DAY_OF_MONTH, calendar.get(Calendar.DAY_OF_MONTH));
                    point.x = c.getTimeInMillis() / 1000;
                    if (list.get(i).mileage <= 0L) {
                        point.y = 0;
                    } else {
                        point.y = list.get(i).energy * 10.0f / list.get(i).mileage;
                    }
                    int index = (int)Math.ceil((c.getTimeInMillis() - minTime) / DAY_UNIT);
                    if (index < 0) {
                        index = 0;
                    }
                    coordiates[index] = point;
                }
            }
            return coordiates;
        }
        return null;
    }

}
