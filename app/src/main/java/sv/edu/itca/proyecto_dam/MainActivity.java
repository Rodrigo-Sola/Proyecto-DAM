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
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;

import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Actividad principal que maneja el login de usuarios
 * Incluye autenticación con Firebase (email/password y Google Sign-In)
 * y verificación de correo electrónico
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    // Componentes de la UI
    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnLogin;
    private TextView tvForgotPassword, tvSignUpLink;
    private LinearLayout llGoogleLogin;

    // Firebase Authentication
    private FirebaseAuth firebaseAuth;

    // Credential Manager para Google Sign-In
    private CredentialManager credentialManager;
    private Executor executor;

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
        initializeGoogleSignIn();
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
        llGoogleLogin = findViewById(R.id.llGoogleLogin);
    }

    /**
     * Configura los listeners de los elementos clickeables
     */
    private void setupClickListeners() {
        btnLogin.setOnClickListener(v -> loginUser());
        tvForgotPassword.setOnClickListener(v -> showForgotPasswordDialog());
        tvSignUpLink.setOnClickListener(v -> navigateToRegister());
        llGoogleLogin.setOnClickListener(v -> googleSignIn());
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
                .setPositiveButton(getString(R.string.resend_verification), (dialog, which) ->
                    sendVerificationEmail(user))
                .setNegativeButton("Más tarde", (dialog, which) -> navigateToHome())
                .setNeutralButton("Cerrar Sesión", (dialog, which) -> firebaseAuth.signOut())
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
     * Inicializa Google Sign-In con Credential Manager
     */
    private void initializeGoogleSignIn() {
        credentialManager = CredentialManager.create(this);
        executor = Executors.newSingleThreadExecutor();
    }

    /**
     * Inicia el flujo de autenticación con Google
     */
    private void googleSignIn() {
        try {
            // Configurar opciones de Google ID
            GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false) // Permitir todas las cuentas, no solo las autorizadas
                    .setServerClientId(getString(R.string.default_web_client_id))
                    .setAutoSelectEnabled(false) // No autoseleccionar
                    .setNonce(null) // Opcional: agregar nonce para seguridad adicional
                    .build();

            // Crear request de credenciales
            GetCredentialRequest request = new GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build();

            // Mostrar mensaje al usuario
            showToast("Iniciando sesión con Google...");

            Log.d(TAG, "Iniciando Google Sign-In con Web Client ID: " + getString(R.string.default_web_client_id));

            // Solicitar credenciales de forma asíncrona
            credentialManager.getCredentialAsync(
                    this,
                    request,
                    null,
                    executor,
                    new CredentialManagerCallback<>() {
                        @Override
                        public void onResult(GetCredentialResponse result) {
                            Log.d(TAG, "Google Sign-In exitoso, procesando resultado");
                            handleGoogleSignInResult(result);
                        }

                        @Override
                        public void onError(@NonNull GetCredentialException e) {
                            Log.e(TAG, "Error en getCredentialAsync: " + e.getClass().getName() + " - " + e.getMessage(), e);
                            runOnUiThread(() -> handleGoogleSignInError(e));
                        }
                    }
            );

        } catch (Exception e) {
            Log.e(TAG, "Error al iniciar Google Sign-In", e);
            showToast("Error al iniciar sesión con Google: " + e.getMessage());
        }
    }

    /**
     * Maneja el resultado exitoso de Google Sign-In
     */
    private void handleGoogleSignInResult(GetCredentialResponse result) {
        Credential credential = result.getCredential();
        Log.d(TAG, "Tipo de credencial recibida: " + credential.getType());

        if (credential instanceof CustomCredential) {
            CustomCredential customCredential = (CustomCredential) credential;
            if (GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL.equals(customCredential.getType())) {
                try {
                    // Extraer el Google ID Token
                    GoogleIdTokenCredential googleIdTokenCredential =
                            GoogleIdTokenCredential.createFrom(customCredential.getData());

                    String idToken = googleIdTokenCredential.getIdToken();
                    Log.d(TAG, "Google ID Token obtenido exitosamente");

                    // Autenticar con Firebase en el hilo principal
                    runOnUiThread(() -> firebaseAuthWithGoogle(idToken));

                } catch (Exception e) {
                    Log.e(TAG, "Error al parsear Google ID Token", e);
                    runOnUiThread(() -> showToast("Error al procesar credenciales de Google"));
                }
            } else {
                Log.e(TAG, "Tipo de credencial no esperado: " + customCredential.getType());
                runOnUiThread(() -> showToast("Tipo de credencial no válido"));
            }
        } else {
            Log.e(TAG, "Credencial no es de tipo CustomCredential");
            runOnUiThread(() -> showToast("Error en el tipo de credencial"));
        }
    }

    /**
     * Maneja los errores de Google Sign-In
     */
    private void handleGoogleSignInError(GetCredentialException e) {
        Log.e(TAG, "Error en Google Sign-In", e);

        String errorMessage;
        String errorType = e.getClass().getSimpleName();

        Log.d(TAG, "Tipo de error: " + errorType);
        Log.d(TAG, "Mensaje de error: " + e.getMessage());

        // Manejar diferentes tipos de errores específicos
        if (e.getType() != null) {
            Log.d(TAG, "Error type: " + e.getType());
        }

        if (e.getMessage() != null) {
            String message = e.getMessage().toLowerCase();

            if (message.contains("no credentials available") || message.contains("no credential")) {
                errorMessage = "No hay cuentas de Google configuradas en este dispositivo.\n\n" +
                        "Por favor:\n" +
                        "1. Ve a Configuración → Cuentas\n" +
                        "2. Agrega una cuenta de Google\n" +
                        "3. Intenta nuevamente";

                // Mostrar un diálogo más informativo
                runOnUiThread(() -> showGoogleSignInErrorDialog(errorMessage));
                return;

            } else if (message.contains("canceled") || message.contains("cancelled")) {
                errorMessage = "Inicio de sesión cancelado";

            } else if (message.contains("network")) {
                errorMessage = "Error de conexión. Verifica tu internet";

            } else if (message.contains("16:")) {
                // Error de configuración de Google Sign-In
                errorMessage = "Error de configuración.\n" +
                        "Verifica que Google Sign-In esté habilitado en Firebase Console";

            } else {
                errorMessage = "Error al iniciar sesión con Google: " + e.getMessage();
            }
        } else {
            errorMessage = "Error desconocido al iniciar sesión con Google";
        }

        showToast(errorMessage);
    }

    /**
     * Muestra un diálogo detallado de error de Google Sign-In
     */
    private void showGoogleSignInErrorDialog(String message) {
        new AlertDialog.Builder(this)
                .setTitle("Error de Google Sign-In")
                .setMessage(message)
                .setPositiveButton("Entendido", null)
                .setNegativeButton("Configurar cuenta", (dialog, which) -> {
                    // Intentar abrir la configuración de cuentas
                    try {
                        Intent intent = new Intent(android.provider.Settings.ACTION_SYNC_SETTINGS);
                        startActivity(intent);
                    } catch (Exception e) {
                        showToast("No se pudo abrir la configuración de cuentas");
                    }
                })
                .show();
    }

    /**
     * Autentica con Firebase usando el token de Google
     */
    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);

        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Login exitoso con Google
                        Log.d(TAG, "signInWithCredential:success");
                        FirebaseUser user = firebaseAuth.getCurrentUser();

                        if (user != null) {
                            String displayName = user.getDisplayName() != null ? user.getDisplayName() : "";
                            showToast("¡Bienvenido " + displayName + "!");
                            navigateToHome();
                        }
                    } else {
                        // Error en autenticación con Firebase
                        Log.w(TAG, "signInWithCredential:failure", task.getException());
                        handleLoginError(task.getException());
                    }
                });
    }
}
