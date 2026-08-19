package com.example.myapplication.domain.document;

import android.content.Context;
import android.net.Uri;
import android.util.Xml;

import com.example.myapplication.data.local.entity.Document;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Parser de XML de NFe. Ignora namespaces e lê apenas as tags necessárias,
 * o que funciona tanto para nfeProc quanto para NFe "cru".
 */
public class NfeXmlDocumentAnalyzer implements DocumentAnalyzer {

    private final Context context;
    private final MaintenanceInterpreter interpreter;

    public NfeXmlDocumentAnalyzer(Context context, MaintenanceInterpreter interpreter) {
        this.context = context.getApplicationContext();
        this.interpreter = interpreter;
    }

    @Override
    public ExtractedInvoice analyze(Uri uri) throws AnalysisException {
        ExtractedInvoice invoice = new ExtractedInvoice();
        invoice.documentType = Document.TYPE_XML;
        invoice.uri = uri.toString();

        InputStream input = null;
        try {
            input = context.getContentResolver().openInputStream(uri);
            if (input == null) {
                throw new AnalysisException("Não foi possível abrir o arquivo selecionado.");
            }
            parse(input, invoice);
        } catch (IOException error) {
            throw new AnalysisException("Falha ao ler o XML: " + error.getMessage(), error);
        } catch (XmlPullParserException error) {
            throw new AnalysisException("XML inválido ou fora do padrão NFe.", error);
        } finally {
            closeQuietly(input);
        }

        if (invoice.products.isEmpty() && invoice.totalValue == 0d) {
            throw new AnalysisException("Não encontramos dados de nota fiscal neste XML.");
        }
        interpreter.interpret(invoice);
        return invoice;
    }

    private void parse(InputStream input, ExtractedInvoice invoice)
            throws XmlPullParserException, IOException {
        XmlPullParser parser = Xml.newPullParser();
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
        parser.setInput(input, null);

        boolean insideEmit = false;
        boolean insideDest = false;
        boolean insideProd = false;
        boolean insideTotal = false;
        ExtractedProduct product = null;
        StringBuilder rawText = new StringBuilder();

        int event = parser.getEventType();
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG) {
                String tag = localName(parser.getName());
                switch (tag) {
                    case "emit":
                        insideEmit = true;
                        break;
                    case "dest":
                        insideDest = true;
                        break;
                    case "total":
                        insideTotal = true;
                        break;
                    case "prod":
                        insideProd = true;
                        product = new ExtractedProduct();
                        break;
                    case "nNF":
                        invoice.invoiceNumber = text(parser);
                        break;
                    case "dhEmi":
                    case "dEmi":
                        invoice.issueDate = parseDate(text(parser));
                        break;
                    case "CNPJ":
                        if (insideEmit && invoice.cnpj == null) {
                            invoice.cnpj = text(parser);
                        }
                        break;
                    case "xNome":
                        if (insideEmit && invoice.companyName == null) {
                            invoice.companyName = text(parser);
                        } else if (!insideDest && invoice.companyName == null) {
                            invoice.companyName = text(parser);
                        }
                        break;
                    case "xProd":
                        if (insideProd && product != null) {
                            product.name = text(parser);
                            rawText.append(product.name).append('\n');
                        }
                        break;
                    case "qCom":
                        if (insideProd && product != null) {
                            product.quantity = parseDouble(text(parser), 1d);
                        }
                        break;
                    case "vUnCom":
                        if (insideProd && product != null) {
                            product.unitPrice = parseDouble(text(parser), 0d);
                        }
                        break;
                    case "vProd":
                        if (insideProd && product != null) {
                            product.totalPrice = parseDouble(text(parser), 0d);
                        }
                        break;
                    case "vNF":
                        if (insideTotal) {
                            invoice.totalValue = parseDouble(text(parser), 0d);
                        }
                        break;
                    default:
                        break;
                }
            } else if (event == XmlPullParser.END_TAG) {
                String tag = localName(parser.getName());
                if ("emit".equals(tag)) {
                    insideEmit = false;
                } else if ("dest".equals(tag)) {
                    insideDest = false;
                } else if ("total".equals(tag)) {
                    insideTotal = false;
                } else if ("prod".equals(tag)) {
                    insideProd = false;
                    if (product != null && product.name != null) {
                        if (product.totalPrice == 0d) {
                            product.totalPrice = product.quantity * product.unitPrice;
                        }
                        invoice.products.add(product);
                    }
                    product = null;
                }
            }
            event = parser.next();
        }

        invoice.rawText = rawText.toString();
        if (invoice.totalValue == 0d) {
            double sum = 0d;
            for (ExtractedProduct item : invoice.products) {
                sum += item.totalPrice;
            }
            invoice.totalValue = sum;
        }
        if (invoice.issueDate == 0L) {
            invoice.issueDate = System.currentTimeMillis();
        }
    }

    private String localName(String name) {
        if (name == null) {
            return "";
        }
        int separator = name.indexOf(':');
        return separator >= 0 ? name.substring(separator + 1) : name;
    }

    private String text(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.next() == XmlPullParser.TEXT) {
            String value = parser.getText();
            return value == null ? null : value.trim();
        }
        return null;
    }

    private double parseDouble(String value, double fallback) {
        if (value == null || value.isEmpty()) {
            return fallback;
        }
        try {
            return Double.parseDouble(value.replace(",", "."));
        } catch (NumberFormatException error) {
            return fallback;
        }
    }

    private long parseDate(String value) {
        if (value == null || value.length() < 10) {
            return 0L;
        }
        try {
            return new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(value.substring(0, 10)).getTime();
        } catch (ParseException error) {
            return 0L;
        }
    }

    private void closeQuietly(InputStream input) {
        if (input != null) {
            try {
                input.close();
            } catch (IOException ignored) {
                // nada a fazer
            }
        }
    }
}
