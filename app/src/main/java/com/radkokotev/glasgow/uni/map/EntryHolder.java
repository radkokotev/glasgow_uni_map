package com.radkokotev.glasgow.uni.map;

/**
 * A class which encapsulates the data kept per building entry.
 */
public class EntryHolder {

    private String id;   // Building ID on map e.g. 'A12'
    private String name; // Name of building e.g.

    // Coordinate of building position on campus map = ratio measured from top left corner. E.g.
    // along the horizontal axis x_coord = (horizontal distance / total width of campus map).
    private String x_coord;
    private String y_coord;

    // Longitude and latitude of the building. Used when exporting to Google maps.
    private String gps_x;
    private String gps_y;

    public EntryHolder(String id, String name, String x_coord, String y_coord,
                       String gps_x, String gps_y) {
        this.id = id;
        this.name = name;
        this.x_coord = x_coord;
        this.y_coord = y_coord;
        this.gps_x = gps_x;
        this.gps_y = gps_y;
    }

    public String getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public String getXCoord() {
        return x_coord;
    }
    
    public String getYCoord() {
        return y_coord;
    }

    public String getGpsX() {
        return gps_x;
    }

    public String getGpsY() {
        return gps_y;
    }
}
