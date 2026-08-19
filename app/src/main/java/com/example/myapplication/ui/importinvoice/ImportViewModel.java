package com.example.myapplication.ui.importinvoice;

import android.app.Application;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.myapplication.AppContainer;
import com.example.myapplication.AutoCareApp;
import com.example.myapplication.domain.document.AnalysisException;
import com.example.myapplication.domain.document.DocumentAnalyzer;
import com.example.myapplication.domain.document.ExtractedInvoice;
import com.example.myapplication.util.AppExecutors;

/** Roda a extração fora da main thread e devolve o resultado para revisão. */
public class ImportViewModel extends AndroidViewModel {

    private final AppContainer container;

    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<ExtractedInvoice> result = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();

    public ImportViewModel(@NonNull Application application) {
        super(application);
        container = AutoCareApp.container(application);
    }

    public LiveData<Boolean> loading() {
        return loading;
    }

    public LiveData<ExtractedInvoice> result() {
        return result;
    }

    public LiveData<String> error() {
        return error;
    }

    public boolean isOcrAvailable() {
        return container.analyzerFactory.isOcrAvailable();
    }

    public void analyze(final Uri uri, final String documentType) {
        loading.setValue(true);
        AppExecutors.get().computation().execute(new Runnable() {
            @Override
            public void run() {
                DocumentAnalyzer analyzer = container.analyzerFactory.create(documentType);
                try {
                    ExtractedInvoice invoice = analyzer.analyze(uri);
                    loading.postValue(false);
                    result.postValue(invoice);
                } catch (AnalysisException failure) {
                    loading.postValue(false);
                    error.postValue(failure.getMessage());
                } catch (RuntimeException failure) {
                    loading.postValue(false);
                    error.postValue("Não foi possível ler o documento: " + failure.getMessage());
                }
            }
        });
    }

    public void consumeResult() {
        result.setValue(null);
    }
}
