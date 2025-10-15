package com.utp.project;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.utp.project.adapters.CalendarAdapter;
import com.utp.project.adapters.PlanAdapter;
import com.utp.project.models.CalendarDayModel;
import com.utp.project.models.PlanModel;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class CalendarFragment extends Fragment implements CalendarAdapter.OnDateSelectedListener {

    private RecyclerView calendarRecyclerView;
    private List<CalendarDayModel> allDates;
    private LinearLayoutManager layoutManager;
    private CalendarAdapter calendarAdapter;

    // Cantidad inicial de días a cargar hacia atrás y adelante desde hoy (60 días total)
    private final int PRELOAD_DAYS = 30;

    // Bandera para evitar llamadas duplicadas de carga durante el scroll
    private boolean isLoading = false;

    private TextView textFechaToday;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Asegúrate de que R.layout.fragment_calendar contenga el RecyclerView con id: recycler_calendar
        View view = inflater.inflate(R.layout.fragment_calendar, container, false);

        // 1. Inicializar y asignar la fecha de hoy
        textFechaToday = view.findViewById(R.id.text_fecha_today);
        setCurrentDate(); // Llama al nuevo metodo para poner la fecha

        calendarRecyclerView = view.findViewById(R.id.recycler_calendar);

        initializeCalendar();

        setupProximosPlanes(view); // <-- LLAMADA PARA INICIALIZAR LA LISTA DE PLANES

        setupPlanesTerminados(view); // <-- LLAMADA PARA INICIALIZAR LA LISTA DE PLANES TERMINADOS

        return view;
    }

    private void setCurrentDate() {
        // Obtenemos una instancia de Calendar para la fecha actual
        Calendar calendar = Calendar.getInstance();

        // Define el formato de fecha en español (ej: "Miércoles, 15 de Octubre")
        // Se usa 'Locale("es", "ES")' para asegurar el idioma
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, d 'de' MMMM", new Locale("es", "ES"));

        // Formatea la fecha
        String formattedDate = sdf.format(calendar.getTime());

        // Aseguramos que la primera letra sea mayúscula para buena presentación
        formattedDate = formattedDate.substring(0, 1).toUpperCase() + formattedDate.substring(1);

        // Asigna el texto al TextView
        textFechaToday.setText(formattedDate);
    }

    private void initializeCalendar() {
        // 1. Generar la lista inicial de fechas (30 días atrás + Hoy + 30 días adelante)
        allDates = generateInitialDates(PRELOAD_DAYS);

        // 2. Configurar el RecyclerView para scroll horizontal
        layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        calendarRecyclerView.setLayoutManager(layoutManager);

        calendarAdapter = new CalendarAdapter(getContext(), allDates, this);
        calendarRecyclerView.setAdapter(calendarAdapter);

        // 3. Desplazar al día de hoy (que está en la posición PRELOAD_DAYS)
        // Usamos post() para asegurar que el RecyclerView ya haya sido medido.
        calendarRecyclerView.post(() -> {
            if (calendarRecyclerView.getChildCount() > 0) {
                // Cálculo para centrar el día actual en la vista de 5 días
                int itemWidth = calendarRecyclerView.getChildAt(0).getWidth();
                int offset = (calendarRecyclerView.getWidth() / 2) - (itemWidth / 2);
                layoutManager.scrollToPositionWithOffset(PRELOAD_DAYS, offset);
            } else {
                // Fallback
                layoutManager.scrollToPosition(PRELOAD_DAYS);
            }
        });


        // 4. Implementar la lógica de carga continua (scroll infinito)
        setupContinuousScroll();
    }

    /**
     * Genera la lista inicial de días simétricamente alrededor de la fecha actual.
     */
    private List<CalendarDayModel> generateInitialDates(int days) {
        List<CalendarDayModel> dates = new ArrayList<>();
        LocalDate today = LocalDate.now();

        // Fechas hacia atrás (Pasado)
        for (int i = days; i >= 1; i--) {
            dates.add(new CalendarDayModel(today.minusDays(i)));
        }

        // Hoy (Centro de la lista inicial)
        dates.add(new CalendarDayModel(today));

        // Fechas hacia adelante (Futuro)
        for (int i = 1; i <= days; i++) {
            dates.add(new CalendarDayModel(today.plusDays(i)));
        }
        return dates;
    }

    /**
     * Configura el listener de scroll para detectar cuando se necesita cargar más fechas
     * al acercarse a los límites de la lista.
     */
    private void setupContinuousScroll() {
        calendarRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                int totalItemCount = layoutManager.getItemCount();
                int lastVisiblePosition = layoutManager.findLastVisibleItemPosition();
                int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                // Cargar más días al final (Futuro) cuando quedan 10 ítems o menos
                if (!isLoading && lastVisiblePosition >= totalItemCount - 10) {
                    loadMoreDates(30, true);
                }

                // Cargar más días al inicio (Pasado) cuando quedan 10 ítems o menos
                if (!isLoading && firstVisibleItemPosition < 10) {
                    loadMoreDates(30, false);
                }
            }
        });
    }

    /**
     * Carga nuevos días y actualiza el adaptador, ajustando el scroll para mantener la posición.
     * @param numDays Cantidad de días a cargar.
     * @param isAppending True para futuro (final), False para pasado (inicio).
     */
    private synchronized void loadMoreDates(int numDays, boolean isAppending) {
        isLoading = true;
        if (allDates.isEmpty()) {
            isLoading = false;
            return;
        }

        if (isAppending) {
            // Cargar fechas al final (Futuro)
            LocalDate lastDate = allDates.get(allDates.size() - 1).getDate();
            for (int i = 1; i <= numDays; i++) {
                allDates.add(new CalendarDayModel(lastDate.plusDays(i)));
            }
            calendarAdapter.notifyItemRangeInserted(allDates.size() - numDays, numDays);
        } else {
            // Cargar fechas al inicio (Pasado)
            LocalDate firstDate = allDates.get(0).getDate();
            List<CalendarDayModel> newDates = new ArrayList<>();
            for (int i = numDays; i >= 1; i--) {
                newDates.add(new CalendarDayModel(firstDate.minusDays(i)));
            }

            // Agregar las nuevas fechas al inicio
            allDates.addAll(0, newDates);
            calendarAdapter.notifyItemRangeInserted(0, numDays);

            // Ajustar el scroll para que la posición visible no cambie al insertar
            int adjustment = numDays;
            layoutManager.scrollToPosition(adjustment);
        }
        isLoading = false;
    }

    /**
     * Callback del adaptador cuando se selecciona un nuevo día.
     */
    @Override
    public void onDateSelected(LocalDate date) {
        // Muestra un mensaje temporal con la fecha seleccionada
        Toast.makeText(getContext(), "Cargando actividades para: " + date, Toast.LENGTH_SHORT).show();
    }

    private void setupProximosPlanes(View view) {
        RecyclerView recyclerView = view.findViewById(R.id.recycler_proximos_planes);

        // 1. Crear la lista de datos de ejemplo (¡Asegúrate de definir estos colores en R.color!)
        List<PlanModel> planes = new ArrayList<>();

        // Nota: Reemplaza R.color.yellow_bar y R.color.yellow_background_alpha con tus colores reales
        // que imiten el #FFE100 y el #60FFE100 de tu XML.

        // Ejemplo Plan 1 (Amarillo)
        planes.add(new PlanModel(
                "Preparar mi cena",
                "11:45 PM - 12:00 AM",
                R.color.yellow_bar, // Color sólido para la barra lateral
                R.color.yellow_background_alpha // Color semitransparente para el fondo
        ));

        // Ejemplo Plan 2 (Amarillo)
        planes.add(new PlanModel(
                "Dormir",
                "12:00 AM - 06:00 AM",
                R.color.yellow_bar,
                R.color.yellow_background_alpha
        ));

        // 2. Configurar el LayoutManager (vertical) y el Adaptador
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        PlanAdapter adapter = new PlanAdapter(getContext(), planes);
        recyclerView.setAdapter(adapter);
    }
    private void setupPlanesTerminados(View view) {
        RecyclerView recyclerView = view.findViewById(R.id.recycler_planes_terminados);

        // 1. Crear la lista de datos de ejemplo
        List<PlanModel> planesTerminados = new ArrayList<>();

        // ¡Asegúrate de definir R.color.red_bar y R.color.red_background_alpha en R.color!

        // Ejemplo Plan 1 (Rojo)
        planesTerminados.add(new PlanModel(
                "Preparar mi cena",
                "11:45 PM - 12:00 AM",
                R.color.red_bar, // Color sólido para la barra lateral (rojo #FF0000)
                R.color.red_background_alpha // Color semitransparente para el fondo (rojo #60FF0000)
        ));

        // 2. Configurar el LayoutManager y el Adaptador
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        // ¡REUTILIZAMOS EL MISMO ADAPTADOR!
        PlanAdapter adapter = new PlanAdapter(getContext(), planesTerminados);
        recyclerView.setAdapter(adapter);
    }
}
