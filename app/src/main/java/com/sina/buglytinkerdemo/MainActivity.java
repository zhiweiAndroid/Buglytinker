package com.sina.buglytinkerdemo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String a ="a";
                if (a.equals("a")){
                    Toast.makeText(MainActivity.this,"我是吐司",Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
