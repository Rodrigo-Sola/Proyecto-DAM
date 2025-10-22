package sv.edu.itca.proyecto_dam;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

/**
 * Fragment para la segunda pantalla del onboarding
 */
public class OnboardingFragment2 extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_onboarding_2, container, false);

        // Obtener referencia al ViewPager2
        ViewPager2 viewPager = requireActivity().findViewById(R.id.main);

        // Configurar botón Saltar (TextView clickeable)
        TextView btnSaltar = view.findViewById(R.id.btnSaltar);
        btnSaltar.setOnClickListener(v -> {
            // Redirigir a MainActivity
            Intent intent = new Intent(requireActivity(), MainActivity.class);
            startActivity(intent);
            requireActivity().finish();
        });

        // Configurar botón Anterior (retroceder)
        Button btnPrevious = view.findViewById(R.id.btnPrevious);
        btnPrevious.setOnClickListener(v -> {
            // Retroceder al fragment anterior
            viewPager.setCurrentItem(0, true);
        });

        // Configurar botón Siguiente
        Button btnNext = view.findViewById(R.id.btnNext);
        btnNext.setOnClickListener(v -> {
            // Avanzar al siguiente fragment
            viewPager.setCurrentItem(2, true);
        });

        return view;
    }
}
