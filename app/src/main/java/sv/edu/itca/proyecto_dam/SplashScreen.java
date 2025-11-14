package sv.edu.itca.proyecto_dam;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.BounceInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.ProgressBar;
import androidx.appcompat.app.AppCompatActivity;

@SuppressLint("CustomSplashScreen")
public class SplashScreen extends AppCompatActivity {

    private ProgressBar progressBar;
    private ImageView ivStar;
    private ImageView ivLogo;
    private ImageView ivBienvenido;
    private final Handler handler = new Handler();

    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_screen);

        // Inicializar SharedPreferences
        prefs = getSharedPreferences("AppPreferences", MODE_PRIVATE);

        progressBar = findViewById(R.id.progressBar);
        ivStar = findViewById(R.id.ivStar);
        ivLogo = findViewById(R.id.ivlogo);
        ivBienvenido = findViewById(R.id.ivBienvenido);

        // Iniciar animación del logo
        startLogoAnimation();
    }

    private void startLogoAnimation() {
        // Obtener el alto de la pantalla para calcular la caída
        int screenHeight = getResources().getDisplayMetrics().heightPixels;

        // Posicionar el logo arriba de la pantalla
        ivLogo.setTranslationY(-screenHeight);
        ivLogo.setAlpha(1f);

        // Animación: Logo cae desde arriba al centro con rebote
        ivLogo.animate()
                .translationY(0)
                .setDuration(1000)
                .setInterpolator(new BounceInterpolator())
                .withEndAction(() -> {
                    // Después de 500ms, mover logo a la izquierda y mostrar imagen "Bienvenido"
                    handler.postDelayed(() -> {
                        // Mover logo hacia la izquierda
                        ivLogo.animate()
                                .translationX(-150)
                                .setDuration(600)
                                .setInterpolator(new AccelerateInterpolator())
                                .start();

                        // Mostrar imagen "Bienvenido" a la derecha (al lado del logo)
                        ivBienvenido.setTranslationX(300);
                        ivBienvenido.animate()
                                .alpha(1f)
                                .translationX(150)
                                .setDuration(600)
                                .setInterpolator(new OvershootInterpolator())
                                .withEndAction(() -> {
                                    // Después de mostrar "Bienvenido", iniciar la barra de progreso
                                    handler.postDelayed(this::startProgressAnimation, 500);
                                })
                                .start();
                    }, 500);
                })
                .start();
    }

    private void startProgressAnimation() {
        // Iniciar el efecto de brillo cada segundo
        startBlinkEffect();

        new Thread(() -> {
            // FASE 1: Cargar del 0% al 80% RÁPIDO (2 segundos)
            for (int i = 0; i <= 80; i++) {
                final int progress = i;
                handler.post(() -> {
                    progressBar.setProgress(progress);
                    moveStarToProgress(progress);
                });

                try {
                    Thread.sleep(2000 / 80); // 2000ms / 80 pasos = 25ms por paso (RÁPIDO)
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            // FASE 2: Cargar del 81% al 100% LENTO (2 segundos)
            for (int i = 81; i <= 100; i++) {
                final int progress = i;
                handler.post(() -> {
                    progressBar.setProgress(progress);
                    moveStarToProgress(progress);
                });

                try {
                    Thread.sleep(2000 / 20); // 2000ms / 20 pasos = 100ms por paso (LENTO)
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            handler.post(() -> {
                // Verificar si el onboarding ya fue completado
                boolean onboardingCompleted = prefs.getBoolean("onboarding_completed", false);

                Intent intent;
                if (onboardingCompleted) {
                    // Si ya completó el onboarding, ir directo al login
                    intent = new Intent(SplashScreen.this, MainActivity.class);
                } else {
                    // Si no, mostrar el onboarding
                    intent = new Intent(SplashScreen.this, OnboardingActivity.class);
                }

                startActivity(intent);
                finish();
            });
        }).start();
    }

    private void moveStarToProgress(int progress) {
        View starGlow = findViewById(R.id.starGlow);

        progressBar.post(() -> {
            int progressBarWidth = progressBar.getWidth();
            int starWidth = ivStar.getWidth();

            // Calcular la posición X basada en el progreso
            float progressPosition = (progressBarWidth - starWidth) * (progress / 100f);

            // Mover la estrella SIN rotación
            ivStar.animate()
                    .translationX(progressPosition)
                    .setDuration(50)
                    .start();

            // Mover resplandor junto con la estrella
            starGlow.animate()
                    .translationX(progressPosition - 10)
                    .setDuration(50)
                    .start();
        });
    }

    private void startBlinkEffect() {
        View starGlow = findViewById(R.id.starGlow);

        final Runnable blinkRunnable = new Runnable() {
            @Override
            public void run() {
                // Mostrar resplandor
                starGlow.setVisibility(View.VISIBLE);
                starGlow.setAlpha(0f);

                // Animar resplandor
                starGlow.animate()
                        .alpha(1f)
                        .scaleX(1.3f)
                        .scaleY(1.3f)
                        .setDuration(200)
                        .withEndAction(() -> starGlow.animate()
                                .alpha(0f)
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(300)
                                .start())
                        .start();

                // Efecto de brillo en la estrella
                ivStar.animate()
                        .scaleX(1.4f)
                        .scaleY(1.4f)
                        .setDuration(200)
                        .withEndAction(() -> ivStar.animate()
                                .scaleX(1.0f)
                                .scaleY(1.0f)
                                .setDuration(200)
                                .start())
                        .start();

                // Repetir cada 1000ms (1 segundo)
                handler.postDelayed(this, 1000);
            }
        };

        // Iniciar el efecto
        handler.post(blinkRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Limpiar todos los callbacks del handler
        handler.removeCallbacksAndMessages(null);
    }
}