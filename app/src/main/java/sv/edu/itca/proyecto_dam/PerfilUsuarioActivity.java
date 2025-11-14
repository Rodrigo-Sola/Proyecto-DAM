package sv.edu.itca.proyecto_dam;

import android.content.Intent;
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

public class PerfilUsuarioActivity extends AppCompatActivity {

    private static final String TAG = "PerfilUsuarioActivity";
    private static final String BASE_URL = "http://172.193.118.141:8080/api";

    // UI Components
    private ImageView imgPerfil;
    private TextView tvNombre;
    private TextView tvUbicacion;
    private Button btnSolicitarIntercambio;

    private int userId = -1;
    private String userName = "";
    private String userBiografia = "";
    private JSONArray userOpiniones = new JSONArray();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Este layout se selecciona automáticamente según la versión de Android
        // En API 31+ usará layout-v31/activity_perfil.xml
        // En versiones anteriores usará layout/activity_perfil.xml
        setContentView(R.layout.activity_perfil);

        // Obtener el userId del Intent
        Intent intent = getIntent();
        userId = intent.getIntExtra("userId", -1);

        if (userId == -1) {
            Toast.makeText(this, "Error: Usuario no identificado", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        loadUserData();
        loadUserSkills();
        loadUserOpinions();
        setupTabLayout();
    }


    private void initializeViews() {
        imgPerfil = findViewById(R.id.imgPerfil);
        tvNombre = findViewById(R.id.tvNombre);
        tvUbicacion = findViewById(R.id.tvUbicacion);
        btnSolicitarIntercambio = findViewById(R.id.btnSolicitarIntercambio);

        // Configurar click listener del botón
        btnSolicitarIntercambio.setOnClickListener(v -> {
            abrirAgendarReunion();
        });
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
                            // Set user name with apellido
                            String nombre = userObj.optString("nombre", "");
                            String apellido = userObj.optString("apellido", "");
                            String nombreCompleto = nombre + " " + apellido;
                            tvNombre.setText(nombreCompleto);

                            // Guardar nombre para usar al agendar reunión
                            userName = nombreCompleto;

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

    private void loadUserOpinions() {
        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient();
                String url = BASE_URL + "/opiniones/byUsuario?id=" + userId;

                Log.d(TAG, "Loading opinions from: " + url);

                Request request = new Request.Builder()
                        .url(url)
                        .get()
                        .build();

                Response response = client.newCall(request).execute();

                if (response.isSuccessful() && response.body() != null) {
                    String responseData = response.body().string();
                    Log.d(TAG, "Opinions data: " + responseData);

                    userOpiniones = new JSONArray(responseData);
                    Log.d(TAG, "Loaded " + userOpiniones.length() + " opinions");
                } else {
                    Log.w(TAG, "Opinions endpoint returned code: " + response.code());
                    userOpiniones = new JSONArray();
                }
                response.close();
            } catch (Exception e) {
                Log.e(TAG, "Error loading user opinions: " + e.getMessage());
                userOpiniones = new JSONArray();
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

            // Si no hay habilidades, mostrar un mensaje
            if (skillsArray.length() == 0) {
                TextView noSkillsTextView = new TextView(this);
                noSkillsTextView.setText("No tiene habilidades registradas");
                noSkillsTextView.setTextColor(getResources().getColor(R.color.texto_oscuro, null));
                noSkillsTextView.setTextSize(14);
                noSkillsTextView.setTypeface(null, android.graphics.Typeface.ITALIC);

                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                params.topMargin = (int) (24 * getResources().getDisplayMetrics().density);
                noSkillsTextView.setLayoutParams(params);

                habilidadesContent.addView(noSkillsTextView);
                return;
            }

            // Crear contenedor horizontal para los chips (con wrap)
            LinearLayout rowLayout = null;
            int chipsInRow = 0;
            int maxChipsPerRow = 3; // Máximo de chips por fila

            // Agregar las habilidades del usuario como chips
            for (int i = 0; i < skillsArray.length(); i++) {
                JSONObject skill = skillsArray.getJSONObject(i);
                String nombreHabilidad = skill.optString("nomHabilidad", "");

                if (!nombreHabilidad.isEmpty()) {
                    // Crear nueva fila si es necesario
                    if (rowLayout == null || chipsInRow >= maxChipsPerRow) {
                        rowLayout = new LinearLayout(this);
                        rowLayout.setOrientation(LinearLayout.HORIZONTAL);
                        rowLayout.setGravity(android.view.Gravity.START);

                        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                        );
                        rowParams.topMargin = (i == 0) ?
                                (int) (16 * getResources().getDisplayMetrics().density) :
                                (int) (8 * getResources().getDisplayMetrics().density);
                        rowLayout.setLayoutParams(rowParams);

                        habilidadesContent.addView(rowLayout);
                        chipsInRow = 0;
                    }

                    // Crear chip de habilidad con el diseño de skill_chip_background
                    TextView chipHabilidad = new TextView(this);
                    chipHabilidad.setText(nombreHabilidad);
                    chipHabilidad.setTextColor(getResources().getColor(R.color.texto_oscuro, null));
                    chipHabilidad.setTextSize(12);
                    chipHabilidad.setTypeface(null, android.graphics.Typeface.BOLD);
                    chipHabilidad.setBackgroundResource(R.drawable.skill_chip_background);
                    chipHabilidad.setGravity(android.view.Gravity.CENTER);

                    // Configurar padding del chip
                    chipHabilidad.setPadding(
                            (int) (12 * getResources().getDisplayMetrics().density),
                            (int) (6 * getResources().getDisplayMetrics().density),
                            (int) (12 * getResources().getDisplayMetrics().density),
                            (int) (6 * getResources().getDisplayMetrics().density)
                    );

                    // Configurar margins del chip
                    LinearLayout.LayoutParams chipParams = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );
                    chipParams.setMarginEnd((int) (8 * getResources().getDisplayMetrics().density));
                    chipHabilidad.setLayoutParams(chipParams);

                    rowLayout.addView(chipHabilidad);
                    chipsInRow++;
                }
            }

            Log.d(TAG, "Displayed " + skillsArray.length() + " skills");
        } catch (Exception e) {
            Log.e(TAG, "Error displaying skills: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void setupTabLayout() {
        TabLayout tabLayout = findViewById(R.id.tabLayout);
        LinearLayout habilidadesContent = findViewById(R.id.habilidadesContent);
        android.widget.ScrollView resenasContent = findViewById(R.id.resenasContent); // Cambiado a ScrollView
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
                        displayOpiniones();
                        break;
                    case 2:
                        habilidadesContent.setVisibility(View.GONE);
                        resenasContent.setVisibility(View.GONE);
                        sobreMiContent.setVisibility(View.VISIBLE);
                        displayBiografia();
                        break;
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                if (tab.getPosition() == 1) {
                    displayOpiniones();
                } else if (tab.getPosition() == 2) {
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

        int childCount = sobreMiContent.getChildCount();
        for (int i = childCount - 1; i > 0; i--) {
            sobreMiContent.removeViewAt(i);
        }

        TextView biografiaTextView = new TextView(this);

        if (userBiografia != null && !userBiografia.isEmpty()) {
            biografiaTextView.setText(userBiografia);
        } else {
            biografiaTextView.setText("Este usuario no ha agregado una biografía.");
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

    private void displayOpiniones() {
        android.widget.ScrollView resenasScrollView = findViewById(R.id.resenasContent);

        if (resenasScrollView == null) {
            Log.e(TAG, "resenasContent ScrollView is null");
            return;
        }

        // Obtener el LinearLayout interno del ScrollView
        LinearLayout resenasContent = (LinearLayout) resenasScrollView.getChildAt(0);

        if (resenasContent == null) {
            Log.e(TAG, "resenasContent LinearLayout is null");
            return;
        }

        // Limpiar contenido existente excepto el título (primer elemento)
        int childCount = resenasContent.getChildCount();
        for (int i = childCount - 1; i > 0; i--) {
            resenasContent.removeViewAt(i);
        }

        try {
            // Si no hay opiniones, mostrar mensaje
            if (userOpiniones.length() == 0) {
                TextView noOpinionesTextView = new TextView(this);
                noOpinionesTextView.setText("Este usuario no ha recibido reseñas.");
                noOpinionesTextView.setTextColor(getResources().getColor(R.color.texto_oscuro, null));
                noOpinionesTextView.setTextSize(14);
                noOpinionesTextView.setTypeface(null, android.graphics.Typeface.ITALIC);

                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                params.topMargin = 24;
                noOpinionesTextView.setLayoutParams(params);

                resenasContent.addView(noOpinionesTextView);
                Log.d(TAG, "No opinions to display");
                return;
            }

            // Mostrar cada opinión
            for (int i = 0; i < userOpiniones.length(); i++) {
                JSONObject opinion = userOpiniones.getJSONObject(i);
                LinearLayout opinionCard = createOpinionCard(opinion);
                resenasContent.addView(opinionCard);
            }

            Log.d(TAG, "Displayed " + userOpiniones.length() + " opinions");
        } catch (Exception e) {
            Log.e(TAG, "Error displaying opinions: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private LinearLayout createOpinionCard(JSONObject opinion) throws Exception {
        LinearLayout cardLayout = new LinearLayout(this);
        cardLayout.setOrientation(LinearLayout.VERTICAL);
        cardLayout.setBackgroundResource(R.color.terciario);
        cardLayout.setPadding(32, 32, 32, 32);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.topMargin = 24;
        cardLayout.setLayoutParams(cardParams);

        LinearLayout headerLayout = new LinearLayout(this);
        headerLayout.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams headerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        headerLayout.setLayoutParams(headerParams);

        TextView autorTextView = new TextView(this);
        JSONObject autor = opinion.optJSONObject("idAutor");
        String nombreAutor = "Usuario";
        if (autor != null) {
            nombreAutor = autor.optString("nombre", "Usuario");
        }
        autorTextView.setText(nombreAutor);
        autorTextView.setTextColor(getResources().getColor(R.color.texto_oscuro, null));
        autorTextView.setTextSize(16);
        autorTextView.setTypeface(null, android.graphics.Typeface.BOLD);

        LinearLayout.LayoutParams autorParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
        );
        autorTextView.setLayoutParams(autorParams);

        TextView valoracionTextView = new TextView(this);
        float valoracion = (float) opinion.optDouble("valorReunion", 0.0);
        String estrellas = getStarsString(valoracion);
        valoracionTextView.setText(estrellas);
        valoracionTextView.setTextColor(getResources().getColor(R.color.secundario, null));
        valoracionTextView.setTextSize(14);

        headerLayout.addView(autorTextView);
        headerLayout.addView(valoracionTextView);

        TextView opinionTextView = new TextView(this);
        String opinionText = opinion.optString("opinion", "");
        opinionTextView.setText(opinionText);
        opinionTextView.setTextColor(getResources().getColor(R.color.texto_oscuro, null));
        opinionTextView.setTextSize(14);

        LinearLayout.LayoutParams opinionParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        opinionParams.topMargin = 16;
        opinionTextView.setLayoutParams(opinionParams);

        cardLayout.addView(headerLayout);
        cardLayout.addView(opinionTextView);

        return cardLayout;
    }

    private String getStarsString(float rating) {
        int fullStars = (int) rating;
        StringBuilder stars = new StringBuilder();

        for (int i = 0; i < fullStars && i < 5; i++) {
            stars.append("★");
        }

        int emptyStars = 5 - fullStars;
        for (int i = 0; i < emptyStars; i++) {
            stars.append("☆");
        }

        return stars.toString();
    }

    private void abrirAgendarReunion() {
        if (userName == null || userName.isEmpty()) {
            Toast.makeText(this, "Espera a que carguen los datos del usuario", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "Abriendo AgendarReunionActivity para usuario: " + userName + " (ID: " + userId + ")");

        Intent intent = new Intent(this, AgendarReunionActivity.class);
        intent.putExtra("userId", userId);
        intent.putExtra("userName", userName);
        startActivity(intent);
    }
}
