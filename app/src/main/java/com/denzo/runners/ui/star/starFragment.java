package com.denzo.runners.ui.star;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import com.carto.layers.CartoBaseMapStyle;
import com.carto.layers.CartoOnlineVectorTileLayer;
import com.carto.ui.MapView;
import com.denzo.runners.R;
import com.denzo.runners.ui.home.HomeViewModel;

public class starFragment extends Fragment {
    private StarViewModel starViewModel;
    final String LICENSE = "YOUR_LICENSE_KEY";

    private MapView mapView;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        starViewModel =
                ViewModelProviders.of(this).get(StarViewModel.class);
        View root = inflater.inflate(R.layout.fragment_star, container, false);



        // Register the license so that CARTO online services can be used
        MapView.registerLicense(LICENSE);

        // Get 'mapView' object from the application layout
        mapView = (MapView) root.findViewById(R.id.mapView);

        // Add basemap layer to mapView
        CartoOnlineVectorTileLayer baseLayer = new CartoOnlineVectorTileLayer(CartoBaseMapStyle.CARTO_BASEMAP_STYLE_VOYAGER);
        mapView.getLayers().add(baseLayer);
        return root;
    }
}
