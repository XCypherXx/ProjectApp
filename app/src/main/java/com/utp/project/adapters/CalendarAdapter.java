package com.utp.project.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.utp.project.R;
import com.utp.project.models.CalendarDayModel;
import java.time.LocalDate;
import java.util.List;

/**
 * Adaptador para la vista horizontal de 5 días (RecyclerView).
 * Maneja el reciclaje de vistas, la aplicación de estilos de Material Design
 * y la lógica de selección de un solo día.
 */
public class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.DayViewHolder> {

    private final List<CalendarDayModel> dayList;
    private final OnDateSelectedListener listener;
    private int selectedPosition = RecyclerView.NO_POSITION;
    private final Context context;

    public interface OnDateSelectedListener {
        void onDateSelected(LocalDate date);
    }

    public CalendarAdapter(Context context, List<CalendarDayModel> dayList, OnDateSelectedListener listener) {
        this.context = context;
        this.dayList = dayList;
        this.listener = listener;
        // Encuentra la posición inicial del día actual y lo selecciona
        findCurrentDayPositionAndSelect();
    }

    /**
     * Busca la posición del día actual al inicio y lo marca como seleccionado.
     */
    private void findCurrentDayPositionAndSelect() {
        LocalDate today = LocalDate.now();
        for (int i = 0; i < dayList.size(); i++) {
            if (dayList.get(i).getDate().isEqual(today)) {
                selectedPosition = i;
                // No break, en caso de que existan duplicados (aunque no debería)
            }
        }
    }

    // METODO NUEVO IMPORTANTE: Ajusta la selección cuando insertamos días al inicio
    public void adjustSelectedPosition(int offset) {
        if (selectedPosition != RecyclerView.NO_POSITION) {
            selectedPosition += offset;
        }
    }

    @NonNull
    @Override
    public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Infla el layout del pilar de día
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_calendar_day, parent, false);
        return new DayViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DayViewHolder holder, int position) {
        CalendarDayModel model = dayList.get(position);

        holder.dayOfWeekText.setText(model.getDayOfWeekName());
        holder.dayNumberText.setText(model.getDayNumber());

        // CORRECCIÓN: La única verdad es si 'position' coincide con 'selectedPosition'
        boolean isSelected = (position == selectedPosition);

        // --- Lógica de Estilo (Colores Material Design) ---
        if (isSelected) {
            // Estilo SELECCIONADO
            holder.dayContainer.setBackgroundResource(R.drawable.rounded_day_pillar_selected);
            holder.dayOfWeekText.setTextColor(ContextCompat.getColor(context, R.color.md_theme_secondaryContainer));
            holder.dayNumberText.setTextColor(ContextCompat.getColor(context, R.color.md_theme_onTertiaryContainer));
            holder.dayNumberText.setBackgroundResource(R.drawable.rounded_selection_circle_active);
        } else {
            // Estilo INACTIVO
            holder.dayContainer.setBackgroundResource(R.drawable.rounded_day_pillar_inactive);
            holder.dayOfWeekText.setTextColor(ContextCompat.getColor(context, R.color.md_theme_secondaryContainer));
            holder.dayNumberText.setTextColor(ContextCompat.getColor(context, R.color.md_theme_tertiaryContainer));
            holder.dayNumberText.setBackgroundResource(0);
        }

        holder.itemView.setOnClickListener(v -> {
            int currentPos = holder.getAdapterPosition();
            if (currentPos == RecyclerView.NO_POSITION) return;

            // Guardamos la posición anterior para actualizarla visualmente
            int previousPos = selectedPosition;
            selectedPosition = currentPos;

            // Notificamos cambios para repintar (despintar el viejo, pintar el nuevo)
            notifyItemChanged(previousPos);
            notifyItemChanged(selectedPosition);

            if (listener != null) {
                listener.onDateSelected(model.getDate());
            }
        });
    }

    @Override
    public int getItemCount() {
        return dayList.size();
    }

    // ViewHolder: Mantiene las referencias a los elementos del layout
    public static class DayViewHolder extends RecyclerView.ViewHolder {
        LinearLayout dayContainer;
        TextView dayOfWeekText;
        TextView dayNumberText;

        public DayViewHolder(@NonNull View itemView) {
            super(itemView);
            dayContainer = itemView.findViewById(R.id.day_container);
            dayOfWeekText = itemView.findViewById(R.id.text_day_of_week);
            dayNumberText = itemView.findViewById(R.id.text_day_number);
        }
    }

    // METODO NUEVO: Permite seleccionar una posición programáticamente
    public void setSelection(int newPosition) {
        if (newPosition < 0 || newPosition >= dayList.size()) return;

        int previousPos = selectedPosition;
        selectedPosition = newPosition;

        // Despintar el anterior
        if (previousPos != RecyclerView.NO_POSITION) {
            notifyItemChanged(previousPos);
        }
        // Pintar el nuevo
        notifyItemChanged(selectedPosition);
    }
}
