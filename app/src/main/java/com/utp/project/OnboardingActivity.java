package com.utp.project;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import java.util.ArrayList;
import java.util.List;
public class OnboardingActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private OnboardingAdapter adapter;
    private List<OnboardingItem> items;
    private Button nextButton, skipButton;
    private LinearLayout pageIndicators;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        // Referencias
        viewPager = findViewById(R.id.view_pager);
        nextButton = findViewById(R.id.next_button);
        skipButton = findViewById(R.id.skip_button);
        pageIndicators = findViewById(R.id.page_indicators);


        items = new ArrayList<>();
        items.add(new OnboardingItem(R.drawable.ic_call, "Llama fácil", "Haz llamadas directamente desde la app"));
        items.add(new OnboardingItem(R.drawable.ic_calendar, "Agenda", "Revisa tus citas y tareas"));
        items.add(new OnboardingItem(R.drawable.ic_home, "Inicio", "Accede rápido a todo lo que necesitas"));

        adapter = new OnboardingAdapter(items);
        viewPager.setAdapter(adapter);
        setupIndicators();
        setCurrentIndicator(0);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                setCurrentIndicator(position);
            }
        });

        nextButton.setOnClickListener(v -> {
            int current = viewPager.getCurrentItem();
            if (current < adapter.getItemCount() - 1) {
                viewPager.setCurrentItem(current + 1);
            } else {
                openHome();
            }
        });
        skipButton.setOnClickListener(v -> openHome());
    }

    private void setupIndicators() {
        pageIndicators.removeAllViews();
        for (int i = 0; i < items.size(); i++) {
            View indicator = new View(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(20, 20);
            params.setMargins(8, 0, 8, 0);
            indicator.setLayoutParams(params);
            indicator.setBackgroundResource(R.drawable.indicator_inactive);
            pageIndicators.addView(indicator);
        }
    }

    private void setCurrentIndicator(int index) {
        for (int i = 0; i < pageIndicators.getChildCount(); i++) {
            View child = pageIndicators.getChildAt(i);
            if (i == index) {
                child.setBackgroundResource(R.drawable.indicator_active);
            } else {
                child.setBackgroundResource(R.drawable.indicator_inactive);
            }
        }
    }

    private void openHome() {
        // Guardar en SharedPreferences que ya vio onboarding
        getSharedPreferences("prefs", MODE_PRIVATE)
                .edit()
                .putBoolean("isFirstTime", false)
                .apply();

        // Abrir Home
        startActivity(new Intent(OnboardingActivity.this, HomeActivity.class));
        finish();
    }
}
