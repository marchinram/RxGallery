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
import android.support.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.MainThreadDisposable;

public final class RxGallery {

    public enum Source {
        GALLERY,
        TAKE_PHOTO,
        TAKE_VIDEO
    }

    public enum MimeType {
        IMAGE("image/*"),
        VIDEO("video/*"),
        AUDIO("audio/*");

        private String mimeTypeString;

        MimeType(String mimeTypeString) {
            this.mimeTypeString = mimeTypeString;
        }

        @Override
        public String toString() {
            return mimeTypeString;
        }
    }

    public static Observable<List<Uri>> request(@NonNull final Context context, @NonNull final Request request) {
        return Observable.create(new ObservableOnSubscribe<List<Uri>>() {
            @Override
            public void subscribe(@io.reactivex.annotations.NonNull final ObservableEmitter<List<Uri>> e) throws Exception {
                final BroadcastReceiver urisReceiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        if (!e.isDisposed()) {
                            if (intent.hasExtra(RxGalleryActivity.EXTRA_ERROR_NO_ACTIVITY)) {
                                e.onError(new ActivityNotFoundException("No activity found to handle request"));
                            } else if (intent.hasExtra(RxGalleryActivity.EXTRA_URIS)) {
                                List<Uri> uris = intent.getParcelableArrayListExtra(RxGalleryActivity.EXTRA_URIS);
                                if (uris == null) {
                                    uris = new ArrayList<>();
                                }
                                e.onNext(uris);
                            }
                            e.onComplete();
                        }
                    }
                };

                IntentFilter intentFilter = new IntentFilter(RxGalleryActivity.FINISHED_ACTION);
                context.registerReceiver(urisReceiver, intentFilter);

                e.setDisposable(new MainThreadDisposable() {
                    @Override
                    protected void onDispose() {
                        context.unregisterReceiver(urisReceiver);
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

        private Request(Source source, List<MimeType> mimeTypes, boolean multiSelectEnabled) {
            this.source = source;
            this.mimeTypes = mimeTypes;
            this.multiSelectEnabled = multiSelectEnabled;
        }

        private Request(Parcel in) {
            source = Source.values()[in.readInt()];

            mimeTypes = new ArrayList<>();
            int mimeTypesSize = in.readInt();
            for (int i = 0; i < mimeTypesSize; i++) {
                mimeTypes.add(MimeType.values()[in.readInt()]);
            }

            multiSelectEnabled = in.readInt() == 1;
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

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(source.ordinal());
            dest.writeInt(mimeTypes.size());
            for (MimeType mimeType : mimeTypes) {
                dest.writeInt(mimeType.ordinal());
            }
            dest.writeInt(multiSelectEnabled ? 1 : 0);
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

            public Builder() {
                mimeTypes.add(MimeType.IMAGE);
            }

            public Builder setSource(@NonNull Source source) {
                this.source = source;
                return this;
            }

            public Builder setMimeType(@NonNull MimeType mimeType) {
                this.mimeTypes = Collections.singletonList(mimeType);
                return this;
            }

            @RequiresApi(Build.VERSION_CODES.KITKAT)
            public Builder setMimeTypes(@NonNull MimeType... mimeTypes) {
                if (mimeTypes.length == 0) {
                    return this;
                }

                this.mimeTypes = new ArrayList<>();
                for (MimeType mimeType : mimeTypes) {
                    if (!this.mimeTypes.contains(mimeType)) {
                        this.mimeTypes.add(mimeType);
                    }
                }
                return this;
            }

            @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
            public Builder setMultiSelectEnabled(boolean multiSelectEnabled) {
                this.multiSelectEnabled = multiSelectEnabled;
                return this;
            }

            public Request build() {
                return new Request(source, mimeTypes, multiSelectEnabled);
            }

        }

    }

}
