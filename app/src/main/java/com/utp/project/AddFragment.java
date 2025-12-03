package com.utp.project;

import android.app.Activity;
import androidx.appcompat.app.AlertDialog;

import android.content.Context;
import android.content.DialogInterface;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// Nuevas importaciones necesarias para Ubicación (Asegúrate de tener el SDK de Places)
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import com.google.android.gms.common.api.Status;
import android.content.pm.PackageManager;
import android.Manifest; // Necesario para el permiso de contactos

import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class AddFragment extends Fragment {

    private TextView textStartDate;
    private TextView textStartTime;
    private TextView textEndDate;
    private TextView textEndTime;
    // Vistas de Notificación (NUEVAS)
    private LinearLayout layoutNotificacion;
    private TextView textNotificacion;
    // NUEVAS VISTAS DE UBICACIÓN
    private LinearLayout layoutUbicacion;
    private TextView textUbi;

    // VISTAS DE CONTACTOS
    private LinearLayout layoutAgregarPersonas;
    private TextView textPersonasSeleccionadas; // Muestra los nombres de los contactos

    // VISTAS DE TÍTULO Y DESCRIPCIÓN
    private EditText editTextTitle;
    private EditText editTextDescription;
    private TextView editTextCategoria;
    private android.widget.ImageView iconCategoriaPreview;
    private android.net.Uri categoriaIconUri;
    private ActivityResultLauncher<Intent> selectImageLauncher;
    // PRIORIDAD
    private LinearLayout layoutPrioridad;
    private TextView textPrioridadValor;
    private String selectedPriority = "Media"; // Valor por defecto
    private final String[] priorityOptions = {"Alta", "Media", "Baja"};
    private View btnGuardar;
    private Calendar startCalendar;
    private Calendar endCalendar;
    private long timeDeltaMillis = 3600000; // 1 hora por defecto (3600 * 1000)
    private int notificationMinutesBefore = 15; // 15 minutos por defecto

    // NUEVAS VARIABLES PARA ALMACENAR LA UBICACIÓN SELECCIONADA
    private String selectedLocationAddress = null;
    private double latitude = 0.0;
    private double longitude = 0.0;
    // Variable para almacenar los contactos seleccionados
    private final List<String> selectedContacts = new ArrayList<>();

    // LANZADOR DE ACTIVIDAD PARA PLACE AUTOCOMPLETE (CLAVE)
    private ActivityResultLauncher<Intent> startAutocomplete;
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private ActivityResultLauncher<Intent> selectContactLauncher;
    private String selectedCategoryIconRef = null; // guarda el ícono de la categoría elegida o creada

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Inicializar el lanzador de resultados de actividad
        initializeLocationLauncher();
        initializeContactLaunchers();
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

        // 1.c. Inicialización de Vistas (UBICACIÓN - NUEVO)
        layoutUbicacion = view.findViewById(R.id.layout_ubicación);
        textUbi = view.findViewById(R.id.text_ubi);

        // VISTAS DE CONTACTOS (IDs del XML)
        layoutAgregarPersonas = view.findViewById(R.id.layout_agregarPersonas);
        textPersonasSeleccionadas = view.findViewById(R.id.text_personas_seleccionadas);

        // VISTAS DE TÍTULO Y DESCRIPCIÓN
        editTextTitle = view.findViewById(R.id.edit_text_title);
        editTextDescription = view.findViewById(R.id.edit_text_description);
        editTextCategoria = view.findViewById(R.id.edit_text_categoria);
        iconCategoriaPreview = view.findViewById(R.id.icon_categoria_preview);

        // Enlazamos el botón a la variable de clase
        btnGuardar = view.findViewById(R.id.btn_guardar);

        // --- INICIO MODIFICACIÓN PRIORIDAD ---
        layoutPrioridad = view.findViewById(R.id.layout_prioridad);
        textPrioridadValor = view.findViewById(R.id.text_prioridad_valor);
        textPrioridadValor.setText(selectedPriority); // Mostrar valor por defecto

        // Lanzador para elegir imagen del icono de categoría
        selectImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        android.net.Uri uri = result.getData().getData();
                        if (uri != null) {
                            categoriaIconUri = uri;
                            iconCategoriaPreview.setImageURI(uri);
                        }
                    }
                }
        );



        startCalendar = Calendar.getInstance();
        endCalendar = (Calendar) startCalendar.clone(); // Iniciar con la misma fecha/hora

        // 2. Establecer valores por defecto (Hora de fin +1 hora por defecto)
        applyTimeDelta();
        updateNotificationText(); // Mostrar el valor inicial de la notificación

        // Si hay una ubicación guardada, la mostramos (opcional, para re-edición)
        if (selectedLocationAddress != null) {
            updateLocationText(selectedLocationAddress, latitude, longitude);
        }

        // 3. Establecer Listeners
        textStartDate.setOnClickListener(v -> showStartDatePickerDialog());
        textStartTime.setOnClickListener(v -> showStartTimePickerDialog());
        textEndDate.setOnClickListener(v -> showEndDatePickerDialog());
        textEndTime.setOnClickListener(v -> showEndTimePickerDialog());

        // Listener para Notificación (NUEVO)
        layoutNotificacion.setOnClickListener(v -> showNotificationOptionsDialog());

        // Listener para Ubicación (NUEVO)
        layoutUbicacion.setOnClickListener(v -> launchPlacePicker());

        // NUEVO: Listener para Agregar Personas
        layoutAgregarPersonas.setOnClickListener(v -> checkContactPermissionAndLaunchPicker());
        // --- INICIO MODIFICACIÓN PRIORIDAD ---
        // Asignar el listener al LinearLayout
        layoutPrioridad.setOnClickListener(v -> showPriorityDialog());

        // --- CATEGORÍA --- //
        editTextCategoria.setFocusable(false);
        editTextCategoria.setClickable(true);
        editTextCategoria.setOnClickListener(v -> showCategoryPickerDialog());

        View btnElegirCategoria = view.findViewById(R.id.layout_categoria);
        if (btnElegirCategoria != null) {
            btnElegirCategoria.setOnClickListener(v -> showCategoryPickerDialog());
        }


        // Listener para botón de retroceso (si existe)
        view.findViewById(R.id.icon_menu).setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });

        // NUEVO: Guardar actividad en Firestore si el botón existe en el layout
        View btnGuardar = view.findViewById(R.id.btn_guardar);
        if (btnGuardar != null) {
            btnGuardar.setOnClickListener(v -> saveActividadFirestore());
        }

        return view;

    }

    // INICIO MODIFICACIÓN PRIORIDAD (Nuevo metodo)

    private void showPriorityDialog() {
        // Encontrar el índice del item actualmente seleccionado
        int checkedItem = -1;
        for (int i = 0; i < priorityOptions.length; i++) {
            if (priorityOptions[i].equals(selectedPriority)) {
                checkedItem = i;
                break;
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Seleccionar Prioridad");

        // Configura el diálogo con las opciones y el ítem chequeado
        builder.setSingleChoiceItems(priorityOptions, checkedItem, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // 'which' es el índice del elemento seleccionado (0="Alta", 1="Media", 2="Baja")
                selectedPriority = priorityOptions[which]; // Actualiza la variable
                textPrioridadValor.setText(selectedPriority); // Actualiza la UI
                dialog.dismiss(); // Cierra el diálogo
            }
        });

        builder.setNegativeButton("Cancelar", null); // Botón para cerrar sin cambiar
        builder.show();
    }

    private void showIconSelectionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Seleccionar icono de categoría");

        // Lista de nombres que el usuario verá
        String[] iconNames = {"Casa", "Trabajo", "Escuela", "Salud", "Subir desde galería"};

        // Inflar vista personalizada con iconos (opcional, versión simple con lista de texto)
        builder.setItems(iconNames, (dialog, which) -> {
            switch (which) {
                case 0:
                    setDefaultCategoryIcon(R.drawable.ic_home);
                    break;
                case 1:
                    setDefaultCategoryIcon(R.drawable.ic_work);
                    break;
                case 2:
                    setDefaultCategoryIcon(R.drawable.ic_school);
                    break;
                case 3:
                    setDefaultCategoryIcon(R.drawable.ic_health);
                    break;
                case 4:
                    pickIconFromGallery();
                    break;
            }
        });

        builder.show();
    }

    // === NUEVO METODO ===
// Muestra todas las categorías guardadas en Firestore y permite crear una nueva
    private void showCategoryPickerDialog() {
        FirebaseFirestore.getInstance()
                .collection("categorias")
                .get()
                .addOnSuccessListener(query -> {
                    List<String> categoryNames = new ArrayList<>();
                    List<String> categoryIcons = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : query) {
                        categoryNames.add(doc.getString("nombre"));
                        categoryIcons.add(doc.getString("icono"));
                    }

                    // Añadir opción extra
                    categoryNames.add("➕ Crear nueva categoría");
                    categoryIcons.add(null);

                    String[] items = categoryNames.toArray(new String[0]);

                    AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                    builder.setTitle("Seleccionar categoría");
                    builder.setItems(items, (dialog, which) -> {
                        if (which == items.length - 1) {
                            showCreateCategoryDialog(); // Crear nueva categoría
                        } else {
                            String nombre = categoryNames.get(which);
                            String icono = categoryIcons.get(which);
                            editTextCategoria.setText(nombre);
                            setCategoryIcon(icono);
                        }
                    });
                    builder.show();
                })
                .addOnFailureListener(e -> Toast.makeText(requireContext(),
                        "Error al cargar categorías", Toast.LENGTH_SHORT).show());
    }

    // Abre un diálogo simple para crear una nueva categoría con nombre + icono
    private void showCreateCategoryDialog() {
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View view = inflater.inflate(R.layout.dialog_add_category, null);

        EditText inputNombre = view.findViewById(R.id.input_category_name);
        ImageView iconPreview = view.findViewById(R.id.icon_preview);
        MaterialButton btnElegirIcono = view.findViewById(R.id.btn_elegir_icono);

        final Uri[] selectedUri = {null};
        final int[] selectedResId = {R.drawable.ic_work};

        btnElegirIcono.setOnClickListener(v -> {
            String[] opciones = {"Casa", "Escuela", "Trabajo", "Salud", "Desde galería"};
            new AlertDialog.Builder(requireContext())
                    .setTitle("Elegir icono")
                    .setItems(opciones, (dialog, which) -> {
                        switch (which) {
                            case 0: selectedResId[0] = R.drawable.ic_home; iconPreview.setImageResource(R.drawable.ic_home); break;
                            case 1: selectedResId[0] = R.drawable.ic_school; iconPreview.setImageResource(R.drawable.ic_school); break;
                            case 2: selectedResId[0] = R.drawable.ic_work; iconPreview.setImageResource(R.drawable.ic_work); break;
                            case 3: selectedResId[0] = R.drawable.ic_health; iconPreview.setImageResource(R.drawable.ic_health); break;
                            case 4:
                                Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                                selectImageLauncher.launch(intent);
                                break;
                        }
                    }).show();
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Nueva categoría");
        builder.setView(view);
        builder.setPositiveButton("Guardar", (dialog, which) -> {
            String nombre = inputNombre.getText().toString().trim();
            if (nombre.isEmpty()) {
                Toast.makeText(requireContext(), "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show();
                return;
            }

            String iconRef = selectedUri[0] != null ? selectedUri[0].toString() :
                    getResources().getResourceEntryName(selectedResId[0]);

            saveCategoriaFirestore(nombre, iconRef);
            editTextCategoria.setText(nombre);
            setCategoryIcon(iconRef);
        });
        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    // Guarda la categoría en Firestore
    private void saveCategoriaFirestore(String nombre, String iconRef) {
        Map<String, Object> data = new HashMap<>();
        data.put("nombre", nombre);
        data.put("icono", iconRef);

        FirebaseFirestore.getInstance()
                .collection("categorias")
                .document(nombre)
                .set(data)
                .addOnSuccessListener(aVoid -> Toast.makeText(requireContext(),
                        "Categoría guardada", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(requireContext(),
                        "Error al guardar categoría", Toast.LENGTH_SHORT).show());
    }

    // Aplica el icono visualmente según si es URI o drawable name
    private void setCategoryIcon(String iconRef) {
        if (iconRef == null) return;
        if (iconRef.startsWith("content://")) {
            iconCategoriaPreview.setImageURI(Uri.parse(iconRef));
        } else {
            int resId = getResources().getIdentifier(iconRef, "drawable", requireContext().getPackageName());
            if (resId != 0) iconCategoriaPreview.setImageResource(resId);
        }
    }



    private void setDefaultCategoryIcon(int drawableRes) {
        categoriaIconUri = null; // si se elige un ícono por defecto, no guardamos URI
        iconCategoriaPreview.setImageResource(drawableRes);
        iconCategoriaPreview.setTag(drawableRes); // guardamos referencia por si queremos recuperarla luego
    }
    private void pickIconFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        selectImageLauncher.launch(intent);
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

    // LÓGICA DE UBICACIÓN (CORREGIDA)

    /**
     * Inicializa el lanzador que manejará el resultado de la selección de ubicación.
     */
    private void initializeLocationLauncher() {
        startAutocomplete = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        // 1. RESULTADO EXITOSO
                        Intent data = result.getData();
                        if (data != null) {
                            Place place = Autocomplete.getPlaceFromIntent(data);
                            handleSelectedPlace(place);
                        }
                    } else if (result.getResultCode() == Activity.RESULT_CANCELED) {
                        // 2. USUARIO CANCELÓ LA BÚSQUEDA
                        Toast.makeText(requireContext(),
                                "Búsqueda de ubicación cancelada.",
                                Toast.LENGTH_SHORT).show();
                    } else {
                        // 3. ERROR (CUALQUIER OTRO CÓDIGO)
                        Intent data = result.getData();
                        // Usamos Autocomplete.getStatusFromIntent(data) para errores
                        Status status = Autocomplete.getStatusFromIntent(data);

                        // Asegúrate de que el mensaje de error no sea nulo antes de mostrarlo
                        String errorMessage = status != null && status.getStatusMessage() != null
                                ? status.getStatusMessage()
                                : "Error desconocido al seleccionar ubicación.";

                        Toast.makeText(requireContext(),
                                "Error de Ubicación: " + errorMessage,
                                Toast.LENGTH_LONG).show();

                    }
                }
        );
    }

    /**
     * Lanza la actividad de Place Autocomplete.
     */
    private void launchPlacePicker() {
        try {
            // Campos de datos que deseamos obtener. LatLng y ADDRESS son obligatorios.
            List<Place.Field> fields = Arrays.asList(
                    Place.Field.NAME,
                    Place.Field.LAT_LNG,
                    Place.Field.ADDRESS
            );

            // Construir la Intent de Autocomplete en modo de pantalla completa.
            Intent intent = new Autocomplete.IntentBuilder(
                    AutocompleteActivityMode.FULLSCREEN,
                    fields)
                    .build(requireContext());

            // Lanzar la actividad usando el lanzador.
            startAutocomplete.launch(intent);

        } catch (Exception e) {
            Toast.makeText(requireContext(),
                    "Error: ¿Places SDK y Clave API configurados correctamente?",
                    Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    /**
     * Procesa la ubicación seleccionada y actualiza la UI.
     */
    private void handleSelectedPlace(Place place) {
        if (place.getLatLng() == null) return; // Evita NullPointerException si falta LatLng

        // Almacenar los datos para guardarlos con el plan
        selectedLocationAddress = place.getAddress();
        latitude = place.getLatLng().latitude;
        longitude = place.getLatLng().longitude;

        Toast.makeText(requireContext(),
                "Ubicación seleccionada: " + (place.getName() != null ? place.getName() : "Dirección"),
                Toast.LENGTH_SHORT).show();

        updateLocationText(place.getName(), latitude, longitude);
    }

    /**
     * Formatea el texto de la ubicación y lo muestra en el TextView.
     */
    private void updateLocationText(String locationName, double lat, double lon) {
        String displayTitle = locationName != null && !locationName.isEmpty() ? locationName : selectedLocationAddress;

        // Formato que muestra el nombre/dirección y las coordenadas (simplificado)
        String displayText = String.format(Locale.getDefault(),
                "%s\n(Lat: %.4f, Lon: %.4f)",
                displayTitle,
                lat,
                lon);

        textUbi.setText(displayText);
        // Asumo que tienes este color definido
        textUbi.setTextColor(requireContext().getResources().getColor(R.color.md_theme_onPrimaryContainer));
    }

    // LÓGICA DE CONTACTOS

    /**
     * Inicializa los lanzadores para permisos y selección de contactos.
     */
    private void initializeContactLaunchers() {
        // 1. Lanzador para solicitar el permiso READ_CONTACTS
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        // Permiso concedido, lanzar el selector de contactos
                        launchContactPicker();
                    } else {
                        // Permiso denegado
                        Toast.makeText(requireContext(),
                                "Necesitas el permiso de contactos para invitar personas.",
                                Toast.LENGTH_LONG).show();
                    }
                }
        );

        // 2. Lanzador para manejar el resultado de la selección del contacto
        selectContactLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        handleSelectedContact(result.getData().getData());
                    } else {
                        Toast.makeText(requireContext(),
                                "Selección de contacto cancelada.",
                                Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    /**
     * Verifica el permiso y lo solicita si es necesario, o lanza el selector.
     */
    private void checkContactPermissionAndLaunchPicker() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_CONTACTS)
                == PackageManager.PERMISSION_GRANTED) {

            // Permiso ya concedido, lanzar selector
            launchContactPicker();
        } else {
            // Solicitar permiso
            requestPermissionLauncher.launch(Manifest.permission.READ_CONTACTS);
        }
    }

    /**
     * Lanza la Intent para abrir la aplicación nativa de selección de contactos.
     */
    private void launchContactPicker() {
        try {
            // ACTION_PICK y ContactsContract.Contacts.CONTENT_URI abre el selector de contactos
            Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
            selectContactLauncher.launch(intent);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "No se pudo iniciar el selector de contactos.", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    /**
     * Procesa el URI del contacto seleccionado y extrae el nombre.
     */
    private void handleSelectedContact(Uri contactUri) {
        String contactName = null;
        if (contactUri == null) return;

        // Solo pedimos el nombre visible (DISPLAY_NAME)
        String[] projection = new String[]{ContactsContract.Contacts.DISPLAY_NAME};

        try (Cursor cursor = requireContext().getContentResolver().query(contactUri, projection, null, null, null)) {

            if (cursor != null && cursor.moveToFirst()) {
                // Obtener el índice de la columna DISPLAY_NAME
                int nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
                if (nameIndex != -1) {
                    contactName = cursor.getString(nameIndex);
                }
            }
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Error al leer contacto.", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            return;
        }

        if (contactName != null && !contactName.isEmpty()) {
            if (!selectedContacts.contains(contactName)) {
                selectedContacts.add(contactName);
                updateSelectedContactsText();
                Toast.makeText(requireContext(), contactName + " añadido.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), contactName + " ya ha sido añadido.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Actualiza el TextView con la lista de personas seleccionadas.
     */
    private void updateSelectedContactsText() {
        if (selectedContacts.isEmpty()) {
            textPersonasSeleccionadas.setText("Agregar personas");
            // Usar un color por defecto (asumo que es onPrimaryContainer para el texto base)
            textPersonasSeleccionadas.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_onPrimaryContainer));
        } else {
            // Muestra los nombres separados por comas y un color de acento
            String names = String.join(", ", selectedContacts);
            textPersonasSeleccionadas.setText(names);
            textPersonasSeleccionadas.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_tertiary));
        }
    }

    private String getPrioridadSeleccionada() {
        String valor = selectedPriority; // USAR LA VARIABLE

        if (valor.equalsIgnoreCase("alta") || valor.equalsIgnoreCase("media") || valor.equalsIgnoreCase("baja")) {
            return valor.toLowerCase();
        }
        return "media";
    }

    private String getCategoriaSeleccionada() {
        String cat = editTextCategoria.getText() != null ? editTextCategoria.getText().toString().trim() : null;
        return (cat != null && !cat.isEmpty()) ? cat : null;
    }

    // NUEVO: Guardar en Firestore
    private void saveActividadFirestore() {
        // 1. DESHABILITAR BOTÓN INMEDIATAMENTE
        if (btnGuardar != null) {
            btnGuardar.setEnabled(false);
        }

        // 2. OBTENER TÍTULO Y DESCRIPCIÓN PRIMERO
        String titulo = editTextTitle.getText().toString().trim();
        if (titulo.isEmpty()) {
            Toast.makeText(requireContext(), "Por favor, ingresa un título", Toast.LENGTH_SHORT).show();
            editTextTitle.requestFocus();
            if (btnGuardar != null) btnGuardar.setEnabled(true);
            return;
        }

        String descripcion = editTextDescription.getText().toString().trim();
        if (descripcion.isEmpty()) {
            descripcion = null;
        }

        // 3. VALIDACIÓN DE FECHAS
        if (endCalendar.before(startCalendar)) {
            validateEndTime();
            if (btnGuardar != null) btnGuardar.setEnabled(true);
            return;
        }

        Timestamp inicio = new Timestamp(startCalendar.getTime());
        Timestamp fin = new Timestamp(endCalendar.getTime());

        // 4. PROGRAMAR LA NOTIFICACIÓN SI CORRESPONDE
        if (notificationMinutesBefore >= 0) {
            try {

                // Obtener username desde SharedPreferences
                SharedPreferences prefs = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
                String username = prefs.getString("username", "usuario");


                //  Calcular hora de notificación
                Calendar notificationTime = (Calendar) startCalendar.clone();
                notificationTime.add(Calendar.MINUTE, -notificationMinutesBefore);

                //  CLAVE: FORZAR A QUE SEA EN PUNTO (sin segundos ni milisegundos)
                notificationTime.set(Calendar.SECOND, 0);
                notificationTime.set(Calendar.MILLISECOND, 0);

                Log.d("AddFragment", "Hora de notificación calculada EXACTA: " + notificationTime.getTime());
                Log.d("AddFragment", " Hora actual del sistema: " + new Date(System.currentTimeMillis()));

                if (notificationTime.getTimeInMillis() < System.currentTimeMillis()) {
                    Log.w("AddFragment", "⚠ La hora de notificación ya pasó. Ajusta notificationMinutesBefore o la hora.");
                }

                int notificationId = (int) (System.currentTimeMillis() % Integer.MAX_VALUE);
                Log.d("AddFragment", " Notification ID generado: " + notificationId);

                //  PROGRAMAR ALARMA
                NotificationReceiver.programarAlarma(
                        requireContext(),
                        notificationTime.getTimeInMillis(),
                        "Recordatorio: " + titulo,
                        "Tu actividad comienza pronto: " + titulo,
                        notificationId,
                        username
                );

                Log.d("AddFragment", " Alarma enviada correctamente al AlarmManager");

            } catch (Exception e) {
                Log.e("AddFragment", " Error al programar notificación: " + e.getMessage(), e);
            }
        }

        // 5. CREAR OBJETO ACTIVIDAD PARA FIRESTORE
        java.util.Map<String, Object> actividad = com.utp.project.data.FirestoreService.buildActividad(
                titulo,
                inicio,
                fin,
                descripcion,
                "pendiente",
                getCategoriaSeleccionada(),
                categoriaIconUri != null ? categoriaIconUri.toString() : selectedCategoryIconRef,
                selectedLocationAddress,
                latitude != 0.0 ? latitude : null,
                longitude != 0.0 ? longitude : null,
                notificationMinutesBefore,
                getPrioridadSeleccionada()
        );

        // 6. GUARDAR EN FIRESTORE
        com.utp.project.data.FirestoreService.addActividad(actividad)
                .addOnSuccessListener(ref -> {
                    Toast.makeText(requireContext(), "Actividad guardada", Toast.LENGTH_SHORT).show();
                    Log.d("AddFragment", "Actividad guardada en Firestore correctamente.");

                    // Opcional: ir al HomeFragment
                    if (getActivity() != null) {
                        getActivity().getSupportFragmentManager().beginTransaction()
                                .replace(R.id.frame_layout, new HomeFragment())
                                .commit();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Error al guardar: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e("AddFragment", "Error guardando actividad: " + e.getMessage(), e);

                    // Reactivar el botón para reintento
                    if (btnGuardar != null) btnGuardar.setEnabled(true);
                });
    }

}
