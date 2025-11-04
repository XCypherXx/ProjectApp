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

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.utp.project.adapters.CalendarAdapter;
import com.utp.project.adapters.PlanAdapter;
import com.utp.project.models.CalendarDayModel;
import com.utp.project.models.PlanModel;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

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

    // Adaptadores para los RecyclerViews de planes
    private PlanAdapter proximosPlanesAdapter;
    private PlanAdapter planesTerminadosAdapter;
    private List<PlanModel> proximosPlanesList;
    private List<PlanModel> planesTerminadosList;
    private ListenerRegistration actividadesListener;

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

        // Cargar actividades desde Firestore
        loadActividadesFromFirestore();

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Limpiar el listener cuando se destruye la vista
        if (actividadesListener != null) {
            actividadesListener.remove();
        }
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

        // Inicializar lista vacía que se llenará con datos de Firestore
        proximosPlanesList = new ArrayList<>();

        // Configurar el LayoutManager (vertical) y el Adaptador
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        proximosPlanesAdapter = new PlanAdapter(
                getContext(),
                proximosPlanesList,
                R.layout.item_plan_proximo,
                (actividadId, completed) -> {
                    if (actividadId == null) return;
                    java.util.Map<String, Object> campos = new java.util.HashMap<>();
                    campos.put("estado", completed ? "completado" : "pendiente");
                    com.utp.project.data.FirestoreService.updateActividad(actividadId, campos);
                }
        );
        recyclerView.setAdapter(proximosPlanesAdapter);
    }

    private void setupPlanesTerminados(View view) {
        RecyclerView recyclerView = view.findViewById(R.id.recycler_planes_terminados);

        // Inicializar lista vacía que se llenará con datos de Firestore
        planesTerminadosList = new ArrayList<>();

        // Configurar el LayoutManager y el Adaptador
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        planesTerminadosAdapter = new PlanAdapter(
                getContext(),
                planesTerminadosList,
                R.layout.item_plan_terminado,
                (actividadId, completed) -> {
                    if (actividadId == null) return;
                    java.util.Map<String, Object> campos = new java.util.HashMap<>();
                    campos.put("estado", completed ? "completado" : "pendiente");
                    com.utp.project.data.FirestoreService.updateActividad(actividadId, campos);
                }
        );
        recyclerView.setAdapter(planesTerminadosAdapter);
    }

    /**
     * Carga las actividades desde Firestore y las separa en próximos y terminados
     */
    private void loadActividadesFromFirestore() {
        actividadesListener = com.utp.project.data.FirestoreService.listenActividades((snap, e) -> {
            if (e != null) {
                Toast.makeText(getContext(), "Error al cargar actividades: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }

            if (snap == null || snap.isEmpty()) {
                // Limpiar_todo
                proximosPlanesList.clear();
                planesTerminadosList.clear();
                clearCurrentPlanUI();
                proximosPlanesAdapter.notifyDataSetChanged();
                planesTerminadosAdapter.notifyDataSetChanged();
                return;
            }

            // Obtener la fecha de hoy para filtrar
            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);
            today.set(Calendar.MILLISECOND, 0);

            Calendar tomorrow = (Calendar) today.clone();
            tomorrow.add(Calendar.DAY_OF_YEAR, 1);

            // Limpiar las listas
            proximosPlanesList.clear();
            planesTerminadosList.clear();

            PlanModel planEnCurso = null;
            // Procesar cada documento
            for (DocumentSnapshot doc : snap.getDocuments()) {
                PlanModel plan = convertDocumentToPlanModel(doc);
                if (plan == null) continue;

                Timestamp fechaInicio = doc.getTimestamp("fechaInicio");
                String estado = doc.getString("estado");

                if (fechaInicio != null) {
                    Calendar fechaInicioCal = Calendar.getInstance();
                    fechaInicioCal.setTimeInMillis(fechaInicio.toDate().getTime());

                    // Verificar si la actividad es de hoy
                    boolean isToday = fechaInicioCal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                            fechaInicioCal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR);
                   // boolean isFuture = fechaInicioCal.getTimeInMillis() >= tomorrow.getTimeInMillis();
                    boolean isFuture = fechaInicioCal.after(today);
                    if (estado != null && estado.equals("completado")) {
                        planesTerminadosList.add(plan);
                    } else if (isToday) {
                        // Solo debe haber uno "en curso" hoy
                        if (planEnCurso == null) planEnCurso = plan;
                    } else if (isFuture) {
                        proximosPlanesList.add(plan);
                    }
                }
            }

            // Ordenar Próximos planes por fecha de inicio (ascendente)
            proximosPlanesList.sort((a, b) -> Long.compare(a.getStartTimeMs(), b.getStartTimeMs()));
            proximosPlanesAdapter.notifyDataSetChanged();
            planesTerminadosAdapter.notifyDataSetChanged();

            // Mostrar plan en curso (si existe)
            if (planEnCurso != null) {
                updateCurrentPlanUI(planEnCurso);
            } else {
                clearCurrentPlanUI();
            }
        });
    }


    /**
     * Muestra el plan actual en la tarjeta de "Plan en curso"
     */
    private void updateCurrentPlanUI(PlanModel plan) {
        View view = getView();
        if (view == null) return;

        TextView time = view.findViewById(R.id.text_current_plan_time);
        TextView title = view.findViewById(R.id.text_current_plan_title);
        TextView details = view.findViewById(R.id.text_current_plan_details);
        LinearProgressIndicator progress = view.findViewById(R.id.progress_current_plan);

        // Mostrar solo la hora final del rango (si existe)
        String horaFinal;
        String[] partes = plan.getTimeRange().split("-");
        if (partes.length > 1) {
            horaFinal = partes[1].trim(); // ejemplo: "10:00 AM"
        } else {
            horaFinal = plan.getTimeRange(); // fallback si no hay "-"
        }

        time.setText(horaFinal);
        title.setText("Siguiente actividad: " + plan.getTitle());
        details.setText("Plan pendiente para hoy");

        progress.setProgress(0, true);
    }

    /**
     * Limpia la tarjeta "Plan en curso" cuando no hay actividades para hoy
     */
    private void clearCurrentPlanUI() {
        View view = getView();
        if (view == null) return;

        TextView time = view.findViewById(R.id.text_current_plan_time);
        TextView title = view.findViewById(R.id.text_current_plan_title);
        TextView details = view.findViewById(R.id.text_current_plan_details);
        LinearProgressIndicator progress = view.findViewById(R.id.progress_current_plan);

        time.setText("--:--");
        title.setText("Sin planes para hoy");
        details.setText("No tienes actividades pendientes hoy.");
        progress.setProgress(0);
    }


    /**
     * Convierte un DocumentSnapshot de Firestore a un PlanModel
     */
    private PlanModel convertDocumentToPlanModel(DocumentSnapshot doc) {
        try {
            String id = doc.getId();
            String titulo = doc.getString("titulo");
            if (titulo == null || titulo.isEmpty()) {
                titulo = "Sin título";
            }

            // Obtener las fechas de inicio y fin
            Timestamp fechaInicio = doc.getTimestamp("fechaInicio");
            Timestamp fechaFin = doc.getTimestamp("fechaFin");

            String timeRange = "Sin hora";
            if (fechaInicio != null && fechaFin != null) {
                timeRange = formatTimeRange(fechaInicio.toDate(), fechaFin.toDate());
            } else if (fechaInicio != null) {
                timeRange = formatTime(fechaInicio.toDate());
            }

            // Determinar el color según el estado
            String estado = doc.getString("estado");
            int colorBar;
            int colorBackground;

            boolean completed = (estado != null && estado.equals("completado"));
            if (completed) {
                // Rojo para terminados
                colorBar = R.color.red_bar;
                colorBackground = R.color.red_background_alpha;
            } else {
                // Amarillo para pendientes
                colorBar = R.color.yellow_bar;
                colorBackground = R.color.yellow_background_alpha;
            }

            long startMs = fechaInicio != null ? fechaInicio.toDate().getTime() : Long.MAX_VALUE;
            return new PlanModel(id, titulo, timeRange, colorBar, colorBackground, completed, startMs);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Formatea un rango de tiempo en formato "HH:mm AM/PM - HH:mm AM/PM"
     */
    private String formatTimeRange(java.util.Date inicio, java.util.Date fin) {
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", new Locale("es", "ES"));
        return sdf.format(inicio) + " - " + sdf.format(fin);
    }

    /**
     * Formatea una fecha en formato "HH:mm AM/PM"
     */
    private String formatTime(java.util.Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", new Locale("es", "ES"));
        return sdf.format(date);
    }
}
