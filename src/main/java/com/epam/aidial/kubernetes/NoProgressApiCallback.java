package com.epam.aidial.kubernetes;

import io.kubernetes.client.openapi.ApiCallback;

public interface NoProgressApiCallback<T> extends ApiCallback<T> {
    @Override
    default void onUploadProgress(long l, long l1, boolean b) {
    }

    @Override
    default void onDownloadProgress(long l, long l1, boolean b) {
    }
}
