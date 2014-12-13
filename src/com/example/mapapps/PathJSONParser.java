package com.example.mapapps;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

/**
 * this class would return a route object upon successfully decoding
 * @author congcongchen
 *
 */
public class PathJSONParser extends URL_InputStream_convertor{
	/**
     * Distance covered. *
     */
	private int distance;
	
	//constructor
	public PathJSONParser(String inputURL){
		super(inputURL);//result is that we get a URL object named: feedUrl
		Log.i("MapApps", "PathJSONParser");
	}
	
	
	/**
     * Parses a url pointing to a Google JSON object to a Route object.
     *
     * @return a Route object based on the JSON object by Haseem Saheed
     */
	public Route parse(){
		Log.i("MapApps", "parse()");
		//turn the stream into a string
		final String result = convertStreamToString(this.getInputStream());
		if(result==null){
			Log.e("MapApps", "null result");
			return null;
		}
		
		//create an empty route
		final Route route = new Route();
		//create an empty segment
		final Segment segment = new Segment();
		try{
			//transform the string into a json object
			final JSONObject json = new JSONObject(result);
			//get the route object
			final JSONObject jsonRoute = json.getJSONArray("routes").getJSONObject(0);
			
			final JSONArray jsonLegs = jsonRoute.getJSONArray("legs");
			Log.i("MapApps", "jsonLegs size: "+jsonLegs.length());
			
			//then get the distance information and start location
			final JSONObject leg = (JSONObject) jsonLegs.get(0);
			final JSONArray steps = leg.getJSONArray("steps");
			
			final int numSteps = steps.length();
			Log.i("MapApps", "steps num: "+numSteps);
			
			int total_distance = leg.getJSONObject("distance").getInt("value");
			route.setLength(total_distance);
			Log.i("MapApps", "total distance: "+route.getLength());
			//get the start location
			JSONObject start_locationJSON = leg.getJSONObject("start_location");
			route.addPoint(new LatLng(start_locationJSON.getDouble("lat"),start_locationJSON.getDouble("lng")));
			
			JSONObject step=null;
			for(int i=0;i<numSteps;i++){
				step=(JSONObject) steps.getJSONObject(i);
				JSONObject end_locationJSON = step.getJSONObject("end_location");
				route.addPoint(new LatLng(end_locationJSON.getDouble("lat"),end_locationJSON.getDouble("lng")));
			}
			
			/*
			
			
			//get the leg, only one leg as we don't support waypoints
			
			final JSONObject leg = jsonRoute.getJSONArray("legs").getJSONObject(0);
			//get the steps for this leg
			final JSONArray steps = leg.getJSONArray("steps");
			//Number of steps for use in for loop
			final int numSteps = steps.length();
			//set the name of this route using the start&End addresses
			route.setName(leg.getString("start_address")+" to "+leg.getString("end_address"));
			route.setCopyright(jsonRoute.getString("copyrights"));
			route.setDurationText(leg.getJSONObject("duration").getString("text"));
			route.setDistanceText(leg.getJSONObject("distance").getString("text"));
			route.setEndAddressText(leg.getString("end_adress"));
			//get the total length of the route
			route.setLength(leg.getJSONObject("distance").getInt("value"));
			//get any warning provided
			if(!jsonRoute.getJSONArray("warnings").isNull(0)){
				route.setWarning(jsonRoute.getJSONArray("warnings").getString(0));
			}
			
			 Loop through the steps, creating a segment for each one and
             * decoding any polylines found as we go to add to the route object's
             * map array. Using an explicit for loop because it is faster!
             
			for(int i=0; i<numSteps; i++){
				//get the individual step
				final JSONObject step = steps.getJSONObject(i);
				//Get the start position for this step and set it on the segment
				final JSONObject start = step.getJSONObject("start_location");
				final LatLng position = new LatLng(start.getDouble("lat"),start.getDouble("lng"));
				segment.setPoint(position);
				//set the length of this segment in meters
				final int length = step.getJSONObject("distance").getInt("value");
				distance+=length;
				segment.setLength(length);
				segment.setDistance(distance/1000);//kilometers
				//Strip html from google directions and set as turn instruction
				segment.setInstruction(step.getString("html_instructions").replaceAll("<(.*?)*>", ""));
				//Retrieve & decode this segment's polyline and add it to the route.
				route.addPoints(decodePolyLine(step.getJSONObject("polyline").getString("points")));
				//push a copy of the segment to the route
				route.addSegment(segment.copy());
			}*/
		}catch (JSONException e) {
			
            Log.e("MapApps", "Routing Error"+ e.getMessage());
            return null;
        }
        return route;
	}
	
	
	/**
     * Convert an inputstream to a string.
     *
     * @param input inputstream to convert.
     * @return a String of the inputstream.
     */
	private static String convertStreamToString(final InputStream input){
		Log.i("MapApps", "convertStreamToString");
		if(input==null){
			Log.e("MapApps", "input stream is null");
			return null;
		}
		final BufferedReader reader = new BufferedReader(new InputStreamReader(input));
		final StringBuilder sbuilder = new StringBuilder();
		
		String line=null;
		try{
			while((line=reader.readLine())!=null){
				sbuilder.append(line);
			}
		}catch(IOException e){
			Log.e("MapApps", "Convert an inputstream to a string Error"+ e.getMessage());
			e.printStackTrace();
		}finally{
			//close and clear resource
			try{
				input.close();
			}catch(IOException e){
				Log.e("MapApps", "close inputstream Error"+e.getMessage());
				//Log.e("close inputstream Error", e.getMessage());
				e.printStackTrace();
			}
		}
		String s = sbuilder.toString();
		int length = s.length();
		int num = length/5;
		int startindex=0;
		for(int i=0;i<5;i++){
			if(startindex+(i+1)*num>length)
				Log.i("MapApps", "output: "+s.substring(startindex+i*num,length));
			else
				Log.i("MapApps", "output: "+s.substring(startindex+i*num,startindex+(i+1)*num));
		}
		
		return sbuilder.toString();
	}

	/**
     * Decode a polyline string into a list of GeoPoints.
     *
     * @param poly polyline encoded string to decode.
     * @return the list of GeoPoints represented by this polystring.
     */
	private List<LatLng> decodePolyLine(final String poly) {
        int len = poly.length();
        int index = 0;
        List<LatLng> decoded = new ArrayList<LatLng>();
        int lat = 0;
        int lng = 0;

        while (index < len) {
            int b;
            int shift = 0;
            int result = 0;
            do {
                b = poly.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = poly.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            decoded.add(new LatLng(
                    lat / 100000d, lng / 100000d
            ));
        }

        return decoded;
	}
}