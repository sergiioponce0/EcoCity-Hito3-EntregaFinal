package view;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ecocity.R;
import com.google.android.material.chip.Chip;

import java.util.List;
import model.Incidencia;

public class IncidenciaAdapter extends RecyclerView.Adapter<IncidenciaAdapter.IncidenciaViewHolder> {

    private List<Incidencia> listaIncidencias;
    private final OnItemInteractionListener listener;

    // 1. La interfaz ahora notifica la edición y el borrado
    public interface OnItemInteractionListener {
        void onEditClick(Incidencia incidencia);
        void onDeleteClick(Incidencia incidencia);
    }

    // 2. El constructor ya no necesita el Controller
    public IncidenciaAdapter(List<Incidencia> listaIncidencias, OnItemInteractionListener listener) {
        this.listaIncidencias = listaIncidencias;
        this.listener = listener;
    }

    // 3. Nuevo método para actualizar la lista de forma eficiente
    public void updateIncidencias(List<Incidencia> nuevasIncidencias) {
        this.listaIncidencias.clear();
        this.listaIncidencias.addAll(nuevasIncidencias);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public IncidenciaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_incidencia, parent, false);
        return new IncidenciaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull IncidenciaViewHolder holder, int position) {
        Incidencia incidencia = listaIncidencias.get(position);
        holder.bind(incidencia, listener);
    }

    @Override
    public int getItemCount() {
        return listaIncidencias.size();
    }

    public static class IncidenciaViewHolder extends RecyclerView.ViewHolder {
        private final Context context;
        View urgencyIndicator;
        ImageView ivCategoryIcon, ivEdit, ivDelete;
        TextView tvTitle, tvDescription, tvDate, tvSyncStatus, tvId, tvLocation;
        Chip chipUrgency, chipCategory;

        public IncidenciaViewHolder(@NonNull View itemView) {
            super(itemView);
            context = itemView.getContext();
            urgencyIndicator = itemView.findViewById(R.id.urgency_indicator);
            ivCategoryIcon = itemView.findViewById(R.id.ivCategoryIcon);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            ivEdit = itemView.findViewById(R.id.ivEdit);
            ivDelete = itemView.findViewById(R.id.ivDelete);
            chipUrgency = itemView.findViewById(R.id.chipUrgency);
            chipCategory = itemView.findViewById(R.id.chipCategory);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvSyncStatus = itemView.findViewById(R.id.tvSyncStatus);
            tvId = itemView.findViewById(R.id.tvId);
            tvLocation = itemView.findViewById(R.id.tvLocation);
        }

        // 4. El método bind ahora es mucho más simple
        public void bind(final Incidencia incidencia, final OnItemInteractionListener listener) {
            tvTitle.setText(incidencia.getTitulo());
            tvDescription.setText(incidencia.getDescripcion());
            tvDate.setText(incidencia.getFecha());
            tvId.setText("ID: " + incidencia.getId());

            chipUrgency.setText(incidencia.getUrgencia());
            switch (incidencia.getUrgencia().toLowerCase()) {
                case "alta":
                    urgencyIndicator.setBackgroundColor(Color.RED);
                    break;
                case "media":
                    urgencyIndicator.setBackgroundColor(Color.YELLOW);
                    break;
                default:
                    urgencyIndicator.setBackgroundColor(Color.parseColor("#4CAF50")); // Verde
                    break;
            }

            String categoria = incidencia.getCategoria() != null ? incidencia.getCategoria() : "General";
            chipCategory.setText(categoria);

            switch (categoria.toLowerCase()) {
                case "residuos":
                    ivCategoryIcon.setImageResource(R.drawable.ic_delete);
                    break;
                case "iluminación":
                    ivCategoryIcon.setImageResource(R.drawable.ic_lightbulb);
                    break;
                case "vías":
                    ivCategoryIcon.setImageResource(R.drawable.ic_road);
                    break;
                default:
                    ivCategoryIcon.setImageResource(R.drawable.ic_report);
                    break;
            }

            if (incidencia.getEstadoSync() == 1) {
                tvSyncStatus.setText("Sincronizado");
                tvSyncStatus.setTextColor(ContextCompat.getColor(context, android.R.color.holo_green_dark));
            } else {
                tvSyncStatus.setText("Pendiente");
                tvSyncStatus.setTextColor(ContextCompat.getColor(context, android.R.color.holo_orange_dark));
            }

            if (incidencia.getLatitud() != 0 && incidencia.getLongitud() != 0) {
                tvLocation.setVisibility(View.VISIBLE);
                String coordinates = String.format(java.util.Locale.getDefault(), "%.4f, %.4f", incidencia.getLatitud(), incidencia.getLongitud());
                tvLocation.setText(coordinates);
                tvLocation.setOnClickListener(v -> {
                    Intent intent = new Intent(Intent.ACTION_VIEW,
                            Uri.parse("geo:" + incidencia.getLatitud() + "," + incidencia.getLongitud() + "?q=" + incidencia.getLatitud() + "," + incidencia.getLongitud() + "(Incidencia)"));
                    context.startActivity(intent);
                });
            } else {
                tvLocation.setVisibility(View.GONE);
            }

            // 5. Los listeners ahora solo notifican a la MainActivity
            itemView.setOnClickListener(v -> listener.onEditClick(incidencia));
            ivEdit.setOnClickListener(v -> listener.onEditClick(incidencia));
            ivDelete.setOnClickListener(v -> listener.onDeleteClick(incidencia));
        }
    }
}
