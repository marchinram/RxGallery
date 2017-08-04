# RxGallery
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
  compile 'com.github.marchinram:RxGallery:0.5.0'
}
```

## Usage
**__Picking items from the gallery__**

```
Single<List<Uri>> RxGallery.gallery(@NonNull Activity activity)
Single<List<Uri>> RxGallery.gallery(@NonNull Activity activity, boolean multiSelectEnabled)
Single<List<Uri>> RxGallery.gallery(@NonNull Activity activity, boolean multiSelectEnabled, @Nullable MimeType... mimeTypes)
```

Example - Picking multiple images/videos from the gallery:
```
RxGallery.gallery(this, true, RxGallery.MimeType.IMAGE, RxGallery.MimeType.VIDEO).subscribe(new Consumer<List<Uri>>() {
    @Override
    public void accept(List<Uri> uris) throws Exception {
        doStuffWithUris(uris);
}, new Consumer<Throwable>() {
    @Override
    public void accept(Throwable throwable) throws Exception {
        Toast.makeText(SomeActivity.this, throwable.getMessage(), Toast.LENGTH_LONG).show();
    }
});
```
**__Taking photos with camera__**

```
Single<Uri> RxGallery.photoCapture(@NonNull Activity activity)
Single<Uri> RxGallery.photoCapture(@NonNull Activity activity, @Nullable Uri outputUri)
```

Example - Taking a photo with the camera and saving it to gallery:
```
RxGallery.photoCapture(this).subscribe(new Consumer<Uri>() {
    @Override
    public void accept(Uri uri) throws Exception {
        if (!uri.equals(Uri.EMPTY)) { // Uri is EMPTY if user presses back on photo activity
            doStuffWithUri(uri);
        }
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
        if (!uri.equals(Uri.EMPTY)) {
            doStuffWithUri(uri);
        }
    }
}, new Consumer<Throwable>() {
    @Override
    public void accept(Throwable throwable) throws Exception {
        Toast.makeText(SomeActivity.this, throwable.getMessage(), Toast.LENGTH_LONG).show();
    }
});
```
