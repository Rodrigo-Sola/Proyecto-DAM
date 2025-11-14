package sv.edu.itca.proyecto_dam;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.appcompat.widget.SearchView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.squareup.picasso.Picasso;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


public class principal extends AppCompatActivity {

    private static final String TAG = "PrincipalActivity";
    private static final String BASE_URL = "http://172.193.118.141:8080/api";

    private LinearLayout usuariosContainer;
    private SearchView searchView;
    private JSONArray allUsuarios = new JSONArray();
    private HashMap<Integer, JSONArray> usuariosHabilidades = new HashMap<>();
    private int currentUserId = -1;

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

        // Obtener ID del usuario actual
        SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
        currentUserId = prefs.getInt("userId", -1);

        initializeViews();
        setupSearchView();
        setupBottomNavigation();
        loadUsuarios();
    }

    private void initializeViews() {
        usuariosContainer = findViewById(R.id.usuariosContainer);
        searchView = findViewById(R.id.searchView);
    }

    private void setupSearchView() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterUsuarios(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterUsuarios(newText);
                return false;
            }
        });
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_nav);
        bottomNavigationView.setOnItemSelectedListener(new BottomNavigationView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();

                if (id == R.id.nav_home) {
                    startActivity(new Intent(principal.this, Home2Activity.class));
                    finish();
                    return true;
                } else if (id == R.id.nav_search) {
                    // Ya estamos aquí
                    return true;
                } else if (id == R.id.nav_noti) {
                    startActivity(new Intent(principal.this, ReunionesActivity.class));
                    finish();
                    return true;
                } else if (id == R.id.nav_profile) {
                    startActivity(new Intent(principal.this, perfil.class));
                    finish();
                    return true;
                }
                return false;
            }
        });
    }

    private void loadUsuarios() {
        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient();
                String url = BASE_URL + "/usuarios/all";

                Log.d(TAG, "Loading usuarios from: " + url);

                Request request = new Request.Builder()
                        .url(url)
                        .get()
                        .build();

                Response response = client.newCall(request).execute();

                if (response.isSuccessful() && response.body() != null) {
                    String responseData = response.body().string();
                    Log.d(TAG, "Usuarios data loaded");

                    allUsuarios = new JSONArray(responseData);

                    runOnUiThread(() -> {
                        displayUsuarios(allUsuarios);
                    });
                } else {
                    Log.w(TAG, "Error loading usuarios: " + response.code());
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Error al cargar usuarios", Toast.LENGTH_SHORT).show();
                    });
                }
                response.close();
            } catch (Exception e) {
                Log.e(TAG, "Error loading usuarios: " + e.getMessage());
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error de conexión", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void displayUsuarios(JSONArray usuarios) {
        if (usuariosContainer == null) {
            Log.e(TAG, "usuariosContainer is null");
            return;
        }

        usuariosContainer.removeAllViews();

        try {
            if (usuarios.length() == 0) {
                TextView noUsuariosText = new TextView(this);
                noUsuariosText.setText("No se encontraron usuarios");
                noUsuariosText.setTextColor(getResources().getColor(R.color.texto_oscuro, null));
                noUsuariosText.setTextSize(16);
                noUsuariosText.setPadding(16, 32, 16, 16);
                usuariosContainer.addView(noUsuariosText);
                return;
            }

            for (int i = 0; i < usuarios.length(); i++) {
                JSONObject usuario = usuarios.getJSONObject(i);

                // No mostrar el usuario actual
                int userId = usuario.optInt("id", -1);
                if (userId == currentUserId) {
                    continue;
                }

                View userCard = createUserCard(usuario);
                usuariosContainer.addView(userCard);
            }

            Log.d(TAG, "Displayed " + usuarios.length() + " usuarios");
        } catch (Exception e) {
            Log.e(TAG, "Error displaying usuarios: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private View createUserCard(JSONObject usuario) throws Exception {
        // Inflar la plantilla de card de usuario
        View cardView = getLayoutInflater().inflate(R.layout.item_usuario_search, usuariosContainer, false);

        // Obtener referencias a las vistas
        ImageView imgPerfil = cardView.findViewById(R.id.imgPerfil);
        TextView tvNombre = cardView.findViewById(R.id.tvNombre);
        TextView tvHabilidades = cardView.findViewById(R.id.tvHabilidades);
        MaterialButton btnVerPerfil = cardView.findViewById(R.id.btnVerPerfil);

        // Configurar datos del usuario
        String nombre = usuario.optString("nombre", "");
        String apellido = usuario.optString("apellido", "");
        tvNombre.setText(nombre + " " + apellido);

        // Cargar foto de perfil
        String fotoPerfil = usuario.optString("fotoPerfil", "");
        if (!fotoPerfil.isEmpty()) {
            String filename = fotoPerfil.substring(fotoPerfil.lastIndexOf("/") + 1);
            String imageUrl = BASE_URL.replace("/api", "") + "/images/" + filename;

            Picasso.get()
                .load(imageUrl)
                .transform(new CircularTransformation())
                .placeholder(R.drawable.ic_profile_placeholder)
                .error(R.drawable.ic_profile_placeholder)
                .into(imgPerfil);
        }

        // Cargar habilidades del usuario
        int userId = usuario.optInt("id", -1);
        loadUserSkills(userId, tvHabilidades);

        // Configurar botón Ver Perfil
        btnVerPerfil.setOnClickListener(v -> {
            Intent intent = new Intent(this, PerfilUsuarioActivity.class);
            intent.putExtra("userId", userId);
            startActivity(intent);
        });

        return cardView;
    }

    private void loadUserSkills(int userId, TextView tvHabilidades) {
        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient();
                String url = BASE_URL + "/habilidades/byUsuario?id=" + userId;

                Request request = new Request.Builder()
                        .url(url)
                        .get()
                        .build();

                Response response = client.newCall(request).execute();

                if (response.isSuccessful() && response.body() != null) {
                    String responseData = response.body().string();
                    JSONArray habilidades = new JSONArray(responseData);

                    // Guardar habilidades en el HashMap para usar en filtrado
                    usuariosHabilidades.put(userId, habilidades);

                    runOnUiThread(() -> {
                        StringBuilder habilidadesText = new StringBuilder();
                        int maxSkills = Math.min(habilidades.length(), 3);

                        for (int i = 0; i < maxSkills; i++) {
                            try {
                                JSONObject habilidad = habilidades.getJSONObject(i);
                                String nombreHabilidad = habilidad.optString("nomHabilidad", "");
                                if (!nombreHabilidad.isEmpty()) {
                                    if (i > 0) habilidadesText.append(", ");
                                    habilidadesText.append(nombreHabilidad);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        if (habilidades.length() > 3) {
                            habilidadesText.append("...");
                        }

                        if (habilidadesText.length() > 0) {
                            tvHabilidades.setText(habilidadesText.toString());
                        } else {
                            tvHabilidades.setText("Sin habilidades");
                        }
                    });
                } else {
                    runOnUiThread(() -> {
                        tvHabilidades.setText("Sin habilidades");
                    });
                }
                response.close();
            } catch (Exception e) {
                Log.e(TAG, "Error loading skills: " + e.getMessage());
                runOnUiThread(() -> {
                    tvHabilidades.setText("Sin habilidades");
                });
            }
        }).start();
    }

    private void filterUsuarios(String query) {
        try {
            if (query == null || query.trim().isEmpty()) {
                displayUsuarios(allUsuarios);
                return;
            }

            JSONArray filteredUsuarios = new JSONArray();
            String queryLower = query.toLowerCase().trim();

            for (int i = 0; i < allUsuarios.length(); i++) {
                JSONObject usuario = allUsuarios.getJSONObject(i);
                String nombre = usuario.optString("nombre", "").toLowerCase();
                String apellido = usuario.optString("apellido", "").toLowerCase();
                String email = usuario.optString("email", "").toLowerCase();
                String biografia = usuario.optString("biografia", "").toLowerCase();
                int userId = usuario.optInt("id", -1);

                // Filtrar por nombre, apellido, email o biografía
                boolean matches = nombre.contains(queryLower) ||
                                apellido.contains(queryLower) ||
                                email.contains(queryLower) ||
                                biografia.contains(queryLower);

                // Si no coincide, buscar en las habilidades (si ya están cargadas)
                if (!matches && usuariosHabilidades.containsKey(userId)) {
                    JSONArray habilidades = usuariosHabilidades.get(userId);
                    if (habilidades != null) {
                        for (int j = 0; j < habilidades.length(); j++) {
                            try {
                                JSONObject habilidad = habilidades.getJSONObject(j);
                                String nombreHabilidad = habilidad.optString("nomHabilidad", "").toLowerCase();

                                if (nombreHabilidad.contains(queryLower)) {
                                    matches = true;
                                    break;
                                }
                            } catch (Exception e) {
                                // Continuar con la siguiente habilidad
                            }
                        }
                    }
                }

                if (matches) {
                    filteredUsuarios.put(usuario);
                }
            }

            displayUsuarios(filteredUsuarios);
        } catch (Exception e) {
            Log.e(TAG, "Error filtering usuarios: " + e.getMessage());
        }
    }
}