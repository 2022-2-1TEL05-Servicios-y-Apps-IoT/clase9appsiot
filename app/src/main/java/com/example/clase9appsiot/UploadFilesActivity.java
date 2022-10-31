package com.example.clase9appsiot;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.ListResult;
import com.google.firebase.storage.OnPausedListener;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class UploadFilesActivity extends AppCompatActivity {

    FirebaseStorage storage;
    StorageReference clase11Ref;
    static int cont = 1;
    ActivityResultLauncher<Intent> launcher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Uri uri = result.getData().getData();
                    try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                        if (cursor != null && cursor.moveToFirst()) {
                            String string = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                            subirArchivo(uri, string);
                        }
                    }
                } else {
                    Toast.makeText(UploadFilesActivity.this, "Debe seleccionar un archivo", Toast.LENGTH_SHORT).show();
                }
            }
    );

    ActivityResultLauncher<Intent> launcherBigFile = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Uri uri = result.getData().getData();
                    subirArchivoConProgreso(uri);
                } else {
                    Toast.makeText(UploadFilesActivity.this, "Debe seleccionar un archivo", Toast.LENGTH_SHORT).show();
                }
            }
    );


    ActivityResultLauncher<Intent> launcherPhotos = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Uri uri = result.getData().getData();
                    StorageReference child = clase11Ref.child("photo.jpg");

                    child.putFile(uri)
                            .addOnSuccessListener(taskSnapshot -> Log.d("msg-test", "Subido correctamente"))
                            .addOnFailureListener(e -> Log.d("msg-test", "error"))
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Log.d("msg-test", "ruta archivo: " + task.getResult());
                                }
                            });
                } else {
                    Toast.makeText(UploadFilesActivity.this, "Debe seleccionar un archivo", Toast.LENGTH_SHORT).show();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_files);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        Log.d("msg-test", "usuario log en ActivityUpload: " + currentUser.getDisplayName());
        String uid = currentUser.getUid();
        storage = FirebaseStorage.getInstance();
        StorageReference storageReference = storage.getReference();
        clase11Ref = storageReference.child("clase11");

        StorageReference photoRef = clase11Ref.child("photo.jpg");
        ImageView imageView = findViewById(R.id.imageViewFromStorage);

        Glide.with(this)
                .load(photoRef)
                .into(imageView);

        File localFile = null;
        try {
            localFile = File.createTempFile("images", "jpg");

            photoRef.getFile(localFile)
                    .addOnSuccessListener(taskSnapshot -> Log.d("msg-test", "archivo descargado"))
                    .addOnFailureListener(e -> Log.d("msg-test", "error", e.getCause()));

        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    public void uploadFileBtn(View view) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("application/pdf");
        launcher.launch(intent);
    }

    public void subirArchivo(Uri uri, String fileName) {
        Log.d("msg-test", String.valueOf(uri));
        StorageReference child = clase11Ref.child(fileName);

        StorageMetadata storageMetadata = new StorageMetadata.Builder()
                .setCustomMetadata("autor", "El profe")
                .setCustomMetadata("curso", "1tel05")
                .build();
        child.putFile(uri, storageMetadata)
                .addOnSuccessListener(taskSnapshot -> Log.d("msg-test", "Subido correctamente"))
                .addOnFailureListener(e -> Log.d("msg-test", "error"))
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("msg-test", "ruta archivo: " + task.getResult());
                    }
                });
    }

    public void uploadBigFileBtn(View view) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("application/zip");
        launcherBigFile.launch(intent);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (uploadTask != null) {
            uploadTask.pause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (uploadTask != null) {
            uploadTask.resume();
        }
    }

    UploadTask uploadTask;

    public void subirArchivoConProgreso(Uri uri) {
        Log.d("msg-test", String.valueOf(uri));
        StorageReference child = clase11Ref.child("archivo-" + (cont++) + ".zip");
        uploadTask = child.putFile(uri);
        uploadTask.addOnSuccessListener(taskSnapshot -> Log.d("msg-test", "Subido correctamente"))
                .addOnFailureListener(e -> Log.d("msg-test", "error"))
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("msg-test", "ruta archivo: " + task.getResult());
                    }
                    uploadTask = null;
                })
                .addOnPausedListener(new OnPausedListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onPaused(@NonNull UploadTask.TaskSnapshot snapshot) {
                        Log.d("msg-test", "paused");
                    }
                })
                .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onProgress(@NonNull UploadTask.TaskSnapshot snapshot) {
                        long bytesTransferred = snapshot.getBytesTransferred();
                        long totalByteCount = snapshot.getTotalByteCount();
                        double progreso = (100.0 * bytesTransferred) / totalByteCount;
                        TextView textView = findViewById(R.id.textViewProgreso);
                        textView.setText("Progreso de subida: " + progreso + "%");
                        ProgressBar progressBar = findViewById(R.id.progressBar);
                        Long round = Math.round(progreso);
                        progressBar.setProgress(round.intValue());
                    }
                });
    }

    public void existeArchivo(View view) {
        EditText editText = findViewById(R.id.inputArchivoBuscar);
        String nombreArchivo = editText.getText().toString();

        StorageReference searchChild = clase11Ref.child(nombreArchivo + ".pdf");

        searchChild.getMetadata()
                .addOnSuccessListener(new OnSuccessListener<StorageMetadata>() {
                    @Override
                    public void onSuccess(StorageMetadata storageMetadata) {
                        Toast.makeText(UploadFilesActivity.this, "Existe!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(UploadFilesActivity.this, "No existe", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    public void subirImagen(View view) {

        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("image/jpeg");
        launcherPhotos.launch(intent);

    }

    public void listarArchivos(View view) {
        clase11Ref.listAll().addOnSuccessListener(listResult -> {
            List<StorageReference> resultItems = listResult.getItems();
            int size = resultItems.size();
            Toast.makeText(UploadFilesActivity.this, "Cantidad de archivos: " + size, Toast.LENGTH_SHORT).show();
            for (StorageReference item : resultItems) {
                Log.d("msg-test", "name: " + item.getName());
            }
        });
    }
}