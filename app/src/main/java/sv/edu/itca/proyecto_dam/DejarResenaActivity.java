package sv.edu.itca.proyecto_dam;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.squareup.picasso.Picasso;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class DejarResenaActivity extends AppCompatActivity {

    private static final String TAG = "DejarResenaActivity";
    private static final String BASE_URL = "http://172.193.118.141:8080/api";

    private ImageView imgPerfilReceptor;
    private TextView tvNombreReceptor, tvRatingValue, tvCaracteres;
    private RatingBar ratingBar;
    private EditText etComentario;
    private Button btnCancelar, btnEnviar;

    private int receptorUserId;
    private String receptorUserName;
    private int reunionId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dejar_resena);

        // Obtener datos del intent
        receptorUserId = getIntent().getIntExtra("receptorUserId", -1);
        receptorUserName = getIntent().getStringExtra("receptorUserName");
        reunionId = getIntent().getIntExtra("reunionId", -1);

        initializeViews();
        setupListeners();
        loadUserData();
    }

    private void initializeViews() {
        imgPerfilReceptor = findViewById(R.id.imgPerfilReceptor);
        tvNombreReceptor = findViewById(R.id.tvNombreReceptor);
        tvRatingValue = findViewById(R.id.tvRatingValue);
        tvCaracteres = findViewById(R.id.tvCaracteres);
        ratingBar = findViewById(R.id.ratingBar);
        etComentario = findViewById(R.id.etComentario);
        btnCancelar = findViewById(R.id.btnCancelar);
        btnEnviar = findViewById(R.id.btnEnviar);

        // Mostrar nombre del receptor
        if (receptorUserName != null && !receptorUserName.isEmpty()) {
            tvNombreReceptor.setText(receptorUserName);
        }
    }

    private void setupListeners() {
        // Listener para actualizar el valor del rating
        ratingBar.setOnRatingBarChangeListener((ratingBar, rating, fromUser) -> {
            tvRatingValue.setText(String.format("%.1f", rating));
        });

        // Listener para contar caracteres
        etComentario.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                tvCaracteres.setText(s.length() + "/500");
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Botón cancelar
        btnCancelar.setOnClickListener(v -> finish());

        // Botón enviar
        btnEnviar.setOnClickListener(v -> enviarResena());
    }

    private void loadUserData() {
        if (receptorUserId == -1) return;

        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient();
                String url = BASE_URL + "/usuarios/" + receptorUserId;

                Request request = new Request.Builder()
                        .url(url)
                        .get()
                        .build();

                Response response = client.newCall(request).execute();

                if (response.isSuccessful() && response.body() != null) {
                    String responseData = response.body().string();
                    org.json.JSONObject usuario = new org.json.JSONObject(responseData);

                    String fotoPerfil = usuario.optString("fotoPerfil", "");
                    String nombre = usuario.optString("nombre", "");
                    String apellido = usuario.optString("apellido", "");

                    runOnUiThread(() -> {
                        tvNombreReceptor.setText(nombre + " " + apellido);

                        if (!fotoPerfil.isEmpty()) {
                            String filename = fotoPerfil.substring(fotoPerfil.lastIndexOf("/") + 1);
                            String imageUrl = BASE_URL.replace("/api", "") + "/images/" + filename;

                            Picasso.get()
                                    .load(imageUrl)
                                    .transform(new CircularTransformation())
                                    .placeholder(R.drawable.ic_profile_placeholder)
                                    .error(R.drawable.ic_profile_placeholder)
                                    .into(imgPerfilReceptor);
                        }
                    });
                }
                response.close();
            } catch (Exception e) {
                Log.e(TAG, "Error loading user data: " + e.getMessage());
            }
        }).start();
    }

    private void enviarResena() {
        float rating = ratingBar.getRating();
        String comentario = etComentario.getText().toString().trim();

        // Validaciones
        if (rating == 0) {
            Toast.makeText(this, "Por favor selecciona una calificación", Toast.LENGTH_SHORT).show();
            return;
        }

        if (comentario.isEmpty()) {
            Toast.makeText(this, "Por favor escribe un comentario", Toast.LENGTH_SHORT).show();
            return;
        }

        // Deshabilitar botón mientras se envía
        btnEnviar.setEnabled(false);
        btnEnviar.setText("Enviando...");

        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient();

                // Obtener ID del usuario actual (autor)
                SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
                int autorUserId = prefs.getInt("userId", -1);

                if (autorUserId == -1) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show();
                        habilitarBoton();
                    });
                    return;
                }

                String url = BASE_URL + "/opiniones/save";

                RequestBody formBody = new FormBody.Builder()
                        .add("idAutor", String.valueOf(autorUserId))
                        .add("idReceptor", String.valueOf(receptorUserId))
                        .add("opinion", comentario)
                        .add("valorReunion", String.valueOf(rating))
                        .build();

                Request request = new Request.Builder()
                        .url(url)
                        .post(formBody)
                        .build();

                Response response = client.newCall(request).execute();

                if (response.isSuccessful()) {
                    Log.d(TAG, "Reseña enviada exitosamente");

                    runOnUiThread(() -> {
                        Toast.makeText(this, "¡Reseña enviada exitosamente!", Toast.LENGTH_LONG).show();
                        finish();
                    });
                } else {
                    String errorBody = response.body() != null ? response.body().string() : "Sin respuesta";
                    Log.e(TAG, "Error al enviar reseña - Código: " + response.code() + ", Body: " + errorBody);

                    runOnUiThread(() -> {
                        Toast.makeText(this, "Error al enviar la reseña", Toast.LENGTH_SHORT).show();
                        habilitarBoton();
                    });
                }
                response.close();
            } catch (Exception e) {
                Log.e(TAG, "Error en enviarResena: " + e.getMessage());
                e.printStackTrace();

                runOnUiThread(() -> {
                    Toast.makeText(this, "Error de conexión", Toast.LENGTH_SHORT).show();
                    habilitarBoton();
                });
            }
        }).start();
    }

    private void habilitarBoton() {
        btnEnviar.setEnabled(true);
        btnEnviar.setText("Enviar Reseña");
    }
}

