package com.marchinram.rxgallery.sample;

import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.marchinram.rxgallery.RxGallery;

import java.util.List;

import io.reactivex.functions.Consumer;

public final class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        RxGallery.Request.Builder builder = new RxGallery.Request.Builder()
                .setSource(RxGallery.Source.GALLERY);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            builder.setMimeTypes(RxGallery.MimeType.IMAGE, RxGallery.MimeType.VIDEO);
            builder.setMultiSelectEnabled(true);
        } else {
            builder.setMimeType(RxGallery.MimeType.IMAGE);
        }
        RxGallery.request(this, builder.build()).subscribe(new Consumer<List<Uri>>() {
            @Override
            public void accept(List<Uri> uris) throws Exception {

            }
        }, new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) throws Exception {
                Toast.makeText(MainActivity.this, throwable.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

}
