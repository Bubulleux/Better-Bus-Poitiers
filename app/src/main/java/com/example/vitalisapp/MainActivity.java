package com.example.vitalisapp;

import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

	private ApiInteractor interactor = new ApiInteractor();
	private TextView output;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
		output = findViewById(R.id.text_output);
		System.out.println("Start");
		interactor.GetToken(token -> {
			System.out.println("Request token Success");
			//output.setText(token);

			interactor.GetAllStations(json -> System.out.println(json));
		});



    }
}