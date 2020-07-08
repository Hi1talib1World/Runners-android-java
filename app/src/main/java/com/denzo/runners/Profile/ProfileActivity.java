package com.denzo.runners.Profile;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import com.denzo.runners.R;

public class ProfileActivity extends AppCompatActivity {


    TextView txtname,txtDesc,txtInfo;
    ImageView Img1;
    

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
    }
}
