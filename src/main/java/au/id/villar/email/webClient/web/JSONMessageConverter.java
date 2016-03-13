package au.id.villar.email.webClient.web;

import au.id.villar.json.JSONReaderException;
import au.id.villar.json.ObjectDeserializer;
import au.id.villar.json.ObjectSerializer;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class JSONMessageConverter<T> implements HttpMessageConverter<T> {

    private final List<MediaType> supportedMediaTypes;

    public JSONMessageConverter() {
        List<MediaType> types = new ArrayList<>();
        types.add(MediaType.APPLICATION_JSON);
        supportedMediaTypes = Collections.unmodifiableList(types);
    }

    @Override
    public boolean canRead(Class<?> aClass, MediaType mediaType) {
        return MediaType.APPLICATION_JSON.isCompatibleWith(mediaType);
    }

    @Override
    public boolean canWrite(Class<?> aClass, MediaType mediaType) {
        return MediaType.APPLICATION_JSON.isCompatibleWith(mediaType);
    }

    @Override
    public List<MediaType> getSupportedMediaTypes() {
        return supportedMediaTypes;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T read(Class<? extends T> aClass, HttpInputMessage httpInputMessage) throws IOException, HttpMessageNotReadableException {
        boolean explicitType = true;
        if(Map.class == aClass || List.class == aClass) {
            explicitType = false;
        }
        T object;
        try (InputStreamReader reader = new InputStreamReader(httpInputMessage.getBody())) {
            object = (T)(explicitType?
                    ObjectDeserializer.getFromReader(reader, aClass):
                    ObjectDeserializer.getFromReader(reader));
        } catch (JSONReaderException e) {
            throw new HttpMessageNotReadableException("Error parsing JSON", e);
        }
        return object;
    }

    @Override
    public void write(T t, MediaType mediaType, HttpOutputMessage httpOutputMessage) throws IOException, HttpMessageNotWritableException {
        try (OutputStreamWriter writer = new OutputStreamWriter(httpOutputMessage.getBody())) {
            ObjectSerializer.write(t, writer);
        }
    }
}
