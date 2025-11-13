package sv.edu.itca.proyecto_dam;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONObject;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Actividad principal que maneja el login de usuarios usando API REST
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    // Componentes de la UI
    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnLogin;
    private TextView tvForgotPassword, tvSignUpLink;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        initializeViews();
        setupClickListeners();
        setupWindowInsets();
        checkIfUserLoggedIn();
    }

    /**
     * Inicializa las vistas de la UI
     */
    private void initializeViews() {
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        tvSignUpLink = findViewById(R.id.tvSignUpLink);
        progressBar = findViewById(R.id.progressBar);

        // Ocultar botones de Google y GitHub si existen
        LinearLayout llGoogleLogin = findViewById(R.id.llGoogleLogin);
        LinearLayout llGitHubLogin = findViewById(R.id.llGitHubLogin);
        if (llGoogleLogin != null) llGoogleLogin.setVisibility(View.GONE);
        if (llGitHubLogin != null) llGitHubLogin.setVisibility(View.GONE);
    }

    /**
     * Configura los listeners de los elementos clickeables
     */
    private void setupClickListeners() {
        btnLogin.setOnClickListener(v -> loginUser());
        tvForgotPassword.setOnClickListener(v -> showToast("Funcionalidad de recuperación de contraseña próximamente"));
        tvSignUpLink.setOnClickListener(v -> navigateToRegister());
    }

    /**
     * Configura window insets de forma segura
     */
    private void setupWindowInsets() {
        View mainView = findViewById(R.id.main);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }
    }

    /**
     * Verifica si hay un usuario ya logueado
     */
    private void checkIfUserLoggedIn() {
        SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
        int userId = prefs.getInt("userId", -1);
        if (userId != -1) {
            // Usuario ya está logueado, navegar a la pantalla principal
            navigateToHome();
        }
    }

    /**
     * Inicia sesión del usuario usando la API
     */
    private void loginUser() {
        String email = getText(etEmail);
        String password = getText(etPassword);

        // Validar los datos de entrada
        if (!validateLoginInputs(email, password)) {
            return;
        }

        showLoading(true);

        // Autenticar usuario con la API
        authenticateWithAPI(email, password);
    }

    /**
     * Autentica al usuario con la API REST
     */
    private void authenticateWithAPI(String email, String password) {
        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient();
                String url = "http://172.193.118.141:8080/api/usuarios/all";

                Request request = new Request.Builder()
                        .url(url)
                        .get()
                        .build();

                Response response = client.newCall(request).execute();

                if (response.isSuccessful() && response.body() != null) {
                    String responseData = response.body().string();
                    Log.d(TAG, "Respuesta completa de la API: " + responseData);
                    
                    JSONArray jsonArray = new JSONArray(responseData);

                    boolean loginSuccess = false;
                    int userId = -1;
                    String userName = "";

                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject userObj = jsonArray.getJSONObject(i);

                        // Log del objeto completo para debugging
                        Log.d(TAG, "Usuario " + i + ": " + userObj.toString());

                        String userEmail = userObj.optString("email", userObj.optString("correo", ""));
                        String userPassword = userObj.optString("password", userObj.optString("contrasena", ""));

                        if (userEmail.equalsIgnoreCase(email) && userPassword.equals(password)) {
                            loginSuccess = true;

                            // Intentar diferentes nombres de campo para el ID
                            if (userObj.has("idUsuario")) {
                                userId = userObj.getInt("idUsuario");
                            } else if (userObj.has("id")) {
                                userId = userObj.getInt("id");
                            } else if (userObj.has("id_usuario")) {
                                userId = userObj.getInt("id_usuario");
                            }

                            userName = userObj.optString("nombre", "");

                            Log.d(TAG, "Usuario encontrado - ID: " + userId + ", Email: " + userEmail + ", Nombre: " + userName);
                            break;
                        }
                    }

                    if (loginSuccess && userId != -1) {
                        // Guardar datos en SharedPreferences
                        SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
                        prefs.edit()
                            .putInt("userId", userId)
                            .putString("userEmail", email)
                            .putString("userName", userName)
                            .apply();

                        Log.d(TAG, "Login exitoso. userId: " + userId);

                        runOnUiThread(() -> {
                            showLoading(false);
                            showToast("Inicio de sesión exitoso");
                            navigateToHome();
                        });
                    } else {
                        runOnUiThread(() -> {
                            showLoading(false);
                            showToast("Email o contraseña incorrectos");
                        });
                    }
                } else {
                    runOnUiThread(() -> {
                        showLoading(false);
                        showToast("Error al conectar con el servidor");
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Error en autenticación: " + e.getMessage());
                runOnUiThread(() -> {
                    showLoading(false);
                    showToast("Error: " + e.getMessage());
                });
            }
        }).start();
    }

    /**
     * Valida los datos de entrada del formulario de login
     */
    private boolean validateLoginInputs(String email, String password) {
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

        return true;
    }

    /**
     * Navega a la actividad de registro
     */
    private void navigateToRegister() {
        Intent intent = new Intent(this, RegisterActivity.class);
        startActivity(intent);
    }

    /**
     * Navega a la pantalla principal
     */
    private void navigateToHome() {
        Intent intent = new Intent(this, Home2Activity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * Muestra u oculta el indicador de carga
     */
    private void showLoading(boolean isLoading) {
        if (progressBar != null) {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
        btnLogin.setEnabled(!isLoading);
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
}

