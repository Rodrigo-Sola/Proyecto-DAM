package sv.edu.itca.proyecto_dam;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONObject;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import androidx.annotation.NonNull;

public class Home2Activity extends AppCompatActivity {

    private static final String TAG = "Home2Activity";
    private static final String BASE_URL = "http://172.193.118.141:8080/api";

    private TextView nomUser;
    private LinearLayout usuariosContainer;

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
        loadUsuarios();

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
        usuariosContainer = findViewById(R.id.usuariosContainer);
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
                    Log.d(TAG, "Usuarios data: " + responseData);

                    JSONArray usuariosArray = new JSONArray(responseData);

                    runOnUiThread(() -> {
                        displayUsuarios(usuariosArray);
                    });
                } else {
                    Log.w(TAG, "Usuarios endpoint returned code: " + response.code());
                }
                response.close();
            } catch (Exception e) {
                Log.e(TAG, "Error loading usuarios: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    private void displayUsuarios(JSONArray usuariosArray) {
        if (usuariosContainer == null) {
            Log.e(TAG, "usuariosContainer is null");
            return;
        }

        // Limpiar contenedor
        usuariosContainer.removeAllViews();

        try {
            // Obtener el ID del usuario actual para no mostrarlo
            SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
            int currentUserId = prefs.getInt("userId", -1);

            for (int i = 0; i < usuariosArray.length(); i++) {
                JSONObject usuario = usuariosArray.getJSONObject(i);
                int userId = usuario.optInt("id", -1);

                // No mostrar el usuario actual
                if (userId == currentUserId) {
                    continue;
                }

                // Crear card para el usuario
                LinearLayout userCard = createUserCard(usuario);
                usuariosContainer.addView(userCard);
            }

            Log.d(TAG, "Displayed " + usuariosArray.length() + " usuarios");
        } catch (Exception e) {
            Log.e(TAG, "Error displaying usuarios: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private LinearLayout createUserCard(JSONObject usuario) throws Exception {
        // Card container
        LinearLayout cardLayout = new LinearLayout(this);
        cardLayout.setOrientation(LinearLayout.VERTICAL);
        cardLayout.setBackgroundResource(R.color.terciario);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                (int) (280 * getResources().getDisplayMetrics().density),
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMarginEnd((int) (12 * getResources().getDisplayMetrics().density));
        cardLayout.setLayoutParams(cardParams);
        cardLayout.setPadding(
                (int) (16 * getResources().getDisplayMetrics().density),
                (int) (16 * getResources().getDisplayMetrics().density),
                (int) (16 * getResources().getDisplayMetrics().density),
                (int) (16 * getResources().getDisplayMetrics().density)
        );
        cardLayout.setElevation(4 * getResources().getDisplayMetrics().density);

        // Header layout (imagen + info)
        LinearLayout headerLayout = new LinearLayout(this);
        headerLayout.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams headerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        headerLayout.setLayoutParams(headerParams);

        // Imagen de perfil circular
        ImageView imgPerfil = new ImageView(this);
        LinearLayout.LayoutParams imgParams = new LinearLayout.LayoutParams(
                (int) (60 * getResources().getDisplayMetrics().density),
                (int) (60 * getResources().getDisplayMetrics().density)
        );
        imgParams.setMarginEnd((int) (12 * getResources().getDisplayMetrics().density));
        imgPerfil.setLayoutParams(imgParams);
        imgPerfil.setBackgroundResource(R.drawable.circular_image);
        imgPerfil.setClipToOutline(true);
        imgPerfil.setScaleType(ImageView.ScaleType.CENTER_CROP);

        // Cargar imagen del usuario
        String fotoPerfil = usuario.optString("fotoPerfil", "");
        if (!fotoPerfil.isEmpty()) {
            String filename = fotoPerfil.contains("/")
                    ? fotoPerfil.substring(fotoPerfil.lastIndexOf("/") + 1)
                    : fotoPerfil;
            String imageUrl = "http://172.193.118.141:8080/images/" + filename;

            Picasso.get()
                    .load(imageUrl)
                    .transform(new CircularTransformation())
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .error(R.drawable.ic_profile_placeholder)
                    .into(imgPerfil);
        } else {
            imgPerfil.setImageResource(R.drawable.ic_profile_placeholder);
        }

        // Info layout
        LinearLayout infoLayout = new LinearLayout(this);
        infoLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
        );
        infoLayout.setLayoutParams(infoParams);

        // Nombre del usuario
        TextView nombreTextView = new TextView(this);
        String nombre = usuario.optString("nombre", "") + " " + usuario.optString("apellido", "");
        nombreTextView.setText(nombre);
        nombreTextView.setTextColor(getResources().getColor(R.color.texto_oscuro, null));
        nombreTextView.setTextSize(16);
        nombreTextView.setTypeface(null, android.graphics.Typeface.BOLD);

        // Label "Enseña:"
        TextView ensenaLabel = new TextView(this);
        ensenaLabel.setText("Enseña:");
        ensenaLabel.setTextColor(getResources().getColor(R.color.texto_oscuro, null));
        ensenaLabel.setTextSize(12);
        LinearLayout.LayoutParams ensenaParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        ensenaParams.topMargin = (int) (6 * getResources().getDisplayMetrics().density);
        ensenaLabel.setLayoutParams(ensenaParams);

        // Habilidades (se cargarán después)
        TextView habilidadesTextView = new TextView(this);
        habilidadesTextView.setText("Cargando...");
        habilidadesTextView.setTextColor(getResources().getColor(R.color.texto_oscuro, null));
        habilidadesTextView.setTextSize(10);

        // Cargar habilidades del usuario
        int userId = usuario.optInt("id", -1);
        if (userId != -1) {
            loadUserHabilidades(userId, habilidadesTextView);
        }

        // Agregar vistas al info layout
        infoLayout.addView(nombreTextView);
        infoLayout.addView(ensenaLabel);
        infoLayout.addView(habilidadesTextView);

        // Agregar imagen e info al header
        headerLayout.addView(imgPerfil);
        headerLayout.addView(infoLayout);

        // Botones layout
        LinearLayout botonesLayout = new LinearLayout(this);
        botonesLayout.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams botonesParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        botonesParams.topMargin = (int) (12 * getResources().getDisplayMetrics().density);
        botonesLayout.setLayoutParams(botonesParams);

        // Botón Ver Perfil
        Button btnVerPerfil = new Button(this);
        LinearLayout.LayoutParams btnPerfilParams = new LinearLayout.LayoutParams(
                0,
                (int) (32 * getResources().getDisplayMetrics().density),
                1f
        );
        btnPerfilParams.setMarginEnd((int) (6 * getResources().getDisplayMetrics().density));
        btnVerPerfil.setLayoutParams(btnPerfilParams);
        btnVerPerfil.setText("Ver Perfil");
        btnVerPerfil.setTextSize(12);
        btnVerPerfil.setTextColor(getResources().getColor(R.color.fondo_principal, null));
        btnVerPerfil.setBackgroundTintList(getResources().getColorStateList(R.color.primario, null));
        btnVerPerfil.setOnClickListener(v -> {
            // Abrir PerfilUsuarioActivity con el userId
            int targetUserId = usuario.optInt("id", -1);
            Log.d(TAG, "Abriendo perfil de usuario ID: " + targetUserId);

            Intent intent = new Intent(Home2Activity.this, PerfilUsuarioActivity.class);
            intent.putExtra("userId", targetUserId);
            startActivity(intent);
        });

        // Botón Conectar
        Button btnConectar = new Button(this);
        LinearLayout.LayoutParams btnConectarParams = new LinearLayout.LayoutParams(
                0,
                (int) (32 * getResources().getDisplayMetrics().density),
                1f
        );
        btnConectar.setLayoutParams(btnConectarParams);
        btnConectar.setText("Conectar");
        btnConectar.setTextSize(12);
        btnConectar.setTextColor(getResources().getColor(R.color.texto_oscuro, null));
        btnConectar.setBackgroundTintList(getResources().getColorStateList(R.color.secundario, null));
        btnConectar.setOnClickListener(v -> {
            // TODO: Iniciar proceso de conexión
            Log.d(TAG, "Conectar con usuario ID: " + usuario.optInt("id", -1));
        });

        // Agregar botones al layout
        botonesLayout.addView(btnVerPerfil);
        botonesLayout.addView(btnConectar);

        // Agregar todo al card
        cardLayout.addView(headerLayout);
        cardLayout.addView(botonesLayout);

        return cardLayout;
    }

    private void loadUserHabilidades(int userId, TextView habilidadesTextView) {
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
                    JSONArray habilidadesArray = new JSONArray(responseData);

                    StringBuilder habilidades = new StringBuilder();
                    int maxHabilidades = Math.min(3, habilidadesArray.length());

                    for (int i = 0; i < maxHabilidades; i++) {
                        JSONObject habilidad = habilidadesArray.getJSONObject(i);
                        String nomHabilidad = habilidad.optString("nomHabilidad", "");
                        if (!nomHabilidad.isEmpty()) {
                            if (habilidades.length() > 0) {
                                habilidades.append("\n");
                            }
                            habilidades.append(nomHabilidad);
                        }
                    }

                    String finalText = habilidades.length() > 0 ? habilidades.toString() : "Sin habilidades";

                    runOnUiThread(() -> {
                        habilidadesTextView.setText(finalText);
                    });
                } else {
                    runOnUiThread(() -> {
                        habilidadesTextView.setText("Sin habilidades");
                    });
                }
                response.close();
            } catch (Exception e) {
                Log.e(TAG, "Error loading habilidades for user " + userId + ": " + e.getMessage());
                runOnUiThread(() -> {
                    habilidadesTextView.setText("Sin habilidades");
                });
            }
        }).start();
    }
}