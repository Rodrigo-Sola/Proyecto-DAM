package sv.edu.itca.proyecto_dam;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.checkerframework.checker.nullness.qual.NonNull;

public class principal extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_principal);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;




        });
        setupClickListeners();

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_nav);
        bottomNavigationView.setOnItemSelectedListener(new BottomNavigationView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();

                if (id == R.id.nav_home) {
                    startActivity(new Intent(principal.this, Home2Activity.class));
                    return true;
                } else if (id == R.id.nav_search) {
                    startActivity(new Intent(principal.this, principal.class));

                    return true;
                }
                else if (id == R.id.nav_noti) {
                    startActivity(new Intent(principal.this, ReunionesActivity.class));
                    return true;
                }
                else if (id == R.id.nav_profile) {
                    startActivity(new Intent(principal.this, perfil.class));
                    return true;
                }
                return false;
            }
        });



    }

    private void setupClickListeners()
    {
        {
            findViewById(R.id.aggHabilidad).setOnClickListener(v -> agregarHabilidades());


        }
    }

    private void agregarHabilidades()
    {
        Intent intent = new Intent(this, form.class);
        startActivity(intent);
        finish();
    }


}