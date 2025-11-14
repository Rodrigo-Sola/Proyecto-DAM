package sv.edu.itca.proyecto_dam;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AgendarReunionActivity extends AppCompatActivity {

    private static final String TAG = "AgendarReunionActivity";
    private static final String BASE_URL = "http://172.193.118.141:8080/api";

    private TextView tvConQuien, tvFechaSeleccionada, tvHoraSeleccionada;
    private LinearLayout layoutFecha, layoutHora;
    private Spinner spinnerDuracion;
    private EditText etMensaje;
    private Button btnCancelar, btnEnviarSolicitud;

    private int otroUsuarioId;
    private String otroUsuarioNombre;
    private Calendar fechaHoraSeleccionada;
    private String duracionSeleccionada = "01:00:00";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_agendar_reunion);

        // Obtener datos del usuario
        otroUsuarioId = getIntent().getIntExtra("userId", -1);
        otroUsuarioNombre = getIntent().getStringExtra("userName");

        if (otroUsuarioId == -1 || otroUsuarioNombre == null) {
            Toast.makeText(this, "Error: Datos de usuario inválidos", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        setupSpinner();
        setupClickListeners();
    }

    private void initializeViews() {
        tvConQuien = findViewById(R.id.tvConQuien);
        tvFechaSeleccionada = findViewById(R.id.tvFechaSeleccionada);
        tvHoraSeleccionada = findViewById(R.id.tvHoraSeleccionada);
        spinnerDuracion = findViewById(R.id.spinnerDuracion);
        etMensaje = findViewById(R.id.etMensaje);
        btnCancelar = findViewById(R.id.btnCancelar);
        btnEnviarSolicitud = findViewById(R.id.btnEnviarSolicitud);

        // Configurar texto
        tvConQuien.setText("Con: " + otroUsuarioNombre);

        // Inicializar calendario
        fechaHoraSeleccionada = Calendar.getInstance();

        // Obtener los layouts clickeables
        layoutFecha = (LinearLayout) tvFechaSeleccionada.getParent();
        layoutHora = (LinearLayout) tvHoraSeleccionada.getParent();
    }

    private void setupSpinner() {
        String[] duraciones = {
            "30 minutos",
            "1 hora",
            "1 hora 30 minutos",
            "2 horas",
            "3 horas"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            this,
            android.R.layout.simple_spinner_item,
            duraciones
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDuracion.setAdapter(adapter);

        // Seleccionar "1 hora" por defecto
        spinnerDuracion.setSelection(1);
    }

    private void setupClickListeners() {
        // Click en fecha
        layoutFecha.setOnClickListener(v -> mostrarDatePicker());

        // Click en hora
        layoutHora.setOnClickListener(v -> mostrarTimePicker());

        // Botón cancelar
        btnCancelar.setOnClickListener(v -> finish());

        // Botón enviar solicitud
        btnEnviarSolicitud.setOnClickListener(v -> validarYEnviarSolicitud());
    }

    private void mostrarDatePicker() {
        Calendar calendar = Calendar.getInstance();

        DatePickerDialog datePickerDialog = new DatePickerDialog(
            this,
            (view, year, month, dayOfMonth) -> {
                fechaHoraSeleccionada.set(Calendar.YEAR, year);
                fechaHoraSeleccionada.set(Calendar.MONTH, month);
                fechaHoraSeleccionada.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                actualizarTextoFecha();
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        );

        // No permitir fechas pasadas
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    private void mostrarTimePicker() {
        Calendar calendar = Calendar.getInstance();

        TimePickerDialog timePickerDialog = new TimePickerDialog(
            this,
            (view, hourOfDay, minute) -> {
                fechaHoraSeleccionada.set(Calendar.HOUR_OF_DAY, hourOfDay);
                fechaHoraSeleccionada.set(Calendar.MINUTE, minute);

                actualizarTextoHora();
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true // Formato 24 horas
        );

        timePickerDialog.show();
    }

    private void actualizarTextoFecha() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        String fechaFormateada = sdf.format(fechaHoraSeleccionada.getTime());
        tvFechaSeleccionada.setText(fechaFormateada);
        tvFechaSeleccionada.setTextColor(getResources().getColor(R.color.primario, null));
    }

    private void actualizarTextoHora() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        String horaFormateada = sdf.format(fechaHoraSeleccionada.getTime());
        tvHoraSeleccionada.setText(horaFormateada);
        tvHoraSeleccionada.setTextColor(getResources().getColor(R.color.primario, null));
    }

    private void validarYEnviarSolicitud() {
        // Validar que se haya seleccionado fecha
        if (tvFechaSeleccionada.getText().toString().equals("Seleccionar fecha")) {
            Toast.makeText(this, "Por favor selecciona una fecha", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validar que se haya seleccionado hora
        if (tvHoraSeleccionada.getText().toString().equals("Seleccionar hora")) {
            Toast.makeText(this, "Por favor selecciona una hora", Toast.LENGTH_SHORT).show();
            return;
        }

        // Obtener duración seleccionada
        int posicionDuracion = spinnerDuracion.getSelectedItemPosition();
        switch (posicionDuracion) {
            case 0: duracionSeleccionada = "00:30:00"; break;
            case 1: duracionSeleccionada = "01:00:00"; break;
            case 2: duracionSeleccionada = "01:30:00"; break;
            case 3: duracionSeleccionada = "02:00:00"; break;
            case 4: duracionSeleccionada = "03:00:00"; break;
        }

        // Deshabilitar botón para evitar doble click
        btnEnviarSolicitud.setEnabled(false);
        btnEnviarSolicitud.setText("Enviando...");

        // Enviar solicitud
        enviarSolicitudReunion();
    }

    private void enviarSolicitudReunion() {
        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient();

                // Obtener ID del usuario actual
                SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
                int currentUserId = prefs.getInt("userId", -1);

                if (currentUserId == -1) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show();
                        habilitarBoton();
                    });
                    return;
                }

                // Formatear fecha y hora para la API (formato ISO 8601)
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
                String fechaHoraISO = sdf.format(fechaHoraSeleccionada.getTime());

                Log.d(TAG, "Enviando solicitud de reunión:");
                Log.d(TAG, "Usuario actual: " + currentUserId);
                Log.d(TAG, "Otro usuario: " + otroUsuarioId);
                Log.d(TAG, "Fecha/Hora: " + fechaHoraISO);
                Log.d(TAG, "Duración: " + duracionSeleccionada);

                String url = BASE_URL + "/reuniones/save";

                // Crear body con form-data
                RequestBody formBody = new FormBody.Builder()
                        .add("idUsuario1", String.valueOf(currentUserId))
                        .add("idUsuario2", String.valueOf(otroUsuarioId))
                        .add("idEstadoR", "1") // 1 = Pendiente
                        .add("fechaHora", fechaHoraISO)
                        .add("duracion", duracionSeleccionada)
                        .build();

                Request request = new Request.Builder()
                        .url(url)
                        .post(formBody)
                        .build();

                Response response = client.newCall(request).execute();

                if (response.isSuccessful()) {
                    Log.d(TAG, "Solicitud de reunión enviada exitosamente");

                    runOnUiThread(() -> {
                        Toast.makeText(this,
                            "Solicitud enviada a " + otroUsuarioNombre,
                            Toast.LENGTH_LONG).show();

                        // Volver a la pantalla anterior
                        finish();
                    });
                } else {
                    String errorBody = response.body() != null ? response.body().string() : "Sin detalles";
                    Log.e(TAG, "Error al enviar solicitud. Code: " + response.code());
                    Log.e(TAG, "Error body: " + errorBody);

                    runOnUiThread(() -> {
                        Toast.makeText(this,
                            "Error al enviar solicitud: " + response.code(),
                            Toast.LENGTH_SHORT).show();
                        habilitarBoton();
                    });
                }
                response.close();
            } catch (Exception e) {
                Log.e(TAG, "Error en enviarSolicitudReunion: " + e.getMessage());
                e.printStackTrace();

                runOnUiThread(() -> {
                    Toast.makeText(this,
                        "Error de conexión: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
                    habilitarBoton();
                });
            }
        }).start();
    }

    private void habilitarBoton() {
        btnEnviarSolicitud.setEnabled(true);
        btnEnviarSolicitud.setText("Enviar Solicitud");
    }
}

