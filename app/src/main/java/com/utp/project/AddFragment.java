package com.utp.project;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class AddFragment extends Fragment {

    private TextView textStartDate;
    private TextView textStartTime;
    private TextView textEndDate;
    private TextView textEndTime;
    private Calendar startCalendar;
    private Calendar endCalendar;
    private long timeDeltaMillis = 3600000; // 1 hora por defecto (3600 * 1000)

    public AddFragment() {
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_add, container, false);

        // 1. Inicialización de Vistas
        textStartDate = view.findViewById(R.id.text_fecha);
        textStartTime = view.findViewById(R.id.text_hora);
        textEndDate = view.findViewById(R.id.text_fecha2);
        textEndTime = view.findViewById(R.id.text_hora2);

        startCalendar = Calendar.getInstance();
        endCalendar = (Calendar) startCalendar.clone(); // Iniciar con la misma fecha/hora

        // 2. Establecer valores por defecto (Hora de fin +1 hora por defecto)
        applyTimeDelta();

        // 3. Establecer Listeners
        textStartDate.setOnClickListener(v -> showStartDatePickerDialog());
        textStartTime.setOnClickListener(v -> showStartTimePickerDialog());
        textEndDate.setOnClickListener(v -> showEndDatePickerDialog());
        textEndTime.setOnClickListener(v -> showEndTimePickerDialog());

        // Listener para botón de retroceso (si existe)
        view.findViewById(R.id.icon_menu).setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });

        return view;
    }

    // LÓGICA DE INICIO (Cuando cambia, la fecha/hora de fin DEBE sincronizarse)

    private void showStartDatePickerDialog() {
        int year = startCalendar.get(Calendar.YEAR);
        int month = startCalendar.get(Calendar.MONTH);
        int day = startCalendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    startCalendar.set(selectedYear, selectedMonth, selectedDay);
                    updateDateText(startCalendar, textStartDate);

                    // Aplicar Delta: Siempre que cambie la Fecha 1, aplicamos la diferencia
                    applyTimeDelta();
                    validateEndTime(); // Validar por si acaso
                },
                year, month, day);

        datePickerDialog.show();
    }

    private void showStartTimePickerDialog() {
        int hour = startCalendar.get(Calendar.HOUR_OF_DAY);
        int minute = startCalendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(
                requireContext(),
                (view, selectedHour, selectedMinute) -> {
                    startCalendar.set(Calendar.HOUR_OF_DAY, selectedHour);
                    startCalendar.set(Calendar.MINUTE, selectedMinute);
                    updateTimeText(startCalendar, textStartTime);

                    // Aplicar Delta: Si la Hora 1 cambia, aplicamos la diferencia
                    applyTimeDelta();
                    validateEndTime();
                },
                hour, minute, false); // false para formato AM/PM

        timePickerDialog.show();
    }

    // LÓGICA DE FIN (Validación y Selección Manual)

    private void showEndDatePickerDialog() {
        int year = endCalendar.get(Calendar.YEAR);
        int month = endCalendar.get(Calendar.MONTH);
        int day = endCalendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    // Guarda la fecha seleccionada temporalmente
                    Calendar tempEndCalendar = (Calendar) endCalendar.clone();
                    tempEndCalendar.set(selectedYear, selectedMonth, selectedDay);

                    // Validamos solo la fecha
                    if (tempEndCalendar.before(startCalendar) && !isSameDay(tempEndCalendar, startCalendar)) {
                        Toast.makeText(requireContext(), "La fecha de fin no puede ser anterior a la fecha de inicio.", Toast.LENGTH_LONG).show();
                        // No actualizamos endCalendar si hay error
                    } else {
                        endCalendar.setTime(tempEndCalendar.getTime());
                        updateDateText(endCalendar, textEndDate);

                        // Calculamos y guardamos el nuevo delta después de la selección manual de fecha
                        calculateAndSaveTimeDelta();
                        validateEndTime();
                    }
                },
                year, month, day);

        datePickerDialog.show();
    }

    private void showEndTimePickerDialog() {
        int hour = endCalendar.get(Calendar.HOUR_OF_DAY);
        int minute = endCalendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(
                requireContext(),
                (view, selectedHour, selectedMinute) -> {
                    // Guarda la hora seleccionada temporalmente
                    Calendar tempEndCalendar = (Calendar) endCalendar.clone();
                    tempEndCalendar.set(Calendar.HOUR_OF_DAY, selectedHour);
                    tempEndCalendar.set(Calendar.MINUTE, selectedMinute);

                    // Validación estricta si es el mismo día
                    if (isSameDay(startCalendar, tempEndCalendar) && tempEndCalendar.before(startCalendar)) {
                        Toast.makeText(requireContext(), "En el mismo día, la hora de fin debe ser posterior a la de inicio.", Toast.LENGTH_LONG).show();
                        // No actualizamos endCalendar
                    } else {
                        endCalendar.setTime(tempEndCalendar.getTime());
                        updateTimeText(endCalendar, textEndTime);

                        // Calculamos y guardamos el nuevo delta después de la selección manual de hora
                        calculateAndSaveTimeDelta();
                        validateEndTime(); // Validar la hora después de la fecha
                    }
                },
                hour, minute, false);

        timePickerDialog.show();
    }

    // LÓGICA DE VALIDACIÓN Y SINCRONIZACIÓN

    /** * Aplica el timeDeltaMillis a la fecha y hora de inicio para establecer la fecha y hora de fin.
     * Esto se llama automáticamente cuando cambia la hora de inicio (Hora 1).
     */
    private void applyTimeDelta() {
        // Clonamos la fecha de inicio
        endCalendar.setTime(startCalendar.getTime());
        // Agregamos el delta almacenado (que por defecto es 1 hora o lo que el usuario haya fijado)
        endCalendar.setTimeInMillis(startCalendar.getTimeInMillis() + timeDeltaMillis);

        // Actualizamos las vistas
        updateDateText(endCalendar, textEndDate);
        updateTimeText(endCalendar, textEndTime);
    }

    /**
     * Calcula la diferencia (delta) entre la hora 2 y la hora 1
     * y la almacena en 'timeDeltaMillis'.
     * Esto se llama cuando el usuario modifica MANUALMENTE la fecha/hora de FIN.
     */
    private void calculateAndSaveTimeDelta() {
        // Calcula la diferencia en milisegundos
        long newDelta = endCalendar.getTimeInMillis() - startCalendar.getTimeInMillis();

        // El delta debe ser positivo o cero (aunque la validación lo impide)
        if (newDelta < 0) {
            // Esto solo debería ocurrir si el usuario ignora la validación, pero lo manejamos
            timeDeltaMillis = 3600000; // 1 hora
        } else {
            timeDeltaMillis = newDelta;
        }
    }

    /**
     * Verifica si dos objetos Calendar están en el mismo día (ignorando la hora).
     */
    private boolean isSameDay(Calendar cal1, Calendar cal2) {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    /**
     * Valida que la hora y fecha de fin sea igual o posterior a la de inicio.
     */
    private void validateEndTime() {
        if (endCalendar.before(startCalendar)) {
            // Muestra el mensaje de error
            Toast.makeText(requireContext(), "ERROR: La hora/fecha de fin no puede ser anterior a la de inicio.", Toast.LENGTH_LONG).show();
        }
    }

    // MÉTODOS DE FORMATO (Actualizados para aceptar Calendar y TextView)

    private void updateDateText(Calendar calendar, TextView textView) {
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, MMM d, yyyy", new Locale("es", "ES"));
        String formattedDate = sdf.format(calendar.getTime());
        formattedDate = formattedDate.substring(0, 1).toUpperCase() + formattedDate.substring(1);
        textView.setText(formattedDate);
    }

    private void updateTimeText(Calendar calendar, TextView textView) {
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", new Locale("es", "ES"));
        String formattedTime = sdf.format(calendar.getTime());
        textView.setText(formattedTime);
    }
}
