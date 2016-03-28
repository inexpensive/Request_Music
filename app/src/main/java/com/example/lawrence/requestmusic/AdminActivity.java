package com.example.lawrence.requestmusic;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;

public class AdminActivity extends AppCompatActivity {

    private Socket serverSocket, currentlyPlayingSocket;
    private ObjectInputStream inFromServer, inFromCurrentlyPlaying;
    private ObjectOutputStream outToServer;
    private boolean connected = false;
    private ArrayAdapter<String> mPlaylistAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //noinspection ConstantConditions
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        ImageButton playButton = (ImageButton) findViewById(R.id.admin_play_button);
        if (playButton != null) {
            playButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //send a play command to the server
                    try {
                        if (connected) outToServer.writeObject("play");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    Snackbar.make(view, "Playing the song :)", Snackbar.LENGTH_SHORT)
                            .setAction("Action", null).show();
                }
            });
        }

        ImageButton pauseButton = (ImageButton) findViewById(R.id.admin_pause_button);
        if (pauseButton != null) {
            pauseButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (connected) try {
                        outToServer.writeObject("pause");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    Snackbar.make(view, "Pausing song...", Snackbar.LENGTH_SHORT)
                            .setAction("Action", null).show();
                }
            });
        }

        ImageButton skipButton = (ImageButton) findViewById(R.id.admin_skip_button);
        if (skipButton != null) {
            skipButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (connected) try {
                        outToServer.writeObject("adminSkip");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    Snackbar.make(view, "Skipping song...", Snackbar.LENGTH_SHORT)
                            .setAction("Action", null).show();
                }
            });
        }

        mPlaylistAdapter = new ArrayAdapter<>(this, R.layout.list_item_playlist, R.id.list_item_playlist_textview, new ArrayList<String>());

        ListView playlist = (ListView) findViewById(R.id.playlist_view);
        if (playlist != null) {
            playlist.setAdapter(mPlaylistAdapter);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!connected) connectToDJServer("192.168.0.61");
        UpdateTask task = new UpdateTask();
        task.execute();
    }

    @Override
    public void onStop() {
        super.onStop();
        //tells the server to close the socket.
        try {
            outToServer.writeObject("close");
            inFromServer.close();
            inFromCurrentlyPlaying.close();
            outToServer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        DisconnectTask task = new DisconnectTask();
        task.execute();
        connected = false;

    }

    private void connectToDJServer(String ip) {
        ConnectTask task = new ConnectTask();
        task.execute(ip);
    }

    //handles the connection to the server socket
    public class ConnectTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... params){
            if (params.length == 0) return null;

            try {
                serverSocket = new Socket(params[0], 1729);
                currentlyPlayingSocket = new Socket(params[0], 1729);
                inFromServer = new ObjectInputStream(serverSocket.getInputStream());
                outToServer = new ObjectOutputStream(serverSocket.getOutputStream());
                inFromCurrentlyPlaying = new ObjectInputStream(currentlyPlayingSocket.getInputStream());
                connected = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;

        }
    }

    public class DisconnectTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            try {
                connected = false;
                serverSocket.close();
                currentlyPlayingSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    public class UpdateTask extends AsyncTask<Void, Void, String[]> {
        @Override
        protected String[] doInBackground(Void... params) {
            try {
                outToServer.writeObject("playlist");
                return (String[]) inFromServer.readObject();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
            return null;
        }
        @Override
        protected void onPostExecute(String[] result) {
            if (result != null) {
                mPlaylistAdapter.clear();
                for (String playlistEntry : result) {
                    mPlaylistAdapter.add(playlistEntry);
                }
                // New data is back from the server.  Hooray!
            }
        }
    }




}
