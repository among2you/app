package com.example.friendchat;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private DatabaseReference chatRef;

    private EditText friendUsernameInput, messageInput;
    private ListView chatListView;
    private ChatAdapter chatAdapter;
    private ArrayList<String> chatMessages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        chatMessages = new ArrayList<>();

        friendUsernameInput = findViewById(R.id.friendUsernameInput);
        messageInput = findViewById(R.id.messageInput);
        chatListView = findViewById(R.id.chatListView);

        findViewById(R.id.addFriendButton).setOnClickListener(v -> addFriend());
        findViewById(R.id.sendMessageButton).setOnClickListener(v -> sendMessage());

        chatAdapter = new ChatAdapter(this, chatMessages);
        chatListView.setAdapter(chatAdapter);
    }

    private void addFriend() {
        String friendUsername = friendUsernameInput.getText().toString().trim();
        if (TextUtils.isEmpty(friendUsername)) {
            Toast.makeText(this, "Enter a valid username", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users").whereEqualTo("username", friendUsername)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String friendId = document.getId();
                            addFriendToDatabase(friendId, friendUsername);
                        }
                    } else {
                        Toast.makeText(ChatActivity.this, "User not found", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void addFriendToDatabase(String friendId, String friendUsername) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            DatabaseReference friendRef = FirebaseDatabase.getInstance().getReference("friends")
                    .child(currentUser.getUid()).child(friendId);
            friendRef.setValue(friendUsername)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(ChatActivity.this, "Friend added!", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void sendMessage() {
        String message = messageInput.getText().toString().trim();
        String friendUsername = friendUsernameInput.getText().toString().trim();

        if (!TextUtils.isEmpty(message) && !TextUtils.isEmpty(friendUsername)) {
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser != null) {
                String userId = currentUser.getUid();

                db.collection("users").whereEqualTo("username", friendUsername)
                        .get()
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful() && !task.getResult().isEmpty()) {
                                for (QueryDocumentSnapshot document : task.getResult()) {
                                    String friendId = document.getId();

                                    chatRef = FirebaseDatabase.getInstance().getReference("chats")
                                            .child(userId).child(friendId);

                                    Map<String, String> chatMessage = new HashMap<>();
                                    chatMessage.put("sender", userId);
                                    chatMessage.put("message", message);

                                    chatRef.push().setValue(chatMessage)
                                            .addOnCompleteListener(task1 -> {
                                                if (task1.isSuccessful()) {
                                                    chatMessages.add("Me: " + message);
                                                    chatAdapter.notifyDataSetChanged();
                                                    messageInput.setText("");
                                                }
                                            });
                                }
                            }
                        });
            }
        }
    }
}
