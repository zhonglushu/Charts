# Charts
The charts library for android

业务的需求是多种多样的，如果通过以下的设置都不能满足，可以在源码的基础上进行修改。

```
        //间距margin, 注意坐标原点为start位置，不是按左右顺序
        defaultBarChart.setxStartMargin(getResources().getDimension(R.dimen.page_bar_chart_xstart_margin));
        defaultBarChart.setxEndMargin(getResources().getDimension(R.dimen.page_bar_chart_xend_margin));
        defaultBarChart.setyStartMargin(getResources().getDimension(R.dimen.page_bar_chart_ystart_margin));
        defaultBarChart.setyEndMargin(getResources().getDimension(R.dimen.page_bar_chart_yend_margin));
        //内补padding
        defaultBarChart.getCoordinate().setCxStartPadding(getResources().getDimension(R.dimen.page_bar_chart_xstart_padding));
              defaultBarChart.getCoordinate().setCxEndPadding(getResources().getDimension(R.dimen.page_bar_chart_xstart_padding));//0.0417f
        defaultBarChart.getCoordinate().setCyStartPadding(getResources().getDimension(R.dimen.page_bar_chart_ystart_padding));
        //在y轴方向，刻度文字（即x轴文字）距离原点的位置，特殊的业务需求
        defaultBarChart.getCoordinate().setCyTextSpace(getResources().getDimension(R.dimen.page_bar_chart_y_text_space));
        //x、y轴的方向，即象限，向右和向上为正方向
        defaultBarChart.getCoordinate().setCxDirection(BarChart.Coordinate.DIRECTION.NEGATIVE);
        defaultBarChart.getCoordinate().setCyDirection(BarChart.Coordinate.DIRECTION.POSITIVE);
        //一般的需求是x轴数据的大小从原点出发向外部呈现递增，但是也有特殊需求，例如时间，坐标原点可能需要显示最近的时间点
        defaultBarChart.getCoordinate().setCxReverse(true);
        defaultBarChart.getCoordinate().setCxyTextSize(getResources().getDimensionPixelSize(R.dimen.battery_chart_unit_textsize));
        //对y轴的数据进行格式化
        defaultBarChart.getCoordinate().setCyFormat(new Chart.Coordinate.UnitFormat() {
            @Override
            public String format(String unit) {
                double value = Double.valueOf(unit);
                return "" + ((long)(value * 100) / 100);
            }
        });
        //对StatusView需要显示的状态数据进行格式化，StatusView请参考后面的视图
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
        //对x轴的数据进行格式化
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
        //是否需要进行高亮、选中等操作
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
        //自定义y轴的显示文字，并且通过range数组自定义数据的大小范围，默认的range是坐标点的最大和最小值
        defaultBarChart.getCoordinate().setCyUnitValueFunc(new Chart.Coordinate.CustomUnitValueFunc() {
        });
            @Override
            public String[] unitValues(Chart.PointD[] pointDs, double[] range) {
                return caluCyUnitValues(pointDs, range);
            }
        });
        //初始坐标显示的序号，例如总数100，开始只需要显示0~30
        int[] scrollMonthIndex = new int[2];
        //创建坐标点数据
        Chart.PointD[] barChartPoints = CommonUtil.createAveDayCoorPoints(scrollMonthIndex);
        if (barChartPoints != null && barChartPoints.length > 0) {
            //设置滑动模式
            defaultBarChart.setScrollMode(Chart.ScrollMode.DEFAULT);
            //所有的坐标点数据
            defaultBarChart.setTotalCoordPoints(barChartPoints);
            //初始显示的序号
            defaultBarChart.setScrollIndex(scrollMonthIndex);
            defaultBarChart.invalidate();
        }

```

### 1.主要有两方面的特点：

- 左右滑动的事件
- 局部刷新

左右滑动事件的处理机制参考ios health app, 其实主要的机制跟listview的滑动机制差不多，具体原理参考下面的简图：

![](https://github.com/zhonglushu/Charts/blob/master/images/scroll.png)

通过设置显示的起始位置和终止位置，同一时间需要绘制的项很少，所以效率很高，当滑动时，左右缓存若干项，并且通过scrollX的值来计算每一项的位置，这样就可以连续滑动了。

### 2.图表的视图层级
图表的视图主要分为三层：

- x,y轴坐标系(CoordinateView)
- 坐标点绘制(ChartView)
- 状态显示(StatusView)

在左右滑动图表时，x轴的文字的位置也需要动态改变，需要实时刷新，因此将坐标系的视图拆分为二，分别为XCoordinateView和YCoordinateView。另外

#### 注意

绘制图表一般会碰到x和y轴上面的文字被裁剪的问题，就像下面展示的图片那样：
![x-coord](https://github.com/zhonglushu/Charts/blob/master/images/x-coord.png)

此时我们可以通过设置margin来解决，但是当左右滑动时，因为计算scrollX的原因，margin区域也将绘制导致体验不好，我选择的方案是，在parent的onLayout方法中设置ChartView的margin。

综合以上描述，图表的视图层级结构图如下：

![struction](https://github.com/zhonglushu/Charts/blob/master/images/struction.png)
