package de.elite12.dnsextract.services;

import de.elite12.dnsextract.data.Upload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;

@Service
public class UploadService {
    private static Logger logger = LoggerFactory.getLogger(UploadService.class);

    private Map<String, Upload> uploads = new HashMap<>();

    public String startUpload(String name, int size) {
        //TODO timeouts
        logger.info("Started Upload: " + name + ", size: "+size);
        String key = randomKey();
        Upload tmp = new Upload(name,size);
        uploads.put(key,tmp);

        return key;
    }

    public String addPart(String key, int chunk, byte[] data) {
        logger.info("Part for key: "+key+", chunk: "+chunk + ", data: " + Arrays.toString(data));

        if(!uploads.containsKey(key)) return "Invalid Key";

        Upload upload = uploads.get(key);

        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (upload) {
            if(upload.getParts().containsKey(chunk)) return "Duplicate Chunk";

            upload.getParts().put(chunk,data);
            upload.setCurrentsize(upload.getCurrentsize() + data.length);

            if(upload.getCurrentsize() >= upload.getFullsize()) {
                uploads.remove(key);
                try {
                    FileOutputStream  writer = new FileOutputStream("output/"+upload.getFilename());
                    byte[][] fulldata = upload.getParts().entrySet().stream().sorted(Comparator.comparing(Map.Entry::getKey)).map(Map.Entry::getValue).toArray(byte[][]::new);
                    for(byte[] e:fulldata) {
                        writer.write(e);
                    }
                    writer.close();
                    logger.info("Upload finished: " + key);
                    return "finished";
                } catch (IOException e) {
                    logger.error("Error writing File",e);
                }
            }
            return "ok";
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
