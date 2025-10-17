package sv.edu.itca.proyecto_dam;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

/**
 * Fragment para la tercera pantalla del onboarding
 */
public class OnboardingFragment3 extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_onboarding_3, container, false);

        // Obtener referencia al ViewPager2
        ViewPager2 viewPager = requireActivity().findViewById(R.id.main);

        // Configurar botón Atrás
        Button btnBack = view.findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> {
            // Retroceder al fragment anterior
            viewPager.setCurrentItem(1, true);
        });

        // Configurar botón Comenzar
        Button btnInit = view.findViewById(R.id.btnInit);
        btnInit.setOnClickListener(v -> {
            // Redirigir a MainActivity
            Intent intent = new Intent(requireActivity(), MainActivity.class);
            startActivity(intent);
            requireActivity().finish();
        });

        return view;
    }
}
