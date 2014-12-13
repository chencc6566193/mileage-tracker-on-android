package com.example.mapapps;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import android.util.Log;

/**
 * create URL object from URL String and then set up connection, and then return inputstream
 * @author congcongchen
 *
 */
public class URL_InputStream_convertor {
	// names of the XML tags
    protected static final String MARKERS = "markers";
    protected static final String MARKER = "marker";

    protected URL feedUrl;

    /*
     * create URL object from URL String
     */
    protected URL_InputStream_convertor(final String feedUrl) {
    	Log.i("MapApps", "URL_InputStream_convertor");
        try {
            this.feedUrl = new URL(feedUrl);
        } catch (MalformedURLException e) {
            Log.e("MapApps","Turing url string to URL object, Error @URL_InputStream_convertor Class"+ e.getMessage());
            e.printStackTrace();
        }
    }

    /*
     * set up connection, and then return inputstream
     */
    protected InputStream getInputStream() {
        try {
            return feedUrl.openConnection().getInputStream();
        } catch (IOException e) {
            Log.e("MapApps","getInputStream Error"+ e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}
