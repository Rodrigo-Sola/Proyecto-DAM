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
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONArray;
import org.json.JSONObject;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import androidx.annotation.NonNull;


public class ReunionesActivity extends AppCompatActivity {

    private static final String TAG = "ReunionesActivity";
    private static final String BASE_URL = "http://172.193.118.141:8080/api";

    private LinearLayout reunionesContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_reuniones);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        reunionesContainer = findViewById(R.id.reunionesContainer);

        // Cargar reuniones pendientes
        loadReuniones();

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_nav);
        bottomNavigationView.setOnItemSelectedListener(new BottomNavigationView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();

                if (id == R.id.nav_home) {
                    startActivity(new Intent(ReunionesActivity.this, Home2Activity.class));
                    finish();
                    return true;
                } else if (id == R.id.nav_search) {
                    startActivity(new Intent(ReunionesActivity.this, principal.class));
                    finish();
                    return true;
                }
                else if (id == R.id.nav_noti) {
                    // Ya estamos aquí
                    return true;
                }
                else if (id == R.id.nav_profile) {
                    startActivity(new Intent(ReunionesActivity.this, perfil.class));
                    finish();
                    return true;
                }
                return false;
            }
        });
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

                    if (responseData.length() > 5000) {
                        Log.d(TAG, "Reuniones data (truncado): " + responseData.substring(0, 5000) + "...");
                    } else {
                        Log.d(TAG, "Reuniones data: " + responseData);
                    }

                    try {
                        JSONArray reunionesArray = new JSONArray(responseData);

                        runOnUiThread(() -> {
                            displayReuniones(reunionesArray);
                        });
                    } catch (Exception parseEx) {
                        Log.e(TAG, "Error parseando JSON: " + parseEx.getMessage());
                        runOnUiThread(() -> {
                            displayReuniones(new JSONArray());
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

    private void displayReuniones(JSONArray reunionesArray) {
        if (reunionesContainer == null) {
            Log.e(TAG, "reunionesContainer is null");
            return;
        }

        reunionesContainer.removeAllViews();

        try {
            SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
            int currentUserId = prefs.getInt("userId", -1);

            // Filtrar solo reuniones pendientes (estado = 1)
            JSONArray reunionesPendientes = new JSONArray();
            for (int i = 0; i < reunionesArray.length(); i++) {
                JSONObject reunion = reunionesArray.getJSONObject(i);
                JSONObject estado = reunion.optJSONObject("idEstadoR");

                if (estado != null && estado.optInt("id", 0) == 1) {
                    reunionesPendientes.put(reunion);
                }
            }

            if (reunionesPendientes.length() == 0) {
                TextView noReunionesTextView = new TextView(this);
                noReunionesTextView.setText("No tienes reuniones pendientes");
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

            // Mostrar cada reunión pendiente
            for (int i = 0; i < reunionesPendientes.length(); i++) {
                JSONObject reunion = reunionesPendientes.getJSONObject(i);
                LinearLayout reunionCard = createReunionCard(reunion, currentUserId);
                reunionesContainer.addView(reunionCard);
            }

            Log.d(TAG, "Displayed " + reunionesPendientes.length() + " reuniones pendientes");
        } catch (Exception e) {
            Log.e(TAG, "Error displaying reuniones: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private LinearLayout createReunionCard(JSONObject reunion, int currentUserId) throws Exception {
        // Inflar el layout de plantilla
        LinearLayout cardLayout = (LinearLayout) getLayoutInflater().inflate(
                R.layout.item_reunion,
                reunionesContainer,
                false
        );

        // Determinar el otro usuario y quién es el receptor
        JSONObject usuario1 = reunion.optJSONObject("idUsuario1");
        JSONObject usuario2 = reunion.optJSONObject("idUsuario2");
        
        int idUsuario1 = usuario1 != null ? usuario1.optInt("id", -1) : -1;
        int idUsuario2 = usuario2 != null ? usuario2.optInt("id", -1) : -1;

        // Determinar si el usuario actual es el receptor (usuario2)
        boolean esReceptor = (currentUserId == idUsuario2);

        Log.d(TAG, "Usuario1 ID: " + idUsuario1 + ", Usuario2 ID: " + idUsuario2);
        Log.d(TAG, "Current User ID: " + currentUserId + ", Es receptor: " + esReceptor);

        final String nombreOtroUsuario;
        if (usuario1 != null && usuario1.optInt("id", -1) != currentUserId) {
            nombreOtroUsuario = usuario1.optString("nombre", "Usuario");
        } else if (usuario2 != null) {
            nombreOtroUsuario = usuario2.optString("nombre", "Usuario");
        } else {
            nombreOtroUsuario = "Usuario";
        }

        // Obtener referencias a las vistas
        TextView tvTitulo = cardLayout.findViewById(R.id.tvTituloReunion);
        TextView tvConQuien = cardLayout.findViewById(R.id.tvConQuien);
        TextView tvFechaReunion = cardLayout.findViewById(R.id.tvFechaReunion);
        Button btnRechazar = cardLayout.findViewById(R.id.btnRechazar);
        Button btnAceptar = cardLayout.findViewById(R.id.btnAceptar);
        LinearLayout botonesLayout = (LinearLayout) btnRechazar.getParent();

        // Configurar título según si es receptor o emisor
        if (esReceptor) {
            tvTitulo.setText("Solicitud de reunión");
            tvConQuien.setText("de " + nombreOtroUsuario);
        } else {
            tvTitulo.setText("Reunión pendiente");
            tvConQuien.setText("con " + nombreOtroUsuario);
        }

        String fechaReunion = reunion.optString("fechaReunion", "");
        String fechaFormateada = fechaReunion.isEmpty() ? "Sin fecha programada" : formatFecha(fechaReunion);
        tvFechaReunion.setText(fechaFormateada);

        // Mostrar u ocultar botones según si es receptor
        if (esReceptor) {
            // Es el receptor (usuario2) - Mostrar botones
            botonesLayout.setVisibility(View.VISIBLE);

            int reunionId = reunion.optInt("id", -1);

            btnRechazar.setOnClickListener(v -> {
                mostrarDialogoConfirmacion("Rechazar", nombreOtroUsuario, reunionId, 4); // 4 = Cancelada
            });

            btnAceptar.setOnClickListener(v -> {
                mostrarDialogoConfirmacion("Aceptar", nombreOtroUsuario, reunionId, 2); // 2 = Confirmada
            });
        } else {
            // Es el emisor (usuario1) - Ocultar botones
            botonesLayout.setVisibility(View.GONE);
            Log.d(TAG, "Ocultando botones porque el usuario es el emisor de la solicitud");
        }

        return cardLayout;
    }

    private String formatFecha(String fechaISO) {
        try {
            if (fechaISO == null || fechaISO.isEmpty() || fechaISO.equals("null")) {
                return "Sin fecha programada";
            }

            if (fechaISO.contains("T")) {
                String[] parts = fechaISO.split("T");
                String fecha = parts[0];

                String hora = "00:00";
                if (parts.length > 1) {
                    String horaPart = parts[1];
                    horaPart = horaPart.replaceAll("Z.*$", "").replaceAll("\\..*$", "");
                    if (horaPart.length() >= 5) {
                        hora = horaPart.substring(0, 5);
                    }
                }

                String[] fechaParts = fecha.split("-");
                if (fechaParts.length == 3) {
                    String dia = fechaParts[2];
                    String mes = fechaParts[1];
                    String ano = fechaParts[0];
                    return hora + " - " + dia + "/" + mes + "/" + ano;
                }
            }

            return fechaISO;
        } catch (Exception e) {
            return "Error en fecha";
        }
    }

    private void mostrarDialogoConfirmacion(String accion, String nombreUsuario, int reunionId, int nuevoEstado) {
        new AlertDialog.Builder(this)
            .setTitle("Confirmar acción")
            .setMessage("¿Estás seguro de que deseas " + accion.toLowerCase() + " la reunión con " + nombreUsuario + "?")
            .setPositiveButton("Sí", (dialog, which) -> {
                actualizarEstadoReunion(reunionId, nuevoEstado, accion);
            })
            .setNegativeButton("No", null)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show();
    }

    private void actualizarEstadoReunion(int reunionId, int nuevoEstado, String accion) {
        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient();
                String url = BASE_URL + "/reuniones/update/" + reunionId;

                RequestBody formBody = new okhttp3.FormBody.Builder()
                        .add("idEstadoR", String.valueOf(nuevoEstado))
                        .build();

                Request request = new Request.Builder()
                        .url(url)
                        .put(formBody)
                        .build();

                Response response = client.newCall(request).execute();

                if (response.isSuccessful()) {
                    Log.d(TAG, "Estado de reunión actualizado correctamente a: " + nuevoEstado);

                    runOnUiThread(() -> {
                        Toast.makeText(ReunionesActivity.this,
                            "Reunión " + accion.toLowerCase() + "ada correctamente",
                            Toast.LENGTH_SHORT).show();
                        // Recargar reuniones
                        loadReuniones();
                    });
                } else {
                    Log.w(TAG, "Error al actualizar estado: " + response.code());
                    runOnUiThread(() -> {
                        Toast.makeText(ReunionesActivity.this,
                            "Error al actualizar la reunión",
                            Toast.LENGTH_SHORT).show();
                    });
                }
                response.close();
            } catch (Exception e) {
                Log.e(TAG, "Error al actualizar estado: " + e.getMessage());
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(ReunionesActivity.this,
                        "Error en la conexión",
                        Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }
}

