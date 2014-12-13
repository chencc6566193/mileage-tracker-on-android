package com.example.mapapps.mgeofence;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationClient.OnAddGeofencesResultListener;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.maps.model.LatLng;

/**
 * client for creating, removing geofence
 * @author congcongchen
 *
 */
public class MGeofenceClient extends Service implements
GooglePlayServicesClient.ConnectionCallbacks,
GooglePlayServicesClient.OnConnectionFailedListener,
LocationListener,
OnAddGeofencesResultListener
{
	/*
     * Use to set an expiration time for a geofence. After this amount
     * of time Location Services will stop tracking the geofence.
     */
    private static final long SECONDS_PER_HOUR = 3600;
    private static final long MILLISECONDS_PER_SECOND = 1000;
    private static final long GEOFENCE_EXPIRATION_IN_HOURS = 12;
    private static final long GEOFENCE_EXPIRATION_TIME =
            GEOFENCE_EXPIRATION_IN_HOURS *
            SECONDS_PER_HOUR *
            MILLISECONDS_PER_SECOND;
	
    //reference to the MGeofence
    private MGeofence mGeofence;
    // Persistent storage for geofences
    private MGeofenceStore mGeofenceStore;
    
    // Holds the location client
    private LocationClient mLocationClient;
    // Stores the PendingIntent used to request geofence monitoring
    private PendingIntent mGeofenceRequestIntent;
    
    // Flag that indicates if a request is underway.
    private boolean mInProgress;
    /*
     * constructor
     * mContext could be mainActivity.this
     */
	public MGeofenceClient(Context mContext, LatLng destination, int expiration_hour, float radius){
		mGeofenceStore = new MGeofenceStore(mContext);
		createGeofence(destination,expiration_hour,radius);
	}
	
	private void createGeofence(LatLng destination,int expiration_hour, float radius){
		/*
         * Create an internal object to store the data. Set its
         * ID to "1". This is a "flattened" object that contains
         * a set of strings
         */
		mGeofence = new MGeofence(
				"1",
				destination.latitude,
				destination.longitude,
				radius,
				//we default the expiration hour to be 12 hours, user could modify this parameter if they want other experiation hours
				GEOFENCE_EXPIRATION_TIME,
				// This geofence records only entry transitions
				Geofence.GEOFENCE_TRANSITION_ENTER
				);
		
		// Store this flat version
		mGeofenceStore.setGeofence("1", mGeofence);
	}
	
	/**
     * Start a request for geofence monitoring by calling
     * LocationClient.connect().
     */
	 public void addGeofences() {
		 /*
	         * Create a new location client object. Since the current
	         * activity class implements ConnectionCallbacks and
	         * OnConnectionFailedListener, pass the current activity object
	         * as the listener for both parameters
	         */
	        mLocationClient = new LocationClient(this, this, this );
	 }
	
/*	public register(){
		
	}
	
	public void sendRequest(){
		
	}
	
	public boolean isRegistered(){
		
	}*/
	
	/*
	 * Specifies a method that Location Services calls once it has added the geofences
	 * @see com.google.android.gms.location.LocationClient.OnAddGeofencesResultListener#onAddGeofencesResult(int, java.lang.String[])
	 */
	@Override
	public void onAddGeofencesResult(int arg0, String[] arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onConnectionFailed(ConnectionResult arg0) {
		// TODO Auto-generated method stub
		Log.i("MapApps", "onConnectionFailed MapAppsLocationUploadService");
		/*Toast.makeText(MainActivity.this, "Connection Failed.",
                Toast.LENGTH_SHORT).show();*/
	}

	@Override
	public void onConnected(Bundle arg0) {
		// Display the connection status
		Log.i("MapApps", "onConnected MapAppsLocationUploadService");
        //Toast.makeText(this, "Connected", Toast.LENGTH_SHORT).show();
        
        //if(ToggleButton_switch.isChecked())
        //	mLocationClient.requestLocationUpdates(mLocationRequest, this);
        Log.i("MapApps", "mLocationClient is: "+mLocationClient.isConnected());
    	//if checked, means that now is at work, so need to activate the location update service
		//mLocationClient.requestLocationUpdates(mLocationRequest,this);
	}

	@Override
	public void onDisconnected() {
		// TODO Auto-generated method stub
		Log.i("MapApps", "onDisconnected MapAppsLocationUploadService");
		/*Toast.makeText(this, "Disconnected from Location Service Client. Please re-connect.",
                Toast.LENGTH_SHORT).show();*/
	}
	
	
	/*
     * Create a PendingIntent that triggers an IntentService in your
     * app when a geofence transition occurs.
     */
    /*private PendingIntent getTransitionPendingIntent() {
        // Create an explicit Intent
        Intent intent = new Intent(this,
                ReceiveTransitionsIntentService.class);
        
         * Return the PendingIntent
         
        return PendingIntent.getService(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
    }*/

	@Override
	public void onLocationChanged(Location arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}
	
	
}
