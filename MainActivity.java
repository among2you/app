package com.example.friendchat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private GoogleSignInClient googleSignInClient;
    private EditText usernameInput;
    private Button loginButton, setUsernameButton;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        googleSignInClient = GoogleSignIn.getClient(this, new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build());

        usernameInput = findViewById(R.id.usernameInput);
        loginButton = findViewById(R.id.loginButton);
        setUsernameButton = findViewById(R.id.setUsernameButton);

        loginButton.setOnClickListener(v -> signInWithGoogle());

        setUsernameButton.setOnClickListener(v -> {
            String username = usernameInput.getText().toString().trim();
            checkUsernameUnique(username);
        });
    }

    private void signInWithGoogle() {
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, 9001);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 9001) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult();
            firebaseAuthWithGoogle(account);
        } catch (Exception e) {
            Log.w("SignInActivity", "signInResult:failed", e);
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        mAuth.signInWithCredential(credential)
            .addOnCompleteListener(this, task -> {
                if (task.isSuccessful()) {
                    FirebaseUser user = mAuth.getCurrentUser();
                    Toast.makeText(MainActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void checkUsernameUnique(String username) {
        db.collection("users")
            .whereEqualTo("username", username)
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    if (task.getResult().isEmpty()) {
                        saveUsername(username);
                    } else {
                        Toast.makeText(MainActivity.this, "Username already taken!", Toast.LENGTH_SHORT).show();
                    }
                }
            });
    }

    private void saveUsername(String username) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            Map<String, Object> user = new HashMap<>();
            user.put("username", username);
            user.put("email", currentUser.getEmail());

            db.collection("users").document(currentUser.getUid())
                .set(user)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(MainActivity.this, "Username set successfully!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(MainActivity.this, ChatActivity.class);
                        startActivity(intent);
                    } else {
                        Toast.makeText(MainActivity.this, "Error setting username.", Toast.LENGTH_SHORT).show();
                    }
                });
        }
    }
}
