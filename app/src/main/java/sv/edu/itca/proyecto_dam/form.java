package sv.edu.itca.proyecto_dam;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.app.TimePickerDialog;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class form extends AppCompatActivity {

    private static final String TAG = "FormActivity";
    private Button Guardar, Cancelar, btnSeleccionarArchivo;
    private int idUsuario;
    private EditText nomHabilidad, descripcionBreve, etHoraInicio, etHoraFin;
    private TextView tvNombreArchivo, tvNoHabilidades;
    private Spinner spinnerCategoria, spinnerNivel;
    private CheckBox checkBoxLunes, checkBoxMartes, checkBoxMiercoles, checkBoxJueves, checkBoxViernes, checkBoxSabado, checkBoxDomingo;
    private LinearLayout containerHabilidades;

    private String horaInicio = "08:00";
    private String horaFin = "18:00";
    private Uri selectedDiplomaUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_form);

        // Obtener el ID del usuario logeado desde SharedPreferences o sesión
        SharedPreferences sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE);
        int idUsuarioLogeado = sharedPreferences.getInt("userId", -1); // -1 si no existe

        Log.d(TAG, "Intentando obtener userId de SharedPreferences...");
        Log.d(TAG, "userId obtenido: " + idUsuarioLogeado);

        if (idUsuarioLogeado == -1) {
            Log.e(TAG, "ERROR: No se encontró userId en SharedPreferences");
            Toast.makeText(this, "Error: Usuario no logeado.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Asignar el ID del usuario logeado a la variable
        idUsuario = idUsuarioLogeado;
        Log.d(TAG, "Usuario logeado correctamente con ID: " + idUsuario);

        Guardar = findViewById(R.id.btnGuardar);
        Cancelar = findViewById(R.id.btnCancelar);
        btnSeleccionarArchivo = findViewById(R.id.btnSeleccionarArchivo);

        nomHabilidad = findViewById(R.id.etNombreHabilidad);
        descripcionBreve = findViewById(R.id.etDescripcion);
        etHoraInicio = findViewById(R.id.etHoraInicio);
        etHoraFin = findViewById(R.id.etHoraFin);

        tvNombreArchivo = findViewById(R.id.tvNombreArchivo);
        tvNoHabilidades = findViewById(R.id.tvNoHabilidades);
        containerHabilidades = findViewById(R.id.containerHabilidades);

        spinnerCategoria = findViewById(R.id.spinnerCategoria);
        spinnerNivel = findViewById(R.id.spinnerNivel);

        checkBoxLunes = findViewById(R.id.cbLunes);
        checkBoxMartes = findViewById(R.id.cbMartes);
        checkBoxMiercoles = findViewById(R.id.cbMiercoles);
        checkBoxJueves = findViewById(R.id.cbJueves);
        checkBoxViernes = findViewById(R.id.cbViernes);
        checkBoxSabado = findViewById(R.id.cbSabado);
        checkBoxDomingo = findViewById(R.id.cbDomingo);

        // Configurar valores iniciales para las horas
        etHoraInicio.setText(horaInicio);
        etHoraFin.setText(horaFin);

        loadCategorias();
        loadNiveles();
        cargarHabilidadesUsuario();

        setonClickListener();
    }

    private void setonClickListener() {
        Guardar.setOnClickListener(view -> {
            guardarHabilidad();
            guardarDisponibilidad();
        });
        Cancelar.setOnClickListener(view -> cancelarActividad());

        // Listener para seleccionar hora de inicio
        etHoraInicio.setOnClickListener(v -> mostrarTimePicker(true));

        // Listener para seleccionar hora de fin
        etHoraFin.setOnClickListener(v -> mostrarTimePicker(false));

        // Listener para seleccionar archivo de diploma
        btnSeleccionarArchivo.setOnClickListener(v -> abrirSelectorArchivo());
    }

    /**
     * Muestra un TimePicker para seleccionar la hora
     */
    private void mostrarTimePicker(boolean esHoraInicio) {
        Calendar calendar = Calendar.getInstance();
        int hora = calendar.get(Calendar.HOUR_OF_DAY);
        int minutos = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(
            this,
            (view, hourOfDay, minute) -> {
                String horaSeleccionada = String.format("%02d:%02d", hourOfDay, minute);
                if (esHoraInicio) {
                    horaInicio = horaSeleccionada;
                    etHoraInicio.setText(horaSeleccionada);
                } else {
                    horaFin = horaSeleccionada;
                    etHoraFin.setText(horaSeleccionada);
                }
            },
            hora,
            minutos,
            true // Formato 24 horas
        );
        timePickerDialog.show();
    }

    /**
     * Launcher para seleccionar archivo de la galería o documentos
     */
    private final ActivityResultLauncher<Intent> archivoLauncher = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(),
        result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null && result.getData().getData() != null) {
                selectedDiplomaUri = result.getData().getData();
                // Mostrar el nombre del archivo seleccionado
                String fileName = getFileName(selectedDiplomaUri);
                tvNombreArchivo.setText(fileName != null ? fileName : "Archivo seleccionado");
                Log.d(TAG, "Archivo seleccionado: " + fileName);
            }
        }
    );

    /**
     * Abre el selector de archivos
     */
    private void abrirSelectorArchivo() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*"); // Aceptar imágenes
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        archivoLauncher.launch(Intent.createChooser(intent, "Seleccionar Diploma/Certificado"));
    }

    /**
     * Obtiene el nombre del archivo desde la URI
     */
    private String getFileName(Uri uri) {
        String fileName = null;
        if (uri.getScheme().equals("content")) {
            try (android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if (nameIndex >= 0) {
                        fileName = cursor.getString(nameIndex);
                    }
                }
            }
        }
        if (fileName == null) {
            fileName = uri.getPath();
            int cut = fileName.lastIndexOf('/');
            if (cut != -1) {
                fileName = fileName.substring(cut + 1);
            }
        }
        return fileName;
    }

    /**
     * Comprime una imagen para reducir su tamaño
     * @param imageUri URI de la imagen original
     * @return File con la imagen comprimida
     */
    private File comprimirImagen(Uri imageUri) {
        try {
            Log.d(TAG, "Iniciando compresión de imagen...");

            // Leer la imagen original
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeStream(inputStream);
            if (inputStream != null) inputStream.close();

            if (bitmap == null) {
                Log.e(TAG, "Error: No se pudo decodificar la imagen");
                return null;
            }

            int originalWidth = bitmap.getWidth();
            int originalHeight = bitmap.getHeight();
            Log.d(TAG, "Dimensiones originales: " + originalWidth + "x" + originalHeight);

            // Calcular nuevas dimensiones (máximo 1920x1920 manteniendo aspecto)
            int maxDimension = 1920;
            int newWidth = originalWidth;
            int newHeight = originalHeight;

            if (originalWidth > maxDimension || originalHeight > maxDimension) {
                float ratio = (float) originalWidth / originalHeight;
                if (originalWidth > originalHeight) {
                    newWidth = maxDimension;
                    newHeight = (int) (maxDimension / ratio);
                } else {
                    newHeight = maxDimension;
                    newWidth = (int) (maxDimension * ratio);
                }
                Log.d(TAG, "Redimensionando a: " + newWidth + "x" + newHeight);

                // Redimensionar bitmap
                bitmap = android.graphics.Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
            }

            // Crear archivo temporal para la imagen comprimida
            File compressedFile = new File(getCacheDir(), "diploma_compressed_" + System.currentTimeMillis() + ".jpg");
            FileOutputStream fos = new FileOutputStream(compressedFile);

            // Comprimir a JPEG con calidad 85% (buen balance entre calidad y tamaño)
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, fos);
            fos.flush();
            fos.close();

            // Liberar memoria
            bitmap.recycle();

            Log.d(TAG, "✓ Imagen comprimida exitosamente");
            Log.d(TAG, "Tamaño final: " + (compressedFile.length() / 1024) + " KB");

            return compressedFile;

        } catch (Exception e) {
            Log.e(TAG, "Error al comprimir imagen: " + e.getMessage(), e);
            return null;
        }
    }

    private void cancelarActividad() {
        Intent intent = new Intent(this, Home2Activity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private void loadCategorias() {
        String url = "http://172.193.118.141:8080/api/categorias/all"; // Endpoint para obtener todas las categorías
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                runOnUiThread(() -> Toast.makeText(form.this, "Error al cargar categorías", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        String responseData = response.body().string();
                        JSONArray jsonArray = new JSONArray(responseData);
                        List<String> categorias = new ArrayList<>();
                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject jsonObject = jsonArray.getJSONObject(i);
                            categorias.add(jsonObject.getString("nombreCategoria"));
                        }
                        runOnUiThread(() -> {
                            ArrayAdapter<String> adapter = new ArrayAdapter<>(form.this, android.R.layout.simple_spinner_item, categorias);
                            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            spinnerCategoria.setAdapter(adapter);
                        });
                    } else {
                        runOnUiThread(() -> Toast.makeText(form.this, "Error al procesar categorías", Toast.LENGTH_SHORT).show());
                    }
                } catch (Exception e) {
                    runOnUiThread(() -> Toast.makeText(form.this, "Error al procesar categorías", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void loadNiveles() {
        String url = "http://172.193.118.141:8080/api/niveles/all"; // Endpoint para obtener todos los niveles
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                runOnUiThread(() -> Toast.makeText(form.this, "Error al cargar niveles", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        String responseData = response.body().string();
                        JSONArray jsonArray = new JSONArray(responseData);
                        List<String> niveles = new ArrayList<>();
                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject jsonObject = jsonArray.getJSONObject(i);
                            niveles.add(jsonObject.getString("nomNivel"));
                        }
                        runOnUiThread(() -> {
                            ArrayAdapter<String> adapter = new ArrayAdapter<>(form.this, android.R.layout.simple_spinner_item, niveles);
                            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            spinnerNivel.setAdapter(adapter);
                        });
                    } else {
                        runOnUiThread(() -> Toast.makeText(form.this, "Error al procesar niveles", Toast.LENGTH_SHORT).show());
                    }
                } catch (Exception e) {
                    runOnUiThread(() -> Toast.makeText(form.this, "Error al procesar niveles", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void guardarHabilidad() {
        String url = "http://172.193.118.141:8080/api/habilidades/save";

        // Validar que el ID de usuario no sea nulo o inválido
        if (idUsuario <= 0) {
            Log.e(TAG, "ERROR en guardarHabilidad: ID de usuario no válido: " + idUsuario);
            Toast.makeText(this, "Error: ID de usuario no válido.", Toast.LENGTH_SHORT).show();
            return;
        }

        String idUsuarioValue = String.valueOf(idUsuario);
        String idCategoriaValue = String.valueOf(spinnerCategoria.getSelectedItemPosition() + 1);
        String idNivelValue = String.valueOf(spinnerNivel.getSelectedItemPosition() + 1);
        String nomHabilidadValue = nomHabilidad.getText().toString();
        String descripcionValue = descripcionBreve.getText().toString();

        Log.d(TAG, "=== Guardando Habilidad ===");
        Log.d(TAG, "idUsuario: " + idUsuarioValue);
        Log.d(TAG, "idCategoria: " + idCategoriaValue);
        Log.d(TAG, "idNivel: " + idNivelValue);
        Log.d(TAG, "nomHabilidad: " + nomHabilidadValue);
        Log.d(TAG, "descripcion: " + descripcionValue);
        Log.d(TAG, "Diploma seleccionado: " + (selectedDiplomaUri != null));

        OkHttpClient client = new OkHttpClient();

        // Crear MultipartBody para soportar archivos
        MultipartBody.Builder builder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("idUsuario", idUsuarioValue)
                .addFormDataPart("idCategoriaHabilidad", idCategoriaValue)
                .addFormDataPart("idNivel", idNivelValue)
                .addFormDataPart("nomHabilidad", nomHabilidadValue)
                .addFormDataPart("descripcionBreve", descripcionValue);

        // Si hay un diploma seleccionado, agregarlo
        if (selectedDiplomaUri != null) {
            try {
                Log.d(TAG, "Procesando imagen del diploma...");

                // Comprimir la imagen antes de enviarla
                File compressedFile = comprimirImagen(selectedDiplomaUri);

                if (compressedFile != null && compressedFile.exists()) {
                    Log.d(TAG, "Imagen comprimida exitosamente");
                    Log.d(TAG, "Archivo: " + compressedFile.getAbsolutePath());
                    Log.d(TAG, "Tamaño comprimido: " + compressedFile.length() + " bytes");

                    // Agregar el archivo comprimido al request
                    MediaType mediaType = MediaType.parse("image/jpeg");
                    RequestBody fileBody = RequestBody.create(compressedFile, mediaType);

                    // Usar solo el nombre de campo que funciona con tu API
                    builder.addFormDataPart("fotoDiploma", compressedFile.getName(), fileBody);

                    Log.d(TAG, "Diploma agregado al request");
                } else {
                    Log.e(TAG, "Error: No se pudo comprimir la imagen");
                    runOnUiThread(() -> Toast.makeText(form.this, "Error al comprimir la imagen", Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error al procesar imagen del diploma: " + e.getMessage(), e);
                runOnUiThread(() -> Toast.makeText(form.this, "Error al procesar imagen: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        } else {
            Log.d(TAG, "No se seleccionó diploma");
        }

        RequestBody formBody = builder.build();

        Request request = new Request.Builder()
                .url(url)
                .post(formBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                runOnUiThread(() -> {
                    Log.e(TAG, "Error al guardar habilidad: " + e.getMessage());
                    Toast.makeText(form.this, "Error al guardar habilidad: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) {
                try {
                    if (response.isSuccessful()) {
                        Log.d(TAG, "Habilidad guardada exitosamente - Código: " + response.code());
                        runOnUiThread(() -> {
                            Toast.makeText(form.this, "Habilidad guardada exitosamente", Toast.LENGTH_SHORT).show();
                            // Limpiar formulario
                            limpiarFormulario();
                        });
                    } else {
                        String errorBody = response.body() != null ? response.body().string() : "Sin respuesta del servidor";
                        Log.e(TAG, "Error al guardar habilidad - Código: " + response.code() + ", Body: " + errorBody);
                        runOnUiThread(() -> Toast.makeText(form.this, "Error al guardar habilidad: " + errorBody, Toast.LENGTH_SHORT).show());
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error procesando respuesta: " + e.getMessage());
                    runOnUiThread(() -> Toast.makeText(form.this, "Error procesando respuesta: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    /**
     * Limpia el formulario después de guardar exitosamente
     */
    private void limpiarFormulario() {
        nomHabilidad.setText("");
        descripcionBreve.setText("");
        etHoraInicio.setText("08:00");
        etHoraFin.setText("18:00");
        horaInicio = "08:00";
        horaFin = "18:00";
        selectedDiplomaUri = null;
        tvNombreArchivo.setText("Ningún archivo seleccionado");

        // Desmarcar todos los checkboxes
        checkBoxLunes.setChecked(false);
        checkBoxMartes.setChecked(false);
        checkBoxMiercoles.setChecked(false);
        checkBoxJueves.setChecked(false);
        checkBoxViernes.setChecked(false);
        checkBoxSabado.setChecked(false);
        checkBoxDomingo.setChecked(false);

        // Reset spinners a primera posición
        spinnerCategoria.setSelection(0);
        spinnerNivel.setSelection(0);

        // Recargar habilidades
        cargarHabilidadesUsuario();
    }

    private void guardarDisponibilidad() {
        String url = "http://172.193.118.141:8080/api/disponibilidad/save";
        OkHttpClient client = new OkHttpClient();

        // Log para verificar el ID de usuario y las horas seleccionadas
        Log.d(TAG, "ID de usuario enviado: " + idUsuario);
        Log.d(TAG, "Hora inicio: " + horaInicio + ", Hora fin: " + horaFin);

        // Lista de días de la semana y sus checkboxes correspondientes
        String[] diasSemana = {"Lunes", "Martes", "Miercoles", "Jueves", "Viernes", "Sabado", "Domingo"};
        CheckBox[] checkBoxes = {checkBoxLunes, checkBoxMartes, checkBoxMiercoles, checkBoxJueves, checkBoxViernes, checkBoxSabado, checkBoxDomingo};

        boolean atLeastOneSaved = false;

        for (int i = 0; i < diasSemana.length; i++) {
            String dia = diasSemana[i];
            boolean disponible = checkBoxes[i].isChecked();

            if (disponible) { // Solo enviar solicitudes para los días seleccionados
                RequestBody formBody = new FormBody.Builder()
                        .add("idUsuario", String.valueOf(idUsuario))
                        .add("diaSemana", dia)
                        .add("horaInicio", horaInicio) // Usar la hora seleccionada por el usuario
                        .add("horaFin", horaFin)       // Usar la hora seleccionada por el usuario
                        .add("disponible", String.valueOf(disponible))
                        .build();

                Request request = new Request.Builder()
                        .url(url)
                        .post(formBody)
                        .build();

                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(@NotNull Call call, @NotNull IOException e) {
                        runOnUiThread(() -> Toast.makeText(form.this, "Error al guardar disponibilidad para " + dia + ": " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    }

                    @Override
                    public void onResponse(@NotNull Call call, @NotNull Response response) {
                        try {
                            if (response.isSuccessful()) {
                                Log.d(TAG, "Disponibilidad guardada para " + dia + " de " + horaInicio + " a " + horaFin);
                            } else {
                                String errorBody = response.body() != null ? response.body().string() : "Sin respuesta del servidor";
                                runOnUiThread(() -> Toast.makeText(form.this, "Error al guardar disponibilidad para " + dia + ": " + errorBody, Toast.LENGTH_SHORT).show());
                            }
                        } catch (Exception e) {
                            runOnUiThread(() -> Toast.makeText(form.this, "Error procesando respuesta para " + dia + ": " + e.getMessage(), Toast.LENGTH_SHORT).show());

                        }
                    }
                });

                atLeastOneSaved = true;
            }
        }

        if (atLeastOneSaved) {
            runOnUiThread(() -> Toast.makeText(form.this, "Disponibilidad guardada exitosamente", Toast.LENGTH_SHORT).show());
        } else {
            runOnUiThread(() -> Toast.makeText(form.this, "No se seleccionó ningún día", Toast.LENGTH_SHORT).show());
        }
    }

    /**
     * Carga las habilidades del usuario desde la API
     * Endpoint correcto: GET /api/habilidades/byUsuario?id={idUsuario}
     */
    private void cargarHabilidadesUsuario() {
        String url = "http://172.193.118.141:8080/api/habilidades/byUsuario?id=" + idUsuario;
        OkHttpClient client = new OkHttpClient();

        Log.d(TAG, "=== CARGANDO HABILIDADES ===");
        Log.d(TAG, "URL: " + url);
        Log.d(TAG, "ID Usuario: " + idUsuario);

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                Log.e(TAG, "Error al conectar con el servidor: " + e.getMessage());
                runOnUiThread(() -> {
                    Toast.makeText(form.this, "No se pudo conectar con el servidor", Toast.LENGTH_LONG).show();
                    mostrarMensajeNoHabilidades();
                });
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) {
                String responseData = "";
                try {
                    Log.d(TAG, "=== RESPUESTA DEL SERVIDOR ===");
                    Log.d(TAG, "Código HTTP: " + response.code());
                    Log.d(TAG, "Mensaje: " + response.message());

                    if (response.body() != null) {
                        responseData = response.body().string();
                        Log.d(TAG, "Longitud de respuesta: " + responseData.length() + " caracteres");
                        Log.d(TAG, "Contenido completo: " + responseData);
                    } else {
                        Log.e(TAG, "❌ Response body es null");
                        runOnUiThread(() -> {
                            Toast.makeText(form.this, "Respuesta vacía del servidor", Toast.LENGTH_LONG).show();
                            mostrarMensajeNoHabilidades();
                        });
                        return;
                    }

                    if (response.isSuccessful()) {
                        Log.d(TAG, "✓ Respuesta exitosa (200-299)");

                        // Validar que la respuesta no esté vacía
                        if (responseData == null || responseData.trim().isEmpty()) {
                            Log.e(TAG, "❌ Respuesta vacía del servidor");
                            runOnUiThread(() -> {
                                Toast.makeText(form.this, "El servidor devolvió una respuesta vacía", Toast.LENGTH_LONG).show();
                                mostrarMensajeNoHabilidades();
                            });
                            return;
                        }

                        // Validar que sea JSON
                        if (!responseData.trim().startsWith("{") && !responseData.trim().startsWith("[")) {
                            Log.e(TAG, "❌ La respuesta no es JSON válido");
                            Log.e(TAG, "Respuesta recibida: " + responseData);
                            runOnUiThread(() -> {
                                Toast.makeText(form.this, "Respuesta inválida del servidor", Toast.LENGTH_LONG).show();
                                mostrarMensajeNoHabilidades();
                            });
                            return;
                        }

                        // Intentar parsear JSON - puede venir como array [] o como objeto {data: []}
                        final JSONArray jsonArray;

                        if (responseData.trim().startsWith("[")) {
                            // La respuesta es un array directo
                            Log.d(TAG, "Respuesta es un array directo");
                            jsonArray = new JSONArray(responseData);
                        } else if (responseData.trim().startsWith("{")) {
                            // La respuesta es un objeto
                            Log.d(TAG, "Respuesta es un objeto JSON");
                            JSONObject jsonResponse = new JSONObject(responseData);

                            String status = jsonResponse.optString("status", "");
                            Log.d(TAG, "Status de API: " + status);

                            if (jsonResponse.has("data")) {
                                jsonArray = jsonResponse.getJSONArray("data");
                            } else {
                                Log.e(TAG, "❌ La respuesta no contiene campo 'data'");
                                Log.e(TAG, "Claves disponibles: " + jsonResponse.keys());
                                runOnUiThread(() -> {
                                    Toast.makeText(form.this, "Formato de respuesta incorrecto", Toast.LENGTH_LONG).show();
                                    mostrarMensajeNoHabilidades();
                                });
                                return;
                            }
                        } else {
                            Log.e(TAG, "❌ Formato de respuesta desconocido");
                            runOnUiThread(() -> {
                                Toast.makeText(form.this, "Formato de respuesta desconocido", Toast.LENGTH_LONG).show();
                                mostrarMensajeNoHabilidades();
                            });
                            return;
                        }

                        // Procesar el array de habilidades
                        Log.d(TAG, "Total habilidades encontradas: " + jsonArray.length());

                        if (jsonArray.length() == 0) {
                            Log.w(TAG, "No se encontraron habilidades para el usuario");
                            runOnUiThread(() -> mostrarMensajeNoHabilidades());
                        } else {
                            Log.d(TAG, "Mostrando " + jsonArray.length() + " habilidades");
                            runOnUiThread(() -> mostrarHabilidades(jsonArray));
                        }
                    } else if (response.code() == 500) {
                        // Error 500 - Error interno del servidor
                        Log.e(TAG, "❌ ERROR 500 - Error interno del servidor");
                        Log.e(TAG, "Respuesta del servidor: " + responseData);
                        Log.e(TAG, "=== POSIBLES CAUSAS ===");
                        Log.e(TAG, "1. El usuario con ID " + idUsuario + " no existe");
                        Log.e(TAG, "2. Error en la base de datos del servidor");
                        Log.e(TAG, "3. El endpoint está mal configurado");
                        Log.e(TAG, "=== REVISA LOS LOGS DEL SERVIDOR BACKEND ===");

                        runOnUiThread(() -> {
                            Toast.makeText(form.this,
                                "Error del servidor (500). ID Usuario: " + idUsuario + ". Revisa logs del backend.",
                                Toast.LENGTH_LONG).show();
                            mostrarMensajeNoHabilidades();
                        });
                    } else {
                        // Otros errores HTTP
                        Log.e(TAG, "❌ Error HTTP " + response.code());
                        Log.e(TAG, "Mensaje: " + response.message());
                        Log.e(TAG, "Respuesta: " + responseData);

                        runOnUiThread(() -> {
                            Toast.makeText(form.this,
                                "Error " + response.code() + ": " + response.message(),
                                Toast.LENGTH_LONG).show();
                            mostrarMensajeNoHabilidades();
                        });
                    }
                } catch (org.json.JSONException e) {
                    Log.e(TAG, "❌ ERROR DE JSON - No se pudo parsear la respuesta");
                    Log.e(TAG, "Mensaje de error: " + e.getMessage());
                    Log.e(TAG, "Respuesta recibida: " + responseData);
                    Log.e(TAG, "Longitud: " + (responseData != null ? responseData.length() : 0));
                    e.printStackTrace();
                    runOnUiThread(() -> {
                        Toast.makeText(form.this,
                            "Error: Respuesta inválida del servidor. Revisa Logcat.",
                            Toast.LENGTH_LONG).show();
                        mostrarMensajeNoHabilidades();
                    });
                } catch (Exception e) {
                    Log.e(TAG, "❌ ERROR INESPERADO: " + e.getClass().getSimpleName());
                    Log.e(TAG, "Mensaje: " + e.getMessage());
                    Log.e(TAG, "Respuesta: " + responseData);
                    e.printStackTrace();
                    runOnUiThread(() -> {
                        Toast.makeText(form.this,
                            "Error procesando datos: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                        mostrarMensajeNoHabilidades();
                    });
                }
            }
        });
    }

    /**
     * Muestra las habilidades en el contenedor
     */
    private void mostrarHabilidades(JSONArray habilidades) {
        Log.d(TAG, "=== MOSTRANDO HABILIDADES EN UI ===");
        Log.d(TAG, "Container: " + (containerHabilidades != null ? "OK" : "NULL"));
        Log.d(TAG, "TextView NoHabilidades: " + (tvNoHabilidades != null ? "OK" : "NULL"));
        Log.d(TAG, "Cantidad de habilidades a mostrar: " + habilidades.length());

        if (containerHabilidades == null) {
            Log.e(TAG, "ERROR: containerHabilidades es null!");
            return;
        }

        containerHabilidades.removeAllViews();

        if (tvNoHabilidades != null) {
            tvNoHabilidades.setVisibility(android.view.View.GONE);
        }

        for (int i = 0; i < habilidades.length(); i++) {
            try {
                JSONObject habilidad = habilidades.getJSONObject(i);
                Log.d(TAG, "Procesando habilidad " + (i+1) + ": " + habilidad.toString());
                agregarHabilidadView(habilidad);
            } catch (Exception e) {
                Log.e(TAG, "Error al procesar habilidad " + (i+1) + ": " + e.getMessage(), e);
            }
        }

        Log.d(TAG, "Total de vistas agregadas al container: " + containerHabilidades.getChildCount());
    }

    /**
     * Agrega una vista de habilidad al contenedor
     */
    private void agregarHabilidadView(JSONObject habilidad) {
        try {Log.d(TAG, "Inflando vista para habilidad...");

            android.view.LayoutInflater inflater = android.view.LayoutInflater.from(this);
            android.view.View itemView = inflater.inflate(R.layout.item_habilidad, containerHabilidades, false);

            Log.d(TAG, "Vista inflada correctamente");

            TextView tvNombre = itemView.findViewById(R.id.tvNombreHabilidad);
            TextView tvCategoria = itemView.findViewById(R.id.tvCategoria);
            TextView tvNivel = itemView.findViewById(R.id.tvNivel);
            TextView tvDisponibilidad = itemView.findViewById(R.id.tvDisponibilidad);
            TextView tvDescripcion = itemView.findViewById(R.id.tvDescripcion);
            android.widget.ImageButton btnEliminar = itemView.findViewById(R.id.btnEliminar);

            // Obtener ID de la habilidad
            int idHabilidad = -1;
            if (habilidad.has("idHabilidad")) {
                idHabilidad = habilidad.getInt("idHabilidad");
            } else if (habilidad.has("id")) {
                idHabilidad = habilidad.getInt("id");
            }

            Log.d(TAG, "ID Habilidad: " + idHabilidad);

            // Obtener datos básicos
            String nombre = habilidad.optString("nomHabilidad", habilidad.optString("nombre", "Sin nombre"));
            String descripcion = habilidad.optString("descripcionBreve", habilidad.optString("descripcion", "Sin descripción"));

            Log.d(TAG, "Nombre: " + nombre);
            Log.d(TAG, "Descripción: " + descripcion);

            // Obtener categoría - la API devuelve el objeto completo con joins
            String categoria = "Sin categoría";
            String nivel = "Sin nivel";

            try {
                if (habilidad.has("idCategoriaHabilidad") && !habilidad.isNull("idCategoriaHabilidad")) {
                    JSONObject catObj = habilidad.getJSONObject("idCategoriaHabilidad");
                    // Buscar el nombre en diferentes posibles campos
                    if (catObj.has("nombreCategoria")) {
                        categoria = catObj.getString("nombreCategoria");
                    } else if (catObj.has("nombre")) {
                        categoria = catObj.getString("nombre");
                    } else {
                        // Si solo tiene ID, usar el ID
                        int idCategoria = catObj.optInt("id", -1);
                        categoria = "Categoría " + idCategoria;
                    }
                    Log.d(TAG, "Categoría: " + categoria);
                } else if (habilidad.has("categoriaHabilidad") && !habilidad.isNull("categoriaHabilidad")) {
                    JSONObject cat = habilidad.getJSONObject("categoriaHabilidad");
                    categoria = cat.optString("nombreCategoria", cat.optString("nombre", "Sin categoría"));
                }
            } catch (Exception e) {
                Log.w(TAG, "Error obteniendo categoría: " + e.getMessage());
            }

            // Obtener nivel - la API devuelve el objeto completo con joins
            try {
                if (habilidad.has("idNivel") && !habilidad.isNull("idNivel")) {
                    JSONObject nivObj = habilidad.getJSONObject("idNivel");
                    // Buscar el nombre en diferentes posibles campos
                    if (nivObj.has("nomNivel")) {
                        nivel = nivObj.getString("nomNivel");
                    } else if (nivObj.has("nombre")) {
                        nivel = nivObj.getString("nombre");
                    } else if (nivObj.has("nombreNivel")) {
                        nivel = nivObj.getString("nombreNivel");
                    } else {
                        // Si solo tiene ID, usar el ID
                        int idNivel = nivObj.optInt("id", -1);
                        nivel = "Nivel " + idNivel;
                    }
                    Log.d(TAG, "Nivel: " + nivel);
                } else if (habilidad.has("nivel") && !habilidad.isNull("nivel")) {
                    JSONObject niv = habilidad.getJSONObject("nivel");
                    nivel = niv.optString("nomNivel", niv.optString("nombre", "Sin nivel"));
                }
            } catch (Exception e) {
                Log.w(TAG, "Error obteniendo nivel: " + e.getMessage());
            }

            // Establecer textos en las vistas
            if (tvNombre != null) tvNombre.setText(nombre);
            if (tvCategoria != null) tvCategoria.setText(categoria);
            if (tvNivel != null) tvNivel.setText("Nivel: " + nivel);
            if (tvDescripcion != null) tvDescripcion.setText(descripcion);

            // Ocultar la disponibilidad
            if (tvDisponibilidad != null) tvDisponibilidad.setVisibility(android.view.View.GONE);

            Log.d(TAG, "Textos establecidos en las vistas");


            // Configurar botón eliminar
            if (btnEliminar != null && idHabilidad != -1) {
                final int idParaEliminar = idHabilidad;
                btnEliminar.setOnClickListener(v -> {
                    new android.app.AlertDialog.Builder(this)
                            .setTitle("Eliminar Habilidad")
                            .setMessage("¿Estás seguro de que deseas eliminar esta habilidad?")
                            .setPositiveButton("Eliminar", (dialog, which) -> eliminarHabilidad(idParaEliminar))
                            .setNegativeButton("Cancelar", null)
                            .show();
                });
            }

            // Agregar vista al contenedor
            containerHabilidades.addView(itemView);
            Log.d(TAG, "Vista agregada al contenedor exitosamente");

        } catch (Exception e) {
            Log.e(TAG, "Error al crear vista de habilidad: " + e.getMessage(), e);
            e.printStackTrace();
        }
    }

    /**
     * Carga la disponibilidad del usuario para mostrarla en la tarjeta de habilidad
     */
    private void cargarDisponibilidadHabilidad(int idHabilidad, TextView tvDisponibilidad) {
        // Intentar con el mismo formato que funciona para habilidades: byUsuario?id=
        String url = "http://172.193.118.141:8080/api/disponibilidad/byUsuario?id=" + idUsuario;
        OkHttpClient client = new OkHttpClient();

        Log.d(TAG, "=== CARGANDO DISPONIBILIDAD ===");
        Log.d(TAG, "URL: " + url);
        Log.d(TAG, "ID Usuario: " + idUsuario);
        Log.d(TAG, "ID Habilidad: " + idHabilidad);

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                Log.e(TAG, "❌ ERROR al cargar disponibilidad");
                Log.e(TAG, "Error: " + e.getMessage());
                e.printStackTrace();
                runOnUiThread(() -> tvDisponibilidad.setText("Sin disponibilidad"));
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) {
                try {
                    Log.d(TAG, "=== RESPUESTA DISPONIBILIDAD ===");
                    Log.d(TAG, "Código HTTP: " + response.code());
                    Log.d(TAG, "Mensaje: " + response.message());

                    if (response.isSuccessful() && response.body() != null) {
                        String responseData = response.body().string();
                        Log.d(TAG, "✓ Respuesta exitosa");
                        Log.d(TAG, "Longitud: " + responseData.length());
                        Log.d(TAG, "Contenido completo: " + responseData);

                        // Parsear la respuesta (puede ser array directo o objeto con data)
                        JSONArray jsonArray;
                        if (responseData.trim().startsWith("[")) {
                            Log.d(TAG, "Parseando como array directo");
                            jsonArray = new JSONArray(responseData);
                        } else {
                            Log.d(TAG, "Parseando como objeto con 'data'");
                            JSONObject jsonObj = new JSONObject(responseData);
                            jsonArray = jsonObj.getJSONArray("data");
                        }

                        Log.d(TAG, "Total registros de disponibilidad: " + jsonArray.length());

                        // Usar LinkedHashSet para mantener orden y eliminar duplicados
                        java.util.LinkedHashSet<String> diasUnicos = new java.util.LinkedHashSet<>();
                        String horario = "";

                        // Procesar cada día de disponibilidad
                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject disp = jsonArray.getJSONObject(i);
                            Log.d(TAG, "Disponibilidad " + (i+1) + ": " + disp.toString());

                            boolean disponible = disp.optBoolean("disponible", false);
                            String dia = disp.optString("diaSemana", "");
                            String horaIni = disp.optString("horaInicio", "");
                            String horaFin = disp.optString("horaFin", "");

                            Log.d(TAG, "  Día: " + dia + ", Disponible: " + disponible + ", Horario: " + horaIni + " - " + horaFin);

                            if (disponible && !dia.isEmpty()) {
                                diasUnicos.add(dia);
                                Log.d(TAG, "  ✓ Día agregado: " + dia);

                                // Guardar el primer horario válido encontrado
                                if (horario.isEmpty() && !horaIni.isEmpty() && !horaFin.isEmpty()) {
                                    // Formatear horario quitando segundos (08:00:00 -> 08:00)
                                    String horaIniFormat = horaIni.length() > 5 ? horaIni.substring(0, 5) : horaIni;
                                    String horaFinFormat = horaFin.length() > 5 ? horaFin.substring(0, 5) : horaFin;
                                    horario = horaIniFormat + " - " + horaFinFormat;
                                    Log.d(TAG, "  ✓ Horario guardado: " + horario);
                                }
                            }
                        }

                        // Convertir Set a List para poder manipular
                        java.util.List<String> dias = new java.util.ArrayList<>(diasUnicos);

                        Log.d(TAG, "Días únicos disponibles: " + dias.size() + " - " + dias);
                        Log.d(TAG, "Horario: " + horario);

                        // Formatear el texto de disponibilidad
                        StringBuilder disponibilidadText = new StringBuilder();

                        if (dias.isEmpty()) {
                            disponibilidadText.append("Sin horario definido");
                            Log.w(TAG, "No hay días disponibles");
                        } else {
                            // Formatear días de manera más legible
                            if (dias.size() == 7) {
                                disponibilidadText.append("Todos los días");
                            } else if (dias.size() >= 5 &&
                                      dias.contains("Lunes") &&
                                      dias.contains("Martes") &&
                                      dias.contains("Miercoles") &&
                                      dias.contains("Jueves") &&
                                      dias.contains("Viernes")) {
                                disponibilidadText.append("Lun-Vie");
                                // Agregar sábado y domingo si están
                                if (dias.contains("Sabado")) disponibilidadText.append(", Sáb");
                                if (dias.contains("Domingo")) disponibilidadText.append(", Dom");
                            } else {
                                // Abreviar nombres de días para que quepa mejor
                                java.util.List<String> diasAbrev = new java.util.ArrayList<>();
                                for (String dia : dias) {
                                    switch (dia) {
                                        case "Lunes": diasAbrev.add("Lun"); break;
                                        case "Martes": diasAbrev.add("Mar"); break;
                                        case "Miercoles": diasAbrev.add("Mié"); break;
                                        case "Jueves": diasAbrev.add("Jue"); break;
                                        case "Viernes": diasAbrev.add("Vie"); break;
                                        case "Sabado": diasAbrev.add("Sáb"); break;
                                        case "Domingo": diasAbrev.add("Dom"); break;
                                        default: diasAbrev.add(dia); break;
                                    }
                                }
                                disponibilidadText.append(String.join(", ", diasAbrev));
                            }

                            // Agregar horario
                            if (!horario.isEmpty()) {
                                disponibilidadText.append(" • ").append(horario);
                            }
                        }

                        String finalText = disponibilidadText.toString();
                        Log.d(TAG, "✓ Disponibilidad formateada: " + finalText);

                        runOnUiThread(() -> {
                            tvDisponibilidad.setText(finalText);
                            Log.d(TAG, "✓ TextView actualizado en UI");
                        });
                    } else {
                        Log.e(TAG, "❌ Respuesta no exitosa: " + response.code());
                        String errorBody = response.body() != null ? response.body().string() : "Sin cuerpo";
                        Log.e(TAG, "Error body: " + errorBody);
                        runOnUiThread(() -> tvDisponibilidad.setText("Sin horario (Error " + response.code() + ")"));
                    }
                } catch (Exception e) {
                    Log.e(TAG, "❌ ERROR procesando disponibilidad");
                    Log.e(TAG, "Tipo de error: " + e.getClass().getSimpleName());
                    Log.e(TAG, "Mensaje: " + e.getMessage());
                    e.printStackTrace();
                    runOnUiThread(() -> tvDisponibilidad.setText("Error: " + e.getMessage()));
                }
            }
        });
    }

    /**
     * Elimina una habilidad
     */
    private void eliminarHabilidad(int idHabilidad) {
        String url = "http://172.193.118.141:8080/api/habilidades/delete/" + idHabilidad;
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(url)
                .delete()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                Log.e(TAG, "Error al eliminar habilidad: " + e.getMessage());
                runOnUiThread(() -> Toast.makeText(form.this, "Error al eliminar habilidad", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) {
                runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        Toast.makeText(form.this, "Habilidad eliminada exitosamente", Toast.LENGTH_SHORT).show();
                        cargarHabilidadesUsuario();
                    } else {
                        Toast.makeText(form.this, "Error al eliminar habilidad", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    /**
     * Muestra mensaje cuando no hay habilidades
     */
    private void mostrarMensajeNoHabilidades() {
        containerHabilidades.removeAllViews();
        tvNoHabilidades.setVisibility(android.view.View.VISIBLE);
    }
}
