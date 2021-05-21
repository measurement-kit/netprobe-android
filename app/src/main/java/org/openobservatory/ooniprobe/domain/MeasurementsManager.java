package org.openobservatory.ooniprobe.domain;

import android.content.Context;

import androidx.annotation.Nullable;

import com.raizlabs.android.dbflow.sql.language.SQLite;

import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.openobservatory.ooniprobe.client.OONIAPIClient;
import org.openobservatory.ooniprobe.client.callback.CheckReportIdCallback;
import org.openobservatory.ooniprobe.common.JsonPrinter;
import org.openobservatory.ooniprobe.domain.callback.DomainCallback;
import org.openobservatory.ooniprobe.domain.callback.GetMeasurementsCallback;
import org.openobservatory.ooniprobe.model.api.ApiMeasurement;
import org.openobservatory.ooniprobe.model.database.Measurement;
import org.openobservatory.ooniprobe.model.database.Measurement_Table;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.inject.Inject;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MeasurementsManager {

    private final Context context;
    private final JsonPrinter jsonPrinter;
    private final OONIAPIClient apiClient;
    private final OkHttpClient httpClient;

    @Inject
    MeasurementsManager(Context context, JsonPrinter jsonPrinter, OONIAPIClient apiClient, OkHttpClient httpClient) {
        this.context = context;
        this.apiClient = apiClient;
        this.httpClient = httpClient;
        this.jsonPrinter = jsonPrinter;
    }

    public Measurement get(int measurementId) {
        return SQLite.select().from(Measurement.class)
                .where(Measurement_Table.id.eq(measurementId)).querySingle();
    }

    public boolean hasUploadables() {
        return Measurement.hasReport(context, Measurement.selectUploadable());
    }

    public boolean canUpload(Measurement measurement) {
        return (!measurement.is_failed
                && (!measurement.is_uploaded || measurement.report_id == null)
                && measurement.hasReportFile(context));
    }

    public String getExplorerUrl(Measurement measurement) {
        String url = "https://explorer.ooni.io/measurement/" + measurement.report_id;
        if (measurement.test_name.equals("web_connectivity"))
            url = url + "?input=" + measurement.url.url;
        return url;
    }

    public void checkReportAndDeleteIt(Measurement measurement, @Nullable CheckReportIdCallback checkReportIdCallback) {
        if (!measurement.hasReportFile(context)) {
            return;
        }

        apiClient.checkReportId(measurement.report_id).enqueue(new CheckReportIdCallback() {
            @Override
            public void onSuccess(Boolean found) {
                if (found) {
                    Measurement.deleteMeasurementWithReportId(context, measurement.report_id);
                }
                if (checkReportIdCallback != null) {
                    checkReportIdCallback.onSuccess(found);
                }
            }

            @Override
            public void onError(String msg) {
                if (checkReportIdCallback != null) {
                    checkReportIdCallback.onError(msg);
                }
            }
        });
    }

    public boolean hasReportId(Measurement measurement) {
        return measurement.report_id != null && !measurement.report_id.isEmpty();
    }

    public String getReadableLog(Measurement measurement) throws IOException {
        File logFile = Measurement.getLogFile(context, measurement.result.id, measurement.test_name);
        return FileUtils.readFileToString(logFile, StandardCharsets.UTF_8);
    }

    public String getReadableEntry(Measurement measurement) throws IOException {
        File entryFile = Measurement.getEntryFile(context, measurement.id, measurement.test_name);
        return jsonPrinter.prettyText(FileUtils.readFileToString(entryFile, StandardCharsets.UTF_8));
    }

    public void downloadReport(Measurement measurement, DomainCallback<String> callback) {
        apiClient.getMeasurement(measurement.report_id, measurement.getUrlString()).enqueue(new GetMeasurementsCallback() {
            @Override
            public void onSuccess(ApiMeasurement.Result result) {
                downloadMeasurement(result, callback);
            }

            @Override
            public void onError(String msg) {
                callback.onError(msg);
            }
        });
    }

    private void downloadMeasurement(ApiMeasurement.Result result, DomainCallback<String> callback) {
        httpClient.newCall(new Request.Builder().url(result.measurement_url).build()).enqueue(new Callback() {
            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) {
                try {
                    callback.onSuccess(jsonPrinter.prettyText(response.body().string()));
                } catch (Exception e) {
                    callback.onError(e.getMessage());
                }
            }

            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                callback.onError(e.getLocalizedMessage());
            }
        });
    }

}
