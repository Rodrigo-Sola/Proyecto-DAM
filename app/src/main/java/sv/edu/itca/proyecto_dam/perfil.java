package sv.edu.itca.proyecto_dam;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import com.google.android.material.tabs.TabLayout;

import androidx.appcompat.app.AppCompatActivity;

public class perfil extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil);

        setupTabLayout();
    }

    private void setupTabLayout() {
        TabLayout tabLayout = findViewById(R.id.tabLayout);
        LinearLayout habilidadesContent = findViewById(R.id.habilidadesContent);
        LinearLayout resenasContent = findViewById(R.id.resenasContent);
        LinearLayout sobreMiContent = findViewById(R.id.sobreMiContent);

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0:
                        habilidadesContent.setVisibility(View.VISIBLE);
                        resenasContent.setVisibility(View.GONE);
                        sobreMiContent.setVisibility(View.GONE);
                        break;
                    case 1:
                        habilidadesContent.setVisibility(View.GONE);
                        resenasContent.setVisibility(View.VISIBLE);
                        sobreMiContent.setVisibility(View.GONE);
                        break;
                    case 2:
                        habilidadesContent.setVisibility(View.GONE);
                        resenasContent.setVisibility(View.GONE);
                        sobreMiContent.setVisibility(View.VISIBLE);
                        break;
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }
}