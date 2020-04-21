package com.denzo.runners.ui.star;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import com.denzo.runners.R;
import com.denzo.runners.ui.home.HomeViewModel;

public class starFragment extends Fragment {
    private StarViewModel starViewModel;
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        starViewModel =
                ViewModelProviders.of(this).get(StarViewModel.class);
        View root = inflater.inflate(R.layout.fragment_star, container, false);


        return root;
    }
}
