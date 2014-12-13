/*package com.example.mapapps;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;


public class MainActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
*/
package com.example.mapapps;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import com.example.mapapps.MapAppsLocationUploadService.LocalBinder;
import com.example.mapapps.mgeofence.OnEnterGefenceListener;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationClient.OnAddGeofencesResultListener;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;



public class MainActivity extends ActionBarActivity implements  
	GooglePlayServicesClient.ConnectionCallbacks,
	GooglePlayServicesClient.OnConnectionFailedListener,
	LocationListener,
	RoutingListener
	{
	//used for log tag
	private final String TAG = "MapApps";
	
	//variables for google map manipulation
	private SupportMapFragment mapFrag=null;
	private GoogleMap gMap;
	private UiSettings mUiSettings;
	//private LocationClient mLocationClient;
	// Stores the PendingIntent used to request geofence monitoring
    //private PendingIntent mGeofenceRequestIntent;
    // Flag that indicates if a request is underway.
    //private boolean mInProgress;
	private Location currentLocation;//variable that hold current location. And we will update this variable upon location changed.
	private boolean IsThisFirstLocationChanged=true;
	private Intent start_location_service_intent;
	private Polyline last_direction_line;//last direction line drawed on the Map.
	/*
	 * Gloabal Constants for location service
	 */
	// Milliseconds per second
    private static final int MILLISECONDS_PER_SECOND = 1000;
    // Update frequency in seconds
    public static final int UPDATE_INTERVAL_IN_SECONDS = 20;//so 555 meters @speed of 100 k/h
    // Update frequency in milliseconds
    private static final long UPDATE_INTERVAL =
            MILLISECONDS_PER_SECOND * UPDATE_INTERVAL_IN_SECONDS;
    
    // The fastest update frequency, in seconds
    //through experiment, I found that this value is more important than UPDATE_INTERVAL, bacause, there are some app at the 
    //same system whose request frequency is higher than UPDATE_INTERVAL, then Android would send location update 
    //at the frequency specified by FASTEST_INTERVAL_IN_SECONDS.
    private static final int FASTEST_INTERVAL_IN_SECONDS = 10;//so 277 meters @speed of 100 k/h
    // A fast frequency ceiling in milliseconds
    private static final long FASTEST_INTERVAL =
            MILLISECONDS_PER_SECOND * FASTEST_INTERVAL_IN_SECONDS;
    //define an object that holds accuracy and frequency parameters
    LocationRequest mLocationRequest;
    //resolve the Logitude and Latitude to street address using ansyTask
    private ProgressBar mActivityIndicator;
    private final int Max_Return_Address_Num=5;
    
    
    //google request for searching for places according to text-based geographic search.
    //Add the four conner markers to the map, bacause we only search user input address in the square specified by 
    //those four conner latitude and longitude
    private final LatLng RightUpConner = new LatLng(30.860071, -95.994415);
    private final LatLng LeftDownConner = new LatLng(30.467161, -96.60141);
    
    //this is at HRBB
    private final LatLng CameraCenterForFirstMapLoad = new LatLng(30.6187162,-96.3389999);
    private ArrayList<Marker> searchResultMarker=null;
    private Marker StartMarker=null;
    
    //mBindService used to store the instance of MapAppsLocationUploadService, returned by IBinder.getService();
    private MapAppsLocationUploadService mBindService;
    private boolean mBound = false;
    
    
    //start and end information for uploading to remote server purpose
    private LatLng startLatLng_upload;
    private String EndAddress_upload="";
    
    //Defines callbacks for service binding, passed to bindService()
    //this method is called after the bindService() is called and client connect to the server service successfully
    private ServiceConnection mBindConnection = new ServiceConnection(){

		@Override
		public void onServiceConnected(ComponentName className, IBinder iBinder) {
			// TODO Auto-generated method stub
			// We've bound to MapAppsLocationUploadService, cast the IBinder and get LocalService instance
			Log.i(TAG, "onServiceConnected "+"Connected");
			LocalBinder mLocalBinder = (LocalBinder) iBinder;
			mBindService = mLocalBinder.getService();//getService() return an instance of the MapAppsLocationUploadService instance
			mBound=true;
			//registe MainActivity.this to MapAppsLocationUploadService
			mBindService.registerContext(MainActivity.this);
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			// TODO Auto-generated method stub
			Log.i(TAG, "onServiceConnected "+"Disconnected");
			mBound=false;
		}
    	
    };
    
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate");
        setContentView(R.layout.activity_main);
        //read from Shared Preferences the work state of the APP 
        final ToggleButton ToggleButton_switch = (ToggleButton) findViewById(R.id.ToggleButton_switch_On_Off);
        SharedPreferences sharedPref = MainActivity.this.getPreferences(Context.MODE_PRIVATE);
        //false is theoptionally a default value to return if the key isn't present. For example: 
        ToggleButton_switch.setChecked(sharedPref.getBoolean(getString(R.string.button_switch_Off_State), false));
        
        if(ToggleButton_switch.isChecked()){
        	//MapAppsLocationUploadService is still running on background
        	//we need to bind service here Bind to LocalService
			Intent intent = new Intent(MainActivity.this, MapAppsLocationUploadService.class);
			bindService(intent,mBindConnection,Context.BIND_AUTO_CREATE);
        }
        //then initialize the location service
        //InitializeLocationService();
        
        //then start to initialize map
        Initializemap();
        
        //get reference to the editText View
        final EditText editText_destination = (EditText) findViewById(R.id.UserInputText_destination);
        //editText_destination.setBackgroundColor(Color.parseColor("#800000"));
        final Button Button_confirm = (Button) findViewById(R.id.Button_confirm_Address);
        editText_destination.setVisibility(ToggleButton_switch.isChecked()?View.VISIBLE : View.INVISIBLE);
        Button_confirm.setVisibility(ToggleButton_switch.isChecked()?View.VISIBLE : View.INVISIBLE);
        
        //Initialize UI elements
        //final ToggleButton ToggleButton_switch = (ToggleButton) findViewById(R.id.ToggleButton_switch_On_Off);
        final TextView TextView_switch = (TextView) findViewById(R.id.text_switch_On_Off);
        final LocationListener MylocationListener = this;
        
        //reference to the intent that start MapAppsLocationUploadService
        start_location_service_intent = new Intent(MainActivity.this,MapAppsLocationUploadService.class);
        
        //should switch the system state when the user click the toggleButton
        ToggleButton_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				// TODO Auto-generated method stub
				//TextView_switch.setText("status: "+isChecked);
				Log.i(TAG, "At work?"+isChecked);
				Toast.makeText(MainActivity.this, "At work? "+isChecked, Toast.LENGTH_SHORT).show();
				if(isChecked){
					//set the button, editText, and map to be visible
					editText_destination.setVisibility(View.VISIBLE);
					Button_confirm.setVisibility(View.VISIBLE);
					if(mapFrag!=null)
			        	mapFrag.getView().setVisibility(View.VISIBLE);
			        
					//if checked, means that now is at work, so need to activate the location update service
					//mLocationClient.requestLocationUpdates(mLocationRequest,MylocationListener);
					//resolve_LatLon_Address();
					
					//need to start the MapAppsLocationUploadService
					//start_location_service_intent.putExtra("context", (CharSequence) getApplicationContext());
					startService(start_location_service_intent);
					//we need to bind service here
					// Bind to LocalService
					Intent intent = new Intent(MainActivity.this, MapAppsLocationUploadService.class);
					bindService(intent,mBindConnection,Context.BIND_AUTO_CREATE);
					
			        
				}else{
					Log.i(TAG, "At work?"+isChecked);
					//set the button, editText, and map to be invisible
					editText_destination.setVisibility(View.INVISIBLE);
					Button_confirm.setVisibility(View.INVISIBLE);
					if(mapFrag!=null)
			        	mapFrag.getView().setVisibility(View.INVISIBLE);
			        
					//if unchecked, means is not at work, so can inactivate the location update service
					//mLocationClient.removeLocationUpdates(MylocationListener);
					
					//unbind from the service
					//unbind from the MapAppsLocationUploadService
					if(mBound){
						unbindService(mBindConnection);
						mBound=false;
					}
					//need to stop the MapAppsLocationUploadService
					stopService(start_location_service_intent);
				}
			}
        }
        );
        
        //add the onclick handler to the address confirm button
        Button_confirm.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				//Toast.makeText(getApplicationContext(), "Enter Address:"+editText_destination.getText().toString()+".", Toast.LENGTH_LONG).show();
				if(editText_destination.getText().toString().equals("")){
					Toast.makeText(getApplicationContext(), "Please Enter Valid Address", Toast.LENGTH_LONG).show();
					return;
				}			
				//Test: how to draw lines on the MAP??
				/*Polyline line = gMap.addPolyline(new PolylineOptions()
			    .add(new LatLng(-37.81319, 144.96298), new LatLng(-31.95285, 115.85734))
			    .width(25)
			    .color(Color.BLUE)
			    .geodesic(true));*/
				
				//search the address
				String userInputAddress = editText_destination.getText().toString();
				search_Address(userInputAddress);
			}
        	
        });
        // Start with the request flag set to false
        //mInProgress=false;
    }
    
    /**
     *resolve the Logitude and Latitude to street address using ansyTask
     */
    private void resolve_LatLon_Address(LatLng loc){
    	Log.e(TAG, "resolve_LatLon_Address "+loc.toString());
    	//get reference to the progressBar
    	mActivityIndicator =(ProgressBar) findViewById(R.id.address_progress);
    	//ensure that a geocoder service is available
    	if(Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.GINGERBREAD && Geocoder.isPresent()){
    		// Show the activity indicator
            mActivityIndicator.setVisibility(View.VISIBLE);
            /*
             * Reverse geocoding is long-running and synchronous.
             * Run it on a background thread.
             * Pass the current location to the background task.
             * When the task finishes,
             * onPostExecute() displays the address.
             */
            (new GetAddressTask(this)).execute(loc);
    	}
    }
    
    
    /**
     *search address using ansyTask based on input of user
     */
    private void search_Address(String UserInput){
    	//get reference to the progressBar
    	mActivityIndicator =(ProgressBar) findViewById(R.id.address_progress);
    	//ensure that a geocoder service is available
    	if(Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.GINGERBREAD && Geocoder.isPresent()){
    		// Show the activity indicator
            mActivityIndicator.setVisibility(View.VISIBLE);
            /*
             * Reverse geocoding is long-running and synchronous.
             * Run it on a background thread.
             * Pass the current location to the background task.
             * When the task finishes,
             * onPostExecute() displays the address.
             */
            (new SearchAddressTask(this)).execute(UserInput);
    	}
    }
    
    /**
	* A subclass of AsyncTask that calls getFromLocation() in the
	* background. The class definition has these generic types:
	* Location - A Location object containing
	* the current location.
	* Void     - indicates that progress units are not used
	* String   - An address passed to onPostExecute()
	*/
	private class SearchAddressTask extends AsyncTask<String, Void, List<Address>>{

		Context mContext;
		public SearchAddressTask(Context context){
			super();
			mContext=context;
		}
		/**
         * Get a Geocoder instance, search the address based on user input
         * look up the address, and return it
         *
         * @params params One or more Location objects
         * @return A string containing the address of the current
         * location, or an empty string if no address can be found,
         * or an error message
         */
		@Override
		protected List<Address> doInBackground(String... params) {
			// TODO Auto-generated method stub
			Geocoder geocoder = new Geocoder(mContext,Locale.getDefault());
			//Get user input from the input parameter list
			String userinput = params[0];
			//create a list to contain the result address
			List<Address> addresses = null;
			try{
				/*
				 * return Max_Return_Address_Num address, Max_Return_Address_Num is predefined constant value.
				 */
				addresses = geocoder.getFromLocationName(userinput, Max_Return_Address_Num, 
						LeftDownConner.latitude, LeftDownConner.longitude, RightUpConner.latitude , RightUpConner.longitude );
				
				// If the reverse geocode returned an address
	            return addresses;
			}catch(IOException e1){
				Log.e("LocationSampleActivity",
	                    "IO Exception in getFromLocation()");
	            e1.printStackTrace();
	            return null;
			}catch(IllegalArgumentException e2){
				// Error message to post in the log
	            String errorString = "Illegal arguments "+userinput+" passed to address service";
	            Log.e("LocationSampleActivity", errorString);
	            e2.printStackTrace();
	            return null;
			}
			
		}
		 /**
         * A method that's called once doInBackground() completes. Turn
         * off the indeterminate activity indicator and set
         * the text of the UI element that shows the address. If the
         * lookup failed, display the error message.
         */
		@Override
        protected void onPostExecute(List<Address> addresses) {
            //Finish searching Address, then set activity indicator visibility to "gone"
            mActivityIndicator.setVisibility(View.GONE);
            // Display the results of the lookup.
            /*
             * Format the first line of address (if available),
             * city, and country name.
             */
			if (addresses != null && addresses.size() > 0) {
				Address address = addresses.get(0);
				String address_text = String.format(
						"%s, %s, %s",
						// If there's a street address, add it
						address.getMaxAddressLineIndex() > 0 ? address
								.getAddressLine(0) : "",
						// Locality is usually a city
						address.getLocality(),
						// The country of the address
						address.getCountryName());
				final EditText editText_destination = (EditText) findViewById(R.id.UserInputText_destination);
				editText_destination.setText(address_text);

				// then I need to move all the previous markers at the map
				if (searchResultMarker != null && searchResultMarker.size()!=0){
					for(Marker eachMarker: searchResultMarker){
						eachMarker.remove();
					}
				}
				if(StartMarker!=null)
					StartMarker.remove();
				searchResultMarker.clear();
				//then I get the latLng of the first address
				LatLng result_latlng = new LatLng(address.getLatitude(),
						address.getLongitude());
				// move the camera to center on this place
				gMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
						result_latlng, 15));
				
				//searchResultMarker.remove();// remove the previous marker
				//then add markers for each address
				float opacity_value=1;
				float difference = (float) (0.8/Max_Return_Address_Num);
				for(Address eachAddress: addresses){
					searchResultMarker.add(gMap
							.addMarker(new MarkerOptions()
							.position(
									new LatLng(eachAddress.getLatitude(),
											eachAddress.getLongitude()))
							.title("destination")
							.snippet(address_text)
							.draggable(false)
							.alpha((float) (opacity_value-=difference))));
				}
				Log.i(TAG, "address Num returned: "+addresses.size());
				
			}else{
				//No address Found
				Toast.makeText(getApplicationContext(), "No Address Found, Please Check Your Input", Toast.LENGTH_LONG).show();
			}
        }
	}
    
    
    /**
	* A subclass of AsyncTask that calls getFromLocation() in the
	* background. The class definition has these generic types:
	* Location - A Location object containing
	* the current location.
	* Void     - indicates that progress units are not used
	* String   - An address passed to onPostExecute()
	*/
	private class GetAddressTask extends AsyncTask<LatLng, Void, String>{

		Context mContext;
		public GetAddressTask(Context context){
			super();
			mContext=context;
		}
		/**
         * Get a Geocoder instance, get the latitude and longitude
         * look up the address, and return it
         *
         * @params params One or more Location objects
         * @return A string containing the address of the current
         * location, or an empty string if no address can be found,
         * or an error message
         */
		@Override
		protected String doInBackground(LatLng... params) {
			Log.e(TAG, "GetAddressTask "+"doInBackground");
			// TODO Auto-generated method stub
			Geocoder geocoder = new Geocoder(mContext,Locale.getDefault());
			//Get the current location from the imput parameter list
			LatLng loc = params[0];
			//create a list to contain the result address
			List<Address> addresses = null;
			try{
				/*
				 * return 1 address
				 */
				double lati =  loc.latitude;
				double longi = loc.longitude;
				addresses = geocoder.getFromLocation(lati,longi, 1);
				
				//addresses = geocoder.getFromLocation(30.6187, -96.3389, 2);
				
			}catch(IOException e1){
				Log.e("LocationSampleActivity",
	                    "IO Exception in getFromLocation()");
	            e1.printStackTrace();
	            return ("IO Exception trying to get address");
			}catch(IllegalArgumentException e2){
				// Error message to post in the log
	            String errorString = "Illegal arguments " +
	                    Double.toString(loc.latitude) +
	                    " , " +
	                    Double.toString(loc.longitude) +
	                    " passed to address service";
	            Log.e("LocationSampleActivity", errorString);
	            e2.printStackTrace();
	            return errorString;
			}
			// If the reverse geocode returned an address
            if (addresses != null && addresses.size() > 0) {
                // Get the first address
                Address address = addresses.get(0);
                /*
                 * Format the first line of address (if available),
                 * city, and country name.
                 */
                String addressText = String.format(
                        "%s, %s, %s",
                        // If there's a street address, add it
                        address.getMaxAddressLineIndex() > 0 ?
                                address.getAddressLine(0) : "",
                        // Locality is usually a city
                        address.getLocality(),
                        // The country of the address
                        address.getCountryName());
                // Return the text
                //return addressText;
                //return loc.latitude+" ; "+loc.longitude+ " @ "+addressText;
                //return "Hint: "+ addressText;
                return ""+addressText;
            }else
            	return "No Address Found";
		}
		
		
		 /**
         * A method that's called once doInBackground() completes. Turn
         * off the indeterminate activity indicator and set
         * the text of the UI element that shows the address. If the
         * lookup failed, display the error message.
         */
		@Override
        protected void onPostExecute(String address) {
			Log.i(TAG, "GetAddressTask "+"onPostExecute address: "+address);
            // Set activity indicator visibility to "gone"
            mActivityIndicator.setVisibility(View.GONE);
            //set the tile of the marker to the address we resolve right now
            searchResultMarker.get(0).setTitle("Destination");
            searchResultMarker.get(0).setSnippet(address);
            //searchResultMarker.get(0).showInfoWindow();
            
            // Display the results of the lookup.
            /*final EditText editText_destination = (EditText) findViewById(R.id.UserInputText_destination);
            editText_destination.setText(address.split("@@")[1]);
            
            final TextView TextView_switch = (TextView) findViewById(R.id.text_switch_On_Off);
            TextView_switch.setText(address.split("@@")[0]);*/
        }
	}
    
    
    /*
     * Create a new location client, using the enclosing class to
     * handle callbacks.
     * create the location client in onCreate(), then connect it in onStart(),Disconnect the client in onStop()
     */
    private void InitializeLocationService(){
    	//mLocationClient = new LocationClient(this, this, this);//create the location clien
    	//mLocationRequest = LocationRequest.create();
    	//use high accuracy
    	//mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    	//set the update interval to 10 seconds
    	//mLocationRequest.setInterval(UPDATE_INTERVAL);
    	//set the fasterst update interval to 5 seconds
    	//mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
    }
    
    //this function is called to initialize the variable for google map API
    private void Initializemap(){
    	//first get reference to the fragment that contains google map
    	mapFrag = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
    	
    	if(mapFrag==null){
        	System.out.println("Empty mapFrag");
        	Toast.makeText(getApplicationContext(), "Could Not Load Map", Toast.LENGTH_SHORT).show();
        	return;
    	}else if(gMap==null){
        		gMap = mapFrag.getMap();
        		if(gMap==null){
            		Toast.makeText(getApplicationContext(), "Fail to Load Map", Toast.LENGTH_SHORT).show();
            		return;
            	}else
            		Toast.makeText(getApplicationContext(), "Success Loading Map", Toast.LENGTH_SHORT).show();
        }
        
    	//set some properties of gMap
    	//so that we can see the compass and myLocation button at the map
    	mUiSettings = gMap.getUiSettings();
    	mUiSettings.setCompassEnabled(true);
    	mUiSettings.setMyLocationButtonEnabled(true);
    	gMap.setMyLocationEnabled(true);//so that could see my location button
    	
    	//set the onclick listener of the marker on the google Map
    	gMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener(){
			@Override
			public boolean onMarkerClick(Marker marker) {
				// TODO Auto-generated method stub
				//first need to check whether this marker is the destination marker, we only call route computing on destination marker.
				//if(marker.equals(searchResultMarker)){
				if(searchResultMarker.contains(marker)){
					
					//Toast.makeText(getApplicationContext(), "end marker clicked", Toast.LENGTH_SHORT).show();
					marker.showInfoWindow();
					
				}else{
					Toast.makeText(getApplicationContext(), "clicked start marker", Toast.LENGTH_SHORT).show();
					return false;
				}
				
				LatLng start;
				LatLng end;
				
				//we assume that when the user click on this marker, 
				//it means that the user wanna to get direction to this marker location
				//then should call the direction API to get directions and show paths on the map
				
				//first get the current location
				//we need to check that whether MapAPPsLocationUploadService is started or not
				final ToggleButton ToggleButton_switch = (ToggleButton) findViewById(R.id.ToggleButton_switch_On_Off);
				if(!ToggleButton_switch.isChecked()){
					Log.e(TAG, "ToggleButton_switch is Unchecked!!! Please Turn On To Work State!");
					Toast.makeText(MainActivity.this, "Please Turn On To Work State!", Toast.LENGTH_LONG);
					return false;
				}else if(!mBound){
					Log.e(TAG, "MainActivity is not bound to MapAppsLocationUploadService");
					Toast.makeText(MainActivity.this, "Connecting to Location Service...Try again Later", Toast.LENGTH_LONG);
					return false;
				}else if(mBindService==null){
					Log.e(TAG, "mBindService is null");
					Toast.makeText(MainActivity.this, "Connecting to Location Service...Try again Later", Toast.LENGTH_LONG);
					return false;
				}else if((currentLocation = mBindService.getCurrentLocation())==null){
					Log.e(TAG, "Location is null, it is Not Updated Yet");
					Toast.makeText(MainActivity.this, "Location Is Not Updated Yet... Try again Later", Toast.LENGTH_LONG);
					return false;
				}
				if(System.currentTimeMillis()- currentLocation.getTime() >2*UPDATE_INTERVAL){
					//mean that the location data is not most recent
					Log.e(TAG, "current location retrieved "+2*UPDATE_INTERVAL/1000+" seconds ago, too stale. Turn to to work state");
					String message = "current location retrieved "+2*UPDATE_INTERVAL/1000+" seconds ago, too stale. Turn to work state";
					Toast.makeText(MainActivity.this,message, Toast.LENGTH_LONG).show();
					return true;
				}else{
					start = new LatLng(currentLocation.getLatitude(),currentLocation.getLongitude());
				}
				//then get  the destination location, namely the marker location
				end = new LatLng((marker.getPosition()).latitude,marker.getPosition().longitude);
				//then need to register this end point to MapAppsLocationUploadService
				//so that, MapAppsLocationUploadService would compare whether usrer have arrive destination
				String destinationAddress = marker.getSnippet();
				
				//update start and destination information for upload purpose
				startLatLng_upload = new LatLng(start.latitude,start.longitude);
				EndAddress_upload = destinationAddress;
				
				//then bind to the service to send the destinationAddress
				mBindService.SetDestinationAddress(destinationAddress);
				
				mBindService.registerStart_end(start,end);
				
				//Toast.makeText(getApplicationContext(), "start: "+start.toString()+" end: "+end.toString(), Toast.LENGTH_LONG).show();
				
				if(StartMarker!=null)
					StartMarker.remove();
				StartMarker = gMap.addMarker(new MarkerOptions().position(start)
	                     .title("start").draggable(false)
	                     .alpha((float) 0.9));
				//searchResultMarker.add(StartMarker);//add this to searchResultMarker, so that can clear the markers
				/*Marker EndMarker = gMap.addMarker(new MarkerOptions().position(end)
	                     .title("end").draggable(false)
	                     .alpha((float) 0.9));*/
				StartMarker.showInfoWindow();
				
				//searchResultMarker.showInfoWindow();
				
				//then send routing request using google direciton API
				Routing routing = new Routing(Routing.TravelMode.DRIVING);
				routing.registerListener(MainActivity.this);
				routing.execute(start,end);
				
				// Request a connection from the client to Location Services
	            //mLocationClient.connect();
				//return true means that we use our customed onclick listener rather than the default
				return false;
			}
    		
    	});
    	final ToggleButton ToggleButton_switch = (ToggleButton) findViewById(R.id.ToggleButton_switch_On_Off);
    	//then set the long click listener of the map
    	//so that user could long click the map to add destination to the map
    	gMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
			
			@Override
			public void onMapLongClick(LatLng location) {
				if(!ToggleButton_switch.isChecked()){
					Toast.makeText(MainActivity.this, "Please Turn On to Work State", Toast.LENGTH_LONG).show();
					return;
				}
					
				//this location is the latitude and longitude where user long click at
				//first clear the searchResultMarker
				if(StartMarker!=null)
					StartMarker.remove();
				if(searchResultMarker!=null){
					for(Marker eachMarker:searchResultMarker)
						eachMarker.remove();
					searchResultMarker.clear();
				}else
					searchResultMarker = new ArrayList<Marker>(); 
				//add this location as a marker to the map, and store reference of this marker to searchResultMarker
				searchResultMarker.add(gMap
						.addMarker(new MarkerOptions()
						.position(location)
						.draggable(false)
						.alpha((float)1)));
				//then we need to call the location to address function
				resolve_LatLon_Address(location);
			}
    	});
    	
    	
    	//then start to manipulate the map
    	 try{
          // Move the camera instantly to HRBB with a zoom of 9.
         	gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(CameraCenterForFirstMapLoad, 9));
          // Zoom to level 11, using 2000 milliseconds animating the camera.
         	gMap.animateCamera(CameraUpdateFactory.zoomTo(11), 2000, null);
         }catch(Error e){
         	//System.out.println(e);
        	 e.printStackTrace();
         }
    	//then try to initialize the searchResultMarker arraylist
    	 searchResultMarker = new ArrayList<Marker>();
    }
    
    /*
     * Called when the Activity becomes visible.
     */
	@Override
	protected void onStart() {
		super.onStart();
		Log.i(TAG, "onStart()");
		//connect to location service
		/*if(!mLocationClient.isConnected()){
			mLocationClient.connect();//connect mLocationClient to location service
		}*/
	}

	@Override
	protected void onRestart() {
		final ToggleButton ToggleButton_switch = (ToggleButton) findViewById(R.id.ToggleButton_switch_On_Off);
        if(ToggleButton_switch.isChecked()){
        	//MapAppsLocationUploadService is still running on background
        	//we need to bind service here Bind to LocalService
        	Log.i(TAG, "bind to MapAppsLocationUploadService at onRestart");
			Intent intent = new Intent(MainActivity.this, MapAppsLocationUploadService.class);
			bindService(intent,mBindConnection,Context.BIND_AUTO_CREATE);
        }
		
		
		super.onRestart();
		Log.i(TAG, "OnRestarted.");
	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.i(TAG, "onResume");
	}

	@Override
	protected void onPause() {
		super.onPause();
		Log.i(TAG, "onPause");
	}

	/*
     * Called when the Activity is no longer visible.
     * stop updates and disconnect
     */
	@Override
	protected void onStop() {
		/*//if the client is connected
		if(mLocationClient.isConnected()){
			
			 * remove location updates for a listener
			 * The current Activity is the listener, so
             * the argument is "this".
			 
			mLocationClient.removeLocationUpdates(this);
			//disconnect to the location service
			mLocationClient.disconnect();
		}*/
		
		//saved the application State, i.e. the app is at work state to SharedPreferences on disk
		final ToggleButton ToggleButton_switch = (ToggleButton) findViewById(R.id.ToggleButton_switch_On_Off);
		SharedPreferences sharedPref = MainActivity.this.getPreferences(Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = sharedPref.edit();
		editor.putBoolean(getString(R.string.button_switch_Off_State), ToggleButton_switch.isChecked());
		editor.commit();
		
		//unbind from the MapAppsLocationUploadService
		if(mBound){
			unbindService(mBindConnection);
			mBound=false;
		}
		
		super.onStop();
		Log.i(TAG, "onStop");
	}

	@Override
	protected void onDestroy() {
		//because onDestory is not guaranteed to be called, we will move the resource cleaning codes into onStop.
		Log.i(TAG, "onDestroyed.");
		super.onDestroy();
		
	}
	
	//I copy the following code from: http://developer.android.com/training/location/retrieve-current.html#DefineCallbacks 
	/*
     * Called by Location Services if the attempt to
     * Location Services fails.
     */
	@Override
	public void onConnectionFailed(ConnectionResult connectionResult) {
		// TODO Auto-generated method stub
		/*
         * Google Play services can resolve some errors it detects.
         * If the error has a resolution, try sending an Intent to
         * start a Google Play services activity that can resolve
         * error.
         */
		if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(
                        this,
                        9000);//9000 is the value of CONNECTION_FAILURE_RESOLUTION_REQUEST
                /*
                 * Thrown if Google Play services canceled the original
                 * PendingIntent
                 */
            } catch (IntentSender.SendIntentException e) {
                // Log the error
                e.printStackTrace();
            }
        } else {
            /*
             * If no resolution is available, display a dialog to the
             * user with the error.
             */
            showDialog(connectionResult.getErrorCode());
        }
		
	}

	/*
     * Called by Location Services when the request to connect the
     * client finishes successfully. At this point, you can
     * request the current location or start periodic updates
     */
	@Override
	public void onConnected(Bundle arg0) {
		// TODO Auto-generated method stub
		// Display the connection status
        Toast.makeText(this, "Connected", Toast.LENGTH_SHORT).show();
        //if(ToggleButton_switch.isChecked())
        //	mLocationClient.requestLocationUpdates(mLocationRequest, this);
	}
	/*
     * Called by Location Services if the connection to the
     * location client drops because of an error.
     */
	@Override
	public void onDisconnected() {
		// TODO Auto-generated method stub
		Toast.makeText(this, "Disconnected from Location Service Client. Please re-connect.",
                Toast.LENGTH_SHORT).show();
	}

	// Define the callback method that receives location updates at given interval, e.g. 10 seconds
	@Override
	public void onLocationChanged(Location location) {
		// TODO Auto-generated method stub
		// Report to the UI that the location was updated
		
        //resolve_LatLon_Address(location);
		//if this is the frist time of LocationChanged call, we need to move our camera to center on this location
		if(IsThisFirstLocationChanged){
			//need to upload the location information to remote server
	     	gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 10));
	         
	      // Zoom in, animating the camera.
	     	gMap.animateCamera(CameraUpdateFactory.zoomTo(15), 2000, null);
	     	IsThisFirstLocationChanged = false;
		}
		
     	/*if(currentLocation!=null){
     		long difference =  (location.getTime()-currentLocation.getTime());
     		String timeDifference = ""+difference;System.currentTimeMillis()-
     		Toast.makeText(this, timeDifference+" "+(System.currentTimeMillis()-currentLocation.getTime()), Toast.LENGTH_SHORT).show();
     	}*/
     	
     	//store this information to current location variable.
     	if(currentLocation==null)
     		currentLocation = new Location(location);
     	else
     		currentLocation.set(location);
     	
     	String msg = "Updated Location: " +
                Double.toString(location.getLatitude()) + "," +
                Double.toString(location.getLongitude());
        //Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
     	
     	//should upload this location information to server. Note: location object contains: latitude, longtitude, time, accurecy...
     	//we can use this information to rebuild the history tracking route of user.
	}

	
	@Override
	public void onRoutingFailure() {
		// TODO Auto-generated method stub
		// The Routing request failed
	}

	@Override
	public void onRoutingStart() {
		// TODO Auto-generated method stub
		// The Routing Request starts
	}

	@Override
	public void onRoutingSuccess(PolylineOptions mPolyOptions, Route route) {
		// TODO Auto-generated method stub
		//If you want to clear "all markers, overlays, and polylines from the map"
		//before we draw new direction lines on the map, we need to clear the last drawed line
		if(last_direction_line!=null)
			last_direction_line.remove();
		Log.i(TAG, "onRoutingSuccess"+route.getPoints().size());
		PolylineOptions polyoptions = new PolylineOptions();
		for (LatLng point : route.getPoints()) {
			Log.i(TAG, point.latitude+" "+point.longitude);
			polyoptions.add(point);
	     }
		polyoptions.color(Color.BLUE);
		polyoptions.width(15);
		polyoptions.geodesic(true);
		
		last_direction_line = gMap.addPolyline(polyoptions);
		Toast.makeText(MainActivity.this, "distance: "+ route.getLength(), Toast.LENGTH_LONG).show();
		
		//send distance to remote server
		new SendPostRequest(this).execute(""+route.getLength(),""+startLatLng_upload.latitude,""+startLatLng_upload.longitude,EndAddress_upload);
		/*//Test: how to draw lines on the MAP??
		Polyline line = gMap.addPolyline(new PolylineOptions()
	    .add(new LatLng(-37.81319, 144.96298), new LatLng(-31.95285, 115.85734))
	    .width(25)
	    .color(Color.BLUE)
	    .geodesic(true));*/
		
	}
	
	
	
	// usually, subclasses of AsyncTask are declared inside the activity class.
	// that way, you can easily modify the UI thread from here
	//this AsyncTask is run at a seperate thread other than main thread
	private class SendPostRequest extends AsyncTask<String,Void,String>{

		private String ID;
		
		//constructor used to initialize context variable mContext
		Context mContext;
		public SendPostRequest(Context context){
			super();
			mContext=context;
		}
		
		@Override
		protected void onPreExecute(){
			final EditText EditText_id = (EditText) findViewById(R.id.text_user_id);
			ID = EditText_id.getText().toString();
			if(ID==null || ID=="")
				ID="Default";
		}
		
		
		@Override
		protected String doInBackground(String... para) {
			//this method is executed at background thread
			String distance = para[0];
			
			//destination and start location information
			String start_destination = "";
			String end_destination = "";
			
			LatLng startLatLng;
			
			if(para.length>=4){
				startLatLng = new LatLng(Double.parseDouble(para[1]),Double.parseDouble(para[2]));
				//then resolve this start LatLng to address using service
				start_destination = new latlngToAddress(mContext).getAddress(startLatLng);
				end_destination = para[3];
			}
			
			
			Log.i(TAG, "***do in back ground***");
			Log.i(TAG, "distance: "+distance);
			
			HttpClient httpClient = new DefaultHttpClient();
			// In a POST request, we don't pass the values in the URL.
            //Therefore we use only the web page URL as the parameter of the HttpPost argument
			HttpPost httpPost = new HttpPost("http://vlsiplayground.com:3000/tracker/index");
			// Because we are not passing values over the URL, we should have a mechanism to pass the values that can be
            //uniquely separate by the other end.
            //To achieve that we use BasicNameValuePair             
            //Things we need to pass with the POST request
			List<NameValuePair> pairs = new ArrayList<NameValuePair>();
			pairs.add(new BasicNameValuePair("track[id]",""));
			pairs.add(new BasicNameValuePair("track[employee_id]",ID));
			
			pairs.add(new BasicNameValuePair("track[distance]",distance));
			
			//add sorce and destination information, if no destination or source found, will upload "", i.e empty string
			pairs.add(new BasicNameValuePair("track[source]",start_destination));
			pairs.add(new BasicNameValuePair("track[dest]",end_destination));
			
			Log.i(TAG, "track[source] : "+start_destination);
			Log.i(TAG, "track[dest]: "+ end_destination);
			
			// UrlEncodedFormEntity is an entity composed of a list of url-encoded pairs. 
            //This is typically useful while sending an HTTP POST request. 
			try {
				httpPost.setEntity(new UrlEncodedFormEntity(pairs));
				// HttpResponse is an interface just like HttpPost.
                //Therefore we can't initialize them
				try {
					HttpResponse httpResponse = httpClient.execute(httpPost);
					
					// According to the JAVA API, InputStream constructor do nothing. 
                    //So we can't initialize InputStream although it is not an interface
					InputStream inputStream = httpResponse.getEntity().getContent();
					InputStreamReader inputStreamReader = new InputStreamReader(inputStream);

                    BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

                    StringBuilder stringBuilder = new StringBuilder();

                    String bufferedStrChunk = null;
                    while((bufferedStrChunk = bufferedReader.readLine()) != null){
                        stringBuilder.append(bufferedStrChunk);
                    }
                    return stringBuilder.toString();
					
				} catch (ClientProtocolException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			return null;
		}
		
		@Override
		protected void onPostExecute(String result){
			//this method is executed at the main thread
			Log.i(TAG, "onPostExecute");
			Log.i(TAG, "Re: "+result);
		}
		
	}
}
