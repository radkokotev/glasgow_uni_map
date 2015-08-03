package com.radkokotev.glasgow.uni.map;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

/**
 * An activity which hosts a web view and loads the campus map image. Places a red circle at the
 * specified coordinates. Web view is used instead of a regular image view, in order to allow easy
 * zooming and scrolling.
 */
public class WebViewActivity extends Activity {
    // String constants used as keys to pass information to the current activity.
    public static final String X_COORD_NAME = "x_coord";
    public static final String Y_COORD_NAME = "y_coord";
    public static final String BUILDING_ID_NAME = "building_id";
    public static final String LATITUDE_NAME = "latitude";
    public static final String LONGITUDE_NAME = "longitude";

    // Dimensions of campus map in pixels
    private static final int MAP_HEIGHT = 3503;
    private static final int MAP_WIDTH = 2905;

    private static final String MAP_IMAGE_NAME = "map.png";
    private static final String RED_CIRCLE_IMAGE_NAME = "circle_big.png";

    private static int scrollToX;
    private static int scrollToY;
    private String latitude;
    private String longitude;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.web_view_layout);

        Intent thisIntent = getIntent();
        
        float x = Float.parseFloat(thisIntent.getStringExtra(X_COORD_NAME));
        float y = Float.parseFloat(thisIntent.getStringExtra(Y_COORD_NAME));
        String buildingID = thisIntent.getStringExtra(BUILDING_ID_NAME);
        latitude = thisIntent.getStringExtra(LATITUDE_NAME);
        longitude = thisIntent.getStringExtra(LONGITUDE_NAME);
        
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        int screenWidth = dm.widthPixels;
        int screenHeight = dm.heightPixels;

        scrollToX =(int)(x * MAP_WIDTH);
        scrollToY =(int)(y * MAP_HEIGHT);

        // A JavaScript function to scroll the window to a given position.
        String script = "<script> function Scroll(x,y) { window.scrollTo(x, y); } </script>";
        String htmlCode = "<html> <head> " +
                "<meta name='viewport' content='width=device-width,target-densityDpi=device-dpi'>" +
                script + " </head> <body> " +
                "<div style='position: relative; left: 0; top: 0;'>    " +
                // Place the campus map image
                "<img src='" + MAP_IMAGE_NAME + "' onLoad = 'Scroll(" +
                    (int)(Math.max(0, x * MAP_WIDTH - screenWidth / 2.0)) + "," +
                    (int)(Math.max(0, y * MAP_HEIGHT - screenHeight / 2.0))+ ")'  " +
                "style='position: relative; top: 0; left: 0;'/> " +
                // Position the red circle image on top of the map to point to the correct location.
                "<img id = 'circleImg'  src='" + RED_CIRCLE_IMAGE_NAME +
                    "' style='position: absolute; top: " +
                    (int)(y * MAP_HEIGHT) + "; left: " + (int)(x * MAP_WIDTH) + ";'/> " +
                "</div> </body> </html>";
        
        WebView webView = (WebView) findViewById(R.id.webView1);
        // Enable zooming of the web view.
        try {
            webView.getSettings().setBuiltInZoomControls(true);
            webView.getSettings().setSupportZoom(true);
        } catch (Exception e){
            Log.e("ZoomControllerErr", e.getMessage());
        } catch (Error e){
            Log.e("ZoomControllerErr", e.getMessage());
        }
        webView.getSettings().setUseWideViewPort(true);
        webView.setWebViewClient(new WebViewClient() {
               public void onPageFinished(WebView view, String url) {
                   // Try to scroll to the desired position.
                   view.scrollTo(WebViewActivity.scrollToX, WebViewActivity.scrollToY);
               }
        });
        // TODO(radkokotev) find a better solution.
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDefaultZoom(WebSettings.ZoomDensity.FAR);
        
        webView.loadDataWithBaseURL("file:///android_asset/", htmlCode, "text/html", "utf-8", null);
        
        Toast.makeText(this, "Building " + buildingID, Toast.LENGTH_LONG).show();
        
    }

    @Override
    public boolean onCreateOptionsMenu(Menu m) {
        super.onCreateOptionsMenu(m);
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menulayout, m );
        return true;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        if (item.getItemId() == R.id.gmaps) { // Show in Google maps
            String mapsURI = "geo:" + latitude + "," + longitude +
                    "?q=" + latitude + "," + longitude + "&z=20";
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(mapsURI));
            startActivity(intent);
            return true;
        } else if (item.getItemId() == R.id.menu_help) {  // Display help dialog
            final Dialog dialog = new Dialog(this);
            dialog.setContentView(R.layout.help_dialog);
            dialog.setTitle(R.string.menu_help);
            dialog.show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
