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
    private LocalDate selectedDate = LocalDate.now();

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

        // 3. MEJORADO: Desplazar al día de hoy y centrarlo en el medio del contenedor
        // Usamos post() para asegurar que el RecyclerView ya haya sido medido.
        calendarRecyclerView.post(() -> {
            int todayPosition = PRELOAD_DAYS; // La posición del día de hoy
            if (todayPosition < allDates.size()) {
                // Obtener el ancho del RecyclerView
                int recyclerWidth = calendarRecyclerView.getWidth();
                // Obtener el ancho de un item (asumiendo que todos tienen el mismo ancho)
                View firstChild = layoutManager.findViewByPosition(0);
                if (firstChild != null) {
                    int itemWidth = firstChild.getWidth();
                    // Calcular el offset para centrar el día de hoy
                    int offset = (recyclerWidth / 2) - (itemWidth / 2);
                    layoutManager.scrollToPositionWithOffset(todayPosition, offset);
                } else {
                    // Si no hay vista visible, usar un delay para esperar a que se renderice
                    calendarRecyclerView.postDelayed(() -> {
                        View child = layoutManager.findViewByPosition(todayPosition);
                        if (child != null) {
                            int itemWidth = child.getWidth();
                            // Reutilizar recyclerWidth del scope externo o recalcular
                            int recyclerWidthDelayed = calendarRecyclerView.getWidth();
                            int offset = (recyclerWidthDelayed / 2) - (itemWidth / 2);
                            layoutManager.scrollToPositionWithOffset(todayPosition, offset);
                        } else {
                            // Fallback: simplemente desplazar a la posición
                            layoutManager.scrollToPosition(todayPosition);
                        }
                    }, 100);
                }
            } else {
                // Fallback
                layoutManager.scrollToPosition(todayPosition);
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
        // Actualizar la fecha seleccionada
        selectedDate = date;

        // Actualizar el texto de la fecha superior
        updateDateText(date);

        // Recargar las actividades filtradas por la fecha seleccionada
        loadActividadesFromFirestore();
    }

    private void updateDateText(LocalDate date) {
        if (textFechaToday == null) return;

        // Convertir LocalDate a Calendar para formatear
        Calendar calendar = Calendar.getInstance();
        calendar.set(date.getYear(), date.getMonthValue() - 1, date.getDayOfMonth());

        // Formatear en español
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, d 'de' MMMM", new Locale("es", "ES"));
        String formattedDate = sdf.format(calendar.getTime());
        formattedDate = formattedDate.substring(0, 1).toUpperCase() + formattedDate.substring(1);

        textFechaToday.setText(formattedDate);
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
        // CAMBIO: Usar item_plan_proximo.xml en lugar de item_plan_terminado.xml
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        planesTerminadosAdapter = new PlanAdapter(
                getContext(),
                planesTerminadosList,
                R.layout.item_plan_terminado,  // CAMBIO: Usar el mismo layout que próximos planes
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
                // Limpiar todo
                proximosPlanesList.clear();
                planesTerminadosList.clear();
                clearCurrentPlanUI();
                proximosPlanesAdapter.notifyDataSetChanged();
                planesTerminadosAdapter.notifyDataSetChanged();
                return;
            }

            // Obtener la fecha y hora actual
            Calendar now = Calendar.getInstance();

            // NUEVO: Obtener la fecha seleccionada en el calendario
            Calendar selectedDateCal = Calendar.getInstance();
            selectedDateCal.set(selectedDate.getYear(), selectedDate.getMonthValue() - 1, selectedDate.getDayOfMonth());
            selectedDateCal.set(Calendar.HOUR_OF_DAY, 0);
            selectedDateCal.set(Calendar.MINUTE, 0);
            selectedDateCal.set(Calendar.SECOND, 0);
            selectedDateCal.set(Calendar.MILLISECOND, 0);

            // Calcular el inicio y fin del día seleccionado
            Calendar startOfSelectedDay = (Calendar) selectedDateCal.clone();
            Calendar endOfSelectedDay = (Calendar) selectedDateCal.clone();
            endOfSelectedDay.add(Calendar.DAY_OF_YEAR, 1);
            endOfSelectedDay.add(Calendar.MILLISECOND, -1);

            // Limpiar las listas
            proximosPlanesList.clear();
            planesTerminadosList.clear();

            PlanModel planEnCurso = null;
            String descripcionEnCurso = null;
            String horaInicioFinEnCurso = null;
            String tituloEnCurso = null;

            // Procesar cada documento
            for (DocumentSnapshot doc : snap.getDocuments()) {
                Timestamp fechaInicio = doc.getTimestamp("fechaInicio");
                Timestamp fechaFin = doc.getTimestamp("fechaFin");
                String estado = doc.getString("estado");

                if (fechaInicio == null) continue;

                Calendar fechaInicioCal = Calendar.getInstance();
                fechaInicioCal.setTimeInMillis(fechaInicio.toDate().getTime());

                Calendar fechaFinCal = null;
                if (fechaFin != null) {
                    fechaFinCal = Calendar.getInstance();
                    fechaFinCal.setTimeInMillis(fechaFin.toDate().getTime());
                }

                // NUEVO: Filtrar por la fecha seleccionada
                // Verificar si la actividad pertenece al día seleccionado
                boolean belongsToSelectedDay = false;

                // Normalizar la fecha de inicio al inicio del día para comparar
                Calendar fechaInicioNormalizada = (Calendar) fechaInicioCal.clone();
                fechaInicioNormalizada.set(Calendar.HOUR_OF_DAY, 0);
                fechaInicioNormalizada.set(Calendar.MINUTE, 0);
                fechaInicioNormalizada.set(Calendar.SECOND, 0);
                fechaInicioNormalizada.set(Calendar.MILLISECOND, 0);

                // Verificar si la fecha de inicio está en el día seleccionado
                if (fechaInicioNormalizada.equals(selectedDateCal)) {
                    belongsToSelectedDay = true;
                } else if (fechaFinCal != null) {
                    // También verificar si la actividad se extiende al día seleccionado
                    Calendar fechaFinNormalizada = (Calendar) fechaFinCal.clone();
                    fechaFinNormalizada.set(Calendar.HOUR_OF_DAY, 0);
                    fechaFinNormalizada.set(Calendar.MINUTE, 0);
                    fechaFinNormalizada.set(Calendar.SECOND, 0);
                    fechaFinNormalizada.set(Calendar.MILLISECOND, 0);

                    // Si la actividad comienza antes del día seleccionado pero termina en o después del día seleccionado
                    if (fechaInicioNormalizada.before(selectedDateCal) &&
                            (fechaFinNormalizada.equals(selectedDateCal) || fechaFinNormalizada.after(selectedDateCal))) {
                        belongsToSelectedDay = true;
                    }
                }

                // Si la actividad no pertenece al día seleccionado, saltarla
                if (!belongsToSelectedDay) {
                    continue;
                }

                // PRIORIDAD: Verificar si está en curso (hora actual entre inicio y fin)
                // Solo verificar si estamos en el día de hoy
                boolean isEnCurso = false;
                boolean isToday = selectedDate.equals(LocalDate.now());

                if (isToday && fechaFinCal != null) {
                    // Verificar si la hora actual está entre inicio y fin (inclusive)
                    isEnCurso = (now.after(fechaInicioCal) || now.equals(fechaInicioCal))
                            && (now.before(fechaFinCal) || now.equals(fechaFinCal));
                } else if (isToday) {
                    // Si no hay fecha fin, verificar si ya pasó la fecha de inicio
                    isEnCurso = now.after(fechaInicioCal) || now.equals(fechaInicioCal);
                }

                // MODIFICACIÓN: Verificar si la actividad ya terminó
                boolean isTerminado = false;
                if (fechaFinCal != null) {
                    // Si ya pasó la hora de fin, está terminado
                    isTerminado = now.after(fechaFinCal);
                } else {
                    // Si no hay fecha fin, considerar terminado si pasó mucho tiempo desde inicio
                    Calendar fechaInicioMas1Hora = (Calendar) fechaInicioCal.clone();
                    fechaInicioMas1Hora.add(Calendar.HOUR_OF_DAY, 1);
                    isTerminado = now.after(fechaInicioMas1Hora);
                }

                // NUEVO: Si la actividad terminó y no está marcada como completada, actualizarla automáticamente
                if (isTerminado && (estado == null || !estado.equals("completado"))) {
                    String actividadId = doc.getId();
                    java.util.Map<String, Object> campos = new java.util.HashMap<>();
                    campos.put("estado", "completado");
                    // Actualizar en Firestore de forma asíncrona
                    com.utp.project.data.FirestoreService.updateActividad(actividadId, campos)
                            .addOnSuccessListener(aVoid -> {
                                // Actualización exitosa, el listener se disparará de nuevo y actualizará la UI
                            })
                            .addOnFailureListener(error -> {
                                // Error al actualizar, pero continuamos con el flujo normal
                                android.util.Log.e("CalendarFragment", "Error al marcar actividad como completada: " + error.getMessage());
                            });
                    // Actualizar el estado localmente para que se refleje inmediatamente
                    estado = "completado";
                }

                PlanModel plan = convertDocumentToPlanModel(doc, estado); // Pasar el estado actualizado
                if (plan == null) continue;

                // Clasificar la actividad: PRIORIDAD para "en curso" (solo si es hoy)
                if (isToday && isEnCurso) {
                    // Plan en curso: desaparece de "próximo plan" y se traslada aquí
                    // Solo uno puede estar en curso (tomar el primero que cumpla la condición)
                    if (planEnCurso == null) {
                        planEnCurso = plan;
                        tituloEnCurso = doc.getString("titulo");
                        if (tituloEnCurso == null || tituloEnCurso.isEmpty()) {
                            tituloEnCurso = "Sin título";
                        }
                        descripcionEnCurso = doc.getString("descripcion");
                        if (descripcionEnCurso == null || descripcionEnCurso.isEmpty()) {
                            descripcionEnCurso = "Sin descripción";
                        }
                        // Formatear hora inicio-fin
                        if (fechaInicio != null && fechaFin != null) {
                            horaInicioFinEnCurso = formatTimeRange(fechaInicio.toDate(), fechaFin.toDate());
                        } else if (fechaInicio != null) {
                            horaInicioFinEnCurso = formatTime(fechaInicio.toDate());
                        }
                    }
                    // IMPORTANTE: No agregar a ninguna lista (ni próximos ni terminados)
                    // porque está en curso
                } else {
                    // Si NO está en curso, verificar si ya terminó o está pendiente
                    // Verificar si está completado manualmente o automáticamente
                    boolean isCompletado = (estado != null && estado.equals("completado"));

                    if (isCompletado || isTerminado) {
                        // Plan terminado (usará item_plan_terminado.xml con color rojo)
                        // Si terminó, el checkbox ya está marcado automáticamente
                        planesTerminadosList.add(plan);
                    } else {
                        // Próximo plan (aún no inicia, no está en curso)
                        proximosPlanesList.add(plan);
                    }
                }
            }

            // Ordenar Próximos planes por fecha de inicio (ascendente)
            proximosPlanesList.sort((a, b) -> Long.compare(a.getStartTimeMs(), b.getStartTimeMs()));

            // Ordenar Planes terminados por fecha de inicio (descendente - más recientes primero)
            planesTerminadosList.sort((a, b) -> Long.compare(b.getStartTimeMs(), a.getStartTimeMs()));

            proximosPlanesAdapter.notifyDataSetChanged();
            planesTerminadosAdapter.notifyDataSetChanged();

            // Mostrar plan en curso (solo si es hoy)
            if (planEnCurso != null && selectedDate.equals(LocalDate.now())) {
                updateCurrentPlanUI(planEnCurso, tituloEnCurso, descripcionEnCurso, horaInicioFinEnCurso);
            } else {
                clearCurrentPlanUI();
            }
        });
    }


    /**
     * Muestra el plan actual en la tarjeta de "Plan en curso"
     */
    private void updateCurrentPlanUI(PlanModel plan, String titulo, String descripcion, String horaInicioFin) {
        View view = getView();
        if (view == null) return;

        TextView time = view.findViewById(R.id.text_current_plan_time);
        TextView title = view.findViewById(R.id.text_current_plan_title);
        TextView details = view.findViewById(R.id.text_current_plan_details);
        LinearProgressIndicator progress = view.findViewById(R.id.progress_current_plan);

        // Mostrar hora inicio-fin en text_current_plan_time
        if (horaInicioFin != null && !horaInicioFin.isEmpty()) {
            time.setText(horaInicioFin);
        } else {
            time.setText("--:--");
        }

        // Mostrar título en text_current_plan_title
        if (titulo != null && !titulo.isEmpty()) {
            title.setText(titulo);
        } else {
            title.setText(plan.getTitle()); // Fallback al título del plan
        }

        // Mostrar descripción en text_current_plan_details
        if (descripcion != null && !descripcion.isEmpty()) {
            details.setText(descripcion);
        } else {
            details.setText("Sin descripción");
        }

        // Calcular progreso basado en el tiempo transcurrido
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
    private PlanModel convertDocumentToPlanModel(DocumentSnapshot doc, String estadoActualizado) {
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

            // Determinar el color según el estado (usar estadoActualizado si está disponible)
            String estado = estadoActualizado != null ? estadoActualizado : doc.getString("estado");
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

    // Mantener el método original para compatibilidad
    private PlanModel convertDocumentToPlanModel(DocumentSnapshot doc) {
        return convertDocumentToPlanModel(doc, null);
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
