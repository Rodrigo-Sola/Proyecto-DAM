package sv.edu.itca.proyecto_dam;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

/**
 * Actividad de registro de usuario con verificación de correo electrónico
 * Implementa las mejores prácticas de Firebase Authentication
 */
public class RegisterActivity extends AppCompatActivity {

    // Componentes de la UI
    private TextInputEditText etFullName, etEmail, etPassword, etConfirmPassword;
    private MaterialButton btnRegister;
    private TextView tvSignInLink;
    private ProgressBar progressBar;

    // Firebase Authentication
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        initializeFirebase();
        initializeViews();
        setupClickListeners();
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
        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);
        tvSignInLink = findViewById(R.id.tvSignInLink);
        progressBar = findViewById(R.id.progressBar);
    }

    /**
     * Configura los listeners de los elementos clickeables
     */
    private void setupClickListeners() {
        btnRegister.setOnClickListener(v -> registerUser());
        tvSignInLink.setOnClickListener(v -> navigateToLogin());
    }

    /**
     * Registra un nuevo usuario en Firebase
     */
    private void registerUser() {
        String fullName = getText(etFullName);
        String email = getText(etEmail);
        String password = getText(etPassword);
        String confirmPassword = getText(etConfirmPassword);

        // Validar los datos de entrada
        if (!validateInputs(fullName, email, password, confirmPassword)) {
            return;
        }

        showLoading(true);

        // Crear usuario en Firebase
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Registro exitoso
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        if (user != null) {
                            updateUserProfile(user, fullName);
                            sendEmailVerification(user);
                        }
                    } else {
                        // Error en el registro
                        showLoading(false);
                        handleRegistrationError(task.getException());
                    }
                });
    }

    /**
     * Actualiza el perfil del usuario con su nombre completo
     */
    private void updateUserProfile(FirebaseUser user, String fullName) {
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(fullName)
                .build();

        user.updateProfile(profileUpdates)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Perfil actualizado exitosamente
                        showToast(getString(R.string.registration_success));
                    }
                });
    }

    /**
     * Envía el correo de verificación al usuario
     */
    private void sendEmailVerification(FirebaseUser user) {
        user.sendEmailVerification()
                .addOnCompleteListener(task -> {
                    showLoading(false);
                    if (task.isSuccessful()) {
                        showToast(getString(R.string.verification_email_sent));
                        // Cerrar sesión para que el usuario verifique su correo
                        firebaseAuth.signOut();
                        navigateToLogin();
                    } else {
                        showToast(getString(R.string.registration_failed));
                    }
                });
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
}
