package com.ocr.labinal;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ocr.labinal.custom.tabs.SlidingTabLayout;
import com.ocr.labinal.custom.tabs.ViewPagerAdapter;
import com.ocr.labinal.events.GoToDetailEvent;
import com.ocr.labinal.utilities.AndroidBus;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

/**
 * Just handles the pager stuff
 */
public class TabFragment extends Fragment {


    public static Bus bus;

    ViewPager pager;
    ViewPagerAdapter adapter;
    SlidingTabLayout tabs;
    CharSequence Titles[];
    int numberOfTabs = 2;

    public TabFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(com.ocr.labinal.R.layout.fragment_tab, container, false);

        bus = new AndroidBus();
        bus.register(this);

        if (isAdded()) {
            Titles = getResources().getStringArray(com.ocr.labinal.R.array.tab_titles);
            // Creating The ViewPagerAdapter and Passing Fragment Manager, Titles fot the Tabs and Number Of Tabs.
            adapter = new ViewPagerAdapter(getChildFragmentManager(), Titles, numberOfTabs);

            // Assigning ViewPager View and setting the adapter
            pager = (ViewPager) view.findViewById(com.ocr.labinal.R.id.tabsPager);
            pager.setAdapter(adapter);

            // Assigning the Sliding Tab Layout View
            tabs = (SlidingTabLayout) view.findViewById(com.ocr.labinal.R.id.tabs);
            tabs.setDistributeEvenly(true); // To make the Tabs Fixed set this true, This makes the tabs Space Evenly in Available width

            tabs.setSelectedIndicatorColors(getResources().getColor(com.ocr.labinal.R.color.tabsScrollColor));

            tabs.setCustomTabView(com.ocr.labinal.R.layout.tab_indicator, android.R.id.text1);

            // Setting the ViewPager For the SlidingTabsLayout
            tabs.setViewPager(pager);
        }
        return view;
    }

    /**
     * comes from main activity and is just a bridge
     * @param event
     */
    @Subscribe
    public void RecyclerItemClicked(final GoToDetailEvent event) {
        if (event != null) {
            pager.setCurrentItem(2);
        }
    }


}
