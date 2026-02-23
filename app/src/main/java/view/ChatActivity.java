package view;

import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ecocity.R;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.android.material.tabs.TabLayout;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import model.Message;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView rvMessages;
    private EditText etMessage;
    private ImageButton btnSend;
    private TabLayout tabLayout;
    private MessageAdapter messageAdapter;

    // 1. Usaremos dos listas separadas para mantener el historial.
    private List<Message> supportMessageList = new ArrayList<>();
    private List<Message> geminiMessageList = new ArrayList<>();

    private GenerativeModelFutures model;
    private boolean isGeminiMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        initGemini();
        initViews();
        setupRecyclerView(); // Lo movemos antes de setupTabs
        setupTabs();
        setupListeners();

        // 2. Añadimos los mensajes iniciales a sus listas correspondientes.
        if (supportMessageList.isEmpty()) {
            addMessage("Conectado con Soporte Técnico EcoCity.", false, false);
        }
        if (geminiMessageList.isEmpty()) {
            addMessage("¡Hola! Soy Eco-Asistente. ¿En qué puedo ayudarte?", false, true);
        }
        
        // 3. Mostramos la lista del chat de soporte por defecto.
        messageAdapter.setMessageList(supportMessageList);
        messageAdapter.notifyDataSetChanged();
    }

    private void initGemini() {
        // ¡IMPORTANTE! Reemplaza "TU_API_KEY" por tu clave real de Google AI Studio
        GenerativeModel gm = new GenerativeModel("gemini-1.5-flash", "AIzaSyBtKV7ztOi6qZzNg3Ov45B4DQXy7Azc_3s");
        model = GenerativeModelFutures.from(gm);
    }

    private void initViews() {
        rvMessages = findViewById(R.id.rvMessages);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        tabLayout = findViewById(R.id.tabLayout);
    }

    private void setupTabs() {
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                isGeminiMode = tab.getPosition() == 1;
                // 4. Cambiamos la lista del adapter en lugar de limpiarla.
                if (isGeminiMode) {
                    etMessage.setHint("Pregúntale a Eco-Asistente...");
                    messageAdapter.setMessageList(geminiMessageList);
                } else {
                    etMessage.setHint("Escribe a Soporte...");
                    messageAdapter.setMessageList(supportMessageList);
                }
                messageAdapter.notifyDataSetChanged();
                rvMessages.scrollToPosition(messageAdapter.getItemCount() - 1);
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupRecyclerView() {
        // Inicializamos el adapter con una lista vacía. Se la cambiaremos dinámicamente.
        messageAdapter = new MessageAdapter(new ArrayList<>());
        rvMessages.setLayoutManager(new LinearLayoutManager(this));
        rvMessages.setAdapter(messageAdapter);
    }

    private void setupListeners() {
        btnSend.setOnClickListener(v -> {
            String text = etMessage.getText().toString().trim();
            if (!text.isEmpty()) {
                sendMessageUser(text);
                if (isGeminiMode) {
                    askGemini(text);
                } else {
                    addMessage("Gracias por tu mensaje. Un agente lo revisará pronto.", false, false);
                }
            }
        });
    }

    private void sendMessageUser(String text) {
        addMessage(text, true, isGeminiMode);
        etMessage.setText("");
    }

    private void askGemini(String prompt) {
        Content content = new Content.Builder().addText(prompt).build();

        ListenableFuture<GenerateContentResponse> response = model.generateContent(content);
        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                String botResponse = result.getText();
                runOnUiThread(() -> addMessage(botResponse, false, true));
            }

            @Override
            public void onFailure(Throwable t) {
                Log.e("GeminiError", "Error al generar contenido", t);
                runOnUiThread(() -> addMessage("Lo siento, no puedo responder ahora mismo. Verifica tu API Key.", false, true));
            }
        }, ContextCompat.getMainExecutor(ChatActivity.this));
    }

    // 5. El método addMessage ahora necesita saber en qué lista añadir el mensaje.
    private void addMessage(String text, boolean isUser, boolean isGemini) {
        String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
        Message message = new Message(text, time, isUser);
        
        List<Message> targetList = isGemini ? geminiMessageList : supportMessageList;
        targetList.add(message);

        // Solo notificamos al adapter si la lista que se está mostrando es la que hemos modificado.
        if (isGemini == isGeminiMode) {
            messageAdapter.notifyItemInserted(targetList.size() - 1);
            rvMessages.scrollToPosition(targetList.size() - 1);
        }
    }
}
