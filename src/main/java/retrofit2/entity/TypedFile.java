package retrofit2.entity;

import okhttp3.MediaType;

import java.io.File;

/**
 * Created by benny on 6/1/16.
 */
public class TypedFile {
    private MediaType mediaType;
    private String name;
    private File file;

    public TypedFile(String name, String filepath){
        this(name, new File(filepath));
    }

    public TypedFile(String name, File file) {
        this(MediaType.parse(MediaTypes.getMediaType(file)), name, file);
    }

    public TypedFile(MediaType mediaType, String name, String filepath) {
        this(mediaType, name, new File(filepath));
    }

    public TypedFile(MediaType mediaType, String name, File file) {
        this.mediaType = mediaType;
        this.name = name;
        this.file = file;
    }

    public String getName() {
        return name;
    }

    public MediaType getMediaType() {
        return mediaType;
    }

    public File getFile() {
        return file;
    }
}
