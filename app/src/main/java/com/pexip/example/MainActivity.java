package com.pexip.example;

import android.opengl.GLSurfaceView;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.pexip.pexkit.Conference;
import com.pexip.pexkit.IResponse;
import com.pexip.pexkit.PexKit;

import java.net.URI;

public class MainActivity extends ActionBarActivity {
    private Conference conference = null;
    private PexKit pexContext = null;
    private GLSurfaceView videoView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        try {
            videoView = (GLSurfaceView) findViewById(R.id.videoView);
            if (this.pexContext == null) {
                this.conference = new Conference("Android Example App", new URI("meet.hani@rd.pexip.com"), "");
                this.pexContext = PexKit.create(getBaseContext(), (GLSurfaceView) findViewById(R.id.videoView));
                Log.i("MainActivity", "done initializing pexkit");
            } else {
                this.pexContext.recreate(getBaseContext(), (GLSurfaceView) findViewById(R.id.videoView));
            }
        } catch (Exception e) {}
    }

    @Override
    public void onPause() {
        super.onPause();
        videoView.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        videoView.onResume();
    }
    public void onLogin(final View v) {
        ((Button) v).setEnabled(false);
        if (this.conference.isLoggedIn()) {
            Log.i("MainActivity", "about to release");
            conference.disconnectMedia(new IResponse() {
                @Override
                public void disconnectMediaResponse(ServiceResponse status) {
                    conference.releaseToken(new IResponse() {
                        @Override
                        public void releaseTokenResponse(IResponse.ServiceResponse status) {
                            Log.i("MainActivity", "release token status is " + status);
                            ((Button) v).setEnabled(true);
                        }
                    });
                }
            });

        } else {
            Log.i("MainActivity", "about to connect");
            this.conference.connect(new IResponse() {
                @Override
                public void connectResponse(IResponse.ServiceResponse status) {
                    conference.requestToken(new IResponse() {
                        @Override
                        public void requestTokenResponse(IResponse.ServiceResponse status) {
                            Log.i("MainActivity", "req token status is " + status);
                            conference.escalateMedia(pexContext, new IResponse() {
                                @Override
                                public void escalateCallResponse(ServiceResponse status) {
                                    Log.i("MainActivity", "escalate call status is " + status);
                                }
                            });
                            ((Button) v).setEnabled(true);
                        }
                    });
                }
            });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
