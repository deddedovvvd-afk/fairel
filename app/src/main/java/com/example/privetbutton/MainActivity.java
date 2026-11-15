package com.example.privetbutton;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

public class MainActivity extends Activity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        FrameLayout root = new FrameLayout(this);
        Button button = new Button(this);
        button.setText("Привет");
        button.setOnClickListener(v -> Toast.makeText(this, "Привет!", Toast.LENGTH_SHORT).show());
        root.addView(button, new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER));
        setContentView(root);
    }
}
