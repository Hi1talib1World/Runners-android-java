package com.denzo.runners.ui.myjourney;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.denzo.runners.R;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.hadiidbouk.charts.ChartProgressBar;

import java.util.ArrayList;

public class DashboardFragment extends Fragment {

    private DashboardViewModel dashboardViewModel;
    private ChartProgressBar mChart;
    ViewPager simpleViewPager;
    TabLayout tabLayout;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        dashboardViewModel =
                ViewModelProviders.of(this).get(DashboardViewModel.class);
        View root = inflater.inflate(R.layout.fragment_dashboard, container, false);


            // get the reference of ViewPager and TabLayout
                    simpleViewPager = (ViewPager) root.findViewById(R.id.simpleViewPager);
                    tabLayout = (TabLayout) root.findViewById(R.id.simpleTabLayout);
            // Create a new Tab named "First"
                    TabLayout.Tab firstTab = tabLayout.newTab();
                    firstTab.setText("First"); // set the Text for the first Tab
                    firstTab.setIcon(R.drawable.ic_launcher); // set an icon for the
            // first tab
                    tabLayout.addTab(firstTab); // add  the tab at in the TabLayout
            // Create a new Tab named "Second"
                    TabLayout.Tab secondTab = tabLayout.newTab();
                    secondTab.setText("Second"); // set the Text for the second Tab
                    secondTab.setIcon(R.drawable.ic_launcher); // set an icon for the second tab
                    tabLayout.addTab(secondTab); // add  the tab  in the TabLayout
            // Create a new Tab named "Third"
                    TabLayout.Tab thirdTab = tabLayout.newTab();
                    thirdTab.setText("Third"); // set the Text for the first Tab
                    thirdTab.setIcon(R.drawable.ic_launcher); // set an icon for the first tab
                    tabLayout.addTab(thirdTab); // add  the tab at in the TabLayout

                    PagerAdapter adapter = new PagerAdapter
                            (getSupportFragmentManager(), tabLayout.getTabCount());
                    simpleViewPager.setAdapter(adapter);
            // addOnPageChangeListener event change the tab on slide
                    simpleViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));



        //Fab Popup
        FloatingActionButton fab = root.findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

        ArrayList<BarData> dataList = new ArrayList<>();

        BarData data = new BarData("Sep", 3.4f, "3.4€");
        dataList.add(data);

        data = new BarData("Oct", 8.0f, "8.0€");
        dataList.add(data);

        data = new BarData("Nov", 1.8f, "1.8€");
        dataList.add(data);

        data = new BarData("Dec", 7.3f, "7.3€");
        dataList.add(data);

        data = new BarData("Jan", 6.2f, "6.2€");
        dataList.add(data);

        data = new BarData("Feb", 3.3f, "3.3€");
        dataList.add(data);

        mChart = (ChartProgressBar) root.findViewById(R.id.ChartProgressBar);

        mChart.setDataList(dataList);
        mChart.build();
        mChart.setOnBarClickedListener(this);
        mChart.disableBar(dataList.size() - 1);


        return root;

    }

}