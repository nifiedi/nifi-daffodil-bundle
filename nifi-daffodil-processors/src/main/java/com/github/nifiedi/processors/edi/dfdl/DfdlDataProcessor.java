package com.github.nifiedi.processors.edi.dfdl;

import org.apache.daffodil.japi.*;
import org.apache.daffodil.japi.infoset.*;
import org.apache.daffodil.japi.io.InputSourceDataInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.AbstractMap;
import java.util.List;

/**
 * @author Chaojun.Xu
 * @date 2025/05/28 20:21
 */

public class DfdlDataProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(DfdlDataProcessor.class);
    private DataProcessor dataProcessor;

    public DfdlDataProcessor(URI uri, AbstractMap<String, String> variables, ValidationMode validationMode, boolean reloadFromDisk) throws Throwable {
        if (reloadFromDisk) {
            final File binSchemaFile = new File(uri);
            dataProcessor = Daffodil.compiler().reload(binSchemaFile);
        } else {
            dataProcessor = compileSource(uri);
        }
        if(variables != null && variables.size() > 0){
            dataProcessor = dataProcessor.withExternalVariables(variables);
        }
        dataProcessor = dataProcessor.withValidationMode(validationMode);
        if (dataProcessor.isError()) {
            final List<Diagnostic> diagnostics = dataProcessor.getDiagnostics();
            logDiagnostics(diagnostics);
            throw diagnostics.get(0).getSomeCause();
        }
    }

    protected DataProcessor compileSource(URI uri) throws Throwable {
        final ProcessorFactory processorFactory = Daffodil.compiler().compileSource(uri);
        if (processorFactory.isError()) {
            final List<Diagnostic> diagnostics = processorFactory.getDiagnostics();
            logDiagnostics(diagnostics);
            throw diagnostics.get(0).getSomeCause();
        }
        final DataProcessor dataProcessor = processorFactory.onPath("/");
        if (dataProcessor.isError()) {
            final List<Diagnostic> diagnostics = dataProcessor.getDiagnostics();
            logDiagnostics(diagnostics);
            throw diagnostics.get(0).getSomeCause();
        }

        return dataProcessor;
    }

    public void parse(InputSourceDataInputStream dataInputStream, MediaType mediaType, OutputStream outputStream) throws Throwable {
        ParseResult res = dataProcessor.parse(dataInputStream, getInfosetOutputter(outputStream,mediaType));
        if(res.isError()){
            List<Diagnostic> diagnostics = res.getDiagnostics();
            logDiagnostics(diagnostics);
            throw diagnostics.get(0).getSomeCause();
        }
    }

    public void unParse(InputStream inputStream, MediaType mediaType, OutputStream outputStream) throws Throwable {
        WritableByteChannel output = Channels.newChannel(outputStream);
        UnparseResult result = dataProcessor.unparse( getInfosetInputter(inputStream, mediaType), output);
        if(result.isError()){
            List<Diagnostic> diagnostics = result.getDiagnostics();
            logDiagnostics(diagnostics);
            throw diagnostics.get(0).getSomeCause();
        }
    }

    private InfosetOutputter getInfosetOutputter(OutputStream outputStream, MediaType mediaType){
        if(mediaType.equals(MediaType.XML)){
            return new XMLTextInfosetOutputter(outputStream,false);
        }else{
            return new JsonInfosetOutputter(outputStream,false);
        }
    }

    private InfosetInputter getInfosetInputter(InputStream inputStream, MediaType mediaType){
        if(mediaType.equals(MediaType.XML)){
            return new XMLTextInfosetInputter(inputStream);
        }else{
            return new JsonInfosetInputter(inputStream);
        }
    }

    private void logDiagnostics(List<Diagnostic> diagnostics){
        for (Diagnostic diagnostic : diagnostics) {
            LOGGER.error(diagnostic.getMessage());
        }
    }
}