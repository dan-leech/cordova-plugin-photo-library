package com.danleech.cordova.plugin.imagePicker.features;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.danleech.cordova.plugin.imagePicker.FakeR;
import com.danleech.cordova.plugin.imagePicker.features.camera.CameraModule;
import com.danleech.cordova.plugin.imagePicker.features.camera.ImageCameraModule;
import com.danleech.cordova.plugin.imagePicker.features.camera.VideoCameraModule;
import com.danleech.cordova.plugin.imagePicker.features.common.BaseConfig;
import com.danleech.cordova.plugin.imagePicker.features.common.BasePresenter;
import com.danleech.cordova.plugin.imagePicker.features.common.ImageLoaderListener;
import com.danleech.cordova.plugin.imagePicker.helper.ConfigUtils;
import com.danleech.cordova.plugin.imagePicker.model.Folder;
import com.danleech.cordova.plugin.imagePicker.model.Image;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ImagePickerPresenter extends BasePresenter<ImagePickerView> {

    private ImageFileLoader imageLoader;
    private CameraModule cameraModule;
    private Handler main = new Handler(Looper.getMainLooper());

    ImagePickerPresenter(ImageFileLoader imageLoader) {
        this.imageLoader = imageLoader;
    }

    CameraModule getCameraModule(boolean isVideo) {
        if (cameraModule == null) {
            if (isVideo)
                cameraModule = new VideoCameraModule();
            else
                cameraModule = new ImageCameraModule();
        }
        return cameraModule;
    }

    /* Set the camera module in onRestoreInstance */
    void setCameraModule(CameraModule cameraModule) {
        this.cameraModule = cameraModule;
    }

    void abortLoad() {
        imageLoader.abortLoadImages();
    }

    void loadImages(boolean onlyVideo, ArrayList<File> excludedImages) {
        if (!isViewAttached()) return;

        runOnUiIfAvailable(() -> getView().showLoading(true));

        imageLoader.loadDeviceImages(onlyVideo, excludedImages, new ImageLoaderListener() {
            @Override
            public void onImageLoaded(final List<Image> images, final List<Folder> folders) {
                runOnUiIfAvailable(() -> {
                    getView().showFetchCompleted(images, folders);

                    final boolean isEmpty = folders != null
                            ? folders.isEmpty()
                            : images.isEmpty();

                    if (isEmpty) {
                        getView().showEmpty();
                    } else {
                        getView().showLoading(false);
                    }
                });
            }

            @Override
            public void onFailed(final Throwable throwable) {
                runOnUiIfAvailable(() -> getView().showError(throwable));
            }
        });
    }

    void onDoneSelectImages(List<Image> selectedImages) {
        if (selectedImages != null && selectedImages.size() > 0) {

            /* Scan selected images which not existed */
            for (int i = 0; i < selectedImages.size(); i++) {
                Image image = selectedImages.get(i);
                File file = new File(image.getPath());
                if (!file.exists()) {
                    selectedImages.remove(i);
                    i--;
                }
            }
            getView().finishPickImages(selectedImages);
        }
    }

    void captureImage(Activity activity, BaseConfig config, int requestCode) {
        Context context = activity.getApplicationContext();
        Intent intent = getCameraModule(false).getCameraIntent(activity, config);
        if (intent == null) {
            Toast.makeText(context, context.getString(FakeR.staticGetId("string", "ip_error_create_image_file")), Toast.LENGTH_LONG).show();
            return;
        }
        activity.startActivityForResult(intent, requestCode);
    }

    void finishCaptureImage(Context context, Intent data, final BaseConfig config) {
        getCameraModule(false).getData(context, data, images -> {
            if (ConfigUtils.shouldReturn(config, true)) {
                getView().finishPickImages(images);
            } else {
                getView().showCapturedImage();
            }
        });
    }

    void abortCaptureImage() {
        getCameraModule(false).removeData();
    }

    void captureVideo(Activity activity, BaseConfig config, int requestCode) {
        Context context = activity.getApplicationContext();
        Intent intent = getCameraModule(true).getCameraIntent(activity, config);
        if (intent == null) {
            Toast.makeText(context, context.getString(FakeR.staticGetId("string", "ip_error_create_image_file")), Toast.LENGTH_LONG).show();
            return;
        }
        activity.startActivityForResult(intent, requestCode);
    }

    void finishCaptureVideo(Context context, Intent data, final BaseConfig config) {
        getCameraModule(true).getData(context, data, images -> {
            if (ConfigUtils.shouldReturn(config, true)) {
                getView().finishPickImages(images);
            } else {
                getView().showCapturedImage();
            }
        });
    }

    void abortCaptureVideo() {
        getCameraModule(true).removeData();
    }

    private void runOnUiIfAvailable(Runnable runnable) {
        main.post(() -> {
            if (isViewAttached()) {
                runnable.run();
            }
        });
    }
}
