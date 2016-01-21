package com.pexip.example;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.os.PowerManager;
import android.os.SystemClock;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Chronometer;

import com.pexip.pexkit.Conference;
import com.pexip.pexkit.ConferenceDelegate;
import com.pexip.pexkit.IStatusResponse;
import com.pexip.pexkit.Participant;
import com.pexip.pexkit.PexKit;
import com.pexip.pexkit.ServiceResponse;

import java.net.URI;

public class MainActivity extends ActionBarActivity {
    private Conference conference = null;
    private PexKit pexContext = null;
    private GLSurfaceView videoView;
    private Chronometer chronometer;
    private PowerManager.WakeLock wl;
    private int originalWidth;
    private int originalHeight;
    private int originalOrientation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        PowerManager pm = (PowerManager)getApplicationContext().getSystemService(
                Context.POWER_SERVICE);
        wl = pm.newWakeLock(
                PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE,
                "MainActivity");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        try {
            Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread t, Throwable e) {
                    Log.e("MainActivity", String.format("Uncaught Exception detected in thread %s %s", t, e));
                }
            });
        } catch (SecurityException e) {
            Log.e("MainActivity", String.format("Could not set the Default Uncaught Exception Handler %s", e));
        }
        try {
            videoView = (GLSurfaceView) findViewById(R.id.videoView);
            this.conference = new Conference("Android Example App", new URI("db@pexipdemo.com"), "9729");
            this.conference.setDelegate(new ConferenceDelegate() {
                @Override
                public void stageUpdate(final Participant[] stage) {
                    Participant[] participants = conference.getParticipants();
                    for (Participant p: participants) {
                        if (p.uuid == conference.getUUID()) {
                            if (p.isMuted) {
                                Log.i("MainActivity", "We were muted.");
                            }
                        }
                    }
                    if (stage.length > 0) {
                        Log.i("Stageupdate", "VAD is " + stage[0].vad);
                    }
                }
            });
            this.pexContext = PexKit.create(getBaseContext(), (GLSurfaceView) findViewById(R.id.videoView));
            pexContext.setCallbacks(new PexKit.Callbacks() {
                @Override
                public void onFirstFrameRendered() {

                }

                @Override
                public void onFrameResolutionChanged(int width, int height, int orientation) {
                    setAspectRatio(width, height, orientation);
                }
            });
            this.chronometer = (Chronometer) findViewById(R.id.chronometer);
            Log.i("MainActivity", "done initializing pexkit");
        } catch (Exception e) {}
    }

    public void setAspectRatio(int width, int height, int orientation) {
        originalWidth = width;
        originalHeight = height;
        originalOrientation = orientation;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                videoView = (GLSurfaceView) findViewById(R.id.videoView);
                int width_ = videoView.getWidth();
                int viewHeight = (width_ * originalHeight ) / originalWidth;
                Log.i("Example", "Setting height to " + viewHeight + " " + width_);
                ViewGroup.LayoutParams lp = videoView.getLayoutParams();
                lp.height = viewHeight;
            }
        });
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
            wl.release();
            chronometer.stop();
            Log.i("MainActivity", "about to release");
            conference.disconnectMedia(new IStatusResponse() {
                @Override
                public void response(ServiceResponse status) {
                    conference.releaseToken(new IStatusResponse() {
                        @Override
                        public void response(ServiceResponse status) {
                            Log.i("MainActivity", "release token status is " + status);

                            try {
                                conference = new Conference("Android Example App", new URI("meet.geir@pexipdemo.com"), "4567");
                                conference.setDelegate(new ConferenceDelegate() {
                                    @Override
                                    public void stageUpdate(final Participant[] stage) {
                                        if (stage.length > 0) {
                                            Log.i("Stageupdate", "VAD is " + stage[0].vad);
                                        }
                                    }
                                });
                                pexContext = PexKit.create(getBaseContext(), (GLSurfaceView) findViewById(R.id.videoView));
                            } catch (Exception e) {
                            }

                            ((Button) v).setEnabled(true);
                        }
                    });
                }
            });

        } else {
            Log.i("MainActivity", "about to connect");
            wl.acquire();
            this.conference.connect(new IStatusResponse() {
                @Override
                public void response(ServiceResponse status) {
                    conference.requestToken(new IStatusResponse() {
                        @Override
                        public void response(ServiceResponse status) {
                            Log.i("MainActivity", "req token status is " + status);
                            conference.escalateMedia(pexContext, new IStatusResponse() {
                                @Override
                                public void response(ServiceResponse status) {
                                    Log.i("MainActivity", "escalate call status is " + status);
                                    conference.listenForEvents();
                                    chronometer.setBase(SystemClock.elapsedRealtime());
                                    chronometer.start();
                                }
                            });
                            ((Button) v).setEnabled(true);
                        }
                    });
                }
            });
        }
    }

    public void onCameraChange(final View v) {
        ((Button) v).setEnabled(false);
        Log.i("MainActivity", "about to switch camera");
        conference.toggleCameraSwitch(new IStatusResponse() {
            @Override
            public void response(ServiceResponse status) {
                Log.i("MainActivity", "Switched Camera");
                ((Button) v).setEnabled(true);
            }
        });
    }

    public void onCameraMute(final View v) {
        Log.i("MainActivity", "about to mute camera");
        if (conference.getVideoMute()) {
            conference.setVideoMute(false);
            pexContext.moveSelfView(72, 72, 25, 25);
        } else {
            conference.setVideoMute(true);
            pexContext.moveSelfView(0, 0, 0, 0);
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
