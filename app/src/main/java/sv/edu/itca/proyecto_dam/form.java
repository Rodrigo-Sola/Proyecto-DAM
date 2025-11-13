package sv.edu.itca.proyecto_dam;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class form extends AppCompatActivity {

    private Button Guardar, Cancelar;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_form);
        Guardar = findViewById(R.id.btnGuardar);
        Cancelar = findViewById(R.id.btnCancelar);

        setonClickListener();
    }

    private void setonClickListener()
    {
        Guardar.setOnClickListener(view ->  guarrarHabilidad());
        Cancelar.setOnClickListener(view -> cancelarActivyti());


    }

    private void cancelarActivyti() {
        Intent intent = new Intent(this, Home2Activity.class);
        startActivity(intent);
        finish();
    }

    private void guarrarHabilidad() {

        Intent intent = new Intent(this, Home2Activity.class);
        startActivity(intent);
        finish();
    }




}