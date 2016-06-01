package retrofit2.entity;

import java.io.File;
import java.util.HashMap;

/**
 * Created by benny on 6/1/16.
 */
public class MediaTypes {

    public static final String APPLICATION_JSON = "application/json";
    public static final String APPLICATION_URLENCODED = "application/x-www-form-urlencoded";
    public static final String MULTIPART_FORM_DATA = "multipart/form-data";
    public static final String TEXT_HTML = "text/html";
    public static final String IMAGE_PNG = "image/png";
    public static final String TEXT_PLAIN = "text/plain";
    public static final String APPLICATION_OCTET_STREAM = "application/octet-stream";

    private static HashMap<String, String> mediaTypeMap = new HashMap<String, String>() {
        {
            put("json", APPLICATION_JSON);
            put("png", IMAGE_PNG);
            put("html", TEXT_HTML);
            put("txt", TEXT_PLAIN);
        }
    };

    public static String getMediaType(File file) {
        String filename = file.getName();
        int index = filename.lastIndexOf(".");
        if (index == -1) {
            return APPLICATION_OCTET_STREAM;
        } else {
            String ext = filename.substring(index);
            String mediaType = mediaTypeMap.get(ext);
            if (mediaType == null) {
                return APPLICATION_OCTET_STREAM;
            } else {
                return mediaType;
            }
        }
    }
}
