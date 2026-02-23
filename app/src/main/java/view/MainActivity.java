package view;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ecocity.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import controller.IncidenciaController;
import model.Incidencia;
import sync.FirebaseSyncManager;
import com.example.ecocity.utils.NetworkUtils;

public class MainActivity extends AppCompatActivity implements IncidenciaAdapter.OnItemInteractionListener {

    private RecyclerView recyclerView;
    private FloatingActionButton fabAgregar;
    private IncidenciaController controller;
    private IncidenciaAdapter adapter;

    private FirebaseAuth mAuth;
    private FirebaseSyncManager syncManager;

    private TextView tvReportCount, tvEcoPoints, tvWeeklyGoal, tvIncidenciasCount;
    private ProgressBar pbWeeklyGoal;
    private ChipGroup chipGroupFilters;
    private MaterialToolbar topAppBar;

    private List<Incidencia> allIncidencias = new ArrayList<>();
    private String currentFilter = "Todos";

    // 1. La forma MODERNA y CORRECTA de comunicar Activities.
    // Este "escuchador" se activará cuando el formulario se cierre.
    private final ActivityResultLauncher<Intent> formLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == AppCompatActivity.RESULT_OK) {
                    // Si el formulario nos devuelve un "OK", significa que se guardó algo.
                    // Entonces, y solo entonces, recargamos los datos.
                    cargarDatos();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        syncManager = new FirebaseSyncManager(this);
        controller = new IncidenciaController(this);
        initViews();
        setupRecyclerView();
        setupListeners();
        cargarDatos(); // Carga inicial de datos al crear la pantalla
    }

    private void initViews() {
        topAppBar = findViewById(R.id.topAppBar);
        recyclerView = findViewById(R.id.rvIncidencias);
        fabAgregar = findViewById(R.id.fabAgregar);
        tvReportCount = findViewById(R.id.tvReportCount);
        tvEcoPoints = findViewById(R.id.tvEcoPoints);
        tvWeeklyGoal = findViewById(R.id.tvWeeklyGoal);
        pbWeeklyGoal = findViewById(R.id.pbWeeklyGoal);
        tvIncidenciasCount = findViewById(R.id.tvIncidenciasCount);
        chipGroupFilters = findViewById(R.id.chipGroupFilters);

        com.google.android.material.bottomnavigation.BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
        bottomNav.setSelectedItemId(R.id.nav_incidencias);
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new IncidenciaAdapter(new ArrayList<>(), this);
        recyclerView.setAdapter(adapter);
    }

    private void setupListeners() {
        topAppBar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_logout) {
                mAuth.signOut();
                startActivity(new Intent(this, LoginActivity.class));
                finish();
                return true;
            } else if (item.getItemId() == R.id.action_sync) {
                sincronizarPendientes();
                return true;
            }
            return false;
        });

        // 2. Usamos el nuevo "lanzador" para CREAR
        fabAgregar.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, FormIncidenciaActivity.class);
            formLauncher.launch(intent);
        });

        chipGroupFilters.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == View.NO_ID) {
                currentFilter = "Todos";
            } else {
                Chip chip = group.findViewById(checkedId);
                if (chip != null) {
                    currentFilter = chip.getText().toString();
                }
            }
            filterAndDisplayData();
        });

        com.google.android.material.bottomnavigation.BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
        bottomNav.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_chat) {
                startActivity(new Intent(MainActivity.this, ChatActivity.class));
                return true;
            }
            return item.getItemId() == R.id.nav_incidencias;
        });
    }

    private void cargarDatos() {
        syncManager.obtenerIncidencias(new FirebaseSyncManager.OnIncidenciasListener() {
            @Override
            public void onIncidenciasObtenidas(List<Incidencia> incidencias, boolean fromCloud) {
                allIncidencias = new ArrayList<>(incidencias);
                runOnUiThread(() -> {
                    actualizarEstadisticas();
                    filterAndDisplayData();
                });
            }

            @Override
            public void onError(String error) {
                allIncidencias = controller.obtenerTodasLasIncidencias();
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Error al sincronizar. Mostrando datos locales.", Toast.LENGTH_SHORT).show();
                    actualizarEstadisticas();
                    filterAndDisplayData();
                });
            }
        });
    }

    private void filterAndDisplayData() {
        List<Incidencia> filteredList;
        if (currentFilter.equalsIgnoreCase("Todos")) {
            filteredList = new ArrayList<>(allIncidencias);
        } else {
            filteredList = allIncidencias.stream()
                    .filter(i -> i.getCategoria() != null && currentFilter.equalsIgnoreCase(i.getCategoria()))
                    .collect(Collectors.toList());
        }
        adapter.updateIncidencias(filteredList);
        tvIncidenciasCount.setText(filteredList.size() + " incidencias");
    }

    private void actualizarEstadisticas() {
        int totalReportes = allIncidencias.size();
        int ecoPoints = totalReportes * 15;
        int goal = 10;

        tvReportCount.setText(String.valueOf(totalReportes));
        tvEcoPoints.setText(String.valueOf(ecoPoints));

        pbWeeklyGoal.setMax(goal);
        pbWeeklyGoal.setProgress(Math.min(totalReportes, goal));
        tvWeeklyGoal.setText(String.format("%d/%d reportes", Math.min(totalReportes, goal), goal));
    }

    private void sincronizarPendientes() {
        if (!NetworkUtils.isNetworkAvailable(this)) {
            Toast.makeText(this, "Sin conexión a internet", Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(this, "Sincronizando...", Toast.LENGTH_SHORT).show();
        syncManager.sincronizarPendientes(count -> runOnUiThread(() -> {
            Toast.makeText(MainActivity.this, count + " incidencias sincronizadas", Toast.LENGTH_SHORT).show();
            cargarDatos();
        }));
    }

    @Override
    protected void onResume() {
        super.onResume();
        // YA NO HACEMOS NADA AQUÍ. La carga se controla con el lanzador.
    }

    // 3. Usamos el nuevo "lanzador" para EDITAR
    @Override
    public void onEditClick(Incidencia incidencia) {
        Intent intent = new Intent(MainActivity.this, FormIncidenciaActivity.class);
        intent.putExtra("incidencia_id", incidencia.getId());
        formLauncher.launch(intent);
    }

    @Override
    public void onDeleteClick(Incidencia incidencia) {
        new AlertDialog.Builder(this)
            .setTitle("Eliminar Reporte")
            .setMessage("¿Estás seguro de que quieres eliminar este reporte?")
            .setPositiveButton("Sí, eliminar", (dialog, which) -> {
                controller.eliminarIncidencia(incidencia.getId());
                allIncidencias.removeIf(i -> i.getId() == incidencia.getId());

                // Actualizamos la UI al instante, sin recargar de la red
                actualizarEstadisticas();
                filterAndDisplayData();

                Toast.makeText(this, "Reporte eliminado", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Cancelar", null)
            .show();
    }
}
