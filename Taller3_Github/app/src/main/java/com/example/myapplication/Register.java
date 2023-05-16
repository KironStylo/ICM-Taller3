package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.myapplication.databinding.ActivityRegisterBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;


public class Register extends AppCompatActivity {


    // Se declaran los objetos necesarios para la creación de un usuario
    TextInputEditText editTextEmail, editTextPassword;
    Button btnReg;

    // Se declaran los objetos para la autenticación en la base de datos
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
        editTextEmail = binding.email;
        editTextPassword = binding.password;
        btnReg = binding.btnRegister;
        textView = binding.loginNow;

        // Se crea un listener para cuando presionen el texto, se redirijan a la pantalla de login
        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Register.this,Login.class);
                startActivity(intent);
            }
        });

        // Barra de progreso y un texto para ir a inicio de sesion
        progressBar = binding.progressBar;
        textView = binding.loginNow;

        // Se inicializa el Firebase Auth
        mAuth = FirebaseAuth.getInstance();

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

}