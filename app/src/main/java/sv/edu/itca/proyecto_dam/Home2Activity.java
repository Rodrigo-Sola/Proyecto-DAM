package sv.edu.itca.proyecto_dam;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.annotation.NonNull;

public class Home2Activity extends AppCompatActivity {

    private TextView nomUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home2);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        inicialzarView();
        updateUserUI();
        setupClickListeners();

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_nav);
        bottomNavigationView.setOnItemSelectedListener(new BottomNavigationView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();

                if (id == R.id.nav_home) {
                    startActivity(new Intent(Home2Activity.this, Home2Activity.class));
                    return true;
                } else if (id == R.id.nav_search) {
                    startActivity(new Intent(Home2Activity.this, principal.class));
                    return true;
                } else if (id == R.id.nav_noti) {
                    startActivity(new Intent(Home2Activity.this, ReunionesActivity.class));
                    return true;
                } else if (id == R.id.nav_profile) {
                    startActivity(new Intent(Home2Activity.this, perfil.class));
                    return true;
                }
                return false;
            }
        });
    }

    private void updateUserUI() {
        SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
        String displayName = prefs.getString("userName", "Usuario");
        nomUser.setText("Bienvenido " + displayName);
    }

    private void inicialzarView() {
        nomUser = findViewById(R.id.Userinfo);
    }

    private void setupClickListeners() {
        findViewById(R.id.aggHabilidad).setOnClickListener(v -> agregarHabilidades());
    }

    private void agregarHabilidades() {
        Intent intent = new Intent(this, form.class);
        startActivity(intent);
    }
}