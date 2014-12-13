package com.example.mapapps;
/**
 * Async Task to access the Google Direction API and return the routing data
 * which is then parsed and converting to a route overlay using some classes created by Hesham Saeed.
 * @author CongcongChen
 * Requires an instance of the map activity and the application's current context for the progress dialog.
 *
 */

import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;


public class Routing extends AsyncTask<LatLng, Void, Route> {
	protected ArrayList<RoutingListener> _aListeners;
	protected TravelMode _mTravelMode;
	
	public enum TravelMode {
        BIKING("biking"),
        DRIVING("driving"),
        WALKING("walking"),
        TRANSIT("transit");

        protected String _sValue;

        private TravelMode(String sValue) {
            this._sValue = sValue;
        }

        protected String getValue() {
            return _sValue;
        }
    }
	
	public Routing(TravelMode mTravelMode) {
        this._aListeners = new ArrayList<RoutingListener>();
        this._mTravelMode = mTravelMode;
    }
	
	public void registerListener(RoutingListener mListener) {
        _aListeners.add(mListener);
    }
	
	protected void dispatchOnStart() {
        for (RoutingListener mListener : _aListeners) {
            mListener.onRoutingStart();
        }
    }
	
	protected void dispatchOnFailure() {
        for (RoutingListener mListener : _aListeners) {
            mListener.onRoutingFailure();
        }
    }
	protected void dispatchOnSuccess(PolylineOptions mOptions, Route route) {
		
        for (RoutingListener mListener : _aListeners) {
        	Log.i("MapApps", "dispatchOnSuccess");
            mListener.onRoutingSuccess(mOptions, route);
        }
    }
	/**
     * Performs the call to the google maps API to acquire routing data and
     * deserializes it to a format the map can display.
     *
     * @param aPoints
     * @return
     */
	@Override
	protected Route doInBackground(LatLng... aPoints) {
		// TODO Auto-generated method stub
		Log.i("MapApps", "doInBackground");
		
		for (LatLng mPoint : aPoints) {
            if (mPoint == null) return null;
            Log.i("MapApps", mPoint.toString());
        }
        return new PathJSONParser(constructURL(aPoints)).parse();
	}
	
	
	protected String constructURL(LatLng... points) {
		Log.e("MapApps", "constructURL");
        LatLng start = points[0];
        LatLng dest = points[1];
        String sJsonURL = "http://maps.googleapis.com/maps/api/directions/json?";
        final StringBuffer mBuf = new StringBuffer(sJsonURL);
        mBuf.append("origin=");
        mBuf.append(start.latitude);
        mBuf.append(',');
        mBuf.append(start.longitude);
        mBuf.append("&destination=");
        mBuf.append(dest.latitude);
        mBuf.append(',');
        mBuf.append(dest.longitude);
        mBuf.append("&sensor=true&mode=");
        mBuf.append(_mTravelMode.getValue());
        Log.e("MapApps", "URL: "+mBuf.toString());
        return mBuf.toString();
    }

    @Override
    protected void onPreExecute() {
        dispatchOnStart();
    }

    @Override
    protected void onPostExecute(Route result) {
    	Log.i("MapApps", "onPostExecute");
        if (result == null) {
        	Log.e("MapApps", "result NULL");
            dispatchOnFailure();
        } else {
        	Log.e("MapApps", "result None NULL");
            //PolylineOptions mOptions = new PolylineOptions();

           // for (LatLng point : result.getPoints()) {
           //     mOptions.add(point);
           // }
            dispatchOnSuccess(null, result);
        }
    }//end onPostExecute method

}
