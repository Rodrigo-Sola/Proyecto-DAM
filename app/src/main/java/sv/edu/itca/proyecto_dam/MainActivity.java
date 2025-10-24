package sv.edu.itca.proyecto_dam;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.OAuthProvider;

/**
 * Actividad principal que maneja el login de usuarios
 * Incluye autenticación con Firebase y verificación de correo electrónico
 */
public class MainActivity extends AppCompatActivity {

    // Componentes de la UI
    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnLogin;
    private TextView tvForgotPassword, tvSignUpLink;
    private LinearLayout llGithubLogin;

    // Firebase Authentication
    private FirebaseAuth firebaseAuth;

    // Constantes para logging
    private static final String TAG = "MainActivity";
    private static final String GITHUB_PROVIDER_ID = "github.com";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        initializeFirebase();
        initializeViews();
        setupClickListeners();
        setupWindowInsets();
        checkIfUserLoggedIn();
    }

    /**
     * Inicializa Firebase Authentication
     */
    private void initializeFirebase() {
        firebaseAuth = FirebaseAuth.getInstance();
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
        llGithubLogin = findViewById(R.id.llGithubLogin);
    }

    /**
     * Configura los listeners de los elementos clickeables
     */
    private void setupClickListeners() {
        btnLogin.setOnClickListener(v -> loginUser());
        tvForgotPassword.setOnClickListener(v -> showForgotPasswordDialog());
        tvSignUpLink.setOnClickListener(v -> navigateToRegister());
        llGithubLogin.setOnClickListener(v -> loginWithGithub());
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
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            // Usuario ya está logueado, navegar a la pantalla principal
            navigateToHome();
        }
    }

    /**
     * Inicia sesión del usuario
     */
    private void loginUser() {
        String email = getText(etEmail);
        String password = getText(etPassword);

        // Validar los datos de entrada
        if (!validateLoginInputs(email, password)) {
            return;
        }

        showLoading(true);

        // Autenticar usuario con Firebase
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    showLoading(false);

                    if (task.isSuccessful()) {
                        // Login exitoso
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        if (user != null) {
                            checkEmailVerification(user);
                        }
                    } else {
                        // Error en el login
                        handleLoginError(task.getException());
                    }
                });
    }

    /**
     * Verifica si el correo del usuario ha sido verificado
     */
    private void checkEmailVerification(FirebaseUser user) {
        if (user.isEmailVerified()) {
            // Email verificado, proceder al home
            showToast(getString(R.string.login_success));
            navigateToHome();
        } else {
            // Email no verificado, mostrar diálogo de verificación
            showEmailVerificationDialog(user);
        }
    }

    /**
     * Muestra un diálogo para manejar la verificación de correo
     */
    private void showEmailVerificationDialog(FirebaseUser user) {
        new AlertDialog.Builder(this)
                .setTitle("Verificación de Correo")
                .setMessage(getString(R.string.email_not_verified))
                .setPositiveButton(getString(R.string.resend_verification), (dialog, which) -> {
                    sendVerificationEmail(user);
                })
                .setNegativeButton("Más tarde", (dialog, which) -> {
                    // Permitir acceso pero mostrar advertencia
                    navigateToHome();
                })
                .setNeutralButton("Cerrar Sesión", (dialog, which) -> {
                    firebaseAuth.signOut();
                })
                .show();
    }

    /**
     * Envía correo de verificación
     */
    private void sendVerificationEmail(FirebaseUser user) {
        user.sendEmailVerification()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        showToast(getString(R.string.verification_email_sent));
                    } else {
                        showToast("Error al enviar correo de verificación");
                    }
                });
    }

    /**
     * Muestra diálogo para recuperar contraseña
     */
    private void showForgotPasswordDialog() {
        // Crear EditText para el email
        TextInputEditText etResetEmail = new TextInputEditText(this);
        etResetEmail.setHint(getString(R.string.hint_email));
        etResetEmail.setText(getText(etEmail)); // Pre-llenar con el email actual si existe

        new AlertDialog.Builder(this)
                .setTitle("Recuperar Contraseña")
                .setMessage("Ingresa tu correo electrónico para recibir un enlace de restablecimiento")
                .setView(etResetEmail)
                .setPositiveButton("Enviar", (dialog, which) -> {
                    String email = etResetEmail.getText() != null ?
                        etResetEmail.getText().toString().trim() : "";
                    resetPassword(email);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    /**
     * Envía enlace de restablecimiento de contraseña
     */
    private void resetPassword(String email) {
        if (TextUtils.isEmpty(email)) {
            showToast(getString(R.string.error_email_required));
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showToast(getString(R.string.error_invalid_email));
            return;
        }

        firebaseAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        showToast(getString(R.string.reset_password_sent));
                    } else {
                        showToast(getString(R.string.reset_password_failed));
                    }
                });
    }

    /**
     * Valida los datos de entrada del login
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
     * Maneja los errores de login
     */
    private void handleLoginError(Exception exception) {
        if (exception != null) {
            String errorMessage = exception.getMessage();
            if (errorMessage != null) {
                if (errorMessage.contains("user not found")) {
                    showToast(getString(R.string.user_not_found));
                } else if (errorMessage.contains("wrong password") ||
                          errorMessage.contains("invalid credential")) {
                    showToast(getString(R.string.invalid_credentials));
                } else if (errorMessage.contains("network error")) {
                    showToast(getString(R.string.network_error));
                } else {
                    showToast(getString(R.string.login_failed));
                }
            } else {
                showToast(getString(R.string.login_failed));
            }
        }
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
        Intent intent = new Intent(this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * Muestra u oculta el indicador de carga
     */
    private void showLoading(boolean isLoading) {
        btnLogin.setEnabled(!isLoading);
        btnLogin.setText(isLoading ? "Iniciando sesión..." : getString(R.string.btn_login));
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
     * Inicia sesión con GitHub
     */
    private void loginWithGithub() {
        // Configurar proveedor de GitHub
        OAuthProvider.Builder provider = OAuthProvider.newBuilder(GITHUB_PROVIDER_ID);

        // Iniciar el flujo de inicio de sesión
        firebaseAuth.startActivityForSignInWithProvider(this, provider.build())
                .addOnSuccessListener(this, authResult -> {
                    // Login exitoso
                    FirebaseUser user = authResult.getUser();
                    if (user != null) {
                        checkEmailVerification(user);
                    }
                })
                .addOnFailureListener(this, e -> {
                    // Manejar errores
                    Log.e(TAG, "Error en login con GitHub: " + e.getMessage());
                    showToast("Error al iniciar sesión con GitHub");
                });
    }
}