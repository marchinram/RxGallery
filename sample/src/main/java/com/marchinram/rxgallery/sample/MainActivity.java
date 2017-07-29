package com.marchinram.rxgallery.sample;

import android.net.Uri;
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

        RxGallery.Request request = new RxGallery.Request.Builder()
                .setSource(RxGallery.Source.PHOTO_CAPTURE)
                .build();
        RxGallery.request(this, request).subscribe(new Consumer<List<Uri>>() {
            @Override
            public void accept(List<Uri> uris) throws Exception {
                Toast.makeText(MainActivity.this, uris.get(0).toString(), Toast.LENGTH_LONG).show();
            }
        }, new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) throws Exception {
                Toast.makeText(MainActivity.this, throwable.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

}
