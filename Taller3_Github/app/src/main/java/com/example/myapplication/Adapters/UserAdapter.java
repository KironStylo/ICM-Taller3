package com.example.myapplication.Adapters;

import static androidx.core.content.ContextCompat.startActivity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.myapplication.Model.DatabasePaths;
import com.example.myapplication.Model.Usuario;
import com.example.myapplication.Posicion;
import com.example.myapplication.R;
import com.example.myapplication.UserActivity;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;

public class UserAdapter extends ArrayAdapter<Usuario> {
    public UserAdapter(Context context, ArrayList<Usuario> users) {
        super(context, 0, users);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Grab the person to render
        Usuario person = getItem(position);

        StorageReference mStorageRef;
        // Initializa Firebase Storage
        mStorageRef = FirebaseStorage.getInstance().getReference(DatabasePaths.IMAGEN);

        // Check if amn existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.user_adapter_layout, parent, false);
        }
        // Get all the fields from the adapter
        TextView name = convertView.findViewById(R.id.nombreUsuario);
        ImageView imagen = convertView.findViewById(R.id.imagen_Contacto);
        Button posicion = convertView.findViewById(R.id.verPosicion);

        posicion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(parent.getContext(), Posicion.class);
                // Se le pasa el UID del usuario en una posicion de la lista
                i.putExtra("UID",person.getUID());
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); //add this line
                parent.getContext().startActivity(i);
            }
        });

        StorageReference imageRefl = mStorageRef.child(person.getUID());
        imageRefl.getBytes(Long.MAX_VALUE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
            @Override
            public void onSuccess(byte[] bytes) {
                // Convertir el arreglo de Bytes a Bitmap
                Bitmap foto_contacto = BitmapFactory.decodeByteArray(bytes,0,bytes.length);
                imagen.setImageBitmap(foto_contacto);

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(UserAdapter.this.getContext(),"No se pudo cargar la imagen",Toast.LENGTH_SHORT).show();
            }
        });
        // Format and set the values in the view
        name.setText(person.getNombre());

        return convertView;
    }
}
