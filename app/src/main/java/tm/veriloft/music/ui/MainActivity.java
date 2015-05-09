/*
 * Copyright (c) 2015  Alashov Berkeli
 * It is licensed under GNU GPL v. 2 or later. For full terms see the file LICENSE.
 */

package tm.veriloft.music.ui;

import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.SearchView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.cocosw.bottomsheet.BottomSheet;
import com.google.android.gcm.GCMRegistrar;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.pnikosis.materialishprogress.ProgressWheel;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import butterknife.ButterKnife;
import butterknife.InjectView;
import tm.veriloft.music.Config;
import tm.veriloft.music.R;
import tm.veriloft.music.adapter.AudioListAdapter;
import tm.veriloft.music.model.Audio;
import tm.veriloft.music.util.AudioWife;
import tm.veriloft.music.util.MusicApiClient;
import tm.veriloft.music.util.U;
import tr.xip.errorview.ErrorView;


/**
 * Created by alashov on 21/03/15.
 */
public class MainActivity extends BaseActivity {

    private ArrayList<Audio> audioList = new ArrayList<>();
    private AudioListAdapter audioListAdapter;
    private MediaPlayer mMediaPlayer;
    private LayoutInflater layoutInflater;
    private SearchView mSearchView;
    private String oldQuery = "";

    @InjectView(R.id.listView) ListView mListView;
    @InjectView(R.id.progress) ProgressWheel progressBar;
    @InjectView(R.id.errorView) ErrorView errorView;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate(savedInstanceState);

        ButterKnife.inject(this);
        layoutInflater = LayoutInflater.from(this);

        errorView.setOnRetryListener(new ErrorView.RetryListener() {
            @Override public void onRetry() {
                U.hideView(errorView);
                search(oldQuery);
            }
        });

        //GCM Registration
        GCMRegistrar.checkDevice(this);
        GCMRegistrar.checkManifest(this);
        final String regId = GCMRegistrar.getRegistrationId(this);
        if (regId.equals("")) {
            GCMRegistrar.register(this, Config.GCM_SENDER_ID);
        } else {
            RequestParams params = new RequestParams();
            params.put("reg_id", regId);
            MusicApiClient.get(Config.ENDPOINT_API + "reg_id.php", params, new JsonHttpResponseHandler() {
                @Override
                public void onSuccess( int statusCode, Header[] headers, JSONObject response ) {
                    U.l(response.toString());
                }
            });
        }
        updateToken();
    }

    @Override
    public boolean onCreateOptionsMenu( Menu menu ) {
        getMenuInflater().inflate(R.menu.main, menu);
        MenuItem searchItem = menu.findItem(R.id.search);
        mSearchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        if (mSearchView != null) {
            mSearchView.setQueryHint(getString(R.string.search_hint));
            mSearchView.setFocusable(true);
            mSearchView.setIconified(false);
            mSearchView.requestFocusFromTouch();
            mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit( String query ) {
                    mSearchView.clearFocus(); //Hide keyboard
                    search(query);
                    return false;
                }

                @Override
                public boolean onQueryTextChange( String query ) {
                    return true;
                }
            });
        }
        return true;
    }

    private void search( String query ) {
        clearList();//clearing old data
        oldQuery = query;
        RequestParams params = new RequestParams();

        params.put("q", query);
        params.put("access_token", settingsManager.getVkToken());
        params.put("autocomplete", Config.VK_CONFIG_AUTOCOMPLETE);
        params.put("sort", Config.VK_CONFIG_SORT);
        params.put("count", Config.VK_CONFIG_COUNT);

        MusicApiClient.get(Config.VK_AUDIO_SEARCH, params, new JsonHttpResponseHandler() {
            @Override public void onStart() {
                U.hideView(mListView);
                U.hideView(errorView);
                U.showView(progressBar);
            }

            @Override
            public void onSuccess( int statusCode, Header[] headers, JSONObject response ) {
                try {
                    if (response.has("error")) { //if we have error
                        //Parsing errors
                        JSONObject errorObject = response.getJSONObject("error");
                        int errorCode = errorObject.getInt("error_code");
                        //showing error
                        if (errorCode == 5) {
                            showError("token");
                        } else {
                            showError(errorObject.getString("error_msg"));
                        }
                        return;
                    }
                    JSONArray audios = response.getJSONArray("response");
                    if (audios.length() >= 2) {
                        for(int i = 1; i < audios.length(); i++)
                            audioList.add(new Audio((JSONObject) audios.get(i)));
                        audioListAdapter = new AudioListAdapter(MainActivity.this, audioList);
                        mListView.setAdapter(audioListAdapter);
                        mListView.setFastScrollEnabled(audioList.size() > 10);

                        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                            @Override
                            public void onItemClick( AdapterView<?> parent, View view, int position, long id ) {
                                final Audio audio = audioList.get(position);
                                new BottomSheet.Builder(MainActivity.this).title(audio.getArtist()).sheet(R.menu.audio_actions).listener(new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick( DialogInterface dialog, int which ) {
                                        switch (which) {
                                            case R.id.download:
                                                DownloadManager mgr = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                                                Uri downloadUri = Uri.parse(audio.getSrc());
                                                DownloadManager.Request request = new DownloadManager.Request(downloadUri);

                                                if (U.isAboveOfVersion(11))
                                                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

                                                request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE)
                                                    .setDestinationInExternalPublicDir("/AlashovMusic",
                                                        encodeFilename(audio.getArtist() + " - " + audio.getTitle()) + ".mp3");
                                                mgr.enqueue(request);
                                                break;
                                            case R.id.play:
                                                playAudio(Uri.parse(audio.getSrc()));
                                                break;
                                            case R.id.copy:
                                                if (! U.isAboveOfVersion(11)) {
                                                    android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                                                    clipboard.setText(audio.getSrc());
                                                } else {
                                                    android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                                                    android.content.ClipData clip = android.content.ClipData.newPlainText("Link", audio.getSrc());
                                                    clipboard.setPrimaryClip(clip);
                                                    U.showCenteredToast(MainActivity.this, R.string.audio_copied);
                                                }
                                                break;
                                        }
                                    }
                                }).show();
                            }
                        });

                    } else showError("notFound");
                } catch (Exception e) {
                    U.showCenteredToast(MainActivity.this, R.string.exception);
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure( int statusCode, Header[] headers, String responseString, Throwable throwable ) {
                showError("network");
            }

            @Override
            public void onFailure( int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse ) {
                showError("network");
            }

            @Override
            public void onFailure( int statusCode, Header[] headers, Throwable throwable, JSONArray errorResponse ) {
                showError("network");
            }

            @Override public void onFinish() {
                U.showView(mListView);
                U.hideView(progressBar);
            }

            @Override public void onProgress( int bytesWritten, int totalSize ) {

            }
        });
    }

    /**
     * Replace illegal filename characters for android ? : " * | / \ < >
     *
     * @param string string to replace
     * @return replaced string
     */
    private String encodeFilename( String string ) {
        String[] illegalCharacters = {"\\x3F", "\\x3A", "\\x22", "\\x2A", "\\x7C", "\\x2F", "\\x5C", "\\x3C", "\\x3E"};
        for(String s : illegalCharacters)
            string = string.replaceAll(s, " ");
        return string;
    }

    /**
     * Shows error by given error type
     *
     * @param error errors type or error message
     */
    private void showError( String error ) {
        U.showView(errorView);
        if (error.equals("network")) {
            errorView.setSubtitle(R.string.network_error);
        } else if (error.equals("token")) {
            updateToken();//updating tokens quickly
            errorView.setSubtitle(R.string.error_token);
        } else if (error.equals("notFound")) {
            errorView.setSubtitle(R.string.error_not_found);
        } else {
            errorView.setSubtitle(getString(R.string.error) + ": " + error);
        }
    }

    /**
     * Getting tokens from server and storing them to shared preferences
     */
    private void updateToken() {
        MusicApiClient.get(Config.ENDPOINT_API + "get_token.php", null, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess( int statusCode, Header[] headers, JSONObject response ) {
                try {
                    settingsManager.setVkToken(response.getString("vkToken"));
                    settingsManager.setLastFmToken(response.getString("lastFmToken"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Clear array and listview
     */
    private void clearList() {
        mListView.setAdapter(null);
        audioList.clear();
        mListView.setFastScrollEnabled(false);
    }


    public void playAudio( Uri uri ) {
        LinearLayout rootView = new LinearLayout(this);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setView(rootView);
        final AlertDialog alertDialog = alertDialogBuilder.create();

        //destroy mediaPlayer when dialog dismissed
        alertDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override public void onDismiss( DialogInterface dialog ) {
                resetPlayer();
            }
        });

        new PrepareAudioTask(rootView, new OnPreparedListener() {
            @Override public void onPrepared( MediaPlayer mediaPlayer ) {
                mMediaPlayer = mediaPlayer;
                alertDialog.show();
            }

            @Override public void onError( Exception e ) {
                U.showCenteredToast(MainActivity.this, R.string.exception);
            }
        }).execute(uri);
    }

    /**
     * stop playing audio
     */
    public void resetPlayer() {
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.reset();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    /**
     * Shows progress dialog while preparing mediaPlayer
     */
    public class PrepareAudioTask extends AsyncTask<Uri, Void, Void> {
        private ViewGroup rootView;
        private OnPreparedListener onPreparedListener;
        private ProgressDialog progressDialog;

        public PrepareAudioTask( ViewGroup rootView, OnPreparedListener onPreparedListener ) {
            this.rootView = rootView;
            this.onPreparedListener = onPreparedListener;
            progressDialog = U.createActionLoading(MainActivity.this);
        }

        @Override protected void onPreExecute() {
            progressDialog.show();
        }

        @Override
        protected Void doInBackground( Uri... params ) {
            try {
                AudioWife.getInstance()
                    .init(MainActivity.this, params[0], new OnPreparedListener() {
                        @Override public void onPrepared( MediaPlayer mediaPlayer ) {
                            progressDialog.dismiss();
                            onPreparedListener.onPrepared(mediaPlayer);
                        }

                        @Override public void onError( Exception e ) {
                            onPreparedListener.onError(e);
                            progressDialog.dismiss();
                        }
                    }).useDefaultUi(rootView, layoutInflater);
            } catch (Exception e) {
                e.printStackTrace();
                progressDialog.dismiss();
                onPreparedListener.onError(e);
            }
            return null;
        }
    }

    public interface OnPreparedListener {
        /**
         * called when audio prepared
         *
         * @param mediaPlayer mediaPlayer
         */
        void onPrepared( MediaPlayer mediaPlayer );

        /**
         * called when catch exception
         *
         * @param e exception
         */
        void onError( Exception e );
    }

    @Override protected int getLayoutResourceId() {
        return R.layout.activity_main;
    }

    @Override protected Boolean isChildActivity() {
        return false;
    }

    @Override protected String getActivityTag() {
        return Config.ACTIVITY_TAG_MAIN;
    }
}
