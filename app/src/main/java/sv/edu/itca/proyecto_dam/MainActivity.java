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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;

import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.OAuthProvider;

/**
 * Actividad principal que maneja el login de usuarios
 * Incluye autenticación con Firebase, Google y GitHub
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    // Componentes de la UI
    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnLogin;
    private TextView tvForgotPassword, tvSignUpLink;
    private LinearLayout llGoogleLogin, llGitHubLogin;

    // Firebase Authentication
    private FirebaseAuth firebaseAuth;

    // Credential Manager para Google Sign-In
    private CredentialManager credentialManager;

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
        credentialManager = CredentialManager.create(this);
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
        llGitHubLogin = findViewById(R.id.llGitHubLogin);
    }

    /**
     * Configura los listeners de los elementos clickeables
     */
    private void setupClickListeners() {
        btnLogin.setOnClickListener(v -> loginUser());
        tvForgotPassword.setOnClickListener(v -> showForgotPasswordDialog());
        tvSignUpLink.setOnClickListener(v -> navigateToRegister());

        // Autenticación con Google
        llGoogleLogin.setOnClickListener(v -> signInWithGoogle());

        // Autenticación con GitHub
        llGitHubLogin.setOnClickListener(v -> signInWithGithub());
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
     * Inicia el flujo de autenticación con Google usando Credential Manager
     */
    private void signInWithGoogle() {
        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(getString(R.string.default_web_client_id))
                .setAutoSelectEnabled(true)
                .build();

        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build();

        credentialManager.getCredentialAsync(
                this,
                request,
                null,
                getMainExecutor(),
                new androidx.credentials.CredentialManagerCallback<GetCredentialResponse, androidx.credentials.exceptions.GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse result) {
                        handleGoogleSignIn(result);
                    }

                    @Override
                    public void onError(androidx.credentials.exceptions.GetCredentialException e) {
                        Log.e(TAG, "Error getting credential", e);
                        String errorMsg = "Error al iniciar sesión con Google";
                        if (e.getMessage() != null && e.getMessage().contains("No credentials available")) {
                            errorMsg = "No hay cuentas de Google disponibles. Por favor, añade una cuenta primero.";
                        } else if (e.getMessage() != null) {
                            errorMsg = "Error: " + e.getMessage();
                        }
                        showToast(errorMsg);
                    }
                }
        );
    }

    /**
     * Maneja el resultado de la autenticación con Google
     */
    private void handleGoogleSignIn(GetCredentialResponse result) {
        Credential credential = result.getCredential();

        Log.d(TAG, "Credential type: " + credential.getType());

        // Verificar si es una credencial de Google ID Token
        if (GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL.equals(credential.getType())) {
            try {
                GoogleIdTokenCredential googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.getData());
                String idToken = googleIdTokenCredential.getIdToken();

                Log.d(TAG, "Google ID Token obtenido correctamente");

                // Autenticar con Firebase usando el token de Google
                firebaseAuthWithGoogle(idToken);
            } catch (Exception e) {
                Log.e(TAG, "Error al crear GoogleIdTokenCredential", e);
                showToast("Error al procesar las credenciales: " + e.getMessage());
            }
        } else if (credential instanceof GoogleIdTokenCredential) {
            // Fallback para el método anterior
            GoogleIdTokenCredential googleIdTokenCredential = (GoogleIdTokenCredential) credential;
            String idToken = googleIdTokenCredential.getIdToken();

            Log.d(TAG, "Google ID Token obtenido (método alternativo)");
            firebaseAuthWithGoogle(idToken);
        } else {
            Log.e(TAG, "Tipo de credencial no soportado: " + credential.getType());
            showToast("Tipo de credencial no compatible. Por favor, intenta nuevamente.");
        }
    }

    /**
     * Autentica con Firebase usando el token de Google
     */
    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);

        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "signInWithCredential:success");
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        if (user != null) {
                            showToast("Bienvenido " + user.getDisplayName());
                            navigateToHome();
                        }
                    } else {
                        Log.w(TAG, "signInWithCredential:failure", task.getException());
                        showToast("Error en autenticación: " +
                                (task.getException() != null ? task.getException().getMessage() : "Desconocido"));
                    }
                });
    }

    /**
     * Inicia el flujo de autenticación con GitHub
     */
    private void signInWithGithub() {
        OAuthProvider.Builder provider = OAuthProvider.newBuilder("github.com");

        // Opcional: solicitar scopes adicionales
        // provider.setScopes(Arrays.asList("user:email"));

        firebaseAuth.startActivityForSignInWithProvider(this, provider.build())
                .addOnSuccessListener(authResult -> {
                    Log.d(TAG, "signInWithGitHub:success");
                    FirebaseUser user = firebaseAuth.getCurrentUser();
                    if (user != null) {
                        showToast("Bienvenido " + user.getDisplayName());
                        navigateToHome();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "signInWithGitHub:failure", e);
                    showToast("Error al iniciar sesión con GitHub: " + e.getMessage());
                });
    }
}
