package sv.edu.itca.proyecto_dam;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

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
    private TextInputEditText etFullName, etEmail, etPassword, etConfirmPassword;
    private MaterialButton btnRegister;
    private TextView tvSignInLink;
    private ProgressBar progressBar;
    private ImageView ivCameraIcon, ivProfilePhoto;

    private Uri selectedImageUri;

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
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
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
        ivCameraIcon.setOnClickListener(v -> openGallery());
    }

    /**
     * Registra un nuevo usuario usando la API
     */
    @SuppressLint("NewApi")
    private void registerUser() {
        String fullName = getText(etFullName);
        String lastName = "Apellido"; // Aquí puedes obtener el apellido del usuario si se agrega un campo en la UI
        String email = getText(etEmail);
        String password = getText(etPassword);
        String confirmPassword = getText(etConfirmPassword);
        String biography = "Esta es mi biografía"; // Aquí puedes obtener la biografía del usuario si se agrega un campo en la UI

        // Validar los datos de entrada
        if (!validateInputs(fullName, email, password, confirmPassword)) {
            return;
        }

        showLoading(true);

        // Enviar datos a la API
        sendUserDataToApi(fullName, lastName, email, password, selectedImageUri, biography);
    }

    /**
     * Valida los datos de entrada del formulario
     */
    private boolean validateInputs(String fullName, String email, String password, String confirmPassword) {
        // Validar nombre completo
        if (TextUtils.isEmpty(fullName)) {
            etFullName.setError("El nombre es obligatorio");
            etFullName.requestFocus();
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
     * Muestra un mensaje Toast
     */
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * Envía los datos del usuario a la API
     */
    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    private void sendUserDataToApi(String fullName, String lastName, String email, String password, Uri profileImageUri, String biography) {
        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient();

                // Crear el cuerpo de la solicitud
                MultipartBody.Builder builder = new MultipartBody.Builder().setType(MultipartBody.FORM);
                builder.addFormDataPart("nombre", fullName);
                builder.addFormDataPart("apellido", lastName);
                builder.addFormDataPart("email", email);
                builder.addFormDataPart("password", password);
                builder.addFormDataPart("biografia", biography); // Agregar biografía

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
                            org.json.JSONObject jsonResponse = new org.json.JSONObject(responseBody);
                            int userId = -1;

                            // Intentar diferentes nombres de campo para el ID
                            if (jsonResponse.has("idUsuario")) {
                                userId = jsonResponse.getInt("idUsuario");
                            } else if (jsonResponse.has("id")) {
                                userId = jsonResponse.getInt("id");
                            } else if (jsonResponse.has("id_usuario")) {
                                userId = jsonResponse.getInt("id_usuario");
                            }

                            if (userId != -1) {
                                // Guardar el ID en SharedPreferences
                                android.content.SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
                                prefs.edit()
                                    .putInt("userId", userId)
                                    .putString("userEmail", email)
                                    .putString("userName", fullName)
                                    .apply();
                                android.util.Log.d("RegisterActivity", "userId guardado en SharedPreferences: " + userId);
                            } else {
                                android.util.Log.w("RegisterActivity", "No se pudo obtener el ID del usuario de la respuesta");
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
                    ivProfilePhoto.setImageURI(selectedImageUri);
                }
            }
    );

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        galleryLauncher.launch(intent);
    }
}
