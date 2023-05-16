package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.myapplication.databinding.ActivityMainBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.ktx.Firebase;


public class MainActivity extends AppCompatActivity {

    // Se declara la variable de Firebase
    private FirebaseAuth mAuth;
    // Se decalra el boton de salida
    Button btnLog;
    // Es la información traida de Firebase
    TextView textView;
    // Se declara la variable asociada al usuario
    FirebaseUser user;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);
        // Se obtiene la instancia de Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Se traen los componentes de la interfaz para editarlos
        btnLog = binding.btnLogout;
        textView = binding.correo;

        // Se asigna a la variable el usuario actual
        user = mAuth.getCurrentUser();

        // Si el usuario no existe, se devuelve a la pantalla de login
        if(user == null){
            Intent intent = new Intent(MainActivity.this, Login.class);
            startActivity(intent);
        }
        else{
            // Mostrar el correo del usuario
            textView.setText(user.getEmail());
        }

        btnLog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mAuth.signOut();
                // Se devuelve al usuario a la pantalla de inicio de sesion
                Intent intent = new Intent(MainActivity.this,Login.class);
                startActivity(intent);
            }
        });

    }
}