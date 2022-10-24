package com.example.clase9appsiot;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.clase9appsiot.beans.Gato;
import com.example.clase9appsiot.beans.Persona;
import com.firebase.ui.auth.AuthMethodPickerLayout;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Arrays;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    FirebaseDatabase firebaseDatabase;
    private String CHANNEL_ID = "canalHigh";
    private boolean primeraVez = true;
    private ChildEventListener childEventListener;
    DatabaseReference gatosRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        createNotificationChannel();

        firebaseDatabase = FirebaseDatabase.getInstance();

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            Log.d("msg-fb", currentUser.getUid());
            Log.d("msg-fb", currentUser.getEmail());
            currentUser.reload().addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (currentUser.isEmailVerified()) {
                        startActivity(new Intent(MainActivity.this, MainActivity2.class));
                        finish();
                    } else {
                        Toast.makeText(MainActivity.this, "debes validar tu correo", Toast.LENGTH_SHORT).show();
                        currentUser.sendEmailVerification().addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                Log.d("msg-fb", "correo enviado");
                            }
                        });
                    }
                }
            });

        }

        gatosRef = firebaseDatabase.getReference().child("gatos");
        childEventListener = gatosRef.addChildEventListener(new MyChildEventListener());

        gatosRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                System.out.println("algo pasó en los gato");
                for (DataSnapshot snapshot1 : snapshot.getChildren()) {
                    Gato gato = snapshot1.getValue(Gato.class);
                    System.out.println(snapshot1.getKey() + " --> " + gato.getNombre());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        Button btnLogin = findViewById(R.id.btn_login);
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                AuthMethodPickerLayout layout = new AuthMethodPickerLayout.Builder(R.layout.login_personalizado)
                        .setEmailButtonId(R.id.btn_user_pass)
                        .setGoogleButtonId(R.id.btn_google)
                        .build();

                Intent fbIntent = AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setAvailableProviders(Arrays.asList(
                                new AuthUI.IdpConfig.EmailBuilder().build(),
                                new AuthUI.IdpConfig.GoogleBuilder().build()
                        ))
                        .setAuthMethodPickerLayout(layout)
                        .setTosAndPrivacyPolicyUrls(
                                "https://example.com/terms.html",
                                "https://example.com/privacy.html")
                        .setLogo(R.drawable.ic_pkm)
                        .build();
                activityResultLauncher.launch(fbIntent);
            }
        });
    }

    private ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(new FirebaseAuthUIActivityResultContract(), result -> {
        onSignInOnResult(result);
    });

    private void onSignInOnResult(FirebaseAuthUIAuthenticationResult result) {
        IdpResponse idpResponse = result.getIdpResponse();
        if (result.getResultCode() == RESULT_OK) {
            Log.d("msg-fb", idpResponse.getEmail());
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            Log.d("msg-fb", currentUser.getUid());

            DatabaseReference ref = firebaseDatabase.getReference()
                    .child("usuarios")
                    .child(currentUser.getUid());

            ref.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (!snapshot.exists()) {
                        HashMap<String, String> map = new HashMap<>();
                        map.put("nombre", currentUser.getDisplayName());
                        map.put("provider", currentUser.getProviderId());
                        map.put("dominio", currentUser.getEmail().split("@")[1]);
                        map.put("rol", "alumno");

                        ref.setValue(map).addOnCompleteListener(task -> {
                            System.out.println("usuario creado exitosamente");
                        });
                    }
                    if (currentUser.isEmailVerified()) {
                        startActivity(new Intent(MainActivity.this, MainActivity2.class));
                        finish();
                    } else {
                        Toast.makeText(MainActivity.this, "debes validar tu correo", Toast.LENGTH_SHORT).show();
                        currentUser.sendEmailVerification().addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                Log.d("msg-fb", "correo enviado");
                            }
                        });
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });

        } else {
            Log.d("msg-fb", "error al loguearse");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        gatosRef.removeEventListener(childEventListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        childEventListener = gatosRef.addChildEventListener(new MyChildEventListener());
    }

    class MyChildEventListener implements ChildEventListener {

        @Override
        public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
            if (!primeraVez) {
                Gato gato = snapshot.getValue(Gato.class);
                TextView textView = findViewById(R.id.textView2);
                textView.setText("Nuevo gato: " + gato.getNombre());
                //notificarNuevoGato(gato.getNombre());
            } else {
                primeraVez = false;
            }
        }

        @Override
        public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

        }

        @Override
        public void onChildRemoved(@NonNull DataSnapshot snapshot) {

        }

        @Override
        public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

        }

        @Override
        public void onCancelled(@NonNull DatabaseError error) {

        }
    }

    public void guardar(View view) {
        DatabaseReference ref = firebaseDatabase.getReference();
        DatabaseReference refPersonas = ref.child("personas");

        EditText editTextNombre = findViewById(R.id.inputNombre);
        EditText editTextApellido = findViewById(R.id.inputApellido);
        EditText editTextDni = findViewById(R.id.inputDni);
        String dni = editTextDni.getText().toString();

        DatabaseReference refPersDni = refPersonas.child(dni);

        //HashMap<String,String> hashMap = new HashMap<>();
        //hashMap.put("nombre",editTextNombre.getText().toString());
        //hashMap.put("apellido",editTextApellido.getText().toString());
        //ref.setValue(hashMap);
        Persona persona = new Persona();
        persona.setNombre(editTextNombre.getText().toString());
        persona.setApellido(editTextApellido.getText().toString());

        refPersDni.setValue(persona).addOnSuccessListener(unused -> {
            //Snackbar.make(findViewById(R.id.MainActivityLayout),"Usuario registrado correctamente",2000).show();
            Toast.makeText(MainActivity.this, "Usuario registrado correctamente", Toast.LENGTH_SHORT).show();
        });
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

    public void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {

            NotificationChannel channelDefault = new NotificationChannel(CHANNEL_ID, "Mensajes de default importancia", NotificationManager.IMPORTANCE_HIGH);
            channelDefault.enableVibration(true);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channelDefault);
        }
    }

    public void notificarNuevoGato(String nombreGato) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID).setSmallIcon(R.drawable.ic_alert).setContentTitle("Nuevo gato").setContentText("Se ha adicionado un nuevo gato con nombre: " + nombreGato).setPriority(NotificationCompat.PRIORITY_HIGH).setAutoCancel(true);

        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(this);
        notificationManagerCompat.notify(4, builder.build());
    }

}