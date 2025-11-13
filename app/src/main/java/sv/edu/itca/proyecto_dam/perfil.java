package sv.edu.itca.proyecto_dam;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.tabs.TabLayout;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONObject;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import androidx.appcompat.app.AppCompatActivity;

public class perfil extends AppCompatActivity {

    private static final String TAG = "PerfilActivity";
    private static final String BASE_URL = "http://172.193.118.141:8080/api";

    // UI Components
    private ImageView imgPerfil;
    private TextView tvNombre;
    private TextView tvUbicacion;

    private int userId = -1;
    private String userBiografia = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil);

        initializeViews();
        getUserId();
        loadUserData();
        loadUserSkills();
        setupTabLayout();
        setupLogoutButton();
    }

    private void initializeViews() {
        imgPerfil = findViewById(R.id.imgPerfil);
        tvNombre = findViewById(R.id.tvNombre);
        tvUbicacion = findViewById(R.id.tvUbicacion);
    }

    private void getUserId() {
        SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
        userId = prefs.getInt("userId", -1);

        if (userId == -1) {
            Toast.makeText(this, "Error: Usuario no identificado", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void loadUserData() {
        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient();
                String url = BASE_URL + "/usuarios/" + userId;

                Request request = new Request.Builder()
                        .url(url)
                        .get()
                        .build();

                Response response = client.newCall(request).execute();

                if (response.isSuccessful() && response.body() != null) {
                    String responseData = response.body().string();
                    Log.d(TAG, "User data: " + responseData);

                    JSONObject userObj = new JSONObject(responseData);

                    runOnUiThread(() -> {
                        try {
                            // Set user name
                            String nombre = userObj.optString("nombre", "");

                            tvNombre.setText(nombre);

                            // Save biography for "Sobre mí" tab
                            userBiografia = userObj.optString("biografia", "");

                            // Show email in location field
                            tvUbicacion.setText(userObj.optString("email", ""));

                            // Load profile picture
                            String fotoPerfil = userObj.optString("fotoPerfil", "");
                            Log.d(TAG, "fotoPerfil from API: " + fotoPerfil);

                            if (!fotoPerfil.isEmpty()) {
                                String imageUrl;

                                // If it's a full URL
                                if (fotoPerfil.startsWith("http://") || fotoPerfil.startsWith("https://")) {
                                    // Extract filename from full URL
                                    String filename = fotoPerfil.substring(fotoPerfil.lastIndexOf("/") + 1);
                                    imageUrl = "http://172.193.118.141:8080/images/" + filename;
                                    Log.d(TAG, "Extracted filename from URL: " + filename);
                                } else {
                                    // If it's just a filename or relative path
                                    String filename = fotoPerfil.contains("/")
                                        ? fotoPerfil.substring(fotoPerfil.lastIndexOf("/") + 1)
                                        : fotoPerfil;
                                    imageUrl = "http://172.193.118.141:8080/images/" + filename;
                                    Log.d(TAG, "Using filename: " + filename);
                                }

                                Log.d(TAG, "Loading image from: " + imageUrl);

                                Picasso.get()
                                        .load(imageUrl)
                                        .transform(new CircularTransformation())
                                        .placeholder(R.drawable.ic_profile_placeholder)
                                        .error(R.drawable.ic_profile_placeholder)
                                        .into(imgPerfil, new com.squareup.picasso.Callback() {
                                            @Override
                                            public void onSuccess() {
                                                Log.d(TAG, "✅ Image loaded successfully from: " + imageUrl);
                                            }

                                            @Override
                                            public void onError(Exception e) {
                                                Log.e(TAG, "❌ Error loading image from: " + imageUrl);
                                                Log.e(TAG, "Error details: " + e.getMessage());
                                                Log.e(TAG, "Suggestion: Verify the image exists at this URL in your browser");
                                                // Try to provide helpful error message
                                                runOnUiThread(() -> {
                                                    Toast.makeText(perfil.this,
                                                        "No se pudo cargar la imagen de perfil. Verifica que el archivo exista en el servidor.",
                                                        Toast.LENGTH_LONG).show();
                                                });
                                            }
                                        });
                            } else {
                                Log.d(TAG, "No profile picture available, using placeholder");
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing user data: " + e.getMessage());
                        }
                    });
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Error al cargar datos del usuario", Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading user data: " + e.getMessage());
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void loadUserSkills() {
        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient();

                // Endpoint correcto según el backend
                String url = BASE_URL + "/habilidades/byUsuario?id=" + userId;

                Log.d(TAG, "Loading skills from: " + url);

                Request request = new Request.Builder()
                        .url(url)
                        .get()
                        .build();

                Response response = client.newCall(request).execute();

                if (response.isSuccessful() && response.body() != null) {
                    String responseData = response.body().string();
                    Log.d(TAG, "Skills data: " + responseData);

                    JSONArray skillsArray = new JSONArray(responseData);

                    runOnUiThread(() -> {
                        displaySkills(skillsArray);
                    });
                } else {
                    Log.w(TAG, "Skills endpoint returned code: " + response.code());
                    runOnUiThread(() -> {
                        displaySkills(new JSONArray());
                    });
                }
                response.close();
            } catch (Exception e) {
                Log.e(TAG, "Error loading user skills: " + e.getMessage());
                runOnUiThread(() -> {
                    displaySkills(new JSONArray());
                });
            }
        }).start();
    }

    private void displaySkills(JSONArray skillsArray) {
        LinearLayout habilidadesContent = findViewById(R.id.habilidadesContent);

        if (habilidadesContent == null) {
            Log.e(TAG, "habilidadesContent is null");
            return;
        }

        try {
            // Remover todos los TextViews existentes excepto el título
            int childCount = habilidadesContent.getChildCount();
            for (int i = childCount - 1; i > 0; i--) {
                habilidadesContent.removeViewAt(i);
            }

            // Agregar las habilidades del usuario
            for (int i = 0; i < skillsArray.length(); i++) {
                JSONObject skill = skillsArray.getJSONObject(i);
                // Usar nomHabilidad según el modelo del backend
                String nombreHabilidad = skill.optString("nomHabilidad", "");

                if (!nombreHabilidad.isEmpty()) {
                    TextView skillTextView = new TextView(this);
                    skillTextView.setText(nombreHabilidad);
                    skillTextView.setTextColor(getResources().getColor(R.color.texto_oscuro, null));
                    skillTextView.setTextSize(16);

                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );
                    params.topMargin = (i == 0) ? 24 : 8; // 24dp para el primero, 8dp para los demás
                    skillTextView.setLayoutParams(params);

                    habilidadesContent.addView(skillTextView);
                }
            }

            // Si no hay habilidades, mostrar un mensaje
            if (skillsArray.length() == 0) {
                TextView noSkillsTextView = new TextView(this);
                noSkillsTextView.setText("No tienes habilidades registradas");
                noSkillsTextView.setTextColor(getResources().getColor(R.color.texto_oscuro, null));
                noSkillsTextView.setTextSize(14);

                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                params.topMargin = 24;
                noSkillsTextView.setLayoutParams(params);

                habilidadesContent.addView(noSkillsTextView);
            }

            Log.d(TAG, "Displayed " + skillsArray.length() + " skills");
        } catch (Exception e) {
            Log.e(TAG, "Error displaying skills: " + e.getMessage());
            e.printStackTrace();
        }
    }


    private void setupLogoutButton() {
        // Buscar botón de logout si existe en el layout
        Button btnLogout = findViewById(R.id.btnLogout);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> logout());
        }
    }

    private void logout() {
        // Limpiar SharedPreferences
        SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
        prefs.edit().clear().apply();

        Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show();

        // Navegar al login
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
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
                        // Mostrar la biografía en la pestaña Sobre mí
                        displayBiografia();
                        break;
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                // Si se vuelve a seleccionar "Sobre mí", actualizar la biografía
                if (tab.getPosition() == 2) {
                    displayBiografia();
                }
            }
        });
    }

    private void displayBiografia() {
        LinearLayout sobreMiContent = findViewById(R.id.sobreMiContent);

        if (sobreMiContent == null) {
            Log.e(TAG, "sobreMiContent is null");
            return;
        }

        // Limpiar contenido existente excepto el título (primer elemento)
        int childCount = sobreMiContent.getChildCount();
        for (int i = childCount - 1; i > 0; i--) {
            sobreMiContent.removeViewAt(i);
        }

        // Crear TextView para la biografía
        TextView biografiaTextView = new TextView(this);

        if (userBiografia != null && !userBiografia.isEmpty()) {
            biografiaTextView.setText(userBiografia);
        } else {
            biografiaTextView.setText("No has agregado una biografía aún.");
            biografiaTextView.setTypeface(null, android.graphics.Typeface.ITALIC);
        }

        biografiaTextView.setTextColor(getResources().getColor(R.color.texto_oscuro, null));
        biografiaTextView.setTextSize(16);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = 24;
        biografiaTextView.setLayoutParams(params);

        sobreMiContent.addView(biografiaTextView);

        Log.d(TAG, "Biografía displayed: " + (userBiografia != null && !userBiografia.isEmpty()));
    }
}