package com.example.weatherapp.view;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.weatherapp.R;
import com.example.weatherapp.model.ApiResponse;
import com.example.weatherapp.model.Weather;
import com.example.weatherapp.retrofit.APIInterface;
import com.squareup.picasso.Picasso;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    private final static String BASE_URL = "https://api.openweathermap.org/data/2.5/";
    private final static String API_KEY = "0d727fcbd54f9689071a8afc15ee22d2";

    private Button submitButton;
    private TextView currentTemp;
    private TextView minTemp;
    private TextView maxTemp;
    private TextView pressure;
    private TextView humidity;
    private TextView weatherDescription;
    private ImageView weatherIcon;
    private EditText inputCity;
    private Button showLocationActivity;
    private Button showWeatherForLocation;

    private String currentCity = "";
    private Double currentCityLat;
    private Double currentCityLong;

    private LocationManager locationManager;
    private LocationListener locationListener;
    private Double currentLatitude;
    private Double currentLongitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        submitButton = findViewById(R.id.button);
        currentTemp = findViewById(R.id.textView);
        minTemp = findViewById(R.id.minTemp);
        maxTemp = findViewById(R.id.maxTemp);
        pressure = findViewById(R.id.pressure);
        humidity = findViewById(R.id.humidityVal);
        inputCity = findViewById(R.id.editText);
        weatherDescription = findViewById(R.id.textView2);
        weatherIcon = findViewById(R.id.imageView);
        showLocationActivity = findViewById(R.id.button2);
        showWeatherForLocation = findViewById(R.id.button3);

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);


        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                Log.i("onLocationChanged", location.toString());
                currentLatitude = location.getLatitude();
                currentLongitude = location.getLongitude();
                getWeatherDataFromServer("", API_KEY, true ,currentLatitude, currentLongitude);
            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {
                Log.i("onStatusChanged", s);
                Log.i("onStatusChanged", i + "");
            }

            @Override
            public void onProviderEnabled(String s) {
                Log.i("onProviderEnabled", s);
            }

            @Override
            public void onProviderDisabled(String s) {
                Log.i("onProviderDisabled", s);
            }
        };

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED){
            // no permission, ask for it
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        } else {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0,9000, locationListener);
        }


        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String _currentValue = inputCity.getText().toString();

                if(!_currentValue.isEmpty()){
                    getWeatherDataFromServer(_currentValue, API_KEY, false ,0.0, 0.0);
                }else {
                    Toast.makeText(MainActivity.this, "Please type a city name", Toast.LENGTH_LONG).show();
                }
            }
        });

        showLocationActivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(!currentCity.isEmpty()){
                    Intent intent = new Intent(MainActivity.this, MapsActivity.class);
                    intent.putExtra("cityName", currentCity);
                    intent.putExtra("cityLat", currentCityLat);
                    intent.putExtra("cityLon", currentCityLong);

                    startActivity(intent);
                }else{
                    Toast.makeText(MainActivity.this, "Please type a city name first!", Toast.LENGTH_LONG).show();
                }

            }
        });

        showWeatherForLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getWeatherDataFromServer("", API_KEY, true ,currentLatitude, currentLongitude);
            }
        });

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){

            if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0,9000, locationListener);
            }

        }

    }

    public APIInterface getInterface(){
        OkHttpClient client = new OkHttpClient.Builder().build();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        APIInterface apiInterface = retrofit.create(APIInterface.class);
        return apiInterface;
    }

    public void getWeatherDataFromServer(String city, String appId, boolean withCoordinates, Double lat, Double lon){
        APIInterface apiInterface = this.getInterface();
        Call<ApiResponse> mService;
        if(!withCoordinates){
            mService = apiInterface.getWeatherdata(city, appId, "metric");
        }else {
            mService = apiInterface.getWeatherByCoordinates(lat, lon, appId, "metric");
        }

        Log.i("mservice", mService.request().toString());

        final ProgressDialog progressDialog;
        progressDialog = new ProgressDialog(MainActivity.this);
        progressDialog.setMax(100);
        progressDialog.setMessage("It's loading....");
        progressDialog.setTitle("Waiting for data from server");
        progressDialog.show();

        mService.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                progressDialog.dismiss();
                ApiResponse responseBody = response.body();
                if(responseBody != null){

                    try{
                        String responseTemp = responseBody.getMain().getTemp() + "°C";
                        String responseMaxTemp = responseBody.getMain().getTemp_max() + "°C";
                        String responseMinTemp = responseBody.getMain().getTemp_min() + "°C";
                        String responsePressure = responseBody.getMain().getPressure() + " hPA";
                        String responseHumidity = responseBody.getMain().getHumidity() + " %";

                        currentCity = responseBody.getName();
                        currentCityLat = responseBody.getCoord().getLat();
                        currentCityLong = responseBody.getCoord().getLon();

                        Weather weatherInfo = responseBody.getWeather().get(0);

                        String currentWeatherDescription = weatherInfo.getDescription();
                        String currentWeatherIcon = weatherInfo.getIcon();
                        String iconUrl = "https://openweathermap.org/img/wn/" + currentWeatherIcon + "@2x.png";

                        currentTemp.setText(responseTemp);
                        maxTemp.setText(responseMaxTemp);
                        minTemp.setText(responseMinTemp);
                        pressure.setText(responsePressure);
                        humidity.setText(responseHumidity);
                        inputCity.setText(responseBody.getName());

                        weatherDescription.setText(currentWeatherDescription);
                        Picasso.get().load(iconUrl).into(weatherIcon);
                    }catch (Exception e){
                        Log.i("exception", e.getLocalizedMessage());
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                call.cancel();
                progressDialog.dismiss();
                Log.i("error", t.getMessage());
            }
        });
    }
}
