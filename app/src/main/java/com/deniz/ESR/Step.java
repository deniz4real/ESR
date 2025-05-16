package com.deniz.ESR;

import com.google.android.gms.maps.model.LatLng;

public class Step {
    public String instructions;
    public LatLng startLocation;
    public LatLng endLocation;

    public Step(String instructions, LatLng startLocation, LatLng endLocation) {
        this.instructions = instructions;
        this.startLocation = startLocation;
        this.endLocation = endLocation;
    }
}
