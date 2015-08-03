package com.radkokotev.glasgow.uni.map;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import android.content.Context;
import android.content.res.AssetManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.util.Log;
import android.util.Xml;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * A class whose main responsibility is to keep a list of buildings. A large portion of this class
 * has to do with the initialization of this list. In particular, a default list of buildings is
 * distributed as part of the application 'assets/buildings.xml'. The list of buildings is only read
 * from this file the first time the application is started. A local XML file is then created,
 * which serves as a source for initialisation for every new start of the application. This file is
 * kept fresh by fetching a new version every time the application is started and the user is
 * connected to the Internet.
 */
public class DataHolder {
    private static final String LOG_TAG = "DataHolderLogTag";

    // The various tag names used in 'buildings.xml'.
    private static final String XML_ROOT_TAG = "list";
    private static final String XML_BUILDING_TAG = "building";
    private static final String XML_ID_TAG = "id";
    private static final String XML_NAME_TAG = "name";
    private static final String XML_X_COORD_TAG = "x_coord";
    private static final String XML_Y_COORD_TAG = "y_coord";
    private static final String XML_GPS_X_TAG = "gps_x";
    private static final String XML_GPS_Y_TAG = "gps_y";

    // The name of the file stored in internal storage.
    private static final String BUILDINGS_FILE_NAME = "internal_buildings.xml";

    // Content of fetched file, when null a new attempt to fetch date will be made.
    private static String fetchedBuildingList;

    // The list of all buildings (entries).
    private ArrayList<EntryHolder> data;

    public DataHolder() {
        this.data = new ArrayList<EntryHolder>();
        fetchedBuildingList = null;
    }

    /**
     * Returns a reference to the whole list of entries.
     */
    public synchronized ArrayList<EntryHolder> getData() {
        return data;
    }
    
    public synchronized EntryHolder get(int index) {
        return data.get(index);
    }
    
    public synchronized int size(){
        return data.size(); 
    }
    
    public synchronized void addItem(EntryHolder entry){
        data.add(entry);
    }
    
    public synchronized void clear(){
        data.clear();
    }

    /**
     * A helper method which reads everything from an input stream and created a String. The
     * resulting string is returned.
     */
    private static String readInputStream(InputStream in) {
        Scanner sc = new Scanner(in);
        StringBuilder sb = new StringBuilder();
        while (sc.hasNext()) {
            sb.append(sc.nextLine());
            sb.append('\n');
        }
        return sb.toString();
    }


    /**
     * Writes the String passed as a parameter into a local file kept in internal storage. The file
     * is private to the application.
     */
    private synchronized void exportBuildingsListOnDevice(String contents) throws IOException {
        FileOutputStream fos = GlaUniActivity.context.openFileOutput(
                BUILDINGS_FILE_NAME, Context.MODE_PRIVATE);
        fos.write(contents.getBytes());
        fos.close();
    }

    // Initializes the list of buildings.
    public synchronized void initialize() {
        fetchBuildingList();

        boolean isFileFound = false;
        InputStream in = null;
        try {
            in = GlaUniActivity.context.openFileInput(BUILDINGS_FILE_NAME);
            isFileFound = true;
        } catch (FileNotFoundException e) {
            Log.e(LOG_TAG, e.getMessage());
        }
        if (!isFileFound) {
            AssetManager am = GlaUniActivity.context.getAssets();
            try {
                in = am.open("buildings.xml");
                exportBuildingsListOnDevice(readInputStream(in));
                in.close();
                in = am.open("buildings.xml");
            } catch (IOException e) {
                // This should never happen, as long as 'buildings.xml' exists under 'assets/'
                Log.e(LOG_TAG, "Asset buildings.xml was not found!!!\n" + e.getMessage());
            }
        }

        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in, null);
            parser.nextTag();
            data = new ArrayList<EntryHolder>(readFeed(parser));
        } catch (IOException e) {
            Log.e(LOG_TAG, e.getMessage());
        } catch (XmlPullParserException e) {
            Log.e(LOG_TAG, e.getMessage());
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException e) {
                Log.e(LOG_TAG, e.getMessage());
            }
        }
    }

    // Parses the whole XML file to extract all building entries.
    private synchronized List<EntryHolder> readFeed(XmlPullParser parser)
            throws IOException, XmlPullParserException {
        List<EntryHolder> entries = new ArrayList<EntryHolder>();

        parser.require(XmlPullParser.START_TAG, null, XML_ROOT_TAG);
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            // Starts by looking for the entry tag
            if (name.equals(XML_BUILDING_TAG)) {
                entries.add(readEntry(parser));
            } else {
                skip(parser);
            }
        }
        return entries;
    }

    // Parses a single building entry to create an EntryHolder object and returns it.
    private EntryHolder readEntry(XmlPullParser parser) throws XmlPullParserException, IOException {
        // The current tag must be <building>
        parser.require(XmlPullParser.START_TAG, null, XML_BUILDING_TAG);

        String id = null;
        String name = null;
        String x_coord = null;
        String y_coord = null;
        String gps_x = null;
        String gps_y = null;
        // Consume everything between <building> and </building>
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String tagName = parser.getName();
            if (tagName.equals(XML_ID_TAG)) {                            // <id> ... </id>
                parser.require(XmlPullParser.START_TAG, null, XML_ID_TAG);
                id = readText(parser);
                parser.require(XmlPullParser.END_TAG, null, XML_ID_TAG);
            } else if (tagName.equals(XML_NAME_TAG)) {                   // <name> ... </name>
                parser.require(XmlPullParser.START_TAG, null, XML_NAME_TAG);
                name = readText(parser);
                parser.require(XmlPullParser.END_TAG, null, XML_NAME_TAG);
            } else if (tagName.equals(XML_X_COORD_TAG)) {                // <x_coord> ... </x_coord>
                parser.require(XmlPullParser.START_TAG, null, XML_X_COORD_TAG);
                x_coord = readText(parser);
                parser.require(XmlPullParser.END_TAG, null, XML_X_COORD_TAG);
            } else if (tagName.equals(XML_Y_COORD_TAG)) {                // <y_coord> ... </y_coord>
                parser.require(XmlPullParser.START_TAG, null, XML_Y_COORD_TAG);
                y_coord = readText(parser);
                parser.require(XmlPullParser.END_TAG, null, XML_Y_COORD_TAG);
            } else if (tagName.equals(XML_GPS_X_TAG)) {                  // <gps_x> ... </gps_x>
                parser.require(XmlPullParser.START_TAG, null, XML_GPS_X_TAG);
                gps_x = readText(parser);
                parser.require(XmlPullParser.END_TAG, null, XML_GPS_X_TAG);
            } else if (tagName.equals(XML_GPS_Y_TAG)) {                  // <gps_y> ... </gps_y>
                parser.require(XmlPullParser.START_TAG, null, XML_GPS_Y_TAG);
                gps_y = readText(parser);
                parser.require(XmlPullParser.END_TAG, null, XML_GPS_Y_TAG);
            } else {  // Any other tag will not be consumed!
                skip(parser);
            }
        }
        return new EntryHolder(id, name, x_coord, y_coord, gps_x, gps_y);
    }

    // Consumes all the text before the next tag and returns it in a string.
    private String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
        String result = "";
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.getText();
            parser.nextTag();
        }
        return result;
    }

    // Skips the current tag and everything it contains.
    private void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }

    private void fetchBuildingList() {
        if (fetchedBuildingList != null) {
            // This has already been fetched.
            return;
        }
        // Check Internet connection.
        ConnectivityManager connMgr = (ConnectivityManager)
                GlaUniActivity.context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            // If device is connected to the Internet, fetch the content asynchronously.
            new BuildingListFetcher().execute();
        } else {
            Log.e(LOG_TAG, "No Internet connection");
        }
    }

    /**
     * A class implementing an asynchronous task, which fetches a fresh 'buildings.xml' file from
     * the Internet. In case of failure to fetch the file, the old version of the file on the device
     * is used. In case this succeeds, the old file is replaced and the list of buildings is
     * initialized again. In this way, the list of new buildings is available to the user as soon as
     * it is parsed.
     */
    private class BuildingListFetcher extends AsyncTask<Void, Void, String> {
        // The URL where the newest version of 'buildings.xml' resides.
        private static final String BUILDINGS_LIST_URL =
                "https://raw.githubusercontent.com/radkokotev/glasgow_uni_map/master" +
                "/app/src/main/assets/buildings.xml";

        @Override
        protected String doInBackground(Void... params) {
            try {
                return downloadUrl(BUILDINGS_LIST_URL);
            } catch (IOException e) {
                Log.e(LOG_TAG, "Unable to fetch XML file " + e.getMessage());
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            if (result == null) {
                // There was a problem with accessing the file. The existing file should not be
                // replaced, as it contains good data. Refresh might succeed some other time.
                return;
            }
            try {
                // Export newly fetched file to internal storage.
                exportBuildingsListOnDevice(result);
                // Reinitialize the list of buildings with the newly fetched data.
                initialize();
                Toast.makeText(GlaUniActivity.context, R.string.buildings_updated_toast,
                        Toast.LENGTH_LONG).show();
            } catch(IOException e) {
                Log.e(LOG_TAG, "Unable to export downloaded file on device " + e.getMessage());
            }
        }

        /**
         * A helper method which tries to establish a connection and download a fresh version of the
         * file. If host returns HTTP_OK (i.e. response is 200), the fresh content is returned.
         */
        private String downloadUrl(String myurl) throws IOException {
            InputStream in = null;
            try {
                URL url = new URL(myurl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout(10000);     // 10 seconds
                conn.setConnectTimeout(15000);  // 15 seconds
                conn.setRequestMethod("GET");
                conn.setDoInput(true);
                conn.connect();
                int response = conn.getResponseCode();
                Log.e(LOG_TAG, "The response is: " + response);
                if (response == HttpURLConnection.HTTP_OK) {
                    in = conn.getInputStream();
                    // Convert the InputStream into a string
                    fetchedBuildingList = readInputStream(in);
                    return fetchedBuildingList;
                }
                fetchedBuildingList = "";  // Do not try to fetch that again for now.
                return null;  // Return null to ensure the existing buildings file is not replaced.
            } finally {
                // Makes sure that the InputStream is closed after the app is finished using it.
                if (in != null) {
                    in.close();
                }
            }
        }
    }
}
