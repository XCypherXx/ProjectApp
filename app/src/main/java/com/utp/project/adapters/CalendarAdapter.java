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
                dayList.get(i).setSelected(true);
                selectedPosition = i;
                // No break, en caso de que existan duplicados (aunque no debería)
            }
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

        // --- Lógica de Estilo (Colores Material Design) ---
        if (model.isSelected()) {
            // Estado SELECCIONADO: Fondo del pilar oscuro, Círculo de número activo
            holder.dayContainer.setBackgroundResource(R.drawable.rounded_day_pillar_selected);
            holder.dayOfWeekText.setTextColor(ContextCompat.getColor(context, R.color.white));
            holder.dayNumberText.setTextColor(ContextCompat.getColor(context, R.color.black));
            holder.dayNumberText.setBackgroundResource(R.drawable.rounded_selection_circle_active);
        } else {
            // Estado INACTIVO: Fondo del pilar claro, Sin círculo, Texto negro
            holder.dayContainer.setBackgroundResource(R.drawable.rounded_day_pillar_inactive);
            holder.dayOfWeekText.setTextColor(ContextCompat.getColor(context, R.color.black));
            holder.dayNumberText.setTextColor(ContextCompat.getColor(context, R.color.black));
            holder.dayNumberText.setBackgroundResource(0); // Sin fondo de círculo
        }

        // Manejar Clic
        holder.itemView.setOnClickListener(v -> {
            if (listener != null && holder.getAdapterPosition() != RecyclerView.NO_POSITION) {
                int newPosition = holder.getAdapterPosition();

                // 1. Desactiva la selección anterior y notifica el cambio
                if (selectedPosition != RecyclerView.NO_POSITION) {
                    dayList.get(selectedPosition).setSelected(false);
                    notifyItemChanged(selectedPosition);
                }

                // 2. Activa la nueva selección y notifica el cambio
                selectedPosition = newPosition;
                dayList.get(selectedPosition).setSelected(true);
                notifyItemChanged(selectedPosition);

                // 3. Notifica al fragmento la fecha seleccionada
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
}
