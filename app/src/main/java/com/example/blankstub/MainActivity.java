package com.example.blankstub;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View blankScreen = new View(this);
        blankScreen.setBackgroundColor(0xFFFFFFFF);
        setContentView(blankScreen);
    }
}
