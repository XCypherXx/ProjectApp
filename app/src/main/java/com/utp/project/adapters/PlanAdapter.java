package com.utp.project.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.CheckBox;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.utp.project.R;
import com.utp.project.models.PlanModel;
import java.util.List;

/**
 * Adaptador para la lista de planes próximos.
 */
public class PlanAdapter extends RecyclerView.Adapter<PlanAdapter.PlanViewHolder> {

    private final List<PlanModel> planList;
    private final Context context;
    private final int itemLayoutResId;
    private final OnPlanActionListener actionListener;

    public PlanAdapter(Context context, List<PlanModel> planList, int itemLayoutResId, OnPlanActionListener actionListener) {
        this.context = context;
        this.planList = planList;
        this.itemLayoutResId = itemLayoutResId;
        this.actionListener = actionListener;
    }

    @NonNull
    @Override
    public PlanViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Infla el layout del item de plan (proximo o terminado)
        View view = LayoutInflater.from(parent.getContext()).inflate(itemLayoutResId, parent, false);
        return new PlanViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlanViewHolder holder, int position) {
        PlanModel model = planList.get(position);

        // 1. Asignar textos
        holder.titleText.setText(model.getTitle());
        holder.timeText.setText(model.getTimeRange());

        // 2. Asignar colores dinámicamente
        int solidColor = ContextCompat.getColor(context, model.getColorResource());
        int backgroundColor = ContextCompat.getColor(context, model.getBackgroundResource());

        // Aplicar color a la barra lateral
        holder.colorBar.setBackgroundColor(solidColor);

        // Aplicar color al fondo de la CardView (el color semitransparente)
        holder.cardView.setCardBackgroundColor(backgroundColor);

        // Los colores del texto (negro o blanco) deben ser manejados en tu tema
        // o definidos con un color fijo en el item_plan_proximo.xml si son constantes.

        // 3. Checkbox (si existe en el layout)
        if (holder.checkboxDone != null) {
            holder.checkboxDone.setOnCheckedChangeListener(null);
            holder.checkboxDone.setChecked(model.isCompleted());
            holder.checkboxDone.setOnCheckedChangeListener((buttonView, isChecked) -> {
                // Optimista: actualizar modelo
                model.setCompleted(isChecked);
                if (actionListener != null) {
                    actionListener.onToggleCompleted(model.getId(), isChecked);
                }
                //NUEVO: refrescar solo este item para que cambie su color/estado visual
                notifyItemChanged(holder.getAdapterPosition());

            });
        }
    }

    @Override
    public int getItemCount() {
        return planList.size();
    }

    // ViewHolder: Mantiene las referencias a los elementos del layout
    public static class PlanViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        View colorBar;
        TextView titleText;
        TextView timeText;
        CheckBox checkboxDone;

        public PlanViewHolder(@NonNull View itemView) {
            super(itemView);
            // Referencia a la CardView principal para cambiar el color de fondo
            cardView = (CardView) itemView;

            colorBar = itemView.findViewById(R.id.color_bar);
            titleText = itemView.findViewById(R.id.text_plan_title);
            timeText = itemView.findViewById(R.id.text_plan_time);
            checkboxDone = itemView.findViewById(R.id.checkbox_done);
        }
    }

    public interface OnPlanActionListener {
        void onToggleCompleted(String actividadId, boolean completed);
    }
}