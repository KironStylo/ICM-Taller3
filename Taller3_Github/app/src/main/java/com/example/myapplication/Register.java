package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.myapplication.databinding.ActivityRegisterBinding;
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
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.logging.Logger;


public class Register extends AppCompatActivity {

    // Trazabilidad de la aplicacion con un logger
    private static final String TAG = Register.class.getName();
    private Logger logger = Logger.getLogger(TAG);

    // Componentes requeridos
    private ImageView imagen;
    private Button obtenerGaleria;

    // Datos para registrar el usuario
    private TextInputEditText editTextEmail, editTextPassword;
    // Boton para registrar el usuario
    private Button btnReg;


    //Lista de permisos requeridos
    private final String[] PERMISOS_REQUERIDOS = new String[]{
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_FINE_LOCATION
    };

    // Código de los permisos
    private final int CODIGO_PERMISOS = 101;

    // Código de cargar la imagen
    private final int ID_LOAD_IMAGE = 102;

    //Identificador para la localización
    private final int LOCATION_PERMISSION_ID = 103;

    //Variables de localizacion
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationCallback mLocationCallback;
    private LocationRequest mLocationRequest;
    private Location mLocation;



    // Autenticacion de Firebase
    private FirebaseAuth mAuth;

    // Se declara un objeto para mostrar una barra de progreso cuando se hace sign-in
    ProgressBar progressBar;

    // Se declara un objeto de texto como un hiperenlace para entrar a la otra pantalla
    TextView textView;

    // Acción de la aplicación al inicio si el usuario ya se habia autenticado
    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser != null){
            Log.i("tag","Un mensaje de que se quiere entrar a login");
            Intent intent = new Intent(Register.this, MainActivity.class);
            startActivity(intent);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityRegisterBinding binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        // Fijar los componentes
        editTextEmail = binding.email;
        editTextPassword = binding.password;
        btnReg = binding.btnRegister;
        textView = binding.loginNow;
        imagen = binding.contacto;
        obtenerGaleria = binding.insertar;

        // Barra de progreso y un texto para ir a inicio de sesion
        progressBar = binding.progressBar;
        textView = binding.loginNow;

        // Se inicializa el Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Se crea la solicitud de subscripción a servicios
        mLocationRequest = createLocationRequest();

        // Se activa el servicio de localizacion
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(Register.this);

        ActivityCompat.requestPermissions(Register.this,PERMISOS_REQUERIDOS,CODIGO_PERMISOS);

        // Se crea un listener para que actualice la localización del usuario al momento de ingresar
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                Location location = locationResult.getLastLocation();
                logger.info("Location update in the callback: " + location);
                if (location != null) {
                    logger.info("En localizacion exitosa");
                    logger.info(String.valueOf(location.getLatitude()));
                    logger.info(String.valueOf(location.getLongitude()));
                    logger.info(String.valueOf(location.getAltitude()));
                }
            }
        };
        // Se muestran los cambios de la localizacion
        turnOnLocationAndStartUpdates();

        // Se verifica si ya los permisos han sido otorgados
        if(allPermissionsGranted()){
            // Se crea un listener para abrir la galeria de fotos
            obtenerGaleria.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    getImageFromGallery();
                }
            });
            // Se crea un listener para cuando presionen el texto, se redirijan a la pantalla de login
            textView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(Register.this,Login.class);
                    startActivity(intent);
                }
            });

            // Se crea un listener para crear al usuario dentro de Firebase
            btnReg.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    progressBar.setVisibility(View.VISIBLE);
                    String email, password;
                    email = String.valueOf(editTextEmail.getText());
                    password = String.valueOf(editTextPassword.getText());
                    if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                        Toast.makeText(Register.this, "No deje campo de textos vacios\n", Toast.LENGTH_SHORT).show();
                    } else {
                        createUserWithEmailandPassword(email, password);
                    }
                }
            });

        }
        else{
            // Se pide al usuario autorizar los permisos
            ActivityCompat.requestPermissions(Register.this,PERMISOS_REQUERIDOS,CODIGO_PERMISOS);
        }
    }

    // Se registra el correo y contraseña ingresada a la aplicación
    public void createUserWithEmailandPassword(String email, String password) {
        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                progressBar.setVisibility(View.GONE);
                if (task.isSuccessful()) {
                    // Si es exitosa la creacion de la cuenta
                    Toast.makeText(Register.this, "Cuenta creada.", Toast.LENGTH_SHORT).show();
                } else {
                    // If sign in fails, display a message to the user.
                    Toast.makeText(Register.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // Funciones para asegurar que se han obtenido los permisos del usuario
    private boolean allPermissionsGranted() {
        for (String permission : PERMISOS_REQUERIDOS) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    // Se identifica que la acción es obtener una imagen de la galeria
    private void getImageFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        try{
            startActivityForResult(intent,ID_LOAD_IMAGE);
        }
        catch(ActivityNotFoundException e){
            Toast.makeText(this,"No se pudo abrir la galería", Toast.LENGTH_SHORT).show();
        }
    }

    // Se procede a cargar la imagen una vez identificada la acción.
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == ID_LOAD_IMAGE && resultCode == RESULT_OK){
            final Uri imageUri = data.getData();
            try {
                final InputStream imageStream = getContentResolver().openInputStream(imageUri);
                final Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
                imagen.setImageBitmap(selectedImage);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        if(requestCode == CODIGO_PERMISOS){
            if (resultCode == RESULT_OK) {
                startLocationUpdates();
            } else {
                Toast.makeText(this, "Sin acceso a localización. Hardware deshabilitado", Toast.LENGTH_LONG);
            }
        }
    }

    // Permisos de localización y otras funciones

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
        if (ContextCompat.checkSelfPermission(this, PERMISOS_REQUERIDOS[1]) == PackageManager.PERMISSION_GRANTED) {
            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, null);
        }
    }
    //Apagar los servicios  de actualizacion
    private void stopLocationUpdates(){
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
    }
    private LocationRequest createLocationRequest(){
        LocationRequest mLocationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY,10000)
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(5000)
                .build();
        return mLocationRequest;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,@NonNull String[] permission,@NonNull int [] grantResults){
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
                            resolvable.startResolutionForResult(Register.this, CODIGO_PERMISOS);
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



}