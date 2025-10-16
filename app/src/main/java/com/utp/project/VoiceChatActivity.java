package com.utp.project;

import android.net.Uri;
import android.os.Bundle;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;
import com.utp.project.R;

public class VoiceChatActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_chat);

        VideoView voiceWave = findViewById(R.id.voice_wave_video);
        Uri videoUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.orb);
        voiceWave.setVideoURI(videoUri);
        voiceWave.setOnPreparedListener(mp -> {
            mp.setLooping(true); // Para que se repita el video
            voiceWave.start();
        });
    }
}
