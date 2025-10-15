package com.utp.project;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
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
    // Vistas de Notificación (NUEVAS)
    private LinearLayout layoutNotificacion;
    private TextView textNotificacion;
    private Calendar startCalendar;
    private Calendar endCalendar;
    private long timeDeltaMillis = 3600000; // 1 hora por defecto (3600 * 1000)
    private int notificationMinutesBefore = 15; // 15 minutos por defecto

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

        // 1.b. Inicialización de Vistas (NOTIFICACIÓN)
        layoutNotificacion = view.findViewById(R.id.layout_notificación);
        // Asegúrate de que este ID (text_notificacion) exista en fragment_add.xml
        textNotificacion = view.findViewById(R.id.text_notificacion);

        startCalendar = Calendar.getInstance();
        endCalendar = (Calendar) startCalendar.clone(); // Iniciar con la misma fecha/hora

        // 2. Establecer valores por defecto (Hora de fin +1 hora por defecto)
        applyTimeDelta();
        updateNotificationText(); // Mostrar el valor inicial de la notificación

        // 3. Establecer Listeners
        textStartDate.setOnClickListener(v -> showStartDatePickerDialog());
        textStartTime.setOnClickListener(v -> showStartTimePickerDialog());
        textEndDate.setOnClickListener(v -> showEndDatePickerDialog());
        textEndTime.setOnClickListener(v -> showEndTimePickerDialog());

        // Listener para Notificación (NUEVO)
        layoutNotificacion.setOnClickListener(v -> showNotificationOptionsDialog());

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

    // LÓGICA DE NOTIFICACIÓN

    /**
     * Muestra el primer diálogo con opciones predefinidas y la opción "Personalizado".
     */
    private void showNotificationOptionsDialog() {
        // Opciones de antelación en minutos
        final int[] minutesOptions = {0, 5, 10, 15, 30, 60};
        // Texto que verá el usuario
        String[] optionNames = {
                "A la hora del plan",
                "5 minutos antes",
                "10 minutos antes",
                "15 minutos antes",
                "30 minutos antes",
                "1 hora antes",
                "Personalizado..." // Opción especial que abre el segundo diálogo
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Seleccionar antelación");

        builder.setItems(optionNames, (dialog, which) -> {
            if (which == optionNames.length - 1) {
                // Última opción: "Personalizado"
                dialog.dismiss(); // Cierra el primer diálogo antes de abrir el segundo
                showCustomNotificationDialog();
            } else {
                // Opción predefinida
                notificationMinutesBefore = minutesOptions[which];
                updateNotificationText();
                dialog.dismiss();
            }
        });

        builder.show();
    }

    /**
     * Muestra el diálogo personalizado para ingresar un número y una unidad (Minutos/Horas/Días/Semanas).
     */
    private void showCustomNotificationDialog() {
        // Inflar la vista personalizada. Asegúrate de que el archivo XML esté en res/layout/
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View customView = inflater.inflate(R.layout.dialog_custom_notification, null);

        final EditText editNumber = customView.findViewById(R.id.edit_number_input);
        final Spinner spinnerUnit = customView.findViewById(R.id.spinner_time_unit);

        // Configurar el Spinner con las unidades de tiempo
        String[] units = {"Minutos antes", "Horas antes", "Días antes", "Semanas antes"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, units);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerUnit.setAdapter(adapter);

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Antelación Personalizada");
        builder.setView(customView);

        builder.setPositiveButton("Aceptar", (dialog, which) -> {
            String numStr = editNumber.getText().toString().trim();
            if (numStr.isEmpty()) {
                Toast.makeText(requireContext(), "Por favor, ingresa un número.", Toast.LENGTH_SHORT).show();
                // No retornar aquí para no cerrar el diálogo si la validación falla
            } else {
                try {
                    int number = Integer.parseInt(numStr);
                    if (number <= 0) {
                        Toast.makeText(requireContext(), "El número debe ser mayor a cero.", Toast.LENGTH_SHORT).show();
                    } else {
                        String unit = (String) spinnerUnit.getSelectedItem();
                        int minutes = convertToMinutes(number, unit);

                        notificationMinutesBefore = minutes;
                        updateNotificationText();
                        dialog.dismiss();
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(requireContext(), "Número inválido.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    /**
     * Convierte la cantidad y unidad seleccionada por el usuario a minutos.
     */
    private int convertToMinutes(int number, String unit) {
        final int MINUTES_IN_HOUR = 60;
        final int MINUTES_IN_DAY = MINUTES_IN_HOUR * 24;
        final int MINUTES_IN_WEEK = MINUTES_IN_DAY * 7;

        if (unit.equals("Minutos antes")) {
            return number;
        } else if (unit.equals("Horas antes")) {
            return number * MINUTES_IN_HOUR;
        } else if (unit.equals("Días antes")) {
            return number * MINUTES_IN_DAY;
        } else if (unit.equals("Semanas antes")) {
            return number * MINUTES_IN_WEEK;
        }
        return 0; // En caso de error
    }

    /** * Actualiza el TextView con el texto de la antelación seleccionada,
     * mostrando la unidad más grande posible (ej: 1440 min se muestra como 1 día).
     */
    private void updateNotificationText() {
        String text;

        if (notificationMinutesBefore == 0) {
            text = "A la hora del plan";
        } else {
            // Unidades de tiempo en minutos
            final int MIN_IN_HOUR = 60;
            final int MIN_IN_DAY = 60 * 24;
            final int MIN_IN_WEEK = 60 * 24 * 7;

            // Determina la unidad más grande
            if (notificationMinutesBefore % MIN_IN_WEEK == 0 && notificationMinutesBefore >= MIN_IN_WEEK) {
                int totalWeeks = notificationMinutesBefore / MIN_IN_WEEK;
                text = totalWeeks + " semana" + (totalWeeks > 1 ? "s" : "") + " antes";
            } else if (notificationMinutesBefore % MIN_IN_DAY == 0 && notificationMinutesBefore >= MIN_IN_DAY) {
                int totalDays = notificationMinutesBefore / MIN_IN_DAY;
                text = totalDays + " día" + (totalDays > 1 ? "s" : "") + " antes";
            } else if (notificationMinutesBefore % MIN_IN_HOUR == 0 && notificationMinutesBefore >= MIN_IN_HOUR) {
                int totalHours = notificationMinutesBefore / MIN_IN_HOUR;
                text = totalHours + " hora" + (totalHours > 1 ? "s" : "") + " antes";
            } else {
                // Si no es divisible o es menor que una hora, mostrar minutos
                text = notificationMinutesBefore + " minutos antes";
            }
        }

        textNotificacion.setText(text);
    }
}
