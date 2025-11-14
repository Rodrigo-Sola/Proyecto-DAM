package sv.edu.itca.proyecto_dam;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Actividad de registro de usuario usando API REST
 */
public class RegisterActivity extends AppCompatActivity {

    // Componentes de la UI
    private TextInputEditText etFullName, etLastName, etEmail, etPassword, etConfirmPassword;
    private EditText etPhone, etBiography;
    private MaterialButton btnRegister;
    private TextView tvSignInLink;
    private ProgressBar progressBar;
    private ImageView ivCameraIcon, ivProfilePhoto;

    private Uri selectedImageUri;

    // Launcher para solicitar permisos
    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    // Permiso concedido, abrir galería
                    openGallery();
                } else {
                    // Permiso denegado, mostrar mensaje
                    showPermissionDeniedDialog();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        initializeViews();
        setupClickListeners();
    }


    /**
     * Inicializa las vistas de la UI
     */
    private void initializeViews() {
        etFullName = findViewById(R.id.etFullName);
        etLastName = findViewById(R.id.etApellido);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        etPhone = findViewById(R.id.etPhoneNumber);
        etBiography = findViewById(R.id.etBiography);
        btnRegister = findViewById(R.id.btnRegister);
        tvSignInLink = findViewById(R.id.tvSignInLink);
        progressBar = findViewById(R.id.progressBar);
        ivCameraIcon = findViewById(R.id.ivCameraIcon);
        ivProfilePhoto = findViewById(R.id.ivProfilePhoto);
    }

    /**
     * Configura los listeners de los elementos clickeables
     */
    private void setupClickListeners() {
        btnRegister.setOnClickListener(v -> registerUser());
        tvSignInLink.setOnClickListener(v -> navigateToLogin());
        ivCameraIcon.setOnClickListener(v -> checkPermissionsAndOpenGallery());

        // También permitir click en el FrameLayout completo
        findViewById(R.id.flProfilePhoto).setOnClickListener(v -> checkPermissionsAndOpenGallery());
    }

    /**
     * Registra un nuevo usuario usando la API
     */
    @SuppressLint("NewApi")
    private void registerUser() {
        String fullName = getText(etFullName);
        String lastName = getText(etLastName);
        String email = getText(etEmail);
        String password = getText(etPassword);
        String confirmPassword = getText(etConfirmPassword);
        String phone = getText(etPhone);
        String biography = getText(etBiography);

        // Validar los datos de entrada
        if (!validateInputs(fullName, lastName, email, password, confirmPassword)) {
            return;
        }

        showLoading(true);

        // Enviar datos a la API
        sendUserDataToApi(fullName, lastName, email, password, phone, selectedImageUri, biography);
    }

    /**
     * Valida los datos de entrada del formulario
     */
    private boolean validateInputs(String fullName, String lastName, String email, String password, String confirmPassword) {
        // Validar nombre completo
        if (TextUtils.isEmpty(fullName)) {
            etFullName.setError("El nombre es obligatorio");
            etFullName.requestFocus();
            return false;
        }

        // Validar apellido
        if (TextUtils.isEmpty(lastName)) {
            etLastName.setError("El apellido es obligatorio");
            etLastName.requestFocus();
            return false;
        }

        // Validar email
        if (TextUtils.isEmpty(email)) {
            etEmail.setError(getString(R.string.error_email_required));
            etEmail.requestFocus();
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError(getString(R.string.error_invalid_email));
            etEmail.requestFocus();
            return false;
        }

        // Validar contraseña
        if (TextUtils.isEmpty(password)) {
            etPassword.setError(getString(R.string.error_password_required));
            etPassword.requestFocus();
            return false;
        }

        if (password.length() < 6) {
            etPassword.setError(getString(R.string.error_short_password));
            etPassword.requestFocus();
            return false;
        }

        // Validar confirmación de contraseña
        if (TextUtils.isEmpty(confirmPassword)) {
            etConfirmPassword.setError("Confirma tu contraseña");
            etConfirmPassword.requestFocus();
            return false;
        }

        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError(getString(R.string.passwords_dont_match));
            etConfirmPassword.requestFocus();
            return false;
        }

        return true;
    }

    /**
     * Maneja los errores de registro
     */
    private void handleRegistrationError(Exception exception) {
        if (exception != null) {
            String errorMessage = exception.getMessage();
            if (errorMessage != null) {
                if (errorMessage.contains("email address is already in use")) {
                    showToast(getString(R.string.email_already_in_use));
                } else if (errorMessage.contains("weak password")) {
                    showToast(getString(R.string.weak_password));
                } else if (errorMessage.contains("network error")) {
                    showToast(getString(R.string.network_error));
                } else {
                    showToast(getString(R.string.registration_failed));
                }
            } else {
                showToast(getString(R.string.registration_failed));
            }
        }
    }

    /**
     * Navega a la actividad de login
     */
    private void navigateToLogin() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    /**
     * Muestra u oculta el indicador de carga
     */
    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnRegister.setEnabled(!isLoading);
    }

    /**
     * Obtiene el texto de un EditText de forma segura
     */
    private String getText(TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }

    /**
     * Obtiene el texto de un EditText de forma segura
     */
    private String getText(EditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }

    /**
     * Muestra un mensaje Toast
     */
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * Envía los datos del usuario a la API
     */
    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    private void sendUserDataToApi(String fullName, String lastName, String email, String password, String phone, Uri profileImageUri, String biography) {
        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient();

                // Crear el cuerpo de la solicitud
                MultipartBody.Builder builder = new MultipartBody.Builder().setType(MultipartBody.FORM);
                builder.addFormDataPart("nombre", fullName);
                builder.addFormDataPart("apellido", lastName);
                builder.addFormDataPart("email", email);
                builder.addFormDataPart("password", password);
                builder.addFormDataPart("telefono", phone != null && !phone.isEmpty() ? phone : "");
                builder.addFormDataPart("biografia", biography != null && !biography.isEmpty() ? biography : "");

                if (profileImageUri != null) {
                    File file = new File(getCacheDir(), "profile_image.jpg");
                    try (InputStream inputStream = getContentResolver().openInputStream(profileImageUri);
                         OutputStream os = new FileOutputStream(file)) {
                        if (inputStream != null) {
                            inputStream.transferTo(os);
                        }
                    }
                    builder.addFormDataPart("fotoPerfil", file.getName(), RequestBody.create(MediaType.parse("image/jpeg"), file));
                }

                RequestBody requestBody = builder.build();

                // Crear la solicitud HTTP
                Request request = new Request.Builder()
                        .url("http://172.193.118.141:8080/api/usuarios/save")
                        .post(requestBody)
                        .build();

                // Enviar la solicitud
                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        // Obtener el ID del usuario recién creado
                        String responseBody = response.body() != null ? response.body().string() : "";
                        android.util.Log.d("RegisterActivity", "Usuario guardado exitosamente. Response: " + responseBody);

                        // Intentar parsear el ID del usuario de la respuesta
                        try {
                            // La respuesta es: "Usuario guardado exitosamente con ID: X"
                            if (responseBody.contains("ID:")) {
                                String[] parts = responseBody.split("ID:");
                                if (parts.length > 1) {
                                    String idStr = parts[1].trim();
                                    int userId = Integer.parseInt(idStr);

                                    // Guardar el ID en SharedPreferences
                                    android.content.SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
                                    prefs.edit()
                                        .putInt("userId", userId)
                                        .putString("userEmail", email)
                                        .putString("userName", fullName)
                                        .apply();
                                    android.util.Log.d("RegisterActivity", "userId guardado en SharedPreferences: " + userId);
                                }
                            }
                        } catch (Exception e) {
                            android.util.Log.e("RegisterActivity", "Error parseando respuesta: " + e.getMessage());
                        }

                        runOnUiThread(() -> {
                            showLoading(false);
                            showToast("Registro exitoso. Por favor inicia sesión.");
                            navigateToLogin();
                        });
                    } else {
                        String errorBody = response.body() != null ? response.body().string() : "Sin respuesta";
                        android.util.Log.e("RegisterActivity", "Error al guardar en API - Código: " + response.code() + ", Body: " + errorBody);
                        runOnUiThread(() -> {
                            showLoading(false);
                            showToast("Error al guardar en la API: " + response.message());
                        });
                    }
                }
            } catch (Exception e) {
                android.util.Log.e("RegisterActivity", "Error en sendUserDataToApi: " + e.getMessage());
                e.printStackTrace();
                runOnUiThread(() -> {
                    showLoading(false);
                    showToast("Error: " + e.getMessage());
                });
            }
        }).start();
    }

    /**
     * Abre la galería para seleccionar una imagen
     */
    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null && result.getData().getData() != null) {
                    selectedImageUri = result.getData().getData();

                    // Mostrar previsualización circular usando Picasso
                    Picasso.get()
                        .load(selectedImageUri)
                        .transform(new CircularTransformation())
                        .placeholder(R.drawable.ic_profile_placeholder)
                        .error(R.drawable.ic_profile_placeholder)
                        .into(ivProfilePhoto);

                    // Ocultar el ícono de cámara después de seleccionar imagen
                    ivCameraIcon.setVisibility(View.GONE);
                }
            }
    );

    /**
     * Verifica permisos y abre la galería
     */
    private void checkPermissionsAndOpenGallery() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ usa READ_MEDIA_IMAGES
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    == PackageManager.PERMISSION_GRANTED) {
                openGallery();
            } else {
                // Solicitar permiso
                requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES);
            }
        } else {
            // Android 12 y anteriores usan READ_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                openGallery();
            } else {
                // Solicitar permiso
                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }
    }

    /**
     * Muestra un diálogo cuando el permiso es denegado
     */
    private void showPermissionDeniedDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Permiso necesario")
                .setMessage("Para seleccionar una foto de perfil, necesitamos acceso a tus imágenes. " +
                        "Por favor, concede el permiso en la configuración de la aplicación.")
                .setPositiveButton("Configuración", (dialog, which) -> {
                    // Abrir configuración de la app
                    Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        galleryLauncher.launch(intent);
    }
}
