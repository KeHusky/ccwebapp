package com.example.webapp.helpers;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;
import com.example.webapp.dao.MetadataRepository;
import com.example.webapp.entities.Metadata;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.ClassUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;


@Service
public class S3Hanlder {
    @Autowired
    MetadataRepository metadataRepository;

    @Value("${aws_access_key}")
    String AWS_ACCESS_KEY;
    @Value("${aws_secret_key}")
    String AWS_SECRET_KEY;
    public static String LOCAL_DIR = ClassUtils.getDefaultClassLoader().getResource("").getPath() + "static/";
    @Value("${bucketName}")
    String bucketName;

    public String uploadfile(MultipartFile file, String fileName, String type) {

        String localFilePath = LOCAL_DIR + fileName;
        try {
            fileCopy(file.getBytes(), LOCAL_DIR, fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
        File localfile = new File(localFilePath);
        try {
            AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
                    .withRegion(Regions.US_EAST_1)
//                    .withCredentials(new ProfileCredentialsProvider("dev"))
                    .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(AWS_ACCESS_KEY, AWS_SECRET_KEY)))
                    .withPathStyleAccessEnabled(true)
                    .build();
            // Upload a file as a new object with ContentType and title specified.
            PutObjectRequest request = new PutObjectRequest(bucketName, fileName, localfile);

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType("image/" + type.toLowerCase());
            String md5 = "";

            try {
                md5 = new String(org.apache.commons.codec.binary.Base64.encodeBase64(DigestUtils.md5(file.getBytes())));
            } catch (IOException e) {
                e.printStackTrace();
            }

            metadata.setContentMD5(md5);
            request.setMetadata(metadata);

            Metadata m = new Metadata();
            m.setImage_id(fileName);
            m.setSize(file.getSize());
            m.setType("image/" + type.toLowerCase());
            m.setMd5(md5);
            metadataRepository.save(m);

            s3Client.putObject(request);

            GeneratePresignedUrlRequest urlRequest = new GeneratePresignedUrlRequest(bucketName, fileName);
            URL url = s3Client.generatePresignedUrl(urlRequest);

            localfile.delete();
            return url.toString();
        } catch (AmazonServiceException e) {
            // The call was transmitted successfully, but Amazon S3 couldn't process
            // it, so it returned an error response.
            e.printStackTrace();
        } catch (SdkClientException e) {
            // Amazon S3 couldn't be contacted for a response, or the client
            // couldn't parse the response from Amazon S3.
            e.printStackTrace();
        }
        return null;

    }

    public void deletefile(String object_name) {

        AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
                .withRegion(Regions.US_EAST_1)
//                    .withCredentials(new ProfileCredentialsProvider("dev"))
                .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(AWS_ACCESS_KEY, AWS_SECRET_KEY)))
                .build();

        try {
            s3Client.deleteObject(bucketName, object_name);
        } catch (AmazonServiceException e) {
            e.printStackTrace();
        }
    }

    public static void fileCopy(byte[] file, String filePath, String fileName) throws IOException {
        File targetfile = new File(filePath);
        if (targetfile.exists()) {
            targetfile.mkdirs();
        }

        FileOutputStream out = new FileOutputStream(filePath + fileName);
        out.write(file);
        out.flush();
        out.close();
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuffer md5str = new StringBuffer();
        int digital;
        for (int i = 0; i < bytes.length; i++) {
            digital = bytes[i];

            if (digital < 0) {
                digital &= 0xFF;
            }
            if (digital < 16) {
                md5str.append("0");
            }
            md5str.append(Integer.toHexString(digital));
        }
        return md5str.toString().toUpperCase();
    }
}


