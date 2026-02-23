package view;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.example.ecocity.R;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import controller.IncidenciaController;
import model.Incidencia;

public class FormIncidenciaActivity extends AppCompatActivity {

    private TextInputEditText etTitulo, etDescripcion;
    private AutoCompleteTextView actvCategoria, actvUrgencia;
    private MaterialCardView btnTomarFoto, btnSeleccionarFoto, btnGrabarAudio, btnUbicacion;
    private Button btnCancelar, btnEnviarReporte;

    private IncidenciaController controller;
    private Incidencia incidenciaActual;
    private boolean isEditing = false;

    private String currentPhotoPath;
    private String currentAudioPath;
    private double latitud, longitud;

    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (!isGranted) {
                    Toast.makeText(this, "Permiso denegado", Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<Intent> takePictureLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Toast.makeText(this, "Foto guardada en: " + currentPhotoPath, Toast.LENGTH_LONG).show();
                }
            });

    private final ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    currentPhotoPath = uri.toString();
                    Toast.makeText(this, "Foto seleccionada", Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<Intent> mapLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    latitud = result.getData().getDoubleExtra("latitud", 0.0);
                    longitud = result.getData().getDoubleExtra("longitud", 0.0);
                    Toast.makeText(this, String.format("Ubicación: %.4f, %.4f", latitud, longitud), Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_form_incidencia);

        controller = new IncidenciaController(this);
        initViews();
        setupDropdowns();
        setupListeners();

        if (getIntent().hasExtra("incidencia_id")) {
            setTitle("Editar Incidencia");
            isEditing = true;
            int id = getIntent().getIntExtra("incidencia_id", -1);
            if (id != -1) {
                loadIncidencia(id);
            }
        } else {
            setTitle("Nuevo Reporte");
        }
    }

    private void initViews() {
        etTitulo = findViewById(R.id.etTitulo);
        actvCategoria = findViewById(R.id.actvCategoria);
        etDescripcion = findViewById(R.id.etDescripcion);
        actvUrgencia = findViewById(R.id.actvUrgencia);
        btnTomarFoto = findViewById(R.id.btnTomarFoto);
        btnSeleccionarFoto = findViewById(R.id.btnSeleccionarFoto);
        btnGrabarAudio = findViewById(R.id.btnGrabarAudio);
        btnUbicacion = findViewById(R.id.btnUbicacion);
        btnCancelar = findViewById(R.id.btnCancelar);
        btnEnviarReporte = findViewById(R.id.btnEnviarReporte);
    }

    private void setupDropdowns() {
        String[] categorias = {"Residuos", "Iluminación", "Vías", "Mobiliario", "Otros"};
        ArrayAdapter<String> categoriaAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, categorias);
        actvCategoria.setAdapter(categoriaAdapter);

        String[] urgencias = {"Baja", "Media", "Alta"};
        ArrayAdapter<String> urgenciaAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, urgencias);
        actvUrgencia.setAdapter(urgenciaAdapter);
    }

    private void setupListeners() {
        btnTomarFoto.setOnClickListener(v -> checkCameraPermissionAndTakePicture());
        btnSeleccionarFoto.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
        btnGrabarAudio.setOnClickListener(v -> Toast.makeText(this, "Función de audio no implementada", Toast.LENGTH_SHORT).show());
        btnUbicacion.setOnClickListener(v -> mapLauncher.launch(new Intent(this, MapaActivity.class)));

        btnCancelar.setOnClickListener(v -> {
            setResult(RESULT_CANCELED); // Avisamos que no se hizo nada
            finish();
        });
        btnEnviarReporte.setOnClickListener(v -> guardarIncidencia());
    }

    private void loadIncidencia(int id) {
        incidenciaActual = controller.obtenerIncidencia(id);
        if (incidenciaActual != null) {
            etTitulo.setText(incidenciaActual.getTitulo());
            etDescripcion.setText(incidenciaActual.getDescripcion());
            actvCategoria.setText(incidenciaActual.getCategoria(), false);
            actvUrgencia.setText(incidenciaActual.getUrgencia(), false);

            currentPhotoPath = incidenciaActual.getFotoUri();
            currentAudioPath = incidenciaActual.getAudioUri();
            latitud = incidenciaActual.getLatitud();
            longitud = incidenciaActual.getLongitud();

            btnEnviarReporte.setText("Actualizar Reporte");
        }
    }

    private void guardarIncidencia() {
        String titulo = etTitulo.getText().toString().trim();
        String categoria = actvCategoria.getText().toString().trim();
        String descripcion = etDescripcion.getText().toString().trim();
        String urgencia = actvUrgencia.getText().toString().trim();

        if (titulo.isEmpty() || categoria.isEmpty() || urgencia.isEmpty()) {
            Toast.makeText(this, "Los campos Título, Categoría y Urgencia son obligatorios", Toast.LENGTH_LONG).show();
            return;
        }

        if (isEditing && incidenciaActual != null) {
            incidenciaActual.setTitulo(titulo);
            incidenciaActual.setCategoria(categoria);
            incidenciaActual.setDescripcion(descripcion);
            incidenciaActual.setUrgencia(urgencia);
            incidenciaActual.setFotoUri(currentPhotoPath);
            incidenciaActual.setAudioUri(currentAudioPath);
            incidenciaActual.setLatitud(latitud);
            incidenciaActual.setLongitud(longitud);
            incidenciaActual.setEstadoSync(0);
            controller.actualizarIncidencia(incidenciaActual);
            Toast.makeText(this, "Reporte actualizado", Toast.LENGTH_SHORT).show();
        } else {
            Incidencia nuevaIncidencia = new Incidencia();
            nuevaIncidencia.setTitulo(titulo);
            nuevaIncidencia.setCategoria(categoria);
            nuevaIncidencia.setDescripcion(descripcion);
            nuevaIncidencia.setUrgencia(urgencia);
            nuevaIncidencia.setFotoUri(currentPhotoPath);
            nuevaIncidencia.setAudioUri(currentAudioPath);
            nuevaIncidencia.setLatitud(latitud);
            nuevaIncidencia.setLongitud(longitud);
            nuevaIncidencia.setFecha(new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date()));
            controller.crearIncidencia(nuevaIncidencia);
            Toast.makeText(this, "Reporte creado", Toast.LENGTH_SHORT).show();
        }

        // ¡¡LA LÍNEA CLAVE QUE FALTABA!!
        // Le decimos a MainActivity que todo ha ido OK.
        setResult(RESULT_OK);
        finish(); // Cerramos el formulario y volvemos a la lista
    }

    private void checkCameraPermissionAndTakePicture() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            dispatchTakePictureIntent();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File photoFile;
        try {
            photoFile = createImageFile();
        } catch (IOException ex) {
            Toast.makeText(this, "Error al crear el archivo de imagen", Toast.LENGTH_SHORT).show();
            return;
        }

        Uri photoURI = FileProvider.getUriForFile(this, "com.example.ecocity.fileprovider", photoFile);
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
        takePictureLauncher.launch(takePictureIntent);
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }
}
