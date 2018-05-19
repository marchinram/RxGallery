package com.marchinram.rxgallery;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.reactivex.Maybe;
import io.reactivex.MaybeEmitter;
import io.reactivex.MaybeOnSubscribe;
import io.reactivex.android.MainThreadDisposable;
import io.reactivex.functions.Function;

public final class RxGallery {

    public enum Source {
        GALLERY,
        PHOTO_CAPTURE,
        VIDEO_CAPTURE
    }

    public enum MimeType {
        IMAGE("image/*"),
        VIDEO("video/*"),
        AUDIO("audio/*");

        private final String mimeTypeString;

        MimeType(String mimeTypeString) {
            this.mimeTypeString = mimeTypeString;
        }

        @Override
        public String toString() {
            return mimeTypeString;
        }
    }

    /**
     * Returns a Maybe for a gallery display.
     *
     * @param activity An Activity to open gallery from.
     * @return A Maybe which calls onSuccess with the Uris of selected gallery items.
     */
    public static Maybe<List<Uri>> gallery(@NonNull Activity activity) {
        return gallery(activity, false);
    }

    /**
     * Returns a Maybe for a gallery display.
     *
     * @param activity           An Activity to open gallery from.
     * @param multiSelectEnabled Whether multiple items can be selected.
     *                           API levels < JELLY_BEAN_MR2 (18) do not support selecting multiple
     *                           items so this value is ignored on those devices.
     * @return A Maybe which calls onSuccess with the Uris of selected gallery items.
     */
    public static Maybe<List<Uri>> gallery(@NonNull Activity activity, boolean multiSelectEnabled) {
        return gallery(activity, multiSelectEnabled, MimeType.IMAGE);
    }

    /**
     * Returns a Maybe for a gallery display.
     *
     * @param activity           An Activity to open gallery from.
     * @param multiSelectEnabled Whether multiple items can be selected.
     *                           API levels < JELLY_BEAN_MR2 (18) do not support selecting multiple
     *                           items so this value is ignored on those devices.
     * @param mimeTypes          Mime types to show for a gallery.
     *                           API levels < KITKAT (19) only allow 1 mime type
     *                           so the remaining types provided are ignored on those devices.
     * @return A Maybe which calls onSuccess with the Uris of selected gallery items.
     */
    public static Maybe<List<Uri>> gallery(@NonNull Activity activity, boolean multiSelectEnabled, @Nullable MimeType... mimeTypes) {
        Request request = new Request.Builder()
                .setSource(Source.GALLERY)
                .setMultiSelectEnabled(multiSelectEnabled)
                .setMimeTypes(mimeTypes)
                .build();
        return request(activity, request);
    }

    /**
     * Returns a Maybe for a photo capture.
     *
     * @param activity An Activity to open photo capture from.
     * @return A Maybe which calls onSuccess with the Uri of captured photo.
     */
    public static Maybe<Uri> photoCapture(@NonNull Activity activity) {
        return photoCapture(activity, null);
    }

    /**
     * Returns a Maybe for a photo capture.
     *
     * @param activity  An Activity to open photo capture from.
     * @param outputUri Uri to output to for photo requests.
     *                  If none is supplied then will output to MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
     *                  which requires WRITE_EXTERNAL_STORAGE permission.
     * @return A Maybe which calls onSuccess with the Uri of captured photo.
     */
    public static Maybe<Uri> photoCapture(@NonNull Activity activity, @Nullable Uri outputUri) {
        Request request = new Request.Builder()
                .setSource(Source.PHOTO_CAPTURE)
                .setOutputUri(outputUri)
                .build();
        return request(activity, request).map(new Function<List<Uri>, Uri>() {
            @Override
            public Uri apply(@io.reactivex.annotations.NonNull List<Uri> uris) throws Exception {
                return uris.get(0);
            }
        });
    }

    /**
     * Returns a Maybe for a video capture.
     *
     * @param activity An Activity to open video capture from.
     * @return A Maybe which calls onSuccess with the Uri of captured video.
     */
    public static Maybe<Uri> videoCapture(@NonNull Activity activity) {
        Request request = new Request.Builder()
                .setSource(Source.VIDEO_CAPTURE)
                .build();
        return request(activity, request).map(new Function<List<Uri>, Uri>() {
            @Override
            public Uri apply(@io.reactivex.annotations.NonNull List<Uri> uris) throws Exception {
                return uris.get(0);
            }
        });
    }

    /**
     * Returns a Maybe for a gallery request.
     *
     * @param context A Context to open gallery or take photo/videos from.
     * @param request  A Request to use.
     * @return A Maybe which calls onSuccess with the Uris of selected gallery items
     * or captured photos/videos.
     */
    public static Maybe<List<Uri>> request(@NonNull final Context context, @NonNull final Request request) {
        final Context appContext = context.getApplicationContext();

        return Maybe.create(new MaybeOnSubscribe<List<Uri>>() {
            @Override
            public void subscribe(@io.reactivex.annotations.NonNull final MaybeEmitter<List<Uri>> e) throws Exception {
                final BroadcastReceiver receiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        if (!e.isDisposed()) {
                            if (intent.hasExtra(RxGalleryActivity.EXTRA_ERROR_NO_ACTIVITY)) {
                                e.onError(new ActivityNotFoundException("No activity found to handle request"));
                            } else if (intent.hasExtra(RxGalleryActivity.EXTRA_ERROR_SECURITY)) {
                                e.onError((Throwable) intent.getSerializableExtra(RxGalleryActivity.EXTRA_ERROR_SECURITY));
                            } else if (intent.hasExtra(RxGalleryActivity.EXTRA_URIS)) {
                                List<Uri> uris = intent.getParcelableArrayListExtra(RxGalleryActivity.EXTRA_URIS);
                                if (uris != null && uris.size() > 0) {
                                    e.onSuccess(uris);
                                } else {
                                    e.onComplete();
                                }
                            }
                        }
                    }
                };

                IntentFilter intentFilter = new IntentFilter(RxGalleryActivity.FINISHED_ACTION);
                appContext.registerReceiver(receiver, intentFilter);

                e.setDisposable(new MainThreadDisposable() {
                    @Override
                    protected void onDispose() {
                        appContext.unregisterReceiver(receiver);
                        appContext.sendBroadcast(new Intent(RxGalleryActivity.DISPOSED_ACTION));
                    }
                });

                Intent intent = new Intent(appContext, RxGalleryActivity.class);
                intent.putExtra(RxGalleryActivity.EXTRA_REQUEST, request);
                context.startActivity(intent);
            }
        });
    }

    public static final class Request implements Parcelable {

        private final Source source;

        private final List<MimeType> mimeTypes;

        private final boolean multiSelectEnabled;

        private final Uri outputUri;

        private Request(Source source, List<MimeType> mimeTypes, boolean multiSelectEnabled, Uri outputUri) {
            this.source = source;
            this.mimeTypes = mimeTypes;
            this.multiSelectEnabled = multiSelectEnabled;
            this.outputUri = outputUri;
        }

        private Request(Parcel in) {
            source = Source.values()[in.readInt()];
            mimeTypes = new ArrayList<>();
            int mimeTypesSize = in.readInt();
            for (int i = 0; i < mimeTypesSize; i++) {
                mimeTypes.add(MimeType.values()[in.readInt()]);
            }
            multiSelectEnabled = in.readInt() == 1;
            String uriString = in.readString();
            outputUri = uriString != null ? Uri.parse(uriString) : null;
        }

        Source getSource() {
            return source;
        }

        List<MimeType> getMimeTypes() {
            return mimeTypes;
        }

        boolean isMultiSelectEnabled() {
            return multiSelectEnabled;
        }

        Uri getOutputUri() {
            return outputUri;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(source.ordinal());
            dest.writeInt(mimeTypes.size());
            for (MimeType mimeType : mimeTypes) {
                dest.writeInt(mimeType.ordinal());
            }
            dest.writeInt(multiSelectEnabled ? 1 : 0);
            dest.writeString(outputUri != null ? outputUri.toString() : null);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public static final Parcelable.Creator<Request> CREATOR = new Parcelable.Creator<Request>() {
            public Request createFromParcel(Parcel in) {
                return new Request(in);
            }

            public Request[] newArray(int size) {
                return new Request[size];
            }
        };

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            if (o instanceof Request) {
                Request that = (Request) o;
                return (getSource().equals(that.getSource()))
                        && (getMimeTypes().equals(that.getMimeTypes()))
                        && (isMultiSelectEnabled() == that.isMultiSelectEnabled())
                        && ((getOutputUri() == null) ? (that.getOutputUri() == null) : getOutputUri().equals(that.getOutputUri()));
            }
            return false;
        }

        @Override
        public int hashCode() {
            int h = 1;
            h *= 1000003;
            h ^= getSource().hashCode();
            h *= 1000003;
            h ^= getMimeTypes().hashCode();
            h *= 1000003;
            h ^= isMultiSelectEnabled() ? 1 : 0;
            h *= 1000003;
            h ^= (getOutputUri() == null) ? 0 : getOutputUri().hashCode();
            return h;
        }

        public static final class Builder {

            private Source source = Source.GALLERY;

            private List<MimeType> mimeTypes = new ArrayList<>();

            private boolean multiSelectEnabled;

            private Uri outputUri;

            /**
             * Creates a {@link Builder} for a {@link Request}.
             */
            public Builder() {
                mimeTypes.add(MimeType.IMAGE);
            }

            /**
             * Sets the {@link Source} for the {@link Request}.
             *
             * @return This {@link Builder} object to allow for chaining of calls.
             */
            public Builder setSource(@NonNull Source source) {
                this.source = source;
                return this;
            }

            /**
             * Sets the mime types to show for a gallery request.
             * <p>
             * API levels < KITKAT (19) only allow 1 mime type
             * so the remaining types provided are ignored on those devices.
             *
             * @return This Builder object to allow for chaining of calls.
             */
            public Builder setMimeTypes(@Nullable MimeType... mimeTypes) {
                if (mimeTypes == null || mimeTypes.length == 0) {
                    return this;
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    this.mimeTypes = new ArrayList<>();
                    for (MimeType mimeType : mimeTypes) {
                        if (!this.mimeTypes.contains(mimeType)) {
                            this.mimeTypes.add(mimeType);
                        }
                    }
                } else {
                    this.mimeTypes = Collections.singletonList(mimeTypes[0]);
                }
                return this;
            }

            /**
             * Sets whether multiple items can be selected for a gallery request.
             * <p>
             * API levels < JELLY_BEAN_MR2 (18) do not support selecting multiple items
             * so this value is ignored on those devices.
             *
             * @return This Builder object to allow for chaining of calls.
             */
            public Builder setMultiSelectEnabled(boolean multiSelectEnabled) {
                this.multiSelectEnabled = multiSelectEnabled;
                return this;
            }

            /**
             * Sets the Uri to output to for photo requests.
             * <p>
             * If none is supplied then will output to MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
             * which requires WRITE_EXTERNAL_STORAGE permission.
             *
             * @return This Builder object to allow for chaining of calls.
             */
            public Builder setOutputUri(Uri outputUri) {
                this.outputUri = outputUri;
                return this;
            }

            /**
             * Creates a Request with the arguments supplied to this builder.
             */
            public Request build() {
                return new Request(source, mimeTypes, multiSelectEnabled, outputUri);
            }

        }

    }

}
