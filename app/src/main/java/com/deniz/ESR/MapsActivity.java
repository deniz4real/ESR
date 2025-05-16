package com.deniz.ESR;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.Manifest;

import android.view.View;

import android.widget.Button;
import android.widget.EditText;
import android.text.TextUtils;
import android.widget.LinearLayout;

import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.PhotoMetadata;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.FetchPhotoRequest;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;

import com.google.gson.Gson;
import com.google.maps.android.PolyUtil;
import com.google.maps.android.SphericalUtil;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import android.location.Address;
import android.location.Geocoder;


public class MapsActivity extends DrawerActivity implements OnMapReadyCallback {


    public GoogleMap mMap;

    public ElevationDatabaseHelper ElevationdbHelper;

    private EditText startLocationEditText;
    private EditText endLocationEditText;

    private RadioGroup wheelchairTypeRadioGroup;
    private RadioButton electricWheelchairRadioButton;
    private String wheelchairType;

    private Button clearButton;
    private Button endButton;
    private Button showAllDirectionsButton;
    private Button addToFavoritesButton;


    private List<Marker> markers = new ArrayList<>();
    private List<Polyline> polylines = new ArrayList<>();
    private List<LatLng> routePoints;
    private List<Step> stepsList = new ArrayList<>();

    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;

    private LatLng userCurrentLocation;
    private LatLng startLatLng;
    private LatLng endLatLng;

    private int currentStepIndex = 0;
    private boolean isInfoPanelVisible = false;
    private boolean requiresCompanion = false;
    private boolean isRouteSuitable = true;
    private double highestSlope = 0.0;


    private static final int AUTOCOMPLETE_REQUEST_CODE_START = 1;
    private static final int AUTOCOMPLETE_REQUEST_CODE_END = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        setTitle("Engelsiz Seyahat");

        setupDrawer();

        Button currentLocationButton = findViewById(R.id.current_location_button);

        currentLocationButton.setOnClickListener(v -> {
            if (startLocationEditText.isFocused()) {

                getCurrentLocationAndSetToEditText(startLocationEditText);
            } else if (endLocationEditText.isFocused()) {

                getCurrentLocationAndSetToEditText(endLocationEditText);
            } else {
                Toast.makeText(this, "Lütfen bir alanı seçin (Başlangıç veya Varış).", Toast.LENGTH_SHORT).show();
            }
        });
        SharedPreferences sharedPreferences = getSharedPreferences("Favorites", MODE_PRIVATE);


        Set<String> favorites = sharedPreferences.getStringSet("Routes", new HashSet<>());
        List<String> favoritesList = new ArrayList<>(favorites);

        startLocationEditText = findViewById(R.id.start_location);
        endLocationEditText = findViewById(R.id.end_location);

        if (startLocationEditText == null) {
            Log.e("ERROR", "startLocationEditText bulunamadı!");
        }


        if (getIntent().hasExtra("startLatLng") && getIntent().hasExtra("endLatLng")) {
            LatLng startLatLng = new Gson().fromJson(getIntent().getStringExtra("startLatLng"), LatLng.class);
            LatLng endLatLng = new Gson().fromJson(getIntent().getStringExtra("endLatLng"), LatLng.class);

            startLocationEditText.setText(getAddressFromLatLng(startLatLng));
            endLocationEditText.setText(getAddressFromLatLng(endLatLng));

            planRoute();
        }


        if (getIntent().hasExtra("selectedRoute")) {
            String routeJson = getIntent().getStringExtra("selectedRoute");
            RouteData routeData = new Gson().fromJson(routeJson, RouteData.class);

            if (routeData != null) {
                startLatLng = routeData.getStartLatLng();
                endLatLng = routeData.getEndLatLng();


                if (startLatLng == null || endLatLng == null) {
                    Toast.makeText(this, "Rota bilgisi eksik. Başlangıç veya varış noktası kaydedilmemiş.", Toast.LENGTH_SHORT).show();
                    return;
                }


                startLocationEditText.setText(getAddressFromLatLng(startLatLng));
                endLocationEditText.setText(getAddressFromLatLng(endLatLng));
                wheelchairType = routeData.getWheelchairType();
            } else {
                Toast.makeText(this, "Rota bilgisi alınamadı.", Toast.LENGTH_SHORT).show();
            }
        }


        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        } else {
            Log.e("MapsActivity", "Map Fragmenti yok");
        }

        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), getString(R.string.google_maps_key));
        }

        startLocationEditText.setOnClickListener(v -> showCurrentLocationDialog());

        startLocationEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                List<Place.Field> fields = Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG);

                Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields)
                        .build(MapsActivity.this);

                startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE_START);
            }
        });

        endLocationEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                List<Place.Field> fields = Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG);

                Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields)
                        .build(MapsActivity.this);

                startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE_END);
            }
        });


        clearButton = findViewById(R.id.clear_button);
        endButton = findViewById(R.id.end_button);
        showAllDirectionsButton = findViewById(R.id.show_all_directions_button);

        clearButton.setVisibility(View.GONE);

        clearButton.setOnClickListener(v -> {
            clearAll();
            clearMap();
            resetRoute();
            clearButton.setVisibility(View.GONE);
        });

        addToFavoritesButton = findViewById(R.id.add_to_favorites_button);
        if (addToFavoritesButton == null) {
            Log.e("MapsActivity", "addToFavoritesButton bulunamadı. Lütfen layout dosyasını kontrol edin.");
        } else {
            addToFavoritesButton.setVisibility(View.GONE);
        }

        Button planRouteButton = findViewById(R.id.plan_route_button);
        planRouteButton.setOnClickListener(v -> {
            planRoute();
            Button infoButton = findViewById(R.id.info_button);
            infoButton.setVisibility(View.VISIBLE);
            addToFavoritesButton.setVisibility(View.VISIBLE);
            currentLocationButton.setVisibility(View.GONE);

        });


        planRouteButton = findViewById(R.id.plan_route_button);
        wheelchairTypeRadioGroup = findViewById(R.id.wheelchair_type_radio_group);
        electricWheelchairRadioButton = findViewById(R.id.electric_wheelchair);

        planRouteButton.setOnClickListener(v -> planRoute());

        endButton.setOnClickListener(v -> {


            TextView stepInstructionTextView = findViewById(R.id.step_instruction_text_view);
            stepInstructionTextView.setVisibility(View.GONE);

            TextView highestSlopeInfoTextView = findViewById(R.id.highest_slope_info);
            highestSlopeInfoTextView.setVisibility(View.GONE);

            TextView directionsTextView = findViewById(R.id.directions_text_view);
            directionsTextView.setVisibility(View.GONE);

            LinearLayout controlsContainer = findViewById(R.id.controls_container);
            controlsContainer.setVisibility(View.VISIBLE);

            showAllDirectionsButton.setVisibility(View.GONE);
            updateInfoPanelVisibility(false);


            assert addToFavoritesButton != null;
            addToFavoritesButton.setVisibility(View.GONE);


            endButton.setVisibility(View.GONE);
            currentLocationButton.setVisibility(View.VISIBLE);

            clearButton.setVisibility(View.VISIBLE);
        });


        setupInfoButton();

        showAllDirectionsButton = findViewById(R.id.show_all_directions_button);
        TextView directionsTextView = findViewById(R.id.directions_text_view);


        showAllDirectionsButton.setVisibility(View.GONE);
        directionsTextView.setVisibility(View.GONE);

        Button infoButton = findViewById(R.id.info_button);
        infoButton.setVisibility(View.GONE);

        showAllDirectionsButton.setOnClickListener(v -> {

            if (directionsTextView.getVisibility() == View.GONE) {
                directionsTextView.setVisibility(View.VISIBLE);

                showAllDirectionsButton.setText("Yol Tariflerini Gizle");
            } else {
                directionsTextView.setVisibility(View.GONE);
                showAllDirectionsButton.setText("Yol Tariflerini Göster");
            }
        });


        ElevationdbHelper = new ElevationDatabaseHelper(this);
        wheelchairTypeRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.manual_wheelchair) {
                wheelchairType = "manual";
            } else if (checkedId == R.id.electric_wheelchair) {
                wheelchairType = "electric";
            }
        });


        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(5000)
                .setFastestInterval(2000);


        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult == null || locationResult.getLocations().isEmpty()) {
                    return;
                }

                Location location = locationResult.getLastLocation();
                userCurrentLocation = new LatLng(location.getLatitude(), location.getLongitude());


                if (stepsList != null && !stepsList.isEmpty() && currentStepIndex < stepsList.size()) {
                    Step currentStep = stepsList.get(currentStepIndex);
                    float[] distance = new float[1];
                    Location.distanceBetween(
                            userCurrentLocation.latitude, userCurrentLocation.longitude,
                            currentStep.endLocation.latitude, currentStep.endLocation.longitude,
                            distance
                    );


                    if (distance[0] < 20) {
                        currentStepIndex++;
                        if (currentStepIndex < stepsList.size()) {

                            showStepInstruction(stepsList.get(currentStepIndex).instructions);
                        } else {

                            Toast.makeText(MapsActivity.this, "Rota tamamlandı!", Toast.LENGTH_SHORT).show();
                        }
                    }
                }

                updateUserLocationOnMap();
            }
        };
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mMap.getUiSettings().setZoomControlsEnabled(true);

        mMap.setOnPoiClickListener(poi -> {

            fetchPlaceDetails(poi.placeId);


        });


        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }


        LatLngBounds sisliBounds = new LatLngBounds(
                new LatLng(41.0386, 28.9730),
                new LatLng(41.0757, 28.9984)
        );
        mMap.setLatLngBoundsForCameraTarget(sisliBounds);

        LatLng sisliCenter = new LatLng(41.0600, 28.9850);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(sisliCenter, 13));
        mMap.setMinZoomPreference(14.0f);

        mMap.setOnMapClickListener(latLng -> {
            if (startLocationEditText.isFocused()) {
                clearSpecificMarker("Başlangıç");
                String address = getAddressFromLatLng(latLng);
                startLocationEditText.setText(address);
                Marker startMarker = mMap.addMarker(new MarkerOptions().position(latLng).title("Başlangıç"));
                markers.add(startMarker);
            } else if (endLocationEditText.isFocused()) {
                clearSpecificMarker("Varış");
                String address = getAddressFromLatLng(latLng);
                endLocationEditText.setText(address);
                Marker endMarker = mMap.addMarker(new MarkerOptions().position(latLng).title("Varış"));
                markers.add(endMarker);
            }
            Button clearButton = findViewById(R.id.clear_button);
            clearButton.setOnClickListener(v -> clearAll());

        });


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        if (requestCode == AUTOCOMPLETE_REQUEST_CODE_START || requestCode == AUTOCOMPLETE_REQUEST_CODE_END) {
            if (resultCode == RESULT_OK && data != null) {
                Place place = Autocomplete.getPlaceFromIntent(data);

                if (place.getLatLng() != null) {
                    if (requestCode == AUTOCOMPLETE_REQUEST_CODE_START) {
                        startLatLng = place.getLatLng();
                        startLocationEditText.setText(place.getName());
                    } else if (requestCode == AUTOCOMPLETE_REQUEST_CODE_END) {
                        endLatLng = place.getLatLng();
                        endLocationEditText.setText(place.getName());
                    }
                } else {
                    Toast.makeText(this, "Mekan bilgisi eksik.", Toast.LENGTH_SHORT).show();
                }
            } else if (resultCode == AutocompleteActivity.RESULT_ERROR) {
                Status status = Autocomplete.getStatusFromIntent(data);
                Log.e("PlaceAutocomplete", "Hata: " + status.getStatusMessage());
            }
        }
    }


    private String getAddressFromLatLng(LatLng latLng) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                return address.getAddressLine(0);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "Adres Bulunamadı";
    }

    private void planRoute() {

        if (wheelchairType == null || wheelchairType.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle("Eksik Bilgi")
                    .setMessage("Lütfen tekerlekli sandalye türünü seçiniz.")
                    .setPositiveButton("Tamam", null)
                    .show();
            return;
        }

        wheelchairTypeRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.manual_wheelchair) {
                wheelchairType = "manual";
            } else if (checkedId == R.id.electric_wheelchair) {
                wheelchairType = "electric";
            } else {
                wheelchairType = null;
            }
        });


        clearMap();
        ProgressBar progressBar = findViewById(R.id.progress_bar);
        progressBar.setVisibility(View.VISIBLE);

        String startLocation = startLocationEditText.getText().toString().trim();
        String endLocation = endLocationEditText.getText().toString().trim();

        if (TextUtils.isEmpty(startLocation) || TextUtils.isEmpty(endLocation)) {
            Toast.makeText(this, "Başlangıç ve varış noktalarını girin.", Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
            return;
        }

        if (startLatLng == null) {
            startLatLng = getLatLngFromAddress(startLocation);
        }
        if (endLatLng == null) {
            endLatLng = getLatLngFromAddress(endLocation);
        }

        if (startLatLng == null || endLatLng == null) {
            Toast.makeText(this, "Geçerli bir başlangıç veya varış konumu girin.", Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
            return;
        }


        Marker startMarker = mMap.addMarker(new MarkerOptions().position(startLatLng).title("Başlangıç"));
        markers.add(startMarker);

        Marker endMarker = mMap.addMarker(new MarkerOptions().position(endLatLng).title("Varış"));
        markers.add(endMarker);


        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(startLatLng, 19));


        LinearLayout controlsContainer = findViewById(R.id.controls_container);
        controlsContainer.setVisibility(View.GONE);


        TextView stepInstructionTextView = findViewById(R.id.step_instruction_text_view);
        stepInstructionTextView.setVisibility(View.VISIBLE);

        Button infoButton = findViewById(R.id.info_button);
        infoButton.setVisibility(View.VISIBLE);

        TextView directionsTextView = findViewById(R.id.directions_text_view);
        directionsTextView.setVisibility(View.GONE);


        Button currentLocationButton = findViewById(R.id.current_location_button);
        currentLocationButton.setVisibility(View.GONE);

        TextView infoTextView = findViewById(R.id.info_text_view);
        infoTextView.setVisibility(View.VISIBLE);


        Button showAllDirectionsButton = findViewById(R.id.show_all_directions_button);
        showAllDirectionsButton.setVisibility(View.VISIBLE);

        addToFavoritesButton.setVisibility(View.VISIBLE);
        addToFavoritesButton.setBackgroundTintList(ColorStateList.valueOf(Color.YELLOW));
        addToFavoritesButton.setTextColor(Color.BLACK);


        List<LatLng> routePoints = new ArrayList<>();


        double[] elevations = new double[routePoints.size()];
        double maxSlope = wheelchairType.equals("manual") ? 5.0 : 10.0;
        Button addToFavoritesButton = findViewById(R.id.add_to_favorites_button);

        if (addToFavoritesButton != null) {
            addToFavoritesButton.setVisibility(View.VISIBLE);

            addToFavoritesButton.setTextColor(Color.BLACK);
            addToFavoritesButton.setOnClickListener(v -> {
                Toast.makeText(this, "Favorilere eklendi!", Toast.LENGTH_SHORT).show();

                saveRouteToFavorites(
                        startLatLng,
                        endLatLng,
                        routePoints,
                        wheelchairType,
                        maxSlope,
                        elevations
                );

                saveToFavorites(startLatLng, endLatLng);
            });
        } else {
            Log.e("MapsActivity", "addToFavoritesButton null. Lütfen layout dosyasını kontrol edin.");
        }


        Button endButton = findViewById(R.id.end_button);

        if (endButton != null) {
            endButton.setBackgroundTintList(ColorStateList.valueOf(Color.RED));
            endButton.setTextColor(Color.WHITE);
        }


        endButton.setVisibility(View.VISIBLE);
        endButton.setOnClickListener(v -> {

            clearAll();
            controlsContainer.setVisibility(View.VISIBLE);
            directionsTextView.setVisibility(View.GONE);
            showAllDirectionsButton.setVisibility(View.GONE);
            TextView highestSlopeInfoTextView = findViewById(R.id.highest_slope_info);
            highestSlopeInfoTextView.setVisibility(View.GONE);

            infoTextView.setVisibility(View.GONE);
            infoButton.setVisibility(View.GONE);
            endButton.setVisibility(View.GONE);
            currentLocationButton.setVisibility(View.VISIBLE);
        });

        fetchRouteFromAPI(startLatLng, endLatLng, wheelchairType, progressBar);
    }


    private LatLng getLatLngFromAddress(String address) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocationName(address, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address location = addresses.get(0);
                return new LatLng(location.getLatitude(), location.getLongitude());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void fetchRouteFromAPI(LatLng startLatLng, LatLng endLatLng, String wheelchairType, ProgressBar progressBar) {
        String apiKey = getString(R.string.google_maps_key);
        String url = "https://maps.googleapis.com/maps/api/directions/json?origin=" + startLatLng.latitude + "," + startLatLng.longitude +
                "&destination=" + endLatLng.latitude + "," + endLatLng.longitude + "&key=" + apiKey + "&language=tr";

        progressBar.setVisibility(View.VISIBLE);
        List<LatLng> routePoints = new ArrayList<>();
        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder().url(url).build();
                Response response = client.newCall(request).execute();

                if (response.body() != null) {
                    String responseData = response.body().string();
                    JSONObject json = new JSONObject(responseData);
                    JSONArray routes = json.getJSONArray("routes");

                    if (routes.length() > 0) {
                        JSONObject route = routes.getJSONObject(0);
                        JSONObject overviewPolyline = route.getJSONObject("overview_polyline");
                        JSONArray legs = route.getJSONArray("legs");
                        routePoints.addAll(PolyUtil.decode(overviewPolyline.getString("points")));


                        List<String> directionsList = new ArrayList<>();
                        List<Step> stepsList = new ArrayList<>();

                        if (legs.length() > 0 && legs.getJSONObject(0).has("steps")) {
                            directionsList.clear();
                            JSONArray steps = legs.getJSONObject(0).getJSONArray("steps");
                            for (int i = 0; i < steps.length(); i++) {
                                JSONObject step = steps.getJSONObject(i);
                                String htmlInstructions = step.getString("html_instructions").replaceAll("<[^>]*>", "");
                                LatLng startLoc = new LatLng(step.getJSONObject("start_location").getDouble("lat"),
                                        step.getJSONObject("start_location").getDouble("lng"));
                                LatLng endLoc = new LatLng(step.getJSONObject("end_location").getDouble("lat"),
                                        step.getJSONObject("end_location").getDouble("lng"));
                                directionsList.add(htmlInstructions);
                                stepsList.add(new Step(htmlInstructions, startLoc, endLoc));
                            }
                        }

                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            updateUIAfterRouteCalculation(directionsList, routePoints, stepsList);

                            TextView directionsTextView = findViewById(R.id.directions_text_view);

                            if (!directionsList.isEmpty()) {
                                directionsTextView.setText(TextUtils.join("\n\n", directionsList));
                            } else {
                                directionsTextView.setText("Yön tarifi bulunamadı.");
                            }
                            directionsTextView.setVisibility(View.GONE);

                            checkElevationSuitability(routePoints, wheelchairType, progressBar, elevations -> {
                                drawRouteWithColors(PolyUtil.encode(routePoints), elevations, wheelchairType, 6.0);
                            });

                        });
                    } else {
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(MapsActivity.this, "Rota bulunamadı.", Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();

                runOnUiThread(() -> {
                    if (mMap != null && !routePoints.isEmpty()) {
                        Log.d("RouteProcessing", "Rota başarıyla çizildi.");
                    } else {
                        Toast.makeText(MapsActivity.this, "Rota işlenirken hata oluştu.", Toast.LENGTH_SHORT).show();
                    }
                });
            }

        }).start();
    }


    private void checkElevationSuitability(
            List<LatLng> routePoints,
            String wheelchairType,
            ProgressBar progressBar,
            ElevationCallback callback) {

        highestSlope = 0.0;

        List<LatLng> missingPoints = new ArrayList<>();
        double[] elevations = new double[routePoints.size()];

        for (int i = 0; i < routePoints.size(); i++) {
            LatLng point = routePoints.get(i);
            Double elevation = ElevationdbHelper.getElevation(point.latitude, point.longitude);

            if (elevation != null) {
                elevations[i] = elevation;
            } else {
                missingPoints.add(point);
            }
        }

        if (!missingPoints.isEmpty()) {
            ElevationdbHelper.fetchElevationFromAPI(this, missingPoints);
        }

        runOnUiThread(() -> {
            for (int i = 0; i < routePoints.size(); i++) {
                if (elevations[i] == 0) {
                    LatLng point = routePoints.get(i);
                    Double elevation = ElevationdbHelper.getElevation(point.latitude, point.longitude);
                    if (elevation != null) {
                        elevations[i] = elevation;
                    }
                }
            }

            calculateSlopeAndUpdateUI(routePoints, wheelchairType, elevations, callback, progressBar);
        });

        String locations = routePoints.stream()
                .map(latLng -> latLng.latitude + "," + latLng.longitude)
                .collect(Collectors.joining("|"));

        String url = "https://maps.googleapis.com/maps/api/elevation/json?locations=" + locations + "&key=" + getString(R.string.google_maps_key);

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(MapsActivity.this, "Eğim bilgisi alınırken hata oluştu.", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseData = response.body().string();
                        JSONObject json = new JSONObject(responseData);
                        JSONArray results = json.getJSONArray("results");


                        double[] elevations = new double[results.length()];
                        for (int i = 0; i < results.length(); i++) {
                            elevations[i] = results.getJSONObject(i).getDouble("elevation");
                        }

                        final double[] highestSlopeHolder = {0.0};


                        for (int i = 0; i < elevations.length - 1; i++) {

                            double elevationDifference = elevations[i + 1] - elevations[i];
                            double distance = SphericalUtil.computeDistanceBetween(routePoints.get(i), routePoints.get(i + 1));
                            double slope = (elevationDifference / distance) * 100;

                            if (slope > highestSlopeHolder[0]) {
                                highestSlopeHolder[0] = slope;
                            }
                            requiresCompanion = false;

                            if (wheelchairType.equals("manual")) {
                                if (slope > 10) {
                                    requiresCompanion = true;
                                    isRouteSuitable = false;
                                } else if (slope > 5) {
                                    requiresCompanion = true;

                                }
                            } else if (wheelchairType.equals("electric")) {
                                if (slope > 16) {
                                    requiresCompanion = true;
                                    isRouteSuitable = false;
                                } else if (slope > 10) {
                                    requiresCompanion = true;
                                }
                            }

                        }


                        highestSlope = highestSlopeHolder[0];


                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            updateHighestSlopeInfo(highestSlope);
                            callback.onElevationCalculated(elevations);
                        });


                    } catch (Exception e) {
                        Log.e("ElevationAPI", "Yanıt işlenirken hata: " + e.getMessage());
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);

                        });
                    }
                } else {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(MapsActivity.this, "Geçerli bir rota bulunamadı.", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }


    private void drawRouteWithColors(String encodedPoints, double[] elevations, String wheelchairType, double maxSlope) {
        List<LatLng> routePoints = PolyUtil.decode(encodedPoints);
        double highestSlope = 0.0;


        for (int i = 0; i < routePoints.size() - 1; i++) {
            LatLng point1 = routePoints.get(i);
            LatLng point2 = routePoints.get(i + 1);

            double elevationDifference = elevations[i + 1] - elevations[i];
            double distance = SphericalUtil.computeDistanceBetween(point1, point2);

            if (distance == 0) {
                Log.e("SlopeError", "Distance is zero for segment: " + i);
                continue;
            }

            double slope = (elevationDifference / distance) * 100;

            if (slope > highestSlope) {
                highestSlope = slope;
            }


            int color = Color.GRAY;
            if (wheelchairType.equals("manual")) {
                if (slope <= 5) {
                    color = Color.GREEN;
                } else if (slope <= 10) {
                    color = Color.rgb(255, 165, 0); // Turuncu

                } else {
                    color = Color.RED;

                }
            } else if (wheelchairType.equals("electric")) {
                if (slope <= 10) {
                    color = Color.GREEN;
                } else if (slope <= 16) {
                    color = Color.rgb(255, 165, 0); // Turuncu

                } else {
                    color = Color.RED;

                }
            }


            PolylineOptions polylineSegment = new PolylineOptions()
                    .add(point1, point2)
                    .color(color)
                    .width(10);
            polylines.add(mMap.addPolyline(polylineSegment));
        }


        if (highestSlope > this.highestSlope) {
            this.highestSlope = highestSlope;
        }

        updateHighestSlopeInfo(this.highestSlope);
    }


    private void updateHighestSlopeInfo(double highestSlope) {
        TextView highestSlopeInfo = findViewById(R.id.highest_slope_info);
        if (highestSlope > 0) {
            highestSlopeInfo.setVisibility(View.VISIBLE);
            String slopeMessage = String.format(Locale.getDefault(), "Maksimum Eğim: %.2f%%\n", highestSlope);

            String suitabilityText = isRouteSuitable ? "uygun." : "UYGUN DEĞİL!";
            int suitabilityColor = isRouteSuitable ? Color.GREEN : Color.RED;


            SpannableString coloredSuitabilityText = new SpannableString(suitabilityText);
            coloredSuitabilityText.setSpan(
                    new ForegroundColorSpan(suitabilityColor),
                    0,
                    suitabilityText.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            );


            SpannableStringBuilder combinedMessage = new SpannableStringBuilder(slopeMessage);
            combinedMessage.append("Rota seyahat için ");
            combinedMessage.append(coloredSuitabilityText);


            highestSlopeInfo.setText(combinedMessage);
            highestSlopeInfo.setVisibility(View.VISIBLE);

        } else {
            highestSlopeInfo.setVisibility(View.GONE);
        }
    }


    private void clearMap() {

        for (Marker marker : markers) {
            marker.remove();
        }
        markers.clear();

        for (Polyline polyline : polylines) {
            polyline.remove();
        }
        polylines.clear();
        stepsList.clear();
        routePoints = null;
        currentStepIndex = 0;
    }


    private void clearSpecificMarker(String title) {

        Iterator<Marker> iterator = markers.iterator();
        while (iterator.hasNext()) {
            Marker marker = iterator.next();
            if (marker.getTitle() != null && marker.getTitle().equals(title)) {
                marker.remove();
                iterator.remove();
                break;
            }
        }
    }


    private interface ElevationCallback {
        void onElevationCalculated(double[] elevations);
    }

    private void resetRoute() {
        clearMap();
        TextView directionsTextView = findViewById(R.id.directions_text_view);
        directionsTextView.setText("");
        Button clearButton = findViewById(R.id.clear_button);
        clearButton.setVisibility(View.GONE);

        Toast.makeText(this, "Rota başarıyla sıfırlandı.", Toast.LENGTH_SHORT).show();
    }


    private void updateUIAfterRouteCalculation(
            List<String> directionsList,
            List<LatLng> routePoints,
            List<Step> stepsList
    ) {
        this.stepsList = stepsList;
        this.routePoints = routePoints;
        this.currentStepIndex = 0;

        try {

            if (!stepsList.isEmpty()) {
                showStepInstruction(stepsList.get(0).instructions);
            } else {
                Log.e("StepInstruction", "Steps list is empty.");
            }


            if (routePoints != null && !routePoints.isEmpty()) {
                double[] elevations = new double[routePoints.size()];
                drawRouteWithColors(PolyUtil.encode(routePoints), elevations, wheelchairType, 6.0); // Eğime göre renkli çizim
            }
        } catch (Exception e) {
            Log.e("UI_ERROR", "UI update failed: " + e.getMessage());
        }
    }

    private void calculateSlopeAndUpdateUI(
            List<LatLng> routePoints,
            String wheelchairType,
            double[] elevations,
            ElevationCallback callback,
            ProgressBar progressBar) {

        final double[] highestSlopeHolder = {0.0};

        for (int i = 0; i < elevations.length - 1; i++) {
            double elevationDifference = elevations[i + 1] - elevations[i];
            double distance = SphericalUtil.computeDistanceBetween(routePoints.get(i), routePoints.get(i + 1));
            double slope = (elevationDifference / distance) * 100;

            if (slope > highestSlopeHolder[0]) {
                highestSlopeHolder[0] = slope;
            }

            requiresCompanion = false;
            isRouteSuitable = true;

            if (wheelchairType.equals("manual")) {
                if (slope > 10) {
                    requiresCompanion = true;
                    isRouteSuitable = false;
                } else if (slope > 5) {
                    requiresCompanion = true;
                }
            } else if (wheelchairType.equals("electric")) {
                if (slope > 16) {
                    requiresCompanion = true;
                    isRouteSuitable = false;
                } else if (slope > 10) {
                    requiresCompanion = true;
                }
            }
        }

        highestSlope = highestSlopeHolder[0];

        runOnUiThread(() -> {
            progressBar.setVisibility(View.GONE);
            updateHighestSlopeInfo(highestSlope);
            callback.onElevationCalculated(elevations);
        });
    }

    private void clearAll() {
        clearMap();


        startLocationEditText.setText("");
        endLocationEditText.setText("");

        Button infoButton = findViewById(R.id.info_button);
        infoButton.setVisibility(View.GONE);
        clearButton.setVisibility(View.GONE);
        TextView directionsTextView = findViewById(R.id.directions_text_view);
        directionsTextView.setVisibility(View.GONE);
        TextView stepInstructionTextView = findViewById(R.id.step_instruction_text_view);
        stepInstructionTextView.setVisibility(View.GONE);
        LinearLayout infoPanel = findViewById(R.id.info_panel);
        infoPanel.setVisibility(View.GONE);
        LinearLayout controlsContainer = findViewById(R.id.controls_container);
        controlsContainer.setVisibility(View.VISIBLE);
        addToFavoritesButton.setVisibility(View.GONE);

        ProgressBar progressBar = findViewById(R.id.progress_bar);
        progressBar.setVisibility(View.GONE);

        Toast.makeText(this, "Tüm bilgiler temizlendi.", Toast.LENGTH_SHORT).show();
    }


    private void updateUserLocationOnMap() {
        if (userCurrentLocation != null && mMap != null) {
            mMap.addMarker(new MarkerOptions()
                    .position(userCurrentLocation)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                    .title("Şu Anki Konum"));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userCurrentLocation, 15));
        }
    }

    private void showStepInstruction(String instruction) {
        TextView stepInstructionTextView = findViewById(R.id.step_instruction_text_view);

        if (stepInstructionTextView == null) {
            Log.e("StepInstruction", "TextView not found!");
            return;
        }

        stepInstructionTextView.setText(instruction);
        stepInstructionTextView.setVisibility(View.VISIBLE);
    }


    private void getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
            return;
        }

        fusedLocationProviderClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                String address = getAddressFromLatLng(currentLatLng);
                startLocationEditText.setText(address);
            } else {
                Toast.makeText(this, "Güncel konum alınamadı.", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Konum alırken hata oluştu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void showCurrentLocationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Başlangıç Konumu")
                .setMessage("Güncel konumunuzu almak ister misiniz?")
                .setPositiveButton("Evet", (dialog, which) -> {
                    getCurrentLocation();
                })
                .setNegativeButton("Hayır", null)
                .show();
    }

    private void setupInfoButton() {
        Button infoButton = findViewById(R.id.info_button);
        LinearLayout infoPanel = findViewById(R.id.info_panel);
        TextView infoTextView = findViewById(R.id.info_text_view);

        if (infoButton != null) {

            infoButton.setOnClickListener(v -> {

                if (isInfoPanelVisible) {

                    infoPanel.setVisibility(View.GONE);
                    isInfoPanelVisible = false;
                } else {

                    SpannableStringBuilder routeInfo = new SpannableStringBuilder();

                    SpannableString title = new SpannableString("Rota Bilgisi\n\n");
                    title.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, title.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    routeInfo.append(title);
                    String suitabilityText = "Tekerlekli sandalye için " + (isRouteSuitable ? "uygun." : "UYGUN DEĞİL! (Lütfen bu rotayı tercih etmeyiniz.)");
                    SpannableString suitabilitySpannable = new SpannableString(suitabilityText + "\n\n");
                    int suitabilityColor = isRouteSuitable ? Color.GREEN : Color.RED;
                    suitabilitySpannable.setSpan(new ForegroundColorSpan(suitabilityColor), 25, suitabilityText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    routeInfo.append(suitabilitySpannable);


                    String companionText = "Refakatçi ihtiyacı " + (requiresCompanion ? "GEREKLİDİR." : "yoktur.");
                    SpannableString companionSpannable = new SpannableString(companionText + "\n\n");
                    int companionColor = requiresCompanion ? Color.RED : Color.GREEN;
                    companionSpannable.setSpan(new ForegroundColorSpan(companionColor), 19, companionText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    routeInfo.append(companionSpannable);


                    SpannableString slopeText = new SpannableString("Rotanızdaki en yüksek eğim: " + String.format(Locale.getDefault(), "%.2f%%", highestSlope) + "\n\n");
                    routeInfo.append(slopeText);

                    SpannableString colorInfoTitle = new SpannableString("Rota Üzerindeki Renkler\n\n");
                    colorInfoTitle.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, colorInfoTitle.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    routeInfo.append(colorInfoTitle);

                    SpannableString greenInfo = new SpannableString("Yeşil: Yalnız seyahat için uygundur.\n");
                    greenInfo.setSpan(new ForegroundColorSpan(Color.GREEN), 0, 6, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    routeInfo.append(greenInfo);

                    SpannableString orangeInfo = new SpannableString("Turuncu: Refakatçi yardımı şarttır.\n");
                    orangeInfo.setSpan(new ForegroundColorSpan(Color.rgb(255, 165, 0)), 0, 7, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    routeInfo.append(orangeInfo);

                    SpannableString redInfo = new SpannableString("Kırmızı: Rota eğimi çok yüksektir, tercih etmeyiniz.\n");
                    redInfo.setSpan(new ForegroundColorSpan(Color.RED), 0, 7, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    routeInfo.append(redInfo);

                    infoTextView.setText(routeInfo);
                    infoPanel.setVisibility(View.VISIBLE);
                    isInfoPanelVisible = true;

                }
            });
        }
    }

    private void updateInfoPanelVisibility(boolean isVisible) {

        LinearLayout infoPanel = findViewById(R.id.info_panel);

        if (isVisible) {
            infoPanel.setVisibility(View.VISIBLE);
            isInfoPanelVisible = true;
        } else {
            infoPanel.setVisibility(View.GONE);
            isInfoPanelVisible = false;
        }
    }


    private void saveToFavorites(LatLng startLatLng, LatLng endLatLng) {

        String startAddress = getAddressFromLatLng(startLatLng);
        String endAddress = getAddressFromLatLng(endLatLng);

        String route = "Başlangıç: " + startAddress + ", Varış: " + endAddress;


        SharedPreferences prefs = getSharedPreferences("FavoriteRoutes", MODE_PRIVATE);
        String existingRoutes = prefs.getString("routes", "");
        SharedPreferences.Editor editor = prefs.edit();

        if (existingRoutes.isEmpty()) {
            editor.putString("routes", route);
        } else {
            editor.putString("routes", existingRoutes + ";" + route);
        }

        editor.apply();
        Toast.makeText(this, "Rota favorilere eklendi!", Toast.LENGTH_SHORT).show();
    }

    private void saveRouteToFavorites(
            LatLng startLatLng,
            LatLng endLatLng,
            List<LatLng> polyline,
            String wheelchairType,
            double maxSlope,
            double[] elevations
    ) {
        SharedPreferences sharedPreferences = getSharedPreferences("Favorites", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        if (startLatLng == null || endLatLng == null) {
            Toast.makeText(this, "Başlangıç veya varış noktası eksik, favoriye eklenemedi.", Toast.LENGTH_SHORT).show();
            return;
        }

        String startAddress = getAddressFromLatLng(startLatLng);
        String endAddress = getAddressFromLatLng(endLatLng);

        Gson gson = new Gson();
        RouteData routeData = new RouteData(
                "Favori Rota",
                startLatLng,
                endLatLng,
                polyline,
                wheelchairType,
                maxSlope,
                elevations
        );

        routeData.setStartAddress(startAddress);
        routeData.setEndAddress(endAddress);

        String routeJson = gson.toJson(routeData);

        FavoritesDatabase dbHelper = new FavoritesDatabase(this);
        dbHelper.addFavorite(routeJson);
        Set<String> favorites = sharedPreferences.getStringSet("Routes", new HashSet<>());
        favorites.add(routeJson);
        editor.putStringSet("Routes", favorites);
        editor.apply();
        Toast.makeText(this, "Rota favorilere eklendi!", Toast.LENGTH_SHORT).show();
    }

    private void fetchPlaceDetails(String placeId) {

        List<Place.Field> placeFields = Arrays.asList(
                Place.Field.NAME,
                Place.Field.ADDRESS,
                Place.Field.PHOTO_METADATAS
        );

        FetchPlaceRequest request = FetchPlaceRequest.builder(placeId, placeFields).build();

        PlacesClient placesClient = Places.createClient(this);
        placesClient.fetchPlace(request).addOnSuccessListener(response -> {
            Place place = response.getPlace();

            String placeName = place.getName();
            String placeAddress = place.getAddress();
            List<PhotoMetadata> photoMetadata = place.getPhotoMetadatas();

            if (photoMetadata != null && !photoMetadata.isEmpty()) {
                PhotoMetadata photo = photoMetadata.get(0);
                fetchPhoto(photo, placeName, placeAddress);
            } else {

                goToPlaceDetailActivity(placeName, placeAddress, null);
            }
        }).addOnFailureListener(exception -> {
            exception.printStackTrace();
            Toast.makeText(this, "Mekan detayları alınamadı.", Toast.LENGTH_SHORT).show();
        });
    }

    private void fetchPhoto(PhotoMetadata photoMetadata, String placeName, String placeAddress) {
        PlacesClient placesClient = Places.createClient(this);
        FetchPhotoRequest photoRequest = FetchPhotoRequest.builder(photoMetadata).build();

        placesClient.fetchPhoto(photoRequest).addOnSuccessListener(response -> {
            Bitmap placePhoto = response.getBitmap();
            savePhotoToFile(placePhoto, "place_photo.png");
            goToPlaceDetailActivity(placeName, placeAddress, "place_photo.png");
        }).addOnFailureListener(exception -> {
            exception.printStackTrace();
            Toast.makeText(this, "Fotoğraf alınamadı.", Toast.LENGTH_SHORT).show();

            goToPlaceDetailActivity(placeName, placeAddress, null);
        });
    }


    private void goToPlaceDetailActivity(String placeName, String placeAddress, String photoFileName) {
        Intent intent = new Intent(MapsActivity.this, PlaceDetailActivity.class);
        intent.putExtra("PLACE_NAME", placeName);
        intent.putExtra("PLACE_ADDRESS", placeAddress);
        intent.putExtra("PLACE_PHOTO_FILE", photoFileName);
        startActivity(intent);
    }


    private void savePhotoToFile(Bitmap photo, String fileName) {
        try {
            File file = new File(getCacheDir(), fileName);
            FileOutputStream fos = new FileOutputStream(file);
            photo.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void getCurrentLocationAndSetToEditText(EditText targetEditText) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
            return;
        }

        fusedLocationProviderClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                String address = getAddressFromLatLng(currentLatLng);

                targetEditText.setText(address);
                if (targetEditText == startLocationEditText) {
                    startLatLng = currentLatLng;
                } else if (targetEditText == endLocationEditText) {
                    endLatLng = currentLatLng;
                }
            } else {
                Toast.makeText(this, "Güncel konum alınamadı.", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Konum alırken hata oluştu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

}


