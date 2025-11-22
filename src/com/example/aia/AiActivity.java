package com.example.aia;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

public class AiActivity extends Activity {

    private EditText apiKeyEdit;
    private EditText promptEdit;
    private TextView responseText;
    private Button sendButton;

    // You can change this endpoint/model to any provider you prefer
    private static final String OPENAI_URL = "https://api.openai.com/v1/chat/completions";
    private static final String MODEL_NAME = "gpt-4.1-mini"; // or any supported model

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        apiKeyEdit = findViewById(R.id.api_key_edit);
        promptEdit = findViewById(R.id.prompt_edit);
        responseText = findViewById(R.id.response_text);
        sendButton = findViewById(R.id.send_button);

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String apiKey = apiKeyEdit.getText().toString().trim();
                String prompt = promptEdit.getText().toString().trim();

                if (apiKey.isEmpty()) {
                    Toast.makeText(AiActivity.this, "Please enter API key", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (prompt.isEmpty()) {
                    Toast.makeText(AiActivity.this, "Please enter a question", Toast.LENGTH_SHORT).show();
                    return;
                }

                callAi(apiKey, prompt);
            }
        });
    }

    private void callAi(final String apiKey, final String prompt) {
        responseText.setText("Asking AI...");

        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection conn = null;
                try {
                    URL url = new URL(OPENAI_URL);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setRequestProperty("Authorization", "Bearer " + apiKey);
                    conn.setDoInput(true);
                    conn.setDoOutput(true);

                    // Build JSON body
                    JSONObject root = new JSONObject();
                    root.put("model", MODEL_NAME);

                    JSONArray messages = new JSONArray();
                    JSONObject userMsg = new JSONObject();
                    userMsg.put("role", "user");
                    userMsg.put("content", prompt);
                    messages.put(userMsg);
                    root.put("messages", messages);

                    String body = root.toString();

                    // Write body
                    OutputStream os = conn.getOutputStream();
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                    writer.write(body);
                    writer.flush();
                    writer.close();
                    os.close();

                    int code = conn.getResponseCode();
                    InputStream is;
                    if (code >= 200 && code < 300) {
                        is = conn.getInputStream();
                    } else {
                        is = conn.getErrorStream();
                    }

                    StringBuilder sb = new StringBuilder();
                    BufferedReader br = new BufferedReader(new InputStreamReader(is));
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line);
                    }
                    br.close();

                    final String rawResponse = sb.toString();
                    final String answer = parseAnswerFromResponse(rawResponse);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (answer != null) {
                                responseText.setText(answer);
                            } else {
                                responseText.setText("Failed to parse response:\n" + rawResponse);
                            }
                        }
                    });

                } catch (final Exception e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            responseText.setText("Error: " + e.getMessage());
                        }
                    });
                } finally {
                    if (conn != null) {
                        conn.disconnect();
                    }
                }
            }
        }).start();
    }

    private String parseAnswerFromResponse(String json) {
        try {
            JSONObject root = new JSONObject(json);
            JSONArray choices = root.getJSONArray("choices");
            if (choices.length() == 0) return null;

            JSONObject first = choices.getJSONObject(0);
            JSONObject message = first.getJSONObject("message");
            return message.getString("content");
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }
}
