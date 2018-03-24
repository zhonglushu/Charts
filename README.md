# Charts
The charts library for android

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
