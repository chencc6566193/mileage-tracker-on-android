package com.example.mapapps;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;

import com.google.android.gms.maps.model.LatLng;

/**
* A subclass of AsyncTask that calls getFromLocation() in the
* background. The class definition has these generic types:
* Location - A Location object containing
* the current location.
* Void     - indicates that progress units are not used
* String   - An address passed to onPostExecute()
*/
public class latlngToAddress{

	String TAG = "MapApps";
	Context mContext;
	
	//constructor
	public latlngToAddress(Context context){
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
	
	public String getAddress(LatLng... params) {
		Log.i(TAG, "getAddress "+" in latlngToAddress class");
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
            return ""+addressText;
        }else
        	return "No Address Found";
	}
}