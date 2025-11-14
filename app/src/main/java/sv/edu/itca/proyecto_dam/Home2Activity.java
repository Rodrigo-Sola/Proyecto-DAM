package sv.edu.itca.proyecto_dam;

import android.app.AlertDialog;
import android.content.DialogInterface;
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
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONObject;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import androidx.annotation.NonNull;

public class Home2Activity extends AppCompatActivity {

    private static final String TAG = "Home2Activity";
    private static final String BASE_URL = "http://172.193.118.141:8080/api";

    private TextView nomUser;
    private LinearLayout usuariosContainer;
    private LinearLayout reunionesContainer;
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
        loadReuniones();

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
        reunionesContainer = findViewById(R.id.reunionesContainer);
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

    private void loadReuniones() {
        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient();

                SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
                int userId = prefs.getInt("userId", -1);

                String url = BASE_URL + "/reuniones/byUsuario?id=" + userId;

                Log.d(TAG, "Loading reuniones from: " + url);

                Request request = new Request.Builder()
                        .url(url)
                        .get()
                        .build();

                Response response = client.newCall(request).execute();

                if (response.isSuccessful() && response.body() != null) {
                    String responseData = response.body().string();

                    // Limitar el log para evitar overflow
                    if (responseData.length() > 5000) {
                        Log.d(TAG, "Reuniones data (truncado): " + responseData.substring(0, 5000) + "...");
                        Log.w(TAG, "Respuesta muy grande, posible referencia circular");
                    } else {
                        Log.d(TAG, "Reuniones data: " + responseData);
                    }

                    // Intentar parsear aunque haya referencias circulares
                    try {
                        JSONArray reunionesArray = new JSONArray(responseData);

                        runOnUiThread(() -> {
                            displayReuniones(reunionesArray);
                        });
                    } catch (Exception parseEx) {
                        Log.e(TAG, "Error parseando JSON de reuniones: " + parseEx.getMessage());
                        // Intentar extraer manualmente las reuniones del JSON truncado
                        JSONArray reunionesArray = extraerReunionesManualmente(responseData);

                        runOnUiThread(() -> {
                            if (reunionesArray.length() > 0) {
                                displayReuniones(reunionesArray);
                            } else {
                                displayReuniones(new JSONArray());
                            }
                        });
                    }
                } else {
                    Log.w(TAG, "Reuniones endpoint returned code: " + response.code());
                    runOnUiThread(() -> {
                        displayReuniones(new JSONArray());
                    });
                }
                response.close();
            } catch (Exception e) {
                Log.e(TAG, "Error loading reuniones: " + e.getMessage());
                e.printStackTrace();
                runOnUiThread(() -> {
                    displayReuniones(new JSONArray());
                });
            }
        }).start();
    }

    private JSONArray extraerReunionesManualmente(String jsonString) {
        JSONArray resultado = new JSONArray();
        try {
            // Si el JSON está truncado por referencias circulares,
            // intentar extraer los datos básicos de la primera reunión

            // Buscar el primer objeto de reunión
            int start = jsonString.indexOf("{");
            if (start == -1) return resultado;

            // Extraer datos básicos usando expresiones regulares simples
            JSONObject reunion = new JSONObject();

            // Extraer ID
            String idPattern = "\"id\":(\\d+)";
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(idPattern);
            java.util.regex.Matcher matcher = pattern.matcher(jsonString);
            if (matcher.find()) {
                reunion.put("id", Integer.parseInt(matcher.group(1)));
            }

            // Extraer fechaReunion
            String fechaPattern = "\"fechaReunion\":\"([^\"]+)\"";
            pattern = java.util.regex.Pattern.compile(fechaPattern);
            matcher = pattern.matcher(jsonString);
            if (matcher.find()) {
                reunion.put("fechaReunion", matcher.group(1));
                Log.d(TAG, "Fecha extraída manualmente: " + matcher.group(1));
            } else {
                reunion.put("fechaReunion", "");
            }

            // Extraer usuarios (simplificado)
            // Por ahora dejamos esto vacío ya que es muy complejo

            // Extraer estado
            String estadoIdPattern = "\"idEstadoR\":\\{\"id\":(\\d+)";
            pattern = java.util.regex.Pattern.compile(estadoIdPattern);
            matcher = pattern.matcher(jsonString);
            if (matcher.find()) {
                JSONObject estado = new JSONObject();
                estado.put("id", Integer.parseInt(matcher.group(1)));
                reunion.put("idEstadoR", estado);
            }

            resultado.put(reunion);
            Log.d(TAG, "Reunión extraída manualmente: " + reunion.toString());

        } catch (Exception e) {
            Log.e(TAG, "Error extrayendo reuniones manualmente: " + e.getMessage());
        }
        return resultado;
    }

    private void displayReuniones(JSONArray reunionesArray) {
        if (reunionesContainer == null) {
            Log.e(TAG, "reunionesContainer is null");
            return;
        }

        // Limpiar contenedor
        reunionesContainer.removeAllViews();

        try {
            if (reunionesArray.length() == 0) {
                // Mostrar mensaje de no hay reuniones
                TextView noReunionesTextView = new TextView(this);
                noReunionesTextView.setText("No tienes sesiones confirmadas");
                noReunionesTextView.setTextColor(getResources().getColor(R.color.texto_oscuro, null));
                noReunionesTextView.setTextSize(14);
                noReunionesTextView.setTypeface(null, android.graphics.Typeface.ITALIC);

                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                params.topMargin = (int) (16 * getResources().getDisplayMetrics().density);
                noReunionesTextView.setLayoutParams(params);

                reunionesContainer.addView(noReunionesTextView);
                return;
            }

            SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
            int currentUserId = prefs.getInt("userId", -1);

            // Filtrar solo reuniones confirmadas (estado = 2)
            int reunionesMostradas = 0;
            for (int i = 0; i < reunionesArray.length(); i++) {
                JSONObject reunion = reunionesArray.getJSONObject(i);
                JSONObject estado = reunion.optJSONObject("idEstadoR");

                // Solo mostrar si está confirmada (estado = 2)
                if (estado != null && estado.optInt("id", 0) == 2) {
                    // Crear card para la reunión
                    LinearLayout reunionCard = createReunionCard(reunion, currentUserId);
                    reunionesContainer.addView(reunionCard);
                    reunionesMostradas++;
                }
            }

            // Si no hay reuniones confirmadas, mostrar mensaje
            if (reunionesMostradas == 0) {
                TextView noReunionesTextView = new TextView(this);
                noReunionesTextView.setText("No tienes sesiones confirmadas");
                noReunionesTextView.setTextColor(getResources().getColor(R.color.texto_oscuro, null));
                noReunionesTextView.setTextSize(14);
                noReunionesTextView.setTypeface(null, android.graphics.Typeface.ITALIC);

                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                params.topMargin = (int) (16 * getResources().getDisplayMetrics().density);
                noReunionesTextView.setLayoutParams(params);

                reunionesContainer.addView(noReunionesTextView);
            }

            Log.d(TAG, "Displayed " + reunionesMostradas + " reuniones confirmadas de " + reunionesArray.length() + " totales");
        } catch (Exception e) {
            Log.e(TAG, "Error displaying reuniones: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private LinearLayout createReunionCard(JSONObject reunion, int currentUserId) throws Exception {
        // Log para debugging
        Log.d(TAG, "=== Creando card de reunión ===");
        Log.d(TAG, "Reunión completa: " + reunion.toString());

        // Card container VERTICAL principal con el background
        LinearLayout cardLayout = new LinearLayout(this);
        cardLayout.setOrientation(LinearLayout.VERTICAL);
        cardLayout.setBackgroundResource(R.drawable.session_card_background);
        cardLayout.setElevation(6 * getResources().getDisplayMetrics().density);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.bottomMargin = (int) (12 * getResources().getDisplayMetrics().density);
        cardParams.setMarginStart((int) (8 * getResources().getDisplayMetrics().density));
        cardParams.setMarginEnd((int) (8 * getResources().getDisplayMetrics().density));
        cardLayout.setLayoutParams(cardParams);
        cardLayout.setPadding(
                (int) (16 * getResources().getDisplayMetrics().density),
                (int) (16 * getResources().getDisplayMetrics().density),
                (int) (16 * getResources().getDisplayMetrics().density),
                (int) (16 * getResources().getDisplayMetrics().density)
        );

        // Layout HORIZONTAL para el contenido principal (icono + info + estado)
        LinearLayout contenidoHorizontalLayout = new LinearLayout(this);
        contenidoHorizontalLayout.setOrientation(LinearLayout.HORIZONTAL);
        contenidoHorizontalLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams contenidoHorizontalParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        contenidoHorizontalLayout.setLayoutParams(contenidoHorizontalParams);

        // Icono de la sesión (school)
        ImageView iconoSesion = new ImageView(this);
        LinearLayout.LayoutParams iconoParams = new LinearLayout.LayoutParams(
                (int) (40 * getResources().getDisplayMetrics().density),
                (int) (40 * getResources().getDisplayMetrics().density)
        );
        iconoParams.setMarginEnd((int) (16 * getResources().getDisplayMetrics().density));
        iconoSesion.setLayoutParams(iconoParams);
        iconoSesion.setImageResource(R.drawable.school);
        iconoSesion.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

        // Determinar el otro usuario
        JSONObject usuario1 = reunion.optJSONObject("idUsuario1");
        JSONObject usuario2 = reunion.optJSONObject("idUsuario2");

        Log.d(TAG, "Usuario1: " + (usuario1 != null ? usuario1.toString() : "null"));
        Log.d(TAG, "Usuario2: " + (usuario2 != null ? usuario2.toString() : "null"));
        Log.d(TAG, "Current userId: " + currentUserId);

        JSONObject otroUsuario = null;
        if (usuario1 != null && usuario1.optInt("id", -1) != currentUserId) {
            otroUsuario = usuario1;
        } else if (usuario2 != null) {
            otroUsuario = usuario2;
        }

        final String nombreOtroUsuario;
        if (otroUsuario != null) {
            nombreOtroUsuario = otroUsuario.optString("nombre", "Usuario");
            Log.d(TAG, "Nombre del otro usuario: " + nombreOtroUsuario);
        } else {
            nombreOtroUsuario = "Usuario";
        }

        // Contenido central con información de la reunión
        LinearLayout contenidoLayout = new LinearLayout(this);
        contenidoLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams contenidoParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
        );
        contenidoLayout.setLayoutParams(contenidoParams);

        // Título de la reunión
        TextView tituloTextView = new TextView(this);
        tituloTextView.setText("Sesión confirmada");
        tituloTextView.setTextColor(getResources().getColor(R.color.texto_oscuro, null));
        tituloTextView.setTextSize(16);
        tituloTextView.setTypeface(null, android.graphics.Typeface.BOLD);

        // Con quién
        TextView conQuienTextView = new TextView(this);
        conQuienTextView.setText("con " + nombreOtroUsuario);
        conQuienTextView.setTextColor(getResources().getColor(R.color.texto_oscuro, null));
        conQuienTextView.setTextSize(14);
        LinearLayout.LayoutParams conQuienParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        conQuienParams.topMargin = (int) (2 * getResources().getDisplayMetrics().density);
        conQuienTextView.setLayoutParams(conQuienParams);

        // Layout para fecha/hora con icono de reloj
        LinearLayout fechaLayout = new LinearLayout(this);
        fechaLayout.setOrientation(LinearLayout.HORIZONTAL);
        fechaLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams fechaLayoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        fechaLayoutParams.topMargin = (int) (6 * getResources().getDisplayMetrics().density);
        fechaLayout.setLayoutParams(fechaLayoutParams);

        // Icono de reloj
        ImageView iconoReloj = new ImageView(this);
        LinearLayout.LayoutParams relojParams = new LinearLayout.LayoutParams(
                (int) (16 * getResources().getDisplayMetrics().density),
                (int) (16 * getResources().getDisplayMetrics().density)
        );
        relojParams.setMarginEnd((int) (6 * getResources().getDisplayMetrics().density));
        iconoReloj.setLayoutParams(relojParams);
        iconoReloj.setImageResource(R.drawable.clock);
        iconoReloj.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

        // Fecha y hora
        TextView fechaTextView = new TextView(this);

        // Intentar obtener la fecha de diferentes formas
        String fechaReunion = "";
        try {
            // Primero intentar como String
            fechaReunion = reunion.optString("fechaReunion", "");
            Log.d(TAG, "fechaReunion como String: '" + fechaReunion + "'");

            // Si está vacío, intentar obtener el objeto completo
            if (fechaReunion.isEmpty() || fechaReunion.equals("null")) {
                Object fechaObj = reunion.opt("fechaReunion");
                Log.d(TAG, "fechaReunion como Object: " + fechaObj);
                Log.d(TAG, "fechaReunion Object class: " + (fechaObj != null ? fechaObj.getClass().getName() : "null"));

                if (fechaObj != null && !fechaObj.toString().equals("null")) {
                    fechaReunion = fechaObj.toString();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error obteniendo fechaReunion: " + e.getMessage());
        }

        Log.d(TAG, "fechaReunion FINAL: '" + fechaReunion + "'");

        // Formatear fecha
        String fechaFormateada;
        if (fechaReunion.isEmpty() || fechaReunion.equals("null")) {
            fechaFormateada = "Sin fecha programada";
            Log.d(TAG, "Fecha vacía, mostrando mensaje por defecto");
        } else {
            fechaFormateada = formatFecha(fechaReunion);
            Log.d(TAG, "Fecha formateada: " + fechaFormateada);
        }

        fechaTextView.setText(fechaFormateada);
        fechaTextView.setTextColor(getResources().getColor(R.color.texto_oscuro, null));
        fechaTextView.setTextSize(12);

        // Agregar icono y fecha al layout de fecha
        fechaLayout.addView(iconoReloj);
        fechaLayout.addView(fechaTextView);

        // Agregar elementos al contenido central
        contenidoLayout.addView(tituloTextView);
        contenidoLayout.addView(conQuienTextView);
        contenidoLayout.addView(fechaLayout);

        // Estado como chip con mejor visibilidad
        TextView estadoTextView = new TextView(this);
        JSONObject estado = reunion.optJSONObject("idEstadoR");
        String nombreEstado = "Pendiente";
        int colorEstado = R.color.secundario;

        if (estado != null) {
            int idEstado = estado.optInt("id", 1);
            Log.d(TAG, "ID del estado: " + idEstado);

            // Determinar nombre y color según el ID
            switch (idEstado) {
                case 1:
                    nombreEstado = "Pendiente";
                    colorEstado = R.color.secundario; // Naranja
                    break;
                case 2:
                    nombreEstado = "Confirmada";
                    colorEstado = R.color.primario; // Verde
                    break;
                case 3:
                    nombreEstado = "Completada";
                    colorEstado = android.R.color.holo_blue_dark; // Azul
                    break;
                case 4:
                    nombreEstado = "Cancelada";
                    colorEstado = android.R.color.holo_red_light; // Rojo
                    break;
                default:
                    nombreEstado = "Pendiente";
                    colorEstado = R.color.secundario;
                    break;
            }

            Log.d(TAG, "Estado de la reunión: " + nombreEstado + " (ID: " + idEstado + ")");
        }

        estadoTextView.setText(nombreEstado);
        estadoTextView.setTextColor(getResources().getColor(android.R.color.white, null));
        estadoTextView.setTextSize(12);
        estadoTextView.setTypeface(null, android.graphics.Typeface.BOLD);
        estadoTextView.setBackgroundResource(R.drawable.skill_chip_background);
        estadoTextView.getBackground().setTint(getResources().getColor(colorEstado, null));
        estadoTextView.setPadding(
                (int) (12 * getResources().getDisplayMetrics().density),
                (int) (6 * getResources().getDisplayMetrics().density),
                (int) (12 * getResources().getDisplayMetrics().density),
                (int) (6 * getResources().getDisplayMetrics().density)
        );
        LinearLayout.LayoutParams estadoParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        estadoTextView.setLayoutParams(estadoParams);

        // Agregar elementos al layout horizontal
        contenidoHorizontalLayout.addView(iconoSesion);
        contenidoHorizontalLayout.addView(contenidoLayout);
        contenidoHorizontalLayout.addView(estadoTextView);

        // Agregar el layout horizontal al card principal
        cardLayout.addView(contenidoHorizontalLayout);

        // Agregar botón "Marcar como completada" DENTRO del card si aplica
        int idEstadoActual = estado != null ? estado.optInt("id", 1) : 1;
        if (idEstadoActual != 3 && idEstadoActual != 4) {
            Button btnCompletar = new Button(this);
            btnCompletar.setText("Marcar como completada");
            btnCompletar.setTextSize(12);
            btnCompletar.setTextColor(getResources().getColor(R.color.fondo_principal, null));
            btnCompletar.setBackgroundResource(R.drawable.button_secondary_rounded);
            btnCompletar.setAllCaps(false);

            LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    (int) (40 * getResources().getDisplayMetrics().density)
            );
            btnParams.topMargin = (int) (12 * getResources().getDisplayMetrics().density);
            btnCompletar.setLayoutParams(btnParams);

            int reunionId = reunion.optInt("id", -1);

            btnCompletar.setOnClickListener(v -> {
                mostrarDialogoConfirmacion(reunionId, nombreOtroUsuario);
            });

            // Agregar el botón al card principal
            cardLayout.addView(btnCompletar);
        }

        return cardLayout;
    }

    private String formatFecha(String fechaISO) {
        try {
            Log.d(TAG, "Formateando fecha: '" + fechaISO + "'");

            if (fechaISO == null || fechaISO.isEmpty() || fechaISO.equals("null")) {
                Log.d(TAG, "Fecha vacía o null");
                return "Sin fecha programada";
            }

            // Formato esperado: "2025-11-13T16:56:17Z" o similar
            if (fechaISO.contains("T")) {
                String[] parts = fechaISO.split("T");
                String fecha = parts[0]; // 2025-11-13

                // Extraer hora - puede tener varios formatos
                String hora = "00:00";
                if (parts.length > 1) {
                    String horaPart = parts[1];
                    // Remover Z, .000Z, etc
                    horaPart = horaPart.replaceAll("Z.*$", "").replaceAll("\\..*$", "");
                    if (horaPart.length() >= 5) {
                        hora = horaPart.substring(0, 5); // HH:mm
                    }
                }

                // Convertir fecha a formato más legible
                String[] fechaParts = fecha.split("-");
                if (fechaParts.length == 3) {
                    String dia = fechaParts[2];
                    String mes = fechaParts[1];
                    String ano = fechaParts[0];
                    String fechaFormateada = hora + " - " + dia + "/" + mes + "/" + ano;
                    Log.d(TAG, "Fecha formateada: " + fechaFormateada);
                    return fechaFormateada;
                }
            }

            Log.w(TAG, "Formato de fecha no reconocido: " + fechaISO);
            return "Formato de fecha inválido";
        } catch (Exception e) {
            Log.e(TAG, "Error formateando fecha: " + e.getMessage());
            return "Error en fecha";
        }
    }

    private void mostrarDialogoConfirmacion(int reunionId, String nombreUsuario) {
        new AlertDialog.Builder(this)
            .setTitle("Confirmar acción")
            .setMessage("¿Estás seguro de que deseas marcar la reunión con " + nombreUsuario + " como completada?")
            .setPositiveButton("Sí", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    // Actualizar estado de la reunión a completada
                    actualizarEstadoReunion(reunionId, 3); // 3 = Completada
                }
            })
            .setNegativeButton("No", null)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show();
    }

    private void actualizarEstadoReunion(int reunionId, int nuevoEstado) {
        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient();
                String url = BASE_URL + "/reuniones/update/" + reunionId;

                // Crear FormBody para enviar como application/x-www-form-urlencoded
                RequestBody formBody = new okhttp3.FormBody.Builder()
                        .add("idEstadoR", String.valueOf(nuevoEstado))
                        .build();

                Request request = new Request.Builder()
                        .url(url)
                        .put(formBody)
                        .build();

                Response response = client.newCall(request).execute();

                if (response.isSuccessful()) {
                    Log.d(TAG, "Estado de reunión actualizado correctamente");

                    // Recargar reuniones
                    runOnUiThread(() -> {
                        Toast.makeText(Home2Activity.this, "Reunión marcada como completada", Toast.LENGTH_SHORT).show();
                        loadReuniones();
                    });
                } else {
                    Log.w(TAG, "Error al actualizar estado de reunión: " + response.code());
                    runOnUiThread(() -> {
                        Toast.makeText(Home2Activity.this, "Error al actualizar estado", Toast.LENGTH_SHORT).show();
                    });
                }
                response.close();
            } catch (Exception e) {
                Log.e(TAG, "Error al actualizar estado de reunión: " + e.getMessage());
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(Home2Activity.this, "Error en la conexión", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }
}

