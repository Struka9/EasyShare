/*
 * Copyright (C) The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.apps.xtrange.easyshare.vision;

import com.apps.xtrange.easyshare.vision.ui.camera.GraphicOverlay;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.barcode.Barcode;

/**
 * Factory for creating a tracker and associated graphic to be associated with a new barcode.  The
 * multi-processor uses this factory to create barcode trackers as needed -- one for each barcode.
 */
public class BarcodeTrackerFactory implements MultiProcessor.Factory<Barcode> {
    private GraphicOverlay mGraphicOverlay;
    private OnBarcodeReceivedListener mListener;

    public BarcodeTrackerFactory(GraphicOverlay graphicOverlay, OnBarcodeReceivedListener listener) {
        mGraphicOverlay = graphicOverlay;
        mListener = listener;
    }

    @Override
    public Tracker<Barcode> create(Barcode barcode) {
        BarcodeGraphic graphic = new BarcodeGraphic(mGraphicOverlay);
        if (mListener != null)
            mListener.onBarcodeReceived(barcode);

        return new GraphicTracker<>(mGraphicOverlay, graphic);
    }

    public interface OnBarcodeReceivedListener {
        public void onBarcodeReceived(Barcode barcode);
    }
}

