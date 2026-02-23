package network;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class FirebaseAuthManager {
    
    private FirebaseAuth mAuth;
    private AuthCallback callback;
    
    public FirebaseAuthManager() {
        mAuth = FirebaseAuth.getInstance();
    }
    
    public void setAuthCallback(AuthCallback callback) {
        this.callback = callback;
    }
    
    public boolean isLoggedIn() {
        return mAuth.getCurrentUser() != null;
    }
    
    public String getCurrentUserId() {
        FirebaseUser user = mAuth.getCurrentUser();
        return user != null ? user.getUid() : null;
    }
    
    public void login(String email, String password, final AuthCallback cb) {
        mAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    FirebaseUser user = mAuth.getCurrentUser();
                    if (cb != null) cb.onSuccess(user);
                    if (callback != null) callback.onSuccess(user);
                } else {
                    String error = task.getException() != null ? 
                        task.getException().getMessage() : "Error de autenticación";
                    if (cb != null) cb.onError(error);
                    if (callback != null) callback.onError(error);
                }
            });
    }
    
    public void register(String email, String password, final AuthCallback cb) {
        mAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    FirebaseUser user = mAuth.getCurrentUser();
                    if (cb != null) cb.onSuccess(user);
                    if (callback != null) callback.onSuccess(user);
                } else {
                    String error = task.getException() != null ? 
                        task.getException().getMessage() : "Error de registro";
                    if (cb != null) cb.onError(error);
                    if (callback != null) callback.onError(error);
                }
            });
    }

    public void loginWithCredential(AuthCredential credential, final AuthCallback cb) {
        mAuth.signInWithCredential(credential)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    FirebaseUser user = mAuth.getCurrentUser();
                    if (cb != null) cb.onSuccess(user);
                    if (callback != null) callback.onSuccess(user);
                } else {
                    String error = task.getException() != null ? 
                        task.getException().getMessage() : "Error de autenticación con credencial";
                    if (cb != null) cb.onError(error);
                    if (callback != null) callback.onError(error);
                }
            });
    }

    public void loginWithGoogle(String idToken, final AuthCallback cb) {
        com.google.firebase.auth.AuthCredential credential = 
            com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null);
        loginWithCredential(credential, cb);
    }
    
    public void logout() {
        mAuth.signOut();
    }
    
    public interface AuthCallback {
        void onSuccess(FirebaseUser user);
        void onError(String error);
    }
}