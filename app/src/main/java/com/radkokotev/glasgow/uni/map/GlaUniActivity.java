package com.radkokotev.glasgow.uni.map;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

public class GlaUniActivity extends Activity {

    DataHolder allBuildings;
    DataHolder matchedBuildings;
    public static Context context;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_screen);
        context = this.getApplicationContext();

        // TODO(radkokotev) redesign the initialization.
        allBuildings = new DataHolder();
        allBuildings.initialize();
        matchedBuildings = new DataHolder();
        
        final EditText id_field = (EditText)findViewById(R.id.nameOfPlace_field);
        id_field.setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN)
                        && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    // If the user pressed ENTER, directly search for the place.
                    onFindButtonClick(findViewById(R.id.findOnMapButton));
                    return true;
                }
                return false;
            }
        });
    }

    /**
     * An on-click handler for the find button. Tries to discover all building relevant to the
     * query based on a substring match.
     */
    public void onFindButtonClick(View view){
        matchedBuildings.clear();  // Remove any results from previous searches.

        EditText queryField = (EditText)findViewById(R.id.nameOfPlace_field);
        String queryText = queryField.getText().toString().toLowerCase();
        
        // Hide the keyboard
        InputMethodManager imm = (InputMethodManager)getSystemService(
                  Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(queryField.getWindowToken(), 0);
        
        TextView noResultsFoundTextView = (TextView)findViewById(R.id.empty);
        noResultsFoundTextView.setVisibility(TextView.INVISIBLE);

        // Loop through the names of all buildings, matching the query text as a substring.
        // TODO(radkokotev) introduce some ranking, to present more relevant results first.
        for (EntryHolder building : allBuildings.getData()) {
            if (building.getName().toLowerCase().contains(queryText) ||
                    building.getId().toLowerCase().contains(queryText)) {
                matchedBuildings.addItem(building);
            }
        }

        PlaceAdapter placeAdapter = new PlaceAdapter(matchedBuildings);
        if(matchedBuildings.size() > 0) {
            final ListView listView = (ListView) findViewById(R.id.listView);
            listView.setChoiceMode(1);
            listView.setAdapter(placeAdapter);
            placeAdapter.notifyDataSetChanged();
        
            listView.setClickable(true);
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                    EntryHolder chosenBuilding = matchedBuildings.get(position);
                    // When a building is selected start the new activity to present the campus map
                    // and pass all needed parameters as extras.
                    Intent myIntent = new Intent(GlaUniActivity.this, WebViewActivity.class);

                    myIntent.putExtra(WebViewActivity.X_COORD_NAME, chosenBuilding.getXCoord());
                    myIntent.putExtra(WebViewActivity.Y_COORD_NAME, chosenBuilding.getYCoord());
                    myIntent.putExtra(WebViewActivity.BUILDING_ID_NAME, chosenBuilding.getId());
                    myIntent.putExtra(WebViewActivity.LATITUDE_NAME, chosenBuilding.getGpsX());
                    myIntent.putExtra(WebViewActivity.LONGITUDE_NAME, chosenBuilding.getGpsY());
                    GlaUniActivity.this.startActivity(myIntent);
                }
            });
        } else {
            // If no buildings matched the query, present a message reporting no results were found.
            placeAdapter.notifyDataSetChanged();
            noResultsFoundTextView.setText(R.string.no_matches_msg_text);
            noResultsFoundTextView.setBackgroundColor(Color.argb(220, 0xff, 0xff, 0xff));
            noResultsFoundTextView.setVisibility(TextView.VISIBLE);
        }
    }
    
    public boolean onCreateOptionsMenu(Menu m) {
        // Shows menu options
        super.onCreateOptionsMenu(m);
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.main, m );
        return true;
    }
    
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        // Check which menu option was selected
        if (item.getItemId() == R.id.menu_help) {
            final Dialog dialog = new Dialog(this);
            dialog.setContentView(R.layout.help_dialog);
            dialog.setTitle(R.string.menu_help);
            dialog.show();
            return true;  // Everything was processed.
        }
        return super.onOptionsItemSelected(item);
    }
}
