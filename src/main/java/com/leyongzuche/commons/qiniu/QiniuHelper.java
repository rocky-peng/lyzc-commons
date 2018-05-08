package com.leyongzuche.commons.qiniu;

import com.qiniu.common.QiniuException;
import com.qiniu.common.Zone;
import com.qiniu.http.Response;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.UploadManager;
import com.qiniu.util.Auth;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.InputStream;
import java.net.URL;

/**
 * @author pengqingsong
 * @date 12/09/2017
 * @desc
 */
public class QiniuHelper {

    private static final UploadManager UPLOAD_MANAGER = new UploadManager(new Configuration(Zone.zone2()));
    private String accessKey;
    private String secretKey;
    private String bucket;
    private String cndDomain;

    public QiniuHelper(String accessKey, String secretKey, String bucket, String cndDomain) {
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.bucket = bucket;
        this.cndDomain = cndDomain;
    }

    public static String upToken(String accessKey, String secretKey, String bucket) {
        Auth auth = Auth.create(accessKey, secretKey);
        String upToken = auth.uploadToken(bucket);
        return upToken;
    }

    public String upToken() {
        Auth auth = Auth.create(accessKey, secretKey);
        String upToken = auth.uploadToken(bucket);
        return upToken;
    }

    public String upload(File file, String fileName) {
        String token = upToken();
        try {
            Response response = UPLOAD_MANAGER.put(file, fileName, token);
            if (response.isOK()) {
                return cndDomain + fileName;
            } else {
                throw new RuntimeException("七牛图片上传失败[" + response.bodyString() + "]");
            }
        } catch (QiniuException e) {
            throw new RuntimeException("七牛图片上传失败", e);
        }
    }

    public String upload(InputStream fileIs, String fileName) {
        String token = upToken();
        try {
            Response response = UPLOAD_MANAGER.put(fileIs, fileName, token, null, null);
            if (response.isOK()) {
                return cndDomain + fileName;
            } else {
                throw new RuntimeException("七牛图片上传失败[" + response.bodyString() + "]");
            }
        } catch (QiniuException e) {
            throw new RuntimeException("七牛图片上传失败", e);
        }
    }

    public String upload(byte[] data, String fileName) {
        String token = upToken();
        try {
            Response response = UPLOAD_MANAGER.put(data, fileName, token);
            if (response.isOK()) {
                return cndDomain + fileName;
            } else {
                throw new RuntimeException("七牛图片上传失败[" + response.bodyString() + "]");
            }
        } catch (QiniuException e) {
            throw new RuntimeException("七牛图片上传失败", e);
        }
    }

    /**
     *
     * @param imgUrl
     * @param cmd  需要带?
     * @return
     */
    public String process(String imgUrl, String cmd) {
        String fileName = imgUrl.substring("http://img.leyongzuche.com".length());
        while (fileName.startsWith("/")) {
            fileName = fileName.substring(1);
        }

        fileName = fileName + "-lyzc";
        try {
            String processedImgUrl = upload(IOUtils.toByteArray(new URL(imgUrl + cmd)), fileName);
            return processedImgUrl;
        } catch (Exception e) {
            throw new RuntimeException("七牛图片处理失败", e);
        }
    }
}
