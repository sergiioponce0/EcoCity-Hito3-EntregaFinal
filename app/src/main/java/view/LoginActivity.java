package view;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ecocity.R;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseUser;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.GoogleAuthProvider;

import network.FirebaseAuthManager;

public class LoginActivity extends AppCompatActivity implements FirebaseAuthManager.AuthCallback {

    private static final int RC_SIGN_IN = 1000;

    private EditText etEmail, etPassword;
    private Button btnLogin, btnRegister;
    private SignInButton btnGoogle;
    private ProgressBar progressBar;
    private TextView tvStatus;
    
    private FirebaseAuthManager authManager;
    private GoogleSignInClient googleSignInClient;    

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        authManager = new FirebaseAuthManager();
        authManager.setAuthCallback(this);
        
        initViews();
        
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);
        
        btnGoogle.setSize(SignInButton.SIZE_WIDE);
        btnGoogle.setOnClickListener(v -> iniciarGoogleSignIn());

        if (authManager.isLoggedIn()) {
            irAlMenuPrincipal();
            return;
        }
    }
    
    private void initViews() {
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);
        btnGoogle = findViewById(R.id.btnGoogle);
        progressBar = findViewById(R.id.progressBar);
        tvStatus = findViewById(R.id.tvStatus);
        
        btnLogin.setOnClickListener(v -> login());
        btnRegister.setOnClickListener(v -> register());
    }
    
    private void login() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        
        if (!validateInput(email, password)) return;
        
        showLoading(true);
        authManager.login(email, password, this);
    }
    
    private void register() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        
        if (!validateInput(email, password)) return;
        
        showLoading(true);
        authManager.register(email, password, this);
    }
    
    private boolean validateInput(String email, String password) {
        if (email.isEmpty()) {
            etEmail.setError("Email requerido");
            etEmail.requestFocus();
            return false;
        }
        
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Email inválido");
            etEmail.requestFocus();
            return false;
        }
        
        if (password.isEmpty()) {
            etPassword.setError("Contraseña requerida");
            etPassword.requestFocus();
            return false;
        }
        
        return true;
    }
    
    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!show);
        btnRegister.setEnabled(!show);
    }
    
    private void irAlMenuPrincipal() {
        startActivity(new Intent(LoginActivity.this, MainActivity.class));
        finish();
    }

    private void iniciarGoogleSignIn() {
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null) {
                    AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
                    showLoading(true);
                    authManager.loginWithCredential(credential, this);
                }
            } catch (ApiException e) {
                showLoading(false);
                Toast.makeText(this, "Google Sign-In failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }
    
    @Override
    public void onSuccess(FirebaseUser user) {
        runOnUiThread(() -> {
            showLoading(false);
            String message = "Bienvenido, " + (user.getEmail() != null ? user.getEmail() : "Usuario");
            Toast.makeText(LoginActivity.this, message, Toast.LENGTH_SHORT).show();
            irAlMenuPrincipal();
        });
    }
    
    @Override
    public void onError(String error) {
        runOnUiThread(() -> {
            showLoading(false);
            tvStatus.setText(error);
            tvStatus.setVisibility(View.VISIBLE);
            Toast.makeText(LoginActivity.this, error, Toast.LENGTH_LONG).show();
        });
    }
}