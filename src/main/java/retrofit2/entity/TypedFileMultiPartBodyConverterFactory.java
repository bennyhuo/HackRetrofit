package retrofit2.entity;

import okhttp3.MultipartBody;
import retrofit2.Converter;
import retrofit2.Retrofit;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

public class TypedFileMultiPartBodyConverterFactory extends Converter.Factory {
    @Override
    public Converter<TypedFile, MultipartBody.Part> arbitraryConverter(Type originalType, Type convertedType, Annotation[] parameterAnnotations, Annotation[] methodAnnotations, Retrofit retrofit) {
        if (originalType == TypedFile.class && convertedType == MultipartBody.Part.class) {
            return new TypedFileMultiPartBodyConverter();
        }
        return null;
    }
}