package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.myapplication.Model.Localizacion;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.Priority;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.example.myapplication.databinding.ActivityMapsBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    // Trazabilidad de la aplicacion con un logger
    private static final String TAG = MapsActivity.class.getName();
    private Logger logger = Logger.getLogger(TAG);

    // Archivo JSON
    private static final String LOCATIONS_FILE = "locations.json";

    // Acercamiento de ciudad una vez

    boolean acercamiento = true;

    // Se declara la variable de Firebase
    private FirebaseAuth mAuth;

    private DatabaseReference reference;

    // Es la información traida de Firebase
    TextView textView;
    // Se declara la variable asociada al usuario
    FirebaseUser user;

    // El marcador del usuario
    Marker mMarker;


    // Permiso de localizacion fina en cadena de texto
    String fineLocationPerm = Manifest.permission.ACCESS_FINE_LOCATION;

    //Identificador de los permisos
    private final int LOCATION_PERMISSION_ID = 103;
    private final int REQUEST_CHECK_SETTINGS = 201;

    //Variables de localizacion
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationCallback mLocationCallback;
    private LocationRequest mLocationRequest;

    // Localizacion del usuario
    Location mLocation;

    boolean disponible;

    // Variable de Google Map
    private GoogleMap mMap;
    private ActivityMapsBinding binding;

    // Localizaciones del Archivo.
    private ArrayList<Localizacion> localizaciones;

    // Se agregaron las localizaciones del Archivo
    private boolean agregadas = false;

    // Spinner de las opciones disponibles
    private Spinner spinner;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());


        // Se obtiene la instancia de Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Se traen los componentes de la interfaz para editarlos
        textView = binding.correo;
        spinner = binding.spinnerOpciones;

        // Se asigna a la variable el usuario actual
        user = mAuth.getCurrentUser();


        // Si el usuario no existe, se devuelve a la pantalla de login
        if(user == null){
            Intent intent = new Intent(MapsActivity.this, Login.class);
            startActivity(intent);
        }
        else{
            // Mostrar el correo del usuario
            textView.setText(user.getEmail());
        }



        // Se crea la solicitud de subscripción a servicios
        mLocationRequest = createLocationRequest();

        // Se activa el servicio de localizacion
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Se hace solicitud de permiso de usar la localizacion
        requestPermission(this,fineLocationPerm,"Se requiere permiso de localizacion",LOCATION_PERMISSION_ID);

        // Se tiene un listener que escucha la opcion que el usuario ha seleccionado para actualizar la base de datos
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                // El item seleccionado
                String selected_item = adapterView.getItemAtPosition(position).toString();
                logger.info("Item seleccionado: " + selected_item);
                Toast.makeText(MapsActivity.this, "Item seleccionado "+ selected_item,Toast.LENGTH_LONG).show();
                switch(selected_item){
                    case "Conectarse":
                    {
                        String llave = mAuth.getCurrentUser().getUid();
                        disponible = true;
                        updateEstado(llave,disponible);
                        break;
                    }
                    case "Desconectarse":{
                        String llave = mAuth.getCurrentUser().getUid();
                        disponible = false;
                        updateEstado(llave,disponible);
                        break;
                    }
                    case "Ver Disponibles":{
                        spinner.setSelection(0);
                        Intent intent = new Intent(MapsActivity.this,UserActivity.class);
                        startActivity(intent);
                        break;
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }

        });
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                Location location = locationResult.getLastLocation();
                logger.info("Location update in the callback: " + location);
                if (location != null) {
                    mLocation = location;
                    logger.info("En localizacion exitosa");
                    logger.info(String.valueOf(location.getLatitude()));
                    logger.info(String.valueOf(location.getLongitude()));
                    logger.info(String.valueOf(location.getAltitude()));

                    // Se actualiza el valor de longitud y latitud
                    String llave = mAuth.getCurrentUser().getUid();
                    updateLocation(llave);

                    // Obtain the SupportMapFragment and get notified when the map is ready to be used.
                    SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                            .findFragmentById(R.id.map);
                    mapFragment.getMapAsync(MapsActivity.this);


                }
            }
        };
    }



    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;

        // Se agregan los marcadores al mapa
        if(!agregadas) {
            try {
                leerArchivoJSON();

                for (int i = 0; i < localizaciones.size(); i++) {
                    LatLng latLng = new LatLng(localizaciones.get(i).getLatitud(),
                            localizaciones.get(i).getLongitud());
                    mMap.addMarker(new MarkerOptions().position(latLng).title(localizaciones.get(i).getNombre()));
                }
                agregadas = true;
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }

        LatLng user = new LatLng(mLocation.getLatitude(), mLocation.getLongitude());
        if(mMarker == null) {
            mMarker = mMap.addMarker(new MarkerOptions().position(user)
                    .title("Tu ubicación")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
        }
        else{
            mMarker.setPosition(user);
        }
        if(acercamiento){
            mMap.moveCamera(CameraUpdateFactory.newLatLng(user));
            mMap.moveCamera(CameraUpdateFactory.zoomTo(10));
            acercamiento = false;
        }
    }

    // Actualizar los datos de localización del usuario
    private void updateLocation(String llave) {
        reference = FirebaseDatabase.getInstance().getReference("users");
        reference.child(llave)
                .child("latitud")
                .setValue(mLocation.getLatitude());
        reference.child(llave)
                .child("longitud")
                .setValue(mLocation.getLongitude());
    }

    // Actualizar el estado del usuario
    private void updateEstado(String llave, boolean estado) {
        HashMap map = new HashMap();
        map.put("disponible",estado);
        reference = FirebaseDatabase.getInstance().getReference("users");
        reference.child(llave)
                .updateChildren(map).addOnSuccessListener(new OnSuccessListener() {
                    @Override
                    public void onSuccess(Object o) {
                        Toast.makeText(MapsActivity.this,"Se actualizo disponible",Toast.LENGTH_LONG).show();
                    }
                });

    }

    // Leer del archivo JSON
    public void leerArchivoJSON() throws JSONException {
        localizaciones = new ArrayList<>();
        try {
            JSONObject jsonFile = loadLocationsFromJson();
            JSONArray locations = jsonFile.getJSONArray("locations");
            for (int i = 0; i < locations.length(); i++) {
                String nombre = locations.getJSONObject(i).getString("name");
                Double latitud = locations.getJSONObject(i).getDouble("latitude");
                Double longitud = locations.getJSONObject(i).getDouble("longitude");
                logger.info("Mensaje " +nombre+""+latitud+""+longitud);
                localizaciones.add(new Localizacion(nombre,latitud,longitud));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    public String loadJSONFromAsset(String assetName) {
        String json = null;
        try {
            InputStream is = this.getAssets().open(assetName);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;
    }

    public JSONObject loadLocationsFromJson() throws JSONException {
        return new JSONObject(loadJSONFromAsset(LOCATIONS_FILE));
    }

    // Funcion para salir de la cuenta
    public void logout(View view){
        mAuth.signOut();
        // Se devuelve al usuario a la pantalla de inicio de sesion
        Intent intent = new Intent(MapsActivity.this,Login.class);
        startActivity(intent);
    }
    // Funciones para ubicar al usuario en el mapa:

    // Al salir de la aplicacion sin cerrarla
    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    // Al entrar de nuevo a la aplicacion sin haberla cerrada
    @Override
    protected void onResume() {
        super.onResume();
        startLocationUpdates();
    }

    // Se inician los servicios de actualizacion de cambios
    private void startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, fineLocationPerm) == PackageManager.PERMISSION_GRANTED) {
            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, null);
        }
    }
    //Apagar los servicios  de actualizacion
    private void stopLocationUpdates(){
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
    }

    // Solicitud de localizacion
    private LocationRequest createLocationRequest(){
        LocationRequest mLocationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY,10000)
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(5000)
                .build();
        return mLocationRequest;
    }


    // Solicitud de permisos
    private void requestPermission(Activity context, String permiso, String justificacion, int idCode){
        // Se revisa si el permiso no se ha otorgado
        if(ActivityCompat.checkSelfPermission(context,permiso) != PackageManager.PERMISSION_GRANTED){
            // Se mira si se debe mostrar un mensaje de justificacion
            if(ActivityCompat.shouldShowRequestPermissionRationale(context,permiso)){
                Toast.makeText(context,justificacion,Toast.LENGTH_LONG);
            }
            // Se hace solicitud del permiso
            ActivityCompat.requestPermissions(context,new String[]{permiso},idCode);
        }
    }

    // Se obtiene el resultado del permiso y si las condiciones son optimas
    // se enciende la subscripcion de cambios de posicion del usuario

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permission, @NonNull int [] grantResults){
        super.onRequestPermissionsResult(requestCode,permission,grantResults);
        switch(requestCode){
            case LOCATION_PERMISSION_ID:{
                if(grantResults.length>0&&grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    // El permiso ha sido autorizado y se sigue con el flujo de la aplicacion
                    Toast.makeText(this,"Autorizado",Toast.LENGTH_SHORT);
                }
                else{
                    // El permiso no fue autorizado
                    Toast.makeText(this,"No hay permiso para acceder a localizacion",Toast.LENGTH_LONG);
                }
            }
        }
        turnOnLocationAndStartUpdates();
    }




    //Se prenden los servicios y las actualizaciones
    private void turnOnLocationAndStartUpdates() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(mLocationRequest);
        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());
        task.addOnSuccessListener(this, locationSettingsResponse -> {
            startLocationUpdates(); // Todas las condiciones para recibiir localizaciones
        });
        task.addOnFailureListener(this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                int statusCode = ((ApiException) e).getStatusCode();
                switch (statusCode) {
                    case CommonStatusCodes.RESOLUTION_REQUIRED:
                        // Location setttings are not satisfied, but this can be fixed by showing the user a dialog.
                        try {
                            // Show the dialog by calling startResolutionForResult(), and check the result in onActivityResult()
                            ResolvableApiException resolvable = (ResolvableApiException) e;
                            resolvable.startResolutionForResult(MapsActivity.this, REQUEST_CHECK_SETTINGS);
                        } catch (IntentSender.SendIntentException sendEx) {
                            // Ignore the error
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        // Location settings are not satisfied. No way to fix the settings so we won't show the dialog.
                        break;
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_CHECK_SETTINGS: {
                if (resultCode == RESULT_OK) {
                    startLocationUpdates();
                } else {
                    Toast.makeText(this, "Sin acceso a localización. Hardware deshabilitado", Toast.LENGTH_LONG);
                }
            }
        }
    }
}