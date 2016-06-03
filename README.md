# HackRetrofit
A hacked edition of Retrofit 2.0.2.

# Introductions

This is kind of for fun... 

1. I added two annotations for type and method and apis of retrofit can share the annotations annotated a type:

@FixField: Provide a fixed field to be posted with other dynamically passed in fields.
@GeneratedField: Generated a field when a post request will be made.

2. Add new support of Mock Server. You can simply use the raw response data to mock a server response like this:

``` java
    @Override public Call<List<Contributor>> contributors(String owner, String repo) {
      return delegate.returningRawResponse(getJsonResponse()).contributors(owner, repo);
    }
    private String getJsonResponse(){
        // Eg. [{"login":"Jake", "contributions":1234},{"login":"John", "contributions":1234}]
        return ...;
    }
```

3. Add support of TypedFile. So when you want to upload a file, simply do it like this:

```java
public interface FileUploadService {
  @Multipart
  @POST("upload")
  Call<ResponseBody> upload(@Part("description") RequestBody description,
                            @Part TypedFile typedFile);
}

...

Retrofit retrofit = new Retrofit.Builder()
    .baseUrl("http://www.println.net/")
    .addConverterFactory(new TypedFileMultiPartBodyConverterFactory())
    .addConverterFactory(GsonConverterFactory.create())
    .build();
 
FileUploadService service = retrofit.create(FileUploadService.class);
TypedFile typedFile = new TypedFile("aFile", filename);
String descriptionString = "This is a description";
RequestBody description =
        RequestBody.create(
                MediaType.parse("multipart/form-data"), descriptionString);
 
Call<ResponseBody> call = service.upload(description, typedFile);
call.enqueue(...);
```

Name of the file to be uploaded will be used to retrieve the MediaType if you don't explictly set it. The retrieve of MediaType is still uncompleted.


# More Infomations:

[Android 下午茶：Hack Retrofit 之 增强参数](http://www.println.net/post/Android-Hack-Retrofit).
[Android 下午茶：Hack Retrofit (2) 之 Mock Server](http://www.println.net/post/Android-Hack-Retrofit-Mock-Server)
[深入浅出 Retrofit，这么牛逼的框架你们还不来看看？](http://bugly.qq.com/bbs/forum.php?mod=viewthread&tid=1117&extra=page%3D1)
