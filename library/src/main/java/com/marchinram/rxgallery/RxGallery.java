package com.marchinram.rxgallery;

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.android.MainThreadDisposable;

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
     * @param context
     * @param request
     * @return
     */
    public static Observable<List<Uri>> requestObservable(@NonNull final Context context, @NonNull final Request request) {
        return requestSingle(context, request).toObservable();
    }

    /**
     * @param context
     * @param request
     * @return
     */
    public static Single<List<Uri>> requestSingle(@NonNull final Context context, @NonNull final Request request) {
        return Single.create(new SingleOnSubscribe<List<Uri>>() {
            @Override
            public void subscribe(@io.reactivex.annotations.NonNull final SingleEmitter<List<Uri>> e) throws Exception {
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
                                e.onSuccess(uris != null ? uris : new ArrayList<Uri>());
                            }
                        }
                    }
                };

                IntentFilter intentFilter = new IntentFilter(RxGalleryActivity.FINISHED_ACTION);
                context.registerReceiver(receiver, intentFilter);

                e.setDisposable(new MainThreadDisposable() {
                    @Override
                    protected void onDispose() {
                        context.unregisterReceiver(receiver);
                        context.sendBroadcast(new Intent(RxGalleryActivity.DISPOSED_ACTION));
                    }
                });

                Intent intent = new Intent(context, RxGalleryActivity.class);
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
                return (this.getSource().equals(that.getSource()))
                        && (this.getMimeTypes().equals(that.getMimeTypes()))
                        && (this.multiSelectEnabled == that.multiSelectEnabled);
            }
            return false;
        }

        @Override
        public int hashCode() {
            int h = 1;
            h *= 1000003;
            h ^= this.getSource().hashCode();
            h *= 1000003;
            h ^= this.getMimeTypes().hashCode();
            h *= 1000003;
            h ^= this.multiSelectEnabled ? 1 : 0;
            return h;
        }

        public static final class Builder {

            private Source source = Source.GALLERY;

            private List<MimeType> mimeTypes = new ArrayList<>();

            private boolean multiSelectEnabled;

            private Uri outputUri;

            /**
             *
             */
            public Builder() {
                mimeTypes.add(MimeType.IMAGE);
            }

            /**
             * @param source
             * @return
             */
            public Builder setSource(@NonNull Source source) {
                this.source = source;
                return this;
            }

            /**
             * @param mimeTypes
             * @return
             */
            public Builder setMimeTypes(@NonNull MimeType... mimeTypes) {
                if (mimeTypes.length == 0) {
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
             * @param multiSelectEnabled
             * @return
             */
            public Builder setMultiSelectEnabled(boolean multiSelectEnabled) {
                this.multiSelectEnabled = multiSelectEnabled;
                return this;
            }

            /**
             * @param outputUri
             */
            public Builder setOutputUri(Uri outputUri) {
                this.outputUri = outputUri;
                return this;
            }

            /**
             * @return
             */
            public Request build() {
                return new Request(source, mimeTypes, multiSelectEnabled, outputUri);
            }

        }

    }

}
