package de.elite12.dnsextract.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Service
public class UploadService {
    private static Logger logger = LoggerFactory.getLogger(UploadService.class);

    private Map<String, ByteBuffer> uploads = new HashMap<>();
    private Map<String, String> filenames = new HashMap<>();

    public String startUpload(String name, int size) {
        //TODO timeouts
        logger.info("Started Upload: " + name + ", size: "+size);
        String key = randomKey();

        ByteBuffer buffer = ByteBuffer.allocate(size);
        uploads.put(key,buffer);
        filenames.put(key,name);

        return key;
    }

    public void addPart(String key, int chunk, byte[] data) {
        logger.info("Part for key: "+key+", chunk: "+chunk + ", data: " + Arrays.toString(data));
        //TODO take care of order
        ByteBuffer buffer = uploads.get(key);

        //TODO take care of error
        if(buffer == null) return;

        buffer.put(data);

        if(buffer.remaining() <= 0) {
            uploads.remove(key);
            try {
                FileOutputStream  writer = new FileOutputStream("output/"+filenames.remove(key));
                writer.write(buffer.array());
                writer.close();
            } catch (IOException e) {
                logger.error("Error writing File",e);
            }
            logger.info("Upload finished: " + key + ", data:" + Arrays.toString(buffer.array()));
        }
    }

    private String randomKey() {
        int leftLimit = 97; // letter 'a'
        int rightLimit = 122; // letter 'z'
        int targetStringLength = 4;
        Random random = new Random();
        StringBuilder buffer = new StringBuilder(targetStringLength);
        for (int i = 0; i < targetStringLength; i++) {
            int randomLimitedInt = leftLimit + (int)
                    (random.nextFloat() * (rightLimit - leftLimit + 1));
            buffer.append((char) randomLimitedInt);
        }

        if(uploads.containsKey(buffer.toString())) return randomKey();

        return buffer.toString();
    }
}
