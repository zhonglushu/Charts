package com.zhonglushu.charts.model;

public class EnergyData {

    /**
     * id
     * id = (type x 1000) + (number % N); N is set to limit the data size
     * type
     * 1 Day 2 Week 3 Month 4 Year
     * 5 10km 6 50km 7 100km 8 500km
     * energy
     * Used power
     * mileage
     * Course in this time
     * number
     * Like number when type is 2(week): means week in year
     * begin
     * Start time
     */
    public long id;
    public int type;
    public long energy;
    public long mileage;
    public long number;
    public long begin;

    @Override
    public String toString() {
        return "EnergyData{" +
                "id=" + id +
                ", type=" + type +
                ", energy=" + energy +
                ", mileage=" + mileage +
                ", number=" + number +
                ", begin=" + begin +
                '}';
    }
}
