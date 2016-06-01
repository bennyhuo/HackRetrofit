package retrofit2.entity;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Converter;

import java.io.IOException;

public class TypedFileMultiPartBodyConverter implements Converter<TypedFile, MultipartBody.Part> {

    @Override
    public MultipartBody.Part convert(TypedFile typedFile) throws IOException {
        RequestBody requestFile =
                RequestBody.create(typedFile.getMediaType(), typedFile.getFile());
        return MultipartBody.Part.createFormData(typedFile.getName(), typedFile.getFile().getName(), requestFile);
    }
}