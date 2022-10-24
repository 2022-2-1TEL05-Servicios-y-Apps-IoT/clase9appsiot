package com.example.clase9appsiot;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.example.clase9appsiot.beans.Gato;
import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class MainActivity2 extends AppCompatActivity {

    FirebaseDatabase firebaseDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        firebaseDatabase = FirebaseDatabase.getInstance();

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        TextView textView = findViewById(R.id.textViewUsuario);
        textView.setText(currentUser.getDisplayName());

        Button button = findViewById(R.id.btn_logout);
        button.setOnClickListener(view -> AuthUI.getInstance().signOut(MainActivity2.this)
                .addOnCompleteListener(task -> startActivity(new Intent(MainActivity2.this, MainActivity.class))));
    }

    public void guardarGato(View view) {
        DatabaseReference ref = firebaseDatabase.getReference().child("gatos");

        EditText editTextGato = findViewById(R.id.inputNombreGato);
        String nombreGato = editTextGato.getText().toString();
        Gato g = new Gato();
        g.setNombre(nombreGato);

        DatabaseReference newRef = ref.push();
        newRef.setValue(g).addOnCompleteListener(task -> {
            Log.d("msg", "new key: " + newRef.getKey());
            System.out.println("prueba");
        });

    }
}