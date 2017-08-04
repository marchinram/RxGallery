package com.marchinram.rxgallery.sample;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.marchinram.rxgallery.RxGallery;
import com.tbruyelle.rxpermissions2.RxPermissions;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;

public final class MainActivity extends AppCompatActivity {

    private static final String AUTHORITY = "com.marchinram.rxgallery.sample.fileprovider";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button takeVideoButton = (Button) findViewById(R.id.take_video_btn);
        takeVideoButton.setEnabled(getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA));
    }

    public void pickFromGallery(View view) {
        RxGallery.gallery(this, true, RxGallery.MimeType.IMAGE, RxGallery.MimeType.VIDEO).subscribe(new Consumer<List<Uri>>() {
            @Override
            public void accept(List<Uri> uris) throws Exception {
                StringBuffer message = new StringBuffer(getResources()
                        .getQuantityString(R.plurals.selected_items, uris.size(), uris.size()));
                for (Uri uri : uris) {
                    message.append("\n").append(uri.toString());
                }
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
            }
        }, new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) throws Exception {
                Toast.makeText(MainActivity.this, throwable.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    public void takeVideo(View view) {
        RxGallery.videoCapture(this).subscribe(new Consumer<Uri>() {
            @Override
            public void accept(Uri uri) throws Exception {
                Toast.makeText(MainActivity.this, uri.toString(), Toast.LENGTH_LONG).show();
            }
        }, new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) throws Exception {
                Toast.makeText(MainActivity.this, throwable.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    public void takePhoto(View view) {
        String[] items = new String[]{getString(R.string.output_dialog_external),
                getString(R.string.output_dialog_mediastore)};

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.output_dialog_title))
                .setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Uri outputUri = null;
                        if (which == 0) {
                            outputUri = getExternalStorageUri();
                        }
                        takePhoto(outputUri);
                        dialog.dismiss();
                    }
                }).show();
    }

    private void takePhoto(final Uri outputUri) {
        Observable<Boolean> permissionObservable = Observable.just(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            permissionObservable = new RxPermissions(this).request(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        permissionObservable.flatMap(new Function<Boolean, ObservableSource<Uri>>() {
            @Override
            public ObservableSource<Uri> apply(@NonNull Boolean granted) throws Exception {
                if (!granted) {
                    return Observable.empty();
                }
                return RxGallery.photoCapture(MainActivity.this, outputUri).toObservable();
            }
        }).subscribe(new Consumer<Uri>() {
            @Override
            public void accept(Uri uri) throws Exception {
                Toast.makeText(MainActivity.this, uri.toString(), Toast.LENGTH_LONG).show();
            }
        }, new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) throws Exception {
                Toast.makeText(MainActivity.this, throwable.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private Uri getExternalStorageUri() {
        Uri uri = null;
        try {
            File directory = new File(getExternalFilesDir(null), "images");
            if (!directory.exists()) {
                if (!directory.mkdir()) {
                    throw new IOException();
                }
            }
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            File file = File.createTempFile(timeStamp, ".jpg", directory);
            uri = FileProvider.getUriForFile(MainActivity.this, AUTHORITY, file);
        } catch (IOException e) {
            Toast.makeText(this, getString(R.string.output_file_error), Toast.LENGTH_LONG).show();
        }
        return uri;
    }

}
