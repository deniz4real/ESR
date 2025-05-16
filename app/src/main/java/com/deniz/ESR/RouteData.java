package com.deniz.ESR;

import com.google.android.gms.maps.model.LatLng;

import java.util.List;

public class RouteData {
    private String routeName;
    private LatLng startLatLng;
    private LatLng endLatLng;
    private List<LatLng> polyline;
    private String wheelchairType;
    private double maxSlope;
    private double[] elevations;
    private String startAddress;
    private String endAddress;

    public RouteData(String routeName, LatLng startLatLng, LatLng endLatLng, List<LatLng> polyline,
                     String wheelchairType, double maxSlope, double[] elevations) {
        this.routeName = routeName;
        this.startLatLng = startLatLng;
        this.endLatLng = endLatLng;
        this.polyline = polyline;
        this.wheelchairType = wheelchairType;
        this.maxSlope = maxSlope;
        this.elevations = elevations;
    }


    public String getStartAddress() {
        return startAddress;
    }

    public void setStartAddress(String startAddress) {
        this.startAddress = startAddress;
    }

    public String getEndAddress() {
        return endAddress;
    }

    public void setEndAddress(String endAddress) {
        this.endAddress = endAddress;
    }


    public LatLng getStartLatLng() {
        return startLatLng;
    }


    public LatLng getEndLatLng() {
        return endLatLng;
    }


    public String getWheelchairType() {
        return wheelchairType;
    }


}
