package com.example.mapapps;

import com.example.mapapps.mgeofence.OnEnterGefenceListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationClient.OnAddGeofencesResultListener;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.LatLng;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.location.Location;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.Toast;
import android.widget.ToggleButton;

/**
 * MainActivity start this service by calling: startService(). 
 * Then this service would run indefinitely, even main service is destroyed.
 * 
 * If MainActivity restart, it can call: bindService() to send request.
 * Then it will request this service to destory it self.
 * @author congcongchen
 *
 */

public class MapAppsLocationUploadService extends Service implements
GooglePlayServicesClient.ConnectionCallbacks,
GooglePlayServicesClient.OnConnectionFailedListener,
LocationListener
{
	private LocationClient mLocationClient;
	private Context mycontext;
	private Location currentLocation;//variable that hold current location. And we will update this variable upon location changed.
	
	/*
	 * Gloabal Constants for location service
	 */
	// Milliseconds per second
    private static final int MILLISECONDS_PER_SECOND = 1000;
    // Update frequency in seconds
    public static final int UPDATE_INTERVAL_IN_SECONDS = 10;//so 555 meters @speed of 100 k/h
    // Update frequency in milliseconds
    private static final long UPDATE_INTERVAL =
            MILLISECONDS_PER_SECOND * UPDATE_INTERVAL_IN_SECONDS;
    // The fastest update frequency, in seconds
    //through experiment, I found that this value is more important than UPDATE_INTERVAL, bacause, there are some app at the 
    //same system whose request frequency is higher than UPDATE_INTERVAL, then Android would send location update 
    //at the frequency specified by FASTEST_INTERVAL_IN_SECONDS.
    private static final int FASTEST_INTERVAL_IN_SECONDS = 5;//so 277 meters @speed of 100 k/h
    // A fast frequency ceiling in milliseconds
    private static final long FASTEST_INTERVAL =
            MILLISECONDS_PER_SECOND * FASTEST_INTERVAL_IN_SECONDS;
    //define an object that holds accuracy and frequency parameters
    LocationRequest mLocationRequest;
    
    //Binder given to clients
    private final IBinder mbinder = new LocalBinder();
	
    //this is the handler of class that implement OnEnterGefenceListener, so that when this happens, 
    //MapAppsLocationUploadService could call the user defined methods
    //private OnEnterGefenceListener onEnterGefenceHandler;
    
    //the start point
    LatLng start;
    //the end point
    LatLng end;
    //boolean indicate whether currently is at ongoing mission
    boolean AtGoinMission=false;
    //define the radius that define the geoFence of destination
    private final float USER_DEFINE_GEOFENCE_RADIUS = 100;//20 meters 
    //define a context variable to hold referece to MainActivity
    private Context mcontext;
    
    //for notification bar
    // Notification ID to allow for future updates
 	private static final int MY_NOTIFICATION_ID = 1;
 	// Notification Count
 	private int mNotificationCount;
 	// Notification Text Elements
 	private final CharSequence tickerText = "You have arrived Destination";
 	private final CharSequence contentText = "Destination: ";
 	// Notification Action Elements
 	private Intent mNotificationIntent;
 	private PendingIntent mContentIntent;
 	// Notification Sound and Vibration on Arrival
 	private long[] mVibratePattern = { 0, 200, 200, 300 };
 	
 	private String DestinationAddress;
 	//this view is used to display notification to user at the notification bar
 	RemoteViews mContentView = new RemoteViews(
			"com.example.mapapps",
			R.layout.user_notification_notification_bar);
    
    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder{
    	//The LocalBinder provides the getService() method for clients to 
    	//retrieve the current instance of LocalService. 
    	//This allows clients to call public methods in the service.
    	MapAppsLocationUploadService getService(){
    		return MapAppsLocationUploadService.this;
    	}
    }
    
    @Override
    public void onCreate(){
    	super.onCreate();
    	Log.i("MapApps", "onCreate MapAppsLocationUploadService");
    	InitializeLocationService();
    	//used to send notification to user once arrive destination
    	//InitializeNotificationBar();
    }
    
    
	/**
	 * The system calls this method when another component, 
	 * such as an activity, requests that the service be started,
	 *  by calling startService().
	 *  
	 *  Once this method executes, the service is started and can 
	 *  run in the background indefinitely. If you implement this, 
	 *  it is your responsibility to stop the service when its work
	 *   is done, by calling stopSelf() or stopService(). 
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startid){

    	Log.i("MapApps", "onStartCommand");
		//connect to location service
		if(!mLocationClient.isConnected()){
			mLocationClient.connect();//connect mLocationClient to location service
		}
		// Don't automatically restart this Service if it is killed
		return START_NOT_STICKY;
		
	}
	
	/**
	 * The system calls this method when another component wants to
	 *  bind with the service (such as to perform RPC), by calling bindService(). 
	 *  
	 *  A bound service offers a client-server interface that allows components 
	 *  to interact with the service, send requests, get results, and even do so 
	 *  across processes with interprocess communication (IPC). A bound service 
	 *  runs only as long as another application component is bound to it. 
	 *  Multiple components can bind to the service at once, but when all of 
	 *  them unbind, the service is destroyed.
	 */
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		Log.i("MapApps", "onBind MapAppsLocationUploadService");
		//The LocalBinder provides the getService() method for clients to 
    	//retrieve the current instance of LocalService. 
    	//This allows clients to call public methods in the service.
		
		return mbinder;
	}
	
	/**
	 * register the listener of OnEnterGefenceListener
	 */
	/*public void register_listener(OnEnterGefenceListener handler){
		onEnterGefenceHandler = handler;
	}*/
	
	
	/**
	 * hen need to register this end point to MapAppsLocationUploadService 
	 * so that, MapAppsLocationUploadService would compare whether usrer have arrive destination
	 */
	public void registerStart_end(LatLng start, LatLng end){
		Log.i("MapApps", "registerStart_end MapAppsLocationUploadService");
		if(!AtGoinMission || !this.end.equals(end)){
			Log.i("MapApps", "registerStart_end successfully!!!");
			this.start=start;
			this.end=end;
			AtGoinMission=true;
		}
	}
	
	/**
	 * register mcontext to hold reference to mainActivity
	 */
	public void registerContext(Context mainActivity){
		Log.i("MapApps", "registerContext MapAppsLocationUploadService");
		mcontext=mainActivity;
	}
	
	/**
	 * The system calls this method when the service is no longer used and is 
	 * being destroyed. Your service should implement this to clean up any 
	 * resources such as threads, registered listeners, receivers, etc. This is
	 *  the last call the service receives.
	 */
	@Override
	public void onDestroy() {
		Log.i("MapApps", "onDestroy MapAppsLocationUploadService");
		//do something to clean up the resource.
		//mLocationClient.removeLocationUpdates(this);
		
		if(mLocationClient.isConnected()){
			/*
			 * remove location updates for a listener
			 * The current Activity is the listener, so
             * the argument is "this".
			 */
			mLocationClient.removeLocationUpdates(this);
			//disconnect to the location service
			mLocationClient.disconnect();
		}
	}
	
	
	/**
	   * The IntentService calls this method from the default worker thread with
	   * the intent that started the service. When this method returns, IntentService
	   * stops the service, as appropriate.
	   */
	/*@Override
	protected void onHandleIntent(Intent arg0) {
		// TODO Auto-generated method stub
		
	}*/
	
	
	/*
     * Create a new location client, using the enclosing class to
     * handle callbacks.
     * create the location client in onCreate(), then connect it in onStart(),Disconnect the client in onStop()
     */
    private void InitializeLocationService(){
    	mLocationClient = new LocationClient(this, this, this);//create the location clien
    	mLocationRequest = LocationRequest.create();
    	//use high accuracy
    	mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    	//set the update interval to 10 seconds
    	mLocationRequest.setInterval(UPDATE_INTERVAL);
    	//set the fasterst update interval to 5 seconds
    	mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
    }

    /**
     * return currentLocation which hold current location. 
     * And we will update currentLocation upon location changed. 
     */
    public Location getCurrentLocation(){
    	Log.i("MapApps", "getCurrentLocation MapAppsLocationUploadService");
    	return currentLocation;//currentLocation would be null, if onLocationChanged is not called at all.
    }
    
	@Override
	public void onLocationChanged(Location location) {
		Log.i("MapApps", "onLocationChanged");
		// TODO Auto-generated method stub
		// TODO Auto-generated method stub
		// Report to the UI that the location was updated
		
        //resolve_LatLon_Address(location);
		//if this is the frist time of LocationChanged call, we need to move our camera to center on this location
		/*if(IsThisFirstLocationChanged){
			//need to upload the location information to remote server
	     	gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 10));
	         
	      // Zoom in, animating the camera.
	     	gMap.animateCamera(CameraUpdateFactory.zoomTo(15), 2000, null);
	     	IsThisFirstLocationChanged = false;
		}*/
		
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
		
        //we could check the distance between currentlocation of destination, if the distance is smaller than predefined value, 
        //then we could say that user has entered the destination
        if(AtGoinMission){
        	InitializeNotificationBar();
        	LatLng currentLatLng = new LatLng(location.getLatitude(),location.getLongitude());
        	float currentDist = calc_distance(currentLatLng,end);
        	Log.i("MapApps", "calculate distance: "+currentDist+" MapAppsLocationUploadService");
        	if(currentDist<= USER_DEFINE_GEOFENCE_RADIUS){
        		Log.i("MapApps", "currentDist<= USER_DEFINE_GEOFENCE_RADIUS");
        		
        		//here we could upload the arrive information to remote server
        		/*
        		 * some code here
        		 */
        		try{
        			//notify User that he/she has arrived destination
            		Toast.makeText(mcontext, "Arrive Destination", Toast.LENGTH_LONG).show();
        		}catch(Error e){
        			e.printStackTrace();
        			Log.i("MapApps", "Error, mcontext NULL "+e.getCause());
        		}
        		
        		//send notification
        		// Define the Notification's expanded message and Intent:
        		mContentView.setTextViewText(R.id.notification_text, "You have Arrive Destination");
        		//mContentView.setTextViewText(R.id.latitdueValue, ""+end.latitude);
        		//mContentView.setTextViewText(R.id.longtitudeValue, ""+end.longitude);
        		
        		//build the notification
        		NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(
						getApplicationContext())
					.setTicker(tickerText)
					.setSmallIcon(android.R.drawable.ic_dialog_info)
					.setAutoCancel(true)
					.setContentIntent(mContentIntent)
					.setVibrate(mVibratePattern)
					.setContent(mContentView);
        		// Pass the Notification to the NotificationManager:
				NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
				mNotificationManager.notify(MY_NOTIFICATION_ID,
						notificationBuilder.build());
				
				//means that have arrive at destination
        		AtGoinMission=false;
        		start=null;
        		end=null;
        	}
        }
	}

	/*
	 * calc distance between two different latlng point
	 * the returned result is in meters
	 */
	private float calc_distance(LatLng current, LatLng destination){
		float[] result = new float[1];
		Location.distanceBetween(current.latitude, current.longitude, destination.latitude, destination.longitude,result );
		//Location.distanceBetween();
		return result[0];
	}
	
	@Override
	public void onConnectionFailed(ConnectionResult connectionResult) {
		// TODO Auto-generated method stub
		Log.i("MapApps", "onConnectionFailed MapAppsLocationUploadService");
		Toast.makeText(this, "Connection Failed.",
                Toast.LENGTH_SHORT).show();

	}

	@Override
	public void onConnected(Bundle arg0) {
		// TODO Auto-generated method stub
		// TODO Auto-generated method stub
		// Display the connection status
		Log.i("MapApps", "onConnected MapAppsLocationUploadService");
        Toast.makeText(this, "Connected", Toast.LENGTH_SHORT).show();
        
        //if(ToggleButton_switch.isChecked())
        //	mLocationClient.requestLocationUpdates(mLocationRequest, this);
        Log.i("MapApps", "mLocationClient is: "+mLocationClient.isConnected());
    	//if checked, means that now is at work, so need to activate the location update service
		mLocationClient.requestLocationUpdates(mLocationRequest,this);
	}

	@Override
	public void onDisconnected() {
		// TODO Auto-generated method stub
		// TODO Auto-generated method stub
		Log.i("MapApps", "onDisconnected MapAppsLocationUploadService");
		Toast.makeText(this, "Disconnected from Location Service Client. Please re-connect.",
                Toast.LENGTH_SHORT).show();
		
	}
	
	
	/**
	 * used to initialize some set up of notification bar	
	 */
	private void InitializeNotificationBar(){
		mNotificationIntent= new Intent(getApplicationContext(),UserNotificationActivity.class);
		mNotificationIntent.putExtra(getString(R.string.user_arrive_notification_Destination_title), DestinationAddress==null? "Destination":DestinationAddress);
		mNotificationIntent.putExtra(getString(R.string.user_arrive_notification_Latitude_title), end.latitude);
		mNotificationIntent.putExtra(getString(R.string.user_arrive_notification_Longtitude_title), end.longitude);
		
		mContentIntent = PendingIntent.getActivity(getApplicationContext(), 0, mNotificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		
	}
	
	//set the destination Address, called by MainActivity when at the onmarker click method
	public void SetDestinationAddress(String dest){
		DestinationAddress = dest;
	}


}
