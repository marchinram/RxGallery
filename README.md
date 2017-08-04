# RxGallery
Gallery &amp; photo/video functionality simplified with RxJava2

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

### Example
Picking multiple images/videos from the gallery:
```
RxGallery.gallery(this, true, RxGallery.MimeType.IMAGE, RxGallery.MimeType.VIDEO).subscribe(new Consumer<List<Uri>>() {
    @Override
    public void accept(List<Uri> uris) throws Exception {
      doStuffWithPickedUris(uris);
}, new Consumer<Throwable>() {
    @Override
    public void accept(Throwable throwable) throws Exception {
      Toast.makeText(SomeActivity.this, throwable.getMessage(), Toast.LENGTH_LONG).show();
    }
});
```
