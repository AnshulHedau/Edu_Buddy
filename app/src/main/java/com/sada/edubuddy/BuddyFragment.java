package com.sada.edubuddy;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


/**
 * A simple {@link Fragment} subclass.
 */
public class BuddyFragment extends Fragment {

    public static BuddyFragment newInstance() {
        
        Bundle args = new Bundle();
        
        BuddyFragment fragment = new BuddyFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public BuddyFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_buddy, container, false);
    }

}
