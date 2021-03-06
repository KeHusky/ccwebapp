package com.example.webapp.helpers;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;


@Service
public class S3Hanlder {

    @Value("${AWS_ACCESS_KEY_ID}")
    String AWS_ACCESS_KEY_ID;
    @Value("${AWS_SECRET_ACCESS_KEY}")
    String AWS_SECRET_ACCESS_KEY;
    public static String LOCAL_DIR = "/tmp/";
    //        public static String LOCAL_DIR = "C:\\Users\\Ke\\Desktop\\6225fall";
    @Value("${bucketName}")
    String bucketName;
    @Value("${AWS_REGION}")
    String AWS_REGION;
    Logger logger = LoggerFactory.getLogger(S3Hanlder.class);

    public String uploadfile(MultipartFile file, String fileName) {

        String localFilePath = LOCAL_DIR + fileName;
        try {
            fileCopy(file.getBytes(), LOCAL_DIR, fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
        File localfile = new File(localFilePath);
        try {
            AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
                    .withRegion(AWS_REGION)
                    .withCredentials(new InstanceProfileCredentialsProvider(false))
//                    .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY)))
                    .withPathStyleAccessEnabled(true)
                    .build();
            // Upload a file as a new object with ContentType and title specified.
            PutObjectRequest request = new PutObjectRequest(bucketName, fileName, localfile);
//            request.setMetadata(metadata);

            s3Client.putObject(request);

            GeneratePresignedUrlRequest urlRequest = new GeneratePresignedUrlRequest(bucketName, fileName);
            URL url = s3Client.generatePresignedUrl(urlRequest);
            logger.info("file uploaded");
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
                .withRegion(AWS_REGION)
                .withCredentials(new InstanceProfileCredentialsProvider(false))
//                .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY)))
                .build();

        try {
            s3Client.deleteObject(bucketName, object_name);
            logger.info("file deleted");
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


