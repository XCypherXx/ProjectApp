package com.utp.project.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
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

    public PlanAdapter(Context context, List<PlanModel> planList) {
        this.context = context;
        this.planList = planList;
    }

    @NonNull
    @Override
    public PlanViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Infla el layout del item de plan
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_plan_proximo, parent, false);
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

        public PlanViewHolder(@NonNull View itemView) {
            super(itemView);
            // Referencia a la CardView principal para cambiar el color de fondo
            cardView = (CardView) itemView;

            colorBar = itemView.findViewById(R.id.color_bar);
            titleText = itemView.findViewById(R.id.text_plan_title);
            timeText = itemView.findViewById(R.id.text_plan_time);
        }
    }
}