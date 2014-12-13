package com.example.mapapps;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.widget.TextView;

public class UserNotificationActivity extends Activity{

	private String destinationText;
	private String latitude;
	private String longtitude;
	
	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		Bundle extras = getIntent().getExtras();
		if(extras!=null){
			destinationText=extras.getString(getString(R.string.user_arrive_notification_Destination_title));
			latitude = ""+extras.getDouble(getString(R.string.user_arrive_notification_Latitude_title));
			longtitude=""+extras.getDouble(getString(R.string.user_arrive_notification_Longtitude_title));
		}else{
			destinationText="None Address";
			longtitude="None Value";
			latitude="None Value";
		}
		setContentView(R.layout.custom_notification);
		
		//get reference to the destination Address
		TextView ArriveText = (TextView) findViewById(R.id.ArriveText);
		ArriveText.setBackgroundColor(Color.parseColor("#E0FFFF"));
				
		//get reference to latitude
		TextView latitdueTitle = (TextView) findViewById(R.id.latitdue);
		latitdueTitle.setBackgroundColor(Color.parseColor("#E0FFFF"));
				
		//get reference to longitude
		TextView longtitudeTitle = (TextView) findViewById(R.id.longtitude);
		longtitudeTitle.setBackgroundColor(Color.parseColor("#E0FFFF"));
		
		
		//get reference to the destination Address
		TextView destination = (TextView) findViewById(R.id.DestinationText);
		destination.setText(destinationText);
		destination.setBackgroundColor(Color.parseColor("#DCDCDC"));
		//get reference to latitude
		TextView latitudeView = (TextView) findViewById(R.id.latitdueValue);
		latitudeView.setText(latitude);
		latitudeView.setBackgroundColor(Color.parseColor("#DCDCDC"));
		//get reference to longitude
		TextView LongtitudeView = (TextView) findViewById(R.id.longtitudeValue);
		LongtitudeView.setText(longtitude);
		
		LongtitudeView.setBackgroundColor(Color.parseColor("#DCDCDC"));
		
	}
}
