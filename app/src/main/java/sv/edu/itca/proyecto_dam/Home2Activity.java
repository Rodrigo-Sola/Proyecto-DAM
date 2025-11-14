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
    private ImageView imgPerfilHeader;

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
        String userPhoto = prefs.getString("userPhoto", "");

        nomUser.setText(displayName);

        // Cargar imagen de perfil del usuario actual
        loadCurrentUserProfileImage(userPhoto);

        // Configurar clic en el icono de perfil para redirigir a perfil
        imgPerfilHeader.setOnClickListener(v -> {
            Intent intent = new Intent(Home2Activity.this, perfil.class);
            startActivity(intent);
        });
    }

    private void loadCurrentUserProfileImage(String fotoPerfil) {
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
                    .into(imgPerfilHeader);
        } else {
            imgPerfilHeader.setImageResource(R.drawable.ic_profile_placeholder);
        }
    }

    private void inicialzarView() {
        nomUser = findViewById(R.id.Userinfo);
        usuariosContainer = findViewById(R.id.usuariosContainer);
        imgPerfilHeader = findViewById(R.id.imgPerfilHeader);
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
        // Card container con bordes redondeados
        LinearLayout cardLayout = new LinearLayout(this);
        cardLayout.setOrientation(LinearLayout.VERTICAL);
        cardLayout.setBackgroundResource(R.drawable.card_rounded_background);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                (int) (320 * getResources().getDisplayMetrics().density),
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMarginEnd((int) (16 * getResources().getDisplayMetrics().density));
        cardParams.bottomMargin = (int) (12 * getResources().getDisplayMetrics().density);
        cardLayout.setLayoutParams(cardParams);
        cardLayout.setPadding(
                (int) (20 * getResources().getDisplayMetrics().density),
                (int) (20 * getResources().getDisplayMetrics().density),
                (int) (20 * getResources().getDisplayMetrics().density),
                (int) (20 * getResources().getDisplayMetrics().density)
        );
        cardLayout.setElevation(8 * getResources().getDisplayMetrics().density);

        // Header layout (imagen + info básica)
        LinearLayout headerLayout = new LinearLayout(this);
        headerLayout.setOrientation(LinearLayout.HORIZONTAL);
        headerLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams headerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        headerLayout.setLayoutParams(headerParams);

        // Imagen de perfil circular más grande y prominente
        ImageView imgPerfil = new ImageView(this);
        LinearLayout.LayoutParams imgParams = new LinearLayout.LayoutParams(
                (int) (70 * getResources().getDisplayMetrics().density),
                (int) (70 * getResources().getDisplayMetrics().density)
        );
        imgParams.setMarginEnd((int) (16 * getResources().getDisplayMetrics().density));
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

        // Info layout con mejor jerarquía visual
        LinearLayout infoLayout = new LinearLayout(this);
        infoLayout.setOrientation(LinearLayout.VERTICAL);
        infoLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
        );
        infoLayout.setLayoutParams(infoParams);

        // Nombre del usuario con tipografía destacada
        TextView nombreTextView = new TextView(this);
        String nombre = usuario.optString("nombre", "") + " " + usuario.optString("apellido", "");
        nombreTextView.setText(nombre);
        nombreTextView.setTextColor(getResources().getColor(R.color.texto_oscuro, null));
        nombreTextView.setTextSize(18);
        nombreTextView.setTypeface(null, android.graphics.Typeface.BOLD);
        nombreTextView.setMaxLines(1);
        nombreTextView.setEllipsize(android.text.TextUtils.TruncateAt.END);

        // Subtítulo profesional
        TextView profesionTextView = new TextView(this);
        profesionTextView.setText("Desarrollador & Mentor");
        profesionTextView.setTextColor(getResources().getColor(android.R.color.darker_gray, null));
        profesionTextView.setTextSize(14);
        LinearLayout.LayoutParams profesionParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        profesionParams.topMargin = (int) (2 * getResources().getDisplayMetrics().density);
        profesionTextView.setLayoutParams(profesionParams);

        // Agregar nombre y profesión al info layout
        infoLayout.addView(nombreTextView);
        infoLayout.addView(profesionTextView);

        // Agregar imagen e info al header
        headerLayout.addView(imgPerfil);
        headerLayout.addView(infoLayout);

        // Sección "ENSEÑA" con mejor espaciado
        TextView ensenaLabel = new TextView(this);
        ensenaLabel.setText("ENSEÑA");
        ensenaLabel.setTextColor(getResources().getColor(android.R.color.darker_gray, null));
        ensenaLabel.setTextSize(12);
        ensenaLabel.setTypeface(null, android.graphics.Typeface.BOLD);
        ensenaLabel.setAllCaps(true);
        LinearLayout.LayoutParams ensenaParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        ensenaParams.topMargin = (int) (20 * getResources().getDisplayMetrics().density);
        ensenaParams.bottomMargin = (int) (8 * getResources().getDisplayMetrics().density);
        ensenaLabel.setLayoutParams(ensenaParams);

        // Container para habilidades con presentación horizontal para chips
        LinearLayout habilidadesContainer = new LinearLayout(this);
        habilidadesContainer.setOrientation(LinearLayout.HORIZONTAL);
        habilidadesContainer.setGravity(android.view.Gravity.START);
        LinearLayout.LayoutParams habilidadesContainerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        habilidadesContainer.setLayoutParams(habilidadesContainerParams);

        // TextView temporal para mostrar "Cargando..." (será reemplazado por chips)
        TextView habilidadesTextView = new TextView(this);
        habilidadesTextView.setText("Cargando...");
        habilidadesTextView.setTextColor(getResources().getColor(R.color.primario, null));
        habilidadesTextView.setTextSize(12);
        habilidadesTextView.setTypeface(null, android.graphics.Typeface.ITALIC);
        LinearLayout.LayoutParams habilidadesParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        habilidadesParams.bottomMargin = (int) (16 * getResources().getDisplayMetrics().density);
        habilidadesTextView.setLayoutParams(habilidadesParams);

        // Cargar habilidades del usuario
        int userId = usuario.optInt("id", -1);
        if (userId != -1) {
            loadUserHabilidades(userId, habilidadesTextView);
        }

        habilidadesContainer.addView(habilidadesTextView);

        // Botones layout con distribución mejorada
        LinearLayout botonesLayout = new LinearLayout(this);
        botonesLayout.setOrientation(LinearLayout.VERTICAL);
        botonesLayout.setGravity(android.view.Gravity.CENTER);
        LinearLayout.LayoutParams botonesParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        botonesParams.topMargin = (int) (8 * getResources().getDisplayMetrics().density);
        botonesLayout.setLayoutParams(botonesParams);

        // Botón principal "Conectar" con bordes redondeados
        Button btnConectar = new Button(this);
        LinearLayout.LayoutParams btnConectarParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (int) (48 * getResources().getDisplayMetrics().density)
        );
        btnConectarParams.bottomMargin = (int) (8 * getResources().getDisplayMetrics().density);
        btnConectar.setLayoutParams(btnConectarParams);
        btnConectar.setText("Conectar");
        btnConectar.setTextSize(14);
        btnConectar.setTextColor(getResources().getColor(R.color.texto_oscuro, null));
        btnConectar.setBackgroundResource(R.drawable.button_primary_rounded);
        btnConectar.setTypeface(null, android.graphics.Typeface.BOLD);
        btnConectar.setAllCaps(false);
        btnConectar.setOnClickListener(v -> {
            // TODO: Iniciar proceso de conexión
            Log.d(TAG, "Conectar con usuario ID: " + usuario.optInt("id", -1));
        });

        // Botón secundario "Ver Perfil" con bordes redondeados
        Button btnVerPerfil = new Button(this);
        LinearLayout.LayoutParams btnPerfilParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (int) (40 * getResources().getDisplayMetrics().density)
        );
        btnVerPerfil.setLayoutParams(btnPerfilParams);
        btnVerPerfil.setText("Ver Perfil");
        btnVerPerfil.setTextSize(13);
        btnVerPerfil.setTextColor(getResources().getColor(R.color.fondo_principal, null));
        btnVerPerfil.setBackgroundResource(R.drawable.button_secondary_rounded);
        btnVerPerfil.setAllCaps(false);
        btnVerPerfil.setOnClickListener(v -> {
            int targetUserId = usuario.optInt("id", -1);
            Log.d(TAG, "Abriendo perfil de usuario ID: " + targetUserId);

            Intent intent = new Intent(Home2Activity.this, PerfilUsuarioActivity.class);
            intent.putExtra("userId", targetUserId);
            startActivity(intent);
        });

        // Agregar botones al layout
        botonesLayout.addView(btnConectar);
        botonesLayout.addView(btnVerPerfil);

        // Agregar todo al card con jerarquía visual clara
        cardLayout.addView(headerLayout);
        cardLayout.addView(ensenaLabel);
        cardLayout.addView(habilidadesContainer);
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

                    runOnUiThread(() -> {
                        // Obtener el contenedor padre de forma segura
                        LinearLayout habilidadesContainer = null;
                        if (habilidadesTextView != null && habilidadesTextView.getParent() instanceof LinearLayout) {
                            habilidadesContainer = (LinearLayout) habilidadesTextView.getParent();
                        }

                        if (habilidadesContainer == null) {
                            Log.e(TAG, "Error: habilidadesContainer is null");
                            return;
                        }

                        // Remover el TextView temporal "Cargando..." de forma segura
                        try {
                            habilidadesContainer.removeView(habilidadesTextView);
                        } catch (Exception e) {
                            Log.e(TAG, "Error removing temporary view: " + e.getMessage());
                        }

                        // Crear chips individuales para cada habilidad (máximo 3)
                        int maxHabilidades = Math.min(3, habilidadesArray.length());

                        if (maxHabilidades == 0) {
                            // Mostrar mensaje si no hay habilidades
                            TextView noHabilidades = new TextView(Home2Activity.this);
                            noHabilidades.setText("Sin habilidades");
                            noHabilidades.setTextColor(getResources().getColor(android.R.color.darker_gray, null));
                            noHabilidades.setTextSize(12);
                            noHabilidades.setTypeface(null, android.graphics.Typeface.ITALIC);
                            habilidadesContainer.addView(noHabilidades);
                        } else {
                            for (int i = 0; i < maxHabilidades; i++) {
                                try {
                                    JSONObject habilidad = habilidadesArray.getJSONObject(i);
                                    String nomHabilidad = habilidad.optString("nomHabilidad", "");

                                    if (!nomHabilidad.isEmpty()) {
                                        // Crear chip individual con bordes redondeados
                                        TextView chipHabilidad = new TextView(Home2Activity.this);
                                        chipHabilidad.setText(nomHabilidad);
                                        chipHabilidad.setTextColor(getResources().getColor(android.R.color.black, null));
                                        chipHabilidad.setTextSize(12);
                                        chipHabilidad.setTypeface(null, android.graphics.Typeface.BOLD);
                                        chipHabilidad.setBackgroundResource(R.drawable.skill_chip_background);
                                        chipHabilidad.setGravity(android.view.Gravity.CENTER);

                                        // Configurar padding y margins para el chip
                                        chipHabilidad.setPadding(
                                                (int) (12 * getResources().getDisplayMetrics().density),
                                                (int) (6 * getResources().getDisplayMetrics().density),
                                                (int) (12 * getResources().getDisplayMetrics().density),
                                                (int) (6 * getResources().getDisplayMetrics().density)
                                        );

                                        LinearLayout.LayoutParams chipParams = new LinearLayout.LayoutParams(
                                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                                LinearLayout.LayoutParams.WRAP_CONTENT
                                        );
                                        chipParams.setMarginEnd((int) (8 * getResources().getDisplayMetrics().density));
                                        chipHabilidad.setLayoutParams(chipParams);

                                        habilidadesContainer.addView(chipHabilidad);
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "Error processing habilidad " + i + ": " + e.getMessage());
                                }
                            }
                        }
                    });
                } else {
                    runOnUiThread(() -> {
                        // Manejo de error de forma segura
                        LinearLayout habilidadesContainer = null;
                        if (habilidadesTextView != null && habilidadesTextView.getParent() instanceof LinearLayout) {
                            habilidadesContainer = (LinearLayout) habilidadesTextView.getParent();
                        }

                        if (habilidadesContainer != null) {
                            try {
                                habilidadesContainer.removeView(habilidadesTextView);
                                TextView noHabilidades = new TextView(Home2Activity.this);
                                noHabilidades.setText("Sin habilidades");
                                noHabilidades.setTextColor(getResources().getColor(android.R.color.darker_gray, null));
                                noHabilidades.setTextSize(12);
                                noHabilidades.setTypeface(null, android.graphics.Typeface.ITALIC);
                                habilidadesContainer.addView(noHabilidades);
                            } catch (Exception e) {
                                Log.e(TAG, "Error handling no skills case: " + e.getMessage());
                            }
                        }
                    });
                }
                response.close();
            } catch (Exception e) {
                Log.e(TAG, "Error loading habilidades for user " + userId + ": " + e.getMessage());
                runOnUiThread(() -> {
                    // Manejo de error de excepción de forma segura
                    LinearLayout habilidadesContainer = null;
                    if (habilidadesTextView != null && habilidadesTextView.getParent() instanceof LinearLayout) {
                        habilidadesContainer = (LinearLayout) habilidadesTextView.getParent();
                    }

                    if (habilidadesContainer != null) {
                        try {
                            habilidadesContainer.removeView(habilidadesTextView);
                            TextView errorHabilidades = new TextView(Home2Activity.this);
                            errorHabilidades.setText("Error al cargar");
                            errorHabilidades.setTextColor(getResources().getColor(android.R.color.darker_gray, null));
                            errorHabilidades.setTextSize(12);
                            errorHabilidades.setTypeface(null, android.graphics.Typeface.ITALIC);
                            habilidadesContainer.addView(errorHabilidades);
                        } catch (Exception ex) {
                            Log.e(TAG, "Error handling exception case: " + ex.getMessage());
                        }
                    }
                });
            }
        }).start();
    }
}
