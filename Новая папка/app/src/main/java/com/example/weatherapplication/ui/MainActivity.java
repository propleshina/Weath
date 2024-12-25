package com.example.weatherapplication.ui;


import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.weatherapplication.R;
import com.example.weatherapplication.data.model.WeatherResponse;
import com.example.weatherapplication.network.RetrofitClient;
import com.example.weatherapplication.utils.Constants;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.Priority;


public class MainActivity extends AppCompatActivity {
    
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private EditText cityInput;
    private TextView cityName;
    private TextView temperature;
    private TextView weatherDescription;
    private ForecastAdapter forecastAdapter;
    private FusedLocationProviderClient fusedLocationProviderClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button getFethWeatherButton = findViewById((R.id.getFetchWeatherButton));
        cityInput = findViewById(R.id.cityInput);
        Button getWeatherButton = findViewById((R.id.getWeatherButton));

        cityName = findViewById(R.id.cityName);
        temperature = findViewById(R.id.temperature);
        weatherDescription = findViewById(R.id.weatherDescription);
        RecyclerView recyclerView = findViewById(R.id.forecastRecyclerView);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);

        recyclerView.setLayoutManager(layoutManager);
        forecastAdapter = new ForecastAdapter();
        recyclerView.setAdapter(forecastAdapter);

        getWeatherButton.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onClick(View v) {
                String city  = cityInput.getText().toString().trim();
                if (!city.isEmpty()) {
                    getWeather(city);
                } else{
                    cityName.setText("City");
                }
            }
        });

        getFethWeatherButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FetchWeather();
            }
        });

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        FetchWeather();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getWeatherByLocation();
            } else {
                Toast.makeText(this, "Permission for geolocation is missing", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void fetchWeatherForecast(double latitude, double longitude) {
        RetrofitClient.getInstance()
                .getWeatherForecast(latitude, longitude, Constants.API_KEY, Constants.UNITS_METRIC)
                .enqueue(new Callback<WeatherResponse>() {
                    @SuppressLint("NotifyDataSetChanged")
                    @Override
                    public void onResponse(@NonNull Call<WeatherResponse> call, @NonNull Response<WeatherResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            WeatherResponse weatherResponse = response.body();
                            List<WeatherResponse.Forecast> forecastList = weatherResponse.getList();

                            if (forecastList != null && !forecastList.isEmpty()) {
                                forecastAdapter.setForecastList(forecastList);
                                forecastAdapter.notifyDataSetChanged();
                            }
                        } else {
                            Log.e("WeatherApp", "Forecast API call failed with code: " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(Call<WeatherResponse> call, Throwable t) {
                        Toast.makeText(MainActivity.this, "Network error", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void FetchWeather(){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            getWeatherByLocation();
        }
    }

    private void getWeather(String city) {
        RetrofitClient.getInstance().getCurrentWeather(city, Constants.API_KEY, Constants.UNITS_METRIC)
                .enqueue(new Callback<WeatherResponse>() {
                    @Override
                    public void onResponse(Call<WeatherResponse> call, Response<WeatherResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            WeatherResponse weather = response.body();
                            cityName.setText(weather.getName());
                            temperature.setText(weather.getMain().getTemp() + "°C");
                            weatherDescription.setText(weather.getWeather().get(0).getDescription());

                            getForecast(city);

                        } else {
                            cityName.setText("Error! City not found");
                        }
                    }

                    @Override
                    public void onFailure(Call<WeatherResponse> call, Throwable t) {
                        cityName.setText("Internet error");
                    }
                });
    }

    private void getWeatherByLocation() throws SecurityException {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        // Проверяем разрешения
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationProviderClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();

                    fetchWeather(latitude, longitude);
                } else {
                    // Если локация null, запрашиваем обновление местоположения
                    requestNewLocation();
                }
            });
        } else {
            // Запрашиваем разрешения
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
    }

    private void requestNewLocation() {
        LocationRequest locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(10000) // Интервал обновлений (10 секунд)
                .setFastestInterval(5000); // Минимальный интервал (5 секунд)

        LocationCallback locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    Toast.makeText(MainActivity.this, "Cannot determine location", Toast.LENGTH_SHORT).show();
                    return;
                }

                Location location = locationResult.getLastLocation();
                if (location != null) {
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();

                    fetchWeather(latitude, longitude);
                    // Останавливаем обновления местоположения после получения
                    fusedLocationProviderClient.removeLocationUpdates(this);
                }
            }
        };

        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }


    private void fetchWeather(double latitude, double longitude) {
        RetrofitClient.getInstance()
                .getCurrentWeatherByCoordinates(latitude, longitude, Constants.API_KEY, Constants.UNITS_METRIC)
                .enqueue(new Callback<WeatherResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<WeatherResponse> call, @NonNull Response<WeatherResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            WeatherResponse weather = response.body();
                            cityName.setText(weather.getName());
                            temperature.setText(weather.getMain().getTemp() + "°C");
                            weatherDescription.setText(weather.getWeather().get(0).getDescription());

                            fetchWeatherForecast(latitude, longitude);
                        } else {
                            cityName.setText("Error: " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(Call<WeatherResponse> call, Throwable t) {
                        cityName.setText("Network error");
                    }
                });
    }

    private void getForecast(String city) {
        RetrofitClient.getInstance().get5DayForecast(city, Constants.API_KEY, Constants.UNITS_METRIC)
                .enqueue(new Callback<WeatherResponse>() {
                    @SuppressLint("NotifyDataSetChanged")
                    @Override
                    public void onResponse(Call<WeatherResponse> call, Response<WeatherResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            WeatherResponse weatherResponse = response.body();
                            List<WeatherResponse.Forecast> forecastList = weatherResponse.getList();

                            if (forecastList != null && !forecastList.isEmpty()) {
                                forecastAdapter.setForecastList(forecastList);
                                forecastAdapter.notifyDataSetChanged();
                            }
                        } else {
                            Log.e("WeatherApp", "Forecast API call failed with code: " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<WeatherResponse> call, @NonNull Throwable t) {
                        Log.e("WeatherApp", "Failed to fetch forecast data", t);
                    }
                });
    }
}