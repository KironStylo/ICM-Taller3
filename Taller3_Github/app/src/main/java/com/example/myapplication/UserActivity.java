package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import com.example.myapplication.Adapters.UserAdapter;
import com.example.myapplication.Model.DatabasePaths;
import com.example.myapplication.Model.User;
import com.example.myapplication.Model.Usuario;
import com.example.myapplication.databinding.ActivityUserBinding;
import com.google.android.gms.auth.api.signin.internal.Storage;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

public class UserActivity extends AppCompatActivity {

    private static final String TAG = UserActivity.class.getName();

    private FirebaseAuth mAuth;

    // Auth user
    FirebaseUser currentUser;

    // Variables for Firebase DB
    FirebaseDatabase database;
    DatabaseReference myRef;



    // Variables para mostrar una vista de la lista adaptada con los datos
    UserAdapter adapter;
    ArrayList<Usuario> userLocal = new ArrayList<>();
    ListView listView;

    ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user);
        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        // Initialize Firebase database
        database = FirebaseDatabase.getInstance();
        myRef = database.getReference();

        // Initialize Adapter
        adapter = new UserAdapter(UserActivity.this, userLocal);
        listView = findViewById(R.id.userList);
        listView.setAdapter(adapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        currentUser = mAuth.getCurrentUser();
        if(currentUser == null) {
            logout();
        }
        loadQueryPeople();
    }


    public void loadQueryPeople() {
        myRef = database.getReference(DatabasePaths.USERS);
        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot datasnapshot) {
                userLocal.clear();
                for (DataSnapshot snapshot: datasnapshot.getChildren()) {
                    User ppl = snapshot.getValue(User.class);
                    Log.i(TAG,"Este el código del usuario"+snapshot.getKey());
                    if(ppl.isDisponible() && !mAuth.getCurrentUser().getUid().equals(snapshot.getKey())) {
                        Usuario usuario = new Usuario(ppl.getNombre(),snapshot.getKey());
                        userLocal.add(usuario);
                    }
                }
                adapter.notifyDataSetChanged();
                Log.i(TAG, "Data changed from realtime DB");
                listView.post(new Runnable() {
                    @Override
                    public void run() {
                        listView.setSelection(userLocal.size()-1);
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w(TAG, "Error en la consulta", databaseError.toException());
            }
        });
    }

    // Salir de la cuenta
    private void logout() {
        mAuth.signOut();
        Intent intent = new Intent(UserActivity.this, Login.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }



}