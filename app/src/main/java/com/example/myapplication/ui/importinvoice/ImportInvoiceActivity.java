package com.example.myapplication.ui.importinvoice;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.example.myapplication.R;
import com.example.myapplication.data.local.entity.Document;
import com.example.myapplication.domain.document.ExtractedInvoice;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.io.File;
import java.io.IOException;

/** "Importar nota fiscal": XML, PDF ou foto. */
public class ImportInvoiceActivity extends AppCompatActivity {

    public static final String EXTRA_ACTION = "action";
    public static final String ACTION_XML = "XML";
    public static final String ACTION_PDF = "PDF";
    public static final String ACTION_PHOTO = "PHOTO";

    private ImportViewModel viewModel;
    private CircularProgressIndicator progress;
    private TextView textStatus;

    private ActivityResultLauncher<String[]> openDocumentLauncher;
    private ActivityResultLauncher<Uri> takePictureLauncher;

    private String pendingType = Document.TYPE_XML;
    private Uri photoUri;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import_invoice);

        progress = findViewById(R.id.progress);
        textStatus = findViewById(R.id.textStatus);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(view -> finish());

        viewModel = new ViewModelProvider(this).get(ImportViewModel.class);

        registerLaunchers();

        findViewById(R.id.buttonXml).setOnClickListener(view -> pickXml());
        findViewById(R.id.buttonPdf).setOnClickListener(view -> pickPdf());
        findViewById(R.id.buttonPhoto).setOnClickListener(view -> takePhoto());

        viewModel.loading().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean loading) {
                boolean running = Boolean.TRUE.equals(loading);
                progress.setVisibility(running ? View.VISIBLE : View.GONE);
                if (running) {
                    textStatus.setText(R.string.import_processing);
                }
            }
        });
        viewModel.result().observe(this, new Observer<ExtractedInvoice>() {
            @Override
            public void onChanged(ExtractedInvoice invoice) {
                if (invoice == null) {
                    return;
                }
                viewModel.consumeResult();
                if (!Document.TYPE_XML.equals(invoice.documentType) && !viewModel.isOcrAvailable()) {
                    Toast.makeText(ImportInvoiceActivity.this, R.string.import_ocr_unavailable,
                            Toast.LENGTH_LONG).show();
                }
                Intent intent = new Intent(ImportInvoiceActivity.this,
                        ConfirmExtractionActivity.class);
                intent.putExtra(ConfirmExtractionActivity.EXTRA_INVOICE, invoice);
                startActivity(intent);
                finish();
            }
        });
        viewModel.error().observe(this, new Observer<String>() {
            @Override
            public void onChanged(String message) {
                if (message != null) {
                    textStatus.setText(message);
                }
            }
        });

        String action = getIntent().getStringExtra(EXTRA_ACTION);
        if (savedInstanceState == null && action != null) {
            if (ACTION_XML.equals(action)) {
                pickXml();
            } else if (ACTION_PDF.equals(action)) {
                pickPdf();
            } else if (ACTION_PHOTO.equals(action)) {
                takePhoto();
            }
        }
    }

    private void registerLaunchers() {
        openDocumentLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(), uri -> {
                    if (uri == null) {
                        return;
                    }
                    try {
                        getContentResolver().takePersistableUriPermission(uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    } catch (SecurityException ignored) {
                        // Nem todo provider concede permissão persistente; seguimos com a leitura imediata.
                    }
                    viewModel.analyze(uri, pendingType);
                });

        takePictureLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(), success -> {
                    if (Boolean.TRUE.equals(success) && photoUri != null) {
                        viewModel.analyze(photoUri, Document.TYPE_PHOTO);
                    }
                });
    }

    private void pickXml() {
        pendingType = Document.TYPE_XML;
        openDocumentLauncher.launch(new String[]{"text/xml", "application/xml", "*/*"});
    }

    private void pickPdf() {
        pendingType = Document.TYPE_PDF;
        openDocumentLauncher.launch(new String[]{"application/pdf"});
    }

    private void takePhoto() {
        try {
            File directory = new File(getFilesDir(), "documents");
            if (!directory.exists() && !directory.mkdirs()) {
                throw new IOException("Não foi possível criar a pasta de documentos");
            }
            File file = new File(directory, "nota_" + System.currentTimeMillis() + ".jpg");
            photoUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
            takePictureLauncher.launch(photoUri);
        } catch (IOException | IllegalArgumentException error) {
            textStatus.setText("Falha ao preparar a câmera: " + error.getMessage());
        }
    }
}
