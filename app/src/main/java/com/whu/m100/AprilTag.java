package com.whu.m100;

public class AprilTag {

    private String id;

    private String latitude;

    private String longitude;

    public AprilTag(String id, String latitude, String longitude) {
        this.id = id;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getId() {
        return id;
    }

    public String getLatitude() {
        return latitude;
    }

    public String getLongitude() {
        return longitude;
    }

}
