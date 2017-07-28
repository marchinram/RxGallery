package com.marchinram.rxgallery;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Pair;

import java.util.ArrayList;

public final class RxGalleryActivity extends Activity {

    static final String FINISHED_ACTION = "com.marchinram.rxgallery.FINISHED_ACTION";

    static final String DISPOSED_ACTION = "com.marchinram.rxgallery.DISPOSED_ACTION";

    static final String EXTRA_REQUEST = "extraRequest";

    static final String EXTRA_URIS = "extraUris";

    static final String EXTRA_ERROR_NO_ACTIVITY = "extraErrorNoActivity";

    private static final int RC_GALLERY = 1000;

    private static final int RC_TAKE_PHOTO = 1001;

    private BroadcastReceiver disposedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            finishActivity(RC_GALLERY);
            finishActivity(RC_TAKE_PHOTO);
            finish();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        registerReceiver(disposedReceiver, new IntentFilter(DISPOSED_ACTION));

        Pair<Intent, Integer> intentRequestPair;

        RxGallery.Request request = getIntent().getParcelableExtra(EXTRA_REQUEST);
        switch (request.getSource()) {
            default:
                intentRequestPair = getGalleryIntentRequestPair(request);
                break;
        }

        if (intentRequestPair.first.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intentRequestPair.first, intentRequestPair.second);
        } else {
            sendErrorNoActivity();
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(disposedReceiver);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Intent intent = new Intent(FINISHED_ACTION);
        ArrayList<Uri> uris = new ArrayList<>();

        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case RC_GALLERY:
                    uris = handleGallery(data);
                    break;
            }
        }

        intent.putParcelableArrayListExtra(EXTRA_URIS, uris);
        sendBroadcast(intent);
        finish();
    }

    private void sendErrorNoActivity() {
        Intent intent = new Intent(FINISHED_ACTION);
        intent.putExtra(EXTRA_ERROR_NO_ACTIVITY, true);
        sendBroadcast(intent);
    }

    private Pair<Intent, Integer> getGalleryIntentRequestPair(RxGallery.Request request) {
        Intent intent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            String[] mimeTypes = new String[request.getMimeTypes().size()];
            for (int i = 0; i < mimeTypes.length; i++) {
                mimeTypes[i] = request.getMimeTypes().get(i).toString();
            }
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
            intent.setType("*/*");
        } else {
            intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType(request.getMimeTypes().get(0).toString());
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, request.isMultiSelectEnabled());
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
        }
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        return new Pair<>(intent, RC_GALLERY);
    }

    private ArrayList<Uri> handleGallery(Intent data) {
        ArrayList<Uri> uris = new ArrayList<>();
        if (data.getData() != null) { // Single select
            uris.add(data.getData());
        } else { // Multi select
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                ClipData clipData = data.getClipData();
                for (int i = 0; i < clipData.getItemCount(); i++) {
                    uris.add(clipData.getItemAt(i).getUri());
                }
            }
        }
        return uris;
    }

}
