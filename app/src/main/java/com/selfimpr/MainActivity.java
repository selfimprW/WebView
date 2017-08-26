package com.selfimpr;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {
private  WVFragment wvFragment;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
             wvFragment= new WVFragment();
            getSupportFragmentManager().beginTransaction()
                    .replace(android.R.id.content,wvFragment )
                    .commitAllowingStateLoss();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (wvFragment!=null){
            wvFragment.onBackPressed();
        }
    }
}
