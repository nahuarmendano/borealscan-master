/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.scandit.datacapture.listbuildingsample;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.scandit.datacapture.barcode.capture.SymbologySettings;
import com.scandit.datacapture.barcode.data.Barcode;
import com.scandit.datacapture.barcode.data.Symbology;
import com.scandit.datacapture.barcode.spark.capture.SparkScan;
import com.scandit.datacapture.barcode.spark.capture.SparkScanListener;
import com.scandit.datacapture.barcode.spark.capture.SparkScanSession;
import com.scandit.datacapture.barcode.spark.capture.SparkScanSettings;
import com.scandit.datacapture.barcode.spark.feedback.SparkScanViewFeedback;
import com.scandit.datacapture.barcode.spark.ui.SparkScanCoordinatorLayout;
import com.scandit.datacapture.barcode.spark.ui.SparkScanScanningBehavior;
import com.scandit.datacapture.barcode.spark.ui.SparkScanView;
import com.scandit.datacapture.barcode.spark.ui.SparkScanViewSettings;
import com.scandit.datacapture.core.capture.DataCaptureContext;
import com.scandit.datacapture.core.common.geometry.Point;
import com.scandit.datacapture.core.data.FrameData;
import com.scandit.datacapture.core.time.TimeInterval;
import com.scandit.datacapture.listbuildingsample.data.ScanResult;
import com.scandit.datacapture.barcode.spark.capture.SparkScanSettings;


import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends CameraPermissionActivity implements SparkScanListener {

	//Acá debajo va la key de scandit
    public static final String SCANDIT_LICENSE_KEY = "AYjTKgwFKLhZGtmHmyNAawklGVUpLfmaJ2JN39hPFcbHRdb8Sh3UX45m7PRkJtORsQzsAeBZw7aAZ/VBZlp5ykVZZOOYUI8ZAxAsZ3tOrh5HXX2CzFyh2yNzGtUXQuR5eFHqhXNx8+mfbsvN2zErPt0+TW4TESKXSx4764U8HnIF/01crbTR4/qxeWvIgdmGJkoV2YZc4wfZjpQI2Uvd3/J2jFcv/WrVHgWZ/VAC2lHTzC3JdwtTNJKxxDpsqKp1sDlARxGjw4hlebrAUbft3aWMjbtpVn2T4D+tBN3GVuwlD9Uo7MN3Sto17fSVSD1JLymYPHP7zxsnByy9mCBhKqTf3YKCh8DughdNJpIIWaaoY6t6OTof+TxY25XAboYM1Ii3FdaK1MjK2x9bVujInqaIYzPRYRwQj6lPyVaYSiRRJTsR6l3RLXyorSeqM6Mjyspyb9Gl3ht1grXe8TzMwVUFLYwBlV1zYcKfCVxHIaPo8irO1X7+sImu0166pNeK962FxzUx+rJMsvEIhy8mzF//yRI8WBLZvuBS5AH8EJHBb5p6DcdLgNVf3AwQWw6S5ENIw1Nu+eS2p+nm7msRRWP5jbqo8TfwgoellmtHaljlvmQ47kXfZvo9feDd7qZtGvWuX22yZkb+3k0OEfNKZaBKLrfzKU6X5TlmMvyhU7mF6mMdkBwex+NuKhRl1fYVjzD1hk75j70/QgXyjMv9nJpSEIXEt//AVHZTG4lGvAT0l3hPOie/zS0ixEH11+LJvbzsZQXYngggsJ40oCbajRxnvrMEcJQ5Lcxnp/Ov8qTmApOqK+XmLAV/s+MdeeIatFNTk6o9xGar+cB8";
    private Set<String> scannedBarcodes = new HashSet<>();

    private static final float CROPPED_IMAGE_PADDING = 1.2f;

    private static final int SCALED_IMAGE_SIZE_IN_PIXELS = 100;

    private DataCaptureContext dataCaptureContext;

    private SparkScan sparkScan;

    private SparkScanView sparkScanView;

    private final ResultListAdapter resultListAdapter = new ResultListAdapter();

    private TextView resultCountTextView;

    private final ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicia sparkscan.
        initialize();

        //setup RecyclerView
        RecyclerView recyclerView = findViewById(R.id.result_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        DividerItemDecoration divider =
            new DividerItemDecoration(this, LinearLayoutManager.VERTICAL);
        divider.setDrawable(getDrawable(R.drawable.recycler_divider));
        recyclerView.addItemDecoration(divider);

        recyclerView.setAdapter(resultListAdapter);

        resultCountTextView = findViewById(R.id.item_count);
        setItemCount(0);

        // boton de vaciado de lista
        Button clearButton = findViewById(R.id.clear_list);
        clearButton.setOnClickListener(view -> {
            clearList();
        });

    }


    private void initialize() { //priv principal, descripciones por el equipo de scandit

        // Create data capture context using your license key.
        dataCaptureContext = DataCaptureContext.forLicenseKey("ARHjEiMeB6OLMLvdojI5H6A84Lr+Ff67JFHiVMFh3FVzdrw8jl4Dc19AtvYCXdJU00DUkVRbhiVGH7URTV7hn1Z5GLE5X46Q6R52btorL9znXy0D4U3odzxiqcHFWe+AOEX/QURKhRKKVOdkhk3P6TdcY3jMSDkUq2zz/4Fi4tUMaQ+YnlOdRCkqO2Rce97eOWU+DKletrXoYlkdMVQdkzJTf1D6Bx7Cbn7ZlypT6NfFJwnwlF8AmSpiujPXLHK7TnVQ9s9Y2ivKRGbIH24NYIxAG29catPvLUoCbXxDmxAFYL8XUXIsXOB6PtwqdUrfO0JZpYlOa9qNUFG9RlJLTRdv0Y4iZYnsnCz0FMN7RGfceuwHKGyWvmZNJw0/HaQGxWafjuNNL7NscgHUrX4pn1Fc1mxrJKKR22MdJU4QbVcAeFX8v26FV0NGx9U7cra88WIHKuFVqlDpOgD36nVcGA9Kc7XTbD7sTnE6tb5KRCF4VLUMAXK1h49Gj6vzL4b900/nt8NN4EMRSUBpQzzRmsl1qMoac12Mhi3GyZMmc1hrNltNSxi7JBVPSnexrM5ehGUgLIdwXvmEo8Dk4fkf7DukYQppDwQfCyQyt+RNr/vkdVQEnjDhAz4W5AnrFtONZO/hFRH9uDTHzzml/IF/LgcsHqrOUUt9h8bvCYayf6PQso7cwuXmwDRLgQElHqK1fZFN9Wu/wMF9eeE7bBt9csR5h8ghzTAce0l2ux6tMpI3hAJzbsb1hhS/C+/Yg0lB4XAiv8E1IF6rZk6My7TXv0TdBZ5IBiy1hJUxMPFgmJ+J3LwB9n8zWqPlWEBKUvHkxG98r38vssxanukeJkOKS66k/BZGwwo1AidwPkJz31dOCvNj6L4BvdGsHOGt/L/pIJa7SezLEpC9piRbdh6vyZkQVipGQC3vS5rTC2RaPajkkpVhungi0WYhCB+N5kaWsFbOBN7DeB3bLAZkstPc3p06VHdKD2b8R2Y9FT4CR6vQFn+r/HrHWk8+KVTfKfDAP1bIw1D7QLpx/Vcrp31FV1lStS97NMFxhYxrBLNNFKE0z9O7MY6/qx4hAxY6G38RyPUUttV8xlY9oQGVxhSAeJplrMQX6m5eNvN968naoJ7jOI+tV6V5ygRfKuxLZvS139d2deCJD0URO7o/fcPncTt8+yi2bsqVl24/pKeZnEUXrdiuhOiOR99+zljgVGhd+zut2en7yildefbYjh9DrGSroHf/C2I=");

        // The spark scan process is configured through SparkScan settings
        // which are then applied to the spark scan instance that manages the spark scan.
        SparkScanSettings sparkScanSettings = new SparkScanSettings();

        // The settings instance initially has all types of barcodes (symbologies) disabled.
        // For the purpose of this sample we enable a very generous set of symbologies.
        // In your own app ensure that you only enable the symbologies that your app requires
        // as every additional enabled symbology has an impact on processing times.
        HashSet<Symbology> symbologies = new HashSet<>();
        symbologies.add(Symbology.QR);

        sparkScanSettings.enableSymbologies(symbologies);

        // Some linear/1d barcode symbologies allow you to encode variable-length data.
        // By default, the Scandit Data Capture SDK only scans barcodes in a certain length range.
        // If your application requires scanning of one of these symbologies, and the length is
        // falling outside the default range, you may need to adjust the "active symbol counts"
        // for this symbology. This is shown in the following few lines of code for one of the
        // variable-length symbologies.
        SymbologySettings symbologySettings =
            sparkScanSettings.getSymbologySettings(Symbology.QR);

        HashSet<Short> activeSymbolCounts = new HashSet<>(
            Arrays.asList(new Short[]{7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20}));

        symbologySettings.setActiveSymbolCounts(activeSymbolCounts);

        // Create the spark scan instance.
        // Spark scan will automatically apply and maintain the optimal camera settings.
        sparkScan = new SparkScan(sparkScanSettings);

        // Register self as a listener to get informed of tracked barcodes.
        sparkScan.addListener(this);

        // The SparkScanCoordinatorLayout container will make sure that the main layout of the view
        // will not break when the SparkScanView will be attached.
        // When creating the SparkScanView instance use the SparkScanCoordinatorLayout
        // as a parent view.
        SparkScanCoordinatorLayout container = findViewById(R.id.spark_scan_coordinator);

        // You can customize the SparkScanView using SparkScanViewSettings.
        SparkScanViewSettings settings = new SparkScanViewSettings();

        // Creating the instance of SparkScanView. The instance will be automatically added
        // to the container.
        sparkScanView =
            SparkScanView.newInstance(container, dataCaptureContext, sparkScan, settings);

    }

    private void clearList() {
        resultListAdapter.clearResults();
        setItemCount(0);
    }

    @Override
    protected void onPause() {
        sparkScanView.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        sparkScan.removeListener(this);
        backgroundExecutor.shutdown();
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        sparkScanView.onResume();

        // revisa los permisos de camara y los solicita si aún no fueron otorgados.
        requestCameraPermission();
    }

    private boolean isValidBarcode(Barcode barcode) {
        return barcode.getData() != null && !barcode.getData().equals("123456789") && !barcode.getData().equals(resultCountTextView.getContentDescription());
    }

    private void validBarcodeScanned(Barcode barcode, FrameData data) {
        // feedback a través de sonido y vibracion
        sparkScanView.emitFeedback(new SparkScanViewFeedback.Success());

        if (data != null) {
            data.retain();

            // cropea y guarda la imagen de los QR en la memoria
            backgroundExecutor.execute(() -> {
                Bitmap image = cropBarcode(barcode, data.getImageBuffer().toBitmap());
                ScanResult result =
                        new ScanResult(barcode.getData(), barcode.getSymbology().name(), image);
                postResult(result);
                data.release();
            });
        }
    }

    private void postResult(ScanResult result) {
        runOnUiThread(() -> {
            resultListAdapter.addResult(result);
            setItemCount(resultListAdapter.getItemCount());
        });
    }

    private Bitmap cropBarcode(Barcode barcode, Bitmap frame) {
        // revisa que la imagen no sea muy pequeña
        if (frame.getWidth() == 1 && frame.getHeight() == 1) return frame;

        List<Point> points = Arrays.asList(
            barcode.getLocation().getBottomLeft(),
            barcode.getLocation().getTopLeft(),
            barcode.getLocation().getTopRight(),
            barcode.getLocation().getBottomRight()
        );

        float minX = points.get(0).getX();
        float minY = points.get(0).getY();
        float maxX = points.get(0).getX();
        float maxY = points.get(0).getY();

        for (Point point : points) {
            if (point.getX() < minX) {
                minX = point.getX();
            }

            if (point.getX() > maxX) {
                maxX = point.getX();
            }

            if (point.getY() < minY) {
                minY = point.getY();
            }

            if (point.getY() > maxY) {
                maxY = point.getY();
            }
        }

        PointF center = new PointF(((minX + maxX) * 0.5f), ((minY + maxY) * 0.5f));
        float largerSize = Math.max((maxY - minY), (maxX - minX));

        int height = (int) (largerSize * CROPPED_IMAGE_PADDING);
        int width = (int) (largerSize * CROPPED_IMAGE_PADDING);

        int x = Math.max((int) (center.x - largerSize * (CROPPED_IMAGE_PADDING / 2)), 0);
        int y = Math.max((int) (center.y - largerSize * (CROPPED_IMAGE_PADDING / 2)), 0);

        if ((y + height) > frame.getHeight()) {
            int difference = ((y + height) - frame.getHeight());

            if (y - difference < 0) {
                height -= difference;
                width -= difference;
            } else {
                y -= difference;
            }
        }

        if ((x + width) > frame.getWidth()) {
            int difference = ((x + width) - frame.getWidth());
            if (x - difference < 0) {
                width -= difference;
                height -= difference;
            } else {
                x -= difference;
            }
        }

        if (width <= 0 || height <= 0) { // safety check
            return frame;
        }

        float scaleFactor = getScaleFactor(width, height, SCALED_IMAGE_SIZE_IN_PIXELS);

        Matrix matrix = new Matrix();
        matrix.postRotate(90f);
        matrix.postScale(scaleFactor, scaleFactor);

        return Bitmap.createBitmap(frame, x, y, width, height, matrix, true);
    }

    private float getScaleFactor(int bitmapWidth, int bitmapHeight, int targetDimension) {
        float smallestDimension = (float) Math.min(bitmapWidth, bitmapHeight);
        return targetDimension / smallestDimension;
    }

    private void setItemCount(int count) {
        resultCountTextView.setText(
            getResources().getQuantityString(R.plurals.results_amount, count, count));
    }

    private void invalidBarcodeScanned() {
        // emite sonido y vibracion
        sparkScanView.emitFeedback(
            new SparkScanViewFeedback.Error("Este código ya fue escaneado",
                TimeInterval.seconds(1f)));
    }

    @Override
    public void onBarcodeScanned(
            @NonNull SparkScan sparkScan, @NonNull SparkScanSession session, @Nullable FrameData data
    ) {
        List<Barcode> barcodes = session.getNewlyRecognizedBarcodes();
        Barcode barcode = barcodes.get(0);

        String barcodeData = barcode.getData();

        if (!scannedBarcodes.contains(barcodeData) && isValidBarcode(barcode)) {
            scannedBarcodes.add(barcodeData);
            validBarcodeScanned(barcode, data);
        } else {
            invalidBarcodeScanned();
        }
    }

    @Override
    public void onSessionUpdated(
        @NonNull SparkScan sparkScan, @NonNull SparkScanSession session, @Nullable FrameData data
    ) {
        // ¿¿
    }
}
