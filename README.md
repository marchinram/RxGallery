# RxGallery [![](https://jitpack.io/v/marchinram/RxGallery.svg)](https://jitpack.io/#marchinram/RxGallery) [![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-RxGallery-brightgreen.svg?style=flat)](https://android-arsenal.com/details/1/6222)
Android gallery &amp; photo/video functionality simplified with RxJava2 

## Setup
To use this library your `minSdkVersion` must be >= 9.

Add it in your root build.gradle at the end of repositories:

```
allprojects {
  repositories {
    ...
    maven { url 'https://jitpack.io' }
  }
}
```
Add the dependency
```
dependencies {
  compile 'com.github.marchinram:RxGallery:0.6.4'
}
```

## Usage
**__Picking items from the gallery__**

```
Maybe<List<Uri>> RxGallery.gallery(@NonNull Activity activity)
Maybe<List<Uri>> RxGallery.gallery(@NonNull Activity activity, boolean multiSelectEnabled)
Maybe<List<Uri>> RxGallery.gallery(@NonNull Activity activity, boolean multiSelectEnabled, @Nullable MimeType... mimeTypes)
```

Example - Picking multiple images/videos from the gallery:
```
RxGallery.gallery(this, true, RxGallery.MimeType.IMAGE, RxGallery.MimeType.VIDEO).subscribe(new Consumer<List<Uri>>() {
    @Override
    public void accept(List<Uri> uris) throws Exception {
        doStuffWithUris(uris);
    }
}, new Consumer<Throwable>() {
    @Override
    public void accept(Throwable throwable) throws Exception {
        Toast.makeText(SomeActivity.this, throwable.getMessage(), Toast.LENGTH_LONG).show();
    }
});
```
**__Taking photos with camera__**

```
Maybe<Uri> RxGallery.photoCapture(@NonNull Activity activity)
Maybe<Uri> RxGallery.photoCapture(@NonNull Activity activity, @Nullable Uri outputUri)
```

Example - Taking a photo with the camera and saving it to gallery:
```
RxGallery.photoCapture(this).subscribe(new Consumer<Uri>() {
    @Override
    public void accept(Uri uri) throws Exception {
        doStuffWithUri(uri);
    }
}, new Consumer<Throwable>() {
    @Override
    public void accept(Throwable throwable) throws Exception {
        Toast.makeText(SomeActivity.this, throwable.getMessage(), Toast.LENGTH_LONG).show();
    }
});
```
In 6.0+ you need to ask for WRITE_EXTERNAL_STORAGE permission to save to gallery, below is an example doing this with [RxPermissions](https://github.com/tbruyelle/RxPermissions) and `flatMap`:
```
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
        return RxGallery.photoCapture(SomeActivity.this).toObservable();
    }
}).subscribe(new Consumer<Uri>() {
    @Override
    public void accept(Uri uri) throws Exception {
        doStuffWithUri(uri);
    }
}, new Consumer<Throwable>() {
    @Override
    public void accept(Throwable throwable) throws Exception {
        Toast.makeText(SomeActivity.this, throwable.getMessage(), Toast.LENGTH_LONG).show();
    }
});
```
**__Taking videos with camera__**

```
Maybe<Uri> RxGallery.videoCapture(@NonNull Activity activity)
```

Example - Taking a video with the camera and saving it to gallery:
```
RxGallery.videoCapture(this).subscribe(new Consumer<Uri>() {
    @Override
    public void accept(Uri uri) throws Exception {
        doStuffWithUri(uri);
}, new Consumer<Throwable>() {
    @Override
    public void accept(Throwable throwable) throws Exception {
        Toast.makeText(MainActivity.this, throwable.getMessage(), Toast.LENGTH_LONG).show();
    }
});
```
## Important
If you want the started Activity (gallery/photo/video) to be destroyed when the Activity which started it is destroyed you must keep a reference to the `Disposable` and call `dispose` as shown below:
```
public final class SomeActivity extends Activity {

    private Disposable disposable;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ...
        
        disposable = RxGallery.gallery(this).subscribe(new Consumer<List<Uri>>() {
            @Override
            public void accept(List<Uri> uris) throws Exception {
                doStuffWithUris(uris);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disposable.dispose();
    }
    
    ...
}
```
Alternatively you can use [RxLifecycle](https://github.com/trello/RxLifecycle)
```
public final class SomeActivity extends RxActivity {

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ...
        
        RxGallery.gallery(this)
                .compose(this.<List<Uri>>bindUntilEvent(ActivityEvent.DESTROY))
                .subscribe(new Consumer<List<Uri>>() {
                    @Override
                    public void accept(List<Uri> uris) throws Exception {
                        doStuffWithUris(uris);
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {

                    }
                });
    }
    
    ...
}
```
