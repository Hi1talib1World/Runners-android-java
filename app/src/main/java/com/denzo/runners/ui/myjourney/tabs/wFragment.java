package com.denzo.runners.ui.myjourney.tabs;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import com.denzo.runners.R;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.ArrayList;

public class wFragment extends Fragment {
    public wFragment() {
// Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {


        View root = inflater.inflate(R.layout.fragment_week, container, false);
        BarChart chart = root.findViewById(R.id.barchart);
        ArrayList NoOfEmp = new ArrayList();

        NoOfEmp.add(new BarEntry(945f, 0));
        NoOfEmp.add(new BarEntry(1040f, 1));
        NoOfEmp.add(new BarEntry(1133f, 2));
        NoOfEmp.add(new BarEntry(1240f, 3));
        NoOfEmp.add(new BarEntry(1369f, 4));
        NoOfEmp.add(new BarEntry(1487f, 5));
        NoOfEmp.add(new BarEntry(1501f, 6));
        NoOfEmp.add(new BarEntry(1645f, 7));
        NoOfEmp.add(new BarEntry(1578f, 8));
        NoOfEmp.add(new BarEntry(1695f, 9));

        ArrayList year = new ArrayList();

        year.add("2008");
        year.add("2009");
        year.add("2010");
        year.add("2011");
        year.add("2012");
        year.add("2013");
        year.add("2014");
        year.add("2015");
        year.add("2016");
        year.add("2017");

        BarDataSet bardataset = new BarDataSet(NoOfEmp, "No Of Employee");
        chart.animateY(5000);
        BarData data = new BarData(year, bardataset);
        bardataset.setColors(ColorTemplate.COLORFUL_COLORS);
        chart.setData(data);




        PieChart pieChart2 = root.findViewById(R.id.piechart);
        ArrayList NoOfEmp2 = new ArrayList();

        NoOfEmp2.add(new Entry(945f, 0));
        NoOfEmp2.add(new Entry(1040f, 1));
        NoOfEmp2.add(new Entry(1133f, 2));
        NoOfEmp2.add(new Entry(1240f, 3));
        NoOfEmp2.add(new Entry(1369f, 4));
        NoOfEmp2.add(new Entry(1487f, 5));
        NoOfEmp2.add(new Entry(1501f, 6));
        NoOfEmp2.add(new Entry(1645f, 7));
        NoOfEmp2.add(new Entry(1578f, 8));
        NoOfEmp2.add(new Entry(1695f, 9));
        PieDataSet dataSet = new PieDataSet(NoOfEmp2, "Number Of Employees");

        ArrayList year2 = new ArrayList();

        year2.add("2008");
        year2.add("2009");
        year2.add("2010");
        year2.add("2011");
        year2.add("2012");
        year2.add("2013");
        year2.add("2014");
        year2.add("2015");
        year2.add("2016");
        year2.add("2017");
        PieData data2 = new PieData(year2, dataSet);
        pieChart2.setData(data2);
        dataSet.setColors(ColorTemplate.COLORFUL_COLORS);
        pieChart2.animateXY(5000, 5000);
        return root;

    }

}
