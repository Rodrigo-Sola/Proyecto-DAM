package sv.edu.itca.proyecto_dam;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

/**
 * Pantalla principal después del login exitoso
 * Muestra información del usuario usando API REST
 */
public class HomeActivity extends AppCompatActivity {

    // Componentes de la UI
    private TextView tvUserEmail, tvUserName, tvEmailVerification;
    private MaterialCardView cardVerification;
    private MaterialButton btnResendVerification, btnRefreshUser, btnLogout;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        initializeViews();
        setupClickListeners();
        loadUserInformation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Actualizar información del usuario cuando la actividad regresa al foco
        updateUserUI();
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
        if (btnResendVerification != null) {
            btnResendVerification.setVisibility(View.GONE);
        }
        if (btnRefreshUser != null) {
            btnRefreshUser.setOnClickListener(v -> openFormActivity());
        }
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> logout());
        }
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
        SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
        int userId = prefs.getInt("userId", -1);

        if (userId != -1) {
            // Usuario logueado, redirigir a Home2Activity
            Intent intent = new Intent(this, Home2Activity.class);
            startActivity(intent);
            finish();
        } else {
            // Si no hay usuario logueado, redirigir al login
            navigateToLogin();
        }
    }


    /**
     * Actualiza la interfaz de usuario con la información del usuario
     */
    private void updateUserUI() {
        SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
        String userEmail = prefs.getString("userEmail", "");
        String userName = prefs.getString("userName", "Usuario");

        // Mostrar email del usuario
        if (tvUserEmail != null) {
            tvUserEmail.setText(userEmail);
        }

        // Mostrar nombre del usuario
        if (tvUserName != null) {
            tvUserName.setText("Nombre: " + userName);
        }

        // Ocultar verificación de email ya que no usamos Firebase
        if (tvEmailVerification != null) {
            tvEmailVerification.setVisibility(View.GONE);
        }
        if (cardVerification != null) {
            cardVerification.setVisibility(View.GONE);
        }
    }

    /**
     * Cierra la sesión del usuario
     */
    private void logout() {
        // Limpiar SharedPreferences
        SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
        prefs.edit().clear().apply();

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
