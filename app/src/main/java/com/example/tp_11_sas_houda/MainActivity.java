package com.example.tp_11_sas_houda;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 101;
    
    private double latitude = 0.0;
    private double longitude = 0.0;
    private double altitude = 0.0;
    private float accuracy = 0.0f;
    
    private RequestQueue requestQueue;
    
    // UI Elements
    private TextView tvInfo;
    private TextView tvGpsStatus;
    private TextView tvServerStatus;
    private TextView tvImei;
    private TextView tvDate;
    private Button btnSendLocation;

    // TODO: Remplacer 10.0.2.2 par l'adresse IP de votre serveur local (ex: 192.168.1.X) si vous testez sur un appareil physique.
    // 10.0.2.2 est l'adresse IP spéciale pointant vers le localhost de la machine hôte depuis l'émulateur Android.
    private String insertUrl = "http://10.0.2.2/localisation/createPosition.php";

    private LocationManager locationManager;
    private LocationListener locationListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialisation des vues
        tvInfo = findViewById(R.id.tvInfo);
        tvGpsStatus = findViewById(R.id.tvGpsStatus);
        tvServerStatus = findViewById(R.id.tvServerStatus);
        tvImei = findViewById(R.id.tvImei);
        tvDate = findViewById(R.id.tvDate);
        btnSendLocation = findViewById(R.id.btnSendLocation);

        requestQueue = Volley.newRequestQueue(getApplicationContext());
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // Configuration du bouton d'envoi forcé
        btnSendLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (latitude == 0.0 && longitude == 0.0) {
                    // Tenter de récupérer la dernière position connue pour ne pas envoyer 0,0
                    tryGetLastKnownLocation();
                }
                
                if (latitude != 0.0 || longitude != 0.0) {
                    addPosition(latitude, longitude);
                } else {
                    Toast.makeText(MainActivity.this, 
                        "Aucune position détectée pour l'instant. Activez le GPS.", 
                        Toast.LENGTH_LONG).show();
                }
            }
        });

        // Demande des permissions au démarrage
        checkAndRequestPermissions();
    }

    private void checkAndRequestPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.READ_PHONE_STATE
                    }, PERMISSION_REQUEST_CODE);
        } else {
            // Permissions déjà accordées
            initLocationUpdates();
            displayDeviceInfo();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean gpsGranted = false;
            boolean phoneStateGranted = false;
            
            for (int i = 0; i < permissions.length; i++) {
                if (permissions[i].equals(Manifest.permission.ACCESS_FINE_LOCATION) 
                        && grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    gpsGranted = true;
                }
                if (permissions[i].equals(Manifest.permission.READ_PHONE_STATE) 
                        && grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    phoneStateGranted = true;
                }
            }
            
            if (gpsGranted) {
                initLocationUpdates();
            } else {
                tvGpsStatus.setText("GPS refusé");
                tvGpsStatus.setTextColor(getResources().getColor(R.color.error_rose, getTheme()));
                Toast.makeText(this, "Permission GPS requise pour localiser l'appareil.", Toast.LENGTH_LONG).show();
            }
            
            displayDeviceInfo();
        }
    }

    private void initLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        tvGpsStatus.setText("Actif (recherche de signal)");
        tvGpsStatus.setTextColor(getResources().getColor(R.color.warning_amber, getTheme()));

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                latitude = location.getLatitude();
                longitude = location.getLongitude();
                altitude = location.getAltitude();
                accuracy = location.getAccuracy();

                String msg = "Latitude : " + latitude
                        + "\nLongitude : " + longitude
                        + "\nAltitude : " + altitude + " m"
                        + "\nPrécision : " + accuracy + " m";

                tvInfo.setText(msg);
                tvGpsStatus.setText("Signal reçu (Actif)");
                tvGpsStatus.setTextColor(getResources().getColor(R.color.success_emerald, getTheme()));
                
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                tvDate.setText(sdf.format(new Date()));
                
                Toast.makeText(getApplicationContext(), "Nouvelle position GPS détectée !", Toast.LENGTH_SHORT).show();

                // Envoi automatique vers le serveur PHP
                addPosition(latitude, longitude);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
                String statusStr = "Inconnu";
                int color = R.color.warning_amber;
                switch (status) {
                    case LocationProvider.OUT_OF_SERVICE:
                        statusStr = "OUT_OF_SERVICE";
                        color = R.color.error_rose;
                        break;
                    case LocationProvider.TEMPORARILY_UNAVAILABLE:
                        statusStr = "TEMPORARILY_UNAVAILABLE";
                        color = R.color.warning_amber;
                        break;
                    case LocationProvider.AVAILABLE:
                        statusStr = "AVAILABLE (Actif)";
                        color = R.color.success_emerald;
                        break;
                }
                tvGpsStatus.setText(statusStr);
                tvGpsStatus.setTextColor(getResources().getColor(color, getTheme()));
            }

            @Override
            public void onProviderEnabled(String provider) {
                tvGpsStatus.setText("Actif (" + provider + ")");
                tvGpsStatus.setTextColor(getResources().getColor(R.color.success_emerald, getTheme()));
                Toast.makeText(getApplicationContext(), "Provider activé : " + provider, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onProviderDisabled(String provider) {
                tvGpsStatus.setText("Désactivé (" + provider + ")");
                tvGpsStatus.setTextColor(getResources().getColor(R.color.error_rose, getTheme()));
                Toast.makeText(getApplicationContext(), "Provider désactivé : " + provider, Toast.LENGTH_SHORT).show();
            }
        };

        // Écouter toutes les 60 secondes ou après un déplacement de 150 mètres
        locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                60000,
                150,
                locationListener
        );
        
        // Tenter de charger immédiatement une dernière position connue pour initialiser l'affichage
        tryGetLastKnownLocation();
    }

    private void tryGetLastKnownLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        
        Location lastKnown = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (lastKnown == null) {
            lastKnown = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        }
        
        if (lastKnown != null) {
            latitude = lastKnown.getLatitude();
            longitude = lastKnown.getLongitude();
            altitude = lastKnown.getAltitude();
            accuracy = lastKnown.getAccuracy();
            
            String msg = "Latitude : " + latitude
                    + "\nLongitude : " + longitude
                    + "\nAltitude : " + altitude + " m"
                    + "\nPrécision : " + accuracy + " m (Dernière connue)";
            tvInfo.setText(msg);
            
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            tvDate.setText(sdf.format(new Date(lastKnown.getTime())));
        }
    }

    private void displayDeviceInfo() {
        tvImei.setText(getDeviceImei());
    }

    private String getDeviceImei() {
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            return "Non autorisé ( READ_PHONE_STATE requis )";
        }
        
        try {
            // Sur Android 10 (API 29) et supérieur, getDeviceId() génère une SecurityException pour les applications non système.
            // Nous utilisons un mécanisme de fallback robuste (Android ID).
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                return Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
            } else {
                String deviceId = telephonyManager.getDeviceId();
                return (deviceId != null) ? deviceId : "Inconnu (null)";
            }
        } catch (SecurityException e) {
            // Fallback si la permission est refusée au niveau OS ou si l'appareil bloque l'appel
            return Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        }
    }

    private void addPosition(final double lat, final double lon) {
        tvServerStatus.setText("Envoi en cours...");
        tvServerStatus.setTextColor(getResources().getColor(R.color.warning_amber, getTheme()));

        StringRequest request = new StringRequest(
                Request.Method.POST,
                insertUrl,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        tvServerStatus.setText("Synchronisé avec succès");
                        tvServerStatus.setTextColor(getResources().getColor(R.color.success_emerald, getTheme()));
                        Toast.makeText(getApplicationContext(), response, Toast.LENGTH_SHORT).show();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        tvServerStatus.setText("Erreur d'envoi");
                        tvServerStatus.setTextColor(getResources().getColor(R.color.error_rose, getTheme()));
                        
                        String errorMsg = "Erreur de connexion avec le serveur distant.";
                        if (error.networkResponse != null) {
                            errorMsg += " Code HTTP : " + error.networkResponse.statusCode;
                        }
                        Toast.makeText(getApplicationContext(), errorMsg, Toast.LENGTH_LONG).show();
                    }
                }
        ) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                HashMap<String, String> params = new HashMap<>();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

                params.put("latitude", String.valueOf(lat));
                params.put("longitude", String.valueOf(lon));
                params.put("date_position", sdf.format(new Date()));
                params.put("imei", getDeviceImei());

                return params;
            }
        };

        requestQueue.add(request);
    }
}