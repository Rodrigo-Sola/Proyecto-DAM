package sv.edu.itca.proyecto_dam;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Pantalla principal después del login exitoso
 * Muestra información del usuario y permite gestionar la verificación de correo
 */
public class HomeActivity extends AppCompatActivity {

    // Componentes de la UI
    private TextView tvUserEmail, tvUserName, tvEmailVerification;
    private MaterialCardView cardVerification;
    private MaterialButton btnResendVerification, btnRefreshUser, btnLogout;

    // Firebase Authentication
    private FirebaseAuth firebaseAuth;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        initializeFirebase();
        initializeViews();
        setupClickListeners();
        loadUserInformation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Actualizar información del usuario cuando la actividad regresa al foco
        refreshUserInfo();

    }

    /**
     * Inicializa Firebase Authentication
     */
    private void initializeFirebase() {
        firebaseAuth = FirebaseAuth.getInstance();
        currentUser = firebaseAuth.getCurrentUser();
    }

    /**
     * Inicializa las vistas de la UI
     */
    private void initializeViews() {
        tvUserEmail = findViewById(R.id.tvUserEmail);
        tvUserName = findViewById(R.id.tvUserName);
        tvEmailVerification = findViewById(R.id.tvEmailVerification);
        cardVerification = findViewById(R.id.cardVerification);
        btnResendVerification = findViewById(R.id.btnResendVerification);
        btnRefreshUser = findViewById(R.id.btnRefreshUser);
        btnLogout = findViewById(R.id.btnLogout);
    }

    /**
     * Configura los listeners de los elementos clickeables
     */
    private void setupClickListeners() {
        btnResendVerification.setOnClickListener(v -> resendVerificationEmail());
        btnRefreshUser.setOnClickListener(v -> openFormActivity());
        btnLogout.setOnClickListener(v -> logout());
    }

    private void openFormActivity() {
        Intent intent = new Intent(this, form.class);
        startActivity(intent);
        finish();
    }

    /**
     * Carga la información inicial del usuario
     */
    private void loadUserInformation() {
        if (currentUser != null) {
            Intent intent = new Intent(this, Home2Activity.class);
            startActivity(intent);
            finish();
            // Usuario Logueado

            updateUserUI();
        } else {
            // Si no hay usuario logueado, redirigir al login
            navigateToLogin();
        }
    }

    /**
     * Actualiza la información del usuario desde Firebase
     */
    private void refreshUserInfo() {
        if (currentUser != null) {
            currentUser.reload().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    currentUser = firebaseAuth.getCurrentUser();
                    updateUserUI();
                    showToast("Información actualizada");
                } else {
                    showToast("Error al actualizar información");
                }
            });
        }
    }

    /**
     * Actualiza la interfaz de usuario con la información del usuario
     */
    private void updateUserUI() {
        if (currentUser != null) {
            // Mostrar email del usuario
            tvUserEmail.setText(currentUser.getEmail());

            // Mostrar nombre del usuario
            String displayName = currentUser.getDisplayName();
            tvUserName.setText("Nombre: " + (displayName != null ? displayName : "No especificado"));

            // Verificar estado de verificación del email
            boolean isEmailVerified = currentUser.isEmailVerified();
            if (isEmailVerified) {
                tvEmailVerification.setText("Estado: Email verificado ✓");
                tvEmailVerification.setTextColor(getColor(android.R.color.holo_green_dark));
                cardVerification.setVisibility(View.GONE);
            } else {
                tvEmailVerification.setText("Estado: Email no verificado ⚠");
                tvEmailVerification.setTextColor(getColor(android.R.color.holo_orange_dark));
                cardVerification.setVisibility(View.VISIBLE);
            }
        }
    }

    /**
     * Reenvía el correo de verificación
     */
    private void resendVerificationEmail() {
        if (currentUser != null) {
            btnResendVerification.setEnabled(false);
            btnResendVerification.setText("Enviando...");

            currentUser.sendEmailVerification()
                    .addOnCompleteListener(task -> {
                        btnResendVerification.setEnabled(true);
                        btnResendVerification.setText(getString(R.string.resend_verification));

                        if (task.isSuccessful()) {
                            showToast(getString(R.string.verification_email_sent));
                        } else {
                            showToast("Error al enviar el correo de verificación");
                        }
                    });
        }
    }

    /**
     * Cierra la sesión del usuario
     */
    private void logout() {
        firebaseAuth.signOut();
        navigateToLogin();
    }

    /**
     * Navega a la actividad de login
     */
    private void navigateToLogin() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * Muestra un mensaje Toast
     */
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
