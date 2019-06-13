package de.elite12.dnsextract.services;

import de.elite12.dnsextract.AppProperties;
import de.elite12.dnsextract.protocol.DNSPackage;
import de.elite12.dnsextract.protocol.DNSPackageParser;
import de.elite12.dnsextract.protocol.TXTRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

import java.io.IOException;
import java.util.Arrays;

@MessageEndpoint
public class DNSServerService {

    private static Logger logger = LoggerFactory.getLogger(DNSServerService.class);

    @Autowired
    private DNSPackageParser parser;

    @Autowired
    private UploadService uploadService;

    @Autowired
    private AppProperties appProperties;

    @ServiceActivator(inputChannel = "udpIn", outputChannel = "udpOut")
    public Message<byte[]> handeMessage(Message<byte[]> message) throws IOException {
        try {
            logger.trace("Raw Query: " + Arrays.toString(message.getPayload()));
            DNSPackage pkg = parser.parsePackage(message.getPayload());
            logger.debug("Query: " + pkg.toString());

            if(!pkg.getHeader().isQuery()) return null;

            if(pkg.getHeader().getQdcount() < 1) return null;

            String[] subdomain = filterBaseDomain(pkg.getQuestions()[0].getLabels());

            if(subdomain.length == 0) return null;

            if(subdomain[subdomain.length-1].equalsIgnoreCase("start")) {
                StringBuilder filename = new StringBuilder();
                for(int i=0;i<subdomain.length-2;i++) {
                    filename.append(subdomain[i+1]);
                    filename.append(".");
                }
                filename.deleteCharAt(filename.length()-1);
                String key = uploadService.startUpload(filename.toString(),Integer.parseInt(subdomain[0]));

                DNSPackage ans = new DNSPackage();
                ans.getHeader().setId(pkg.getHeader().getId());
                ans.getHeader().setQr(true);
                ans.getHeader().setRd(true);
                ans.getHeader().setRa(true);
                ans.getHeader().setQdcount((short) 1);
                ans.getHeader().setAncount((short) 1);
                ans.setQuestions(pkg.getQuestions());
                ans.setAnswers(new DNSPackage.ResourceRecord[]{
                        new TXTRecord(key)
                });
                logger.debug("Answer: " + ans.toString());
                byte[] rawpkg = parser.packagetobyte(ans);
                logger.trace("Raw Answer: " + Arrays.toString(rawpkg));
                return MessageBuilder.withPayload(rawpkg).build();
            }

            if(subdomain[subdomain.length-1].equalsIgnoreCase("u")) {
                StringBuilder datastring = new StringBuilder();
                for(int i=0;i<subdomain.length-3;i++) {
                    datastring.append(subdomain[i]);
                }
                byte[] rawdata = hexStringToByteArray(datastring.toString());
                uploadService.addPart(subdomain[subdomain.length-2], Integer.valueOf(subdomain[subdomain.length-3]), rawdata);

                DNSPackage ans = new DNSPackage();
                ans.getHeader().setId(pkg.getHeader().getId());
                ans.getHeader().setQr(true);
                ans.getHeader().setRd(true);
                ans.getHeader().setRa(true);
                ans.getHeader().setQdcount((short) 1);
                ans.getHeader().setAncount((short) 1);
                ans.setQuestions(pkg.getQuestions());
                ans.setAnswers(new DNSPackage.ResourceRecord[]{
                        new TXTRecord("ok")
                });
                logger.debug("Answer: " + ans.toString());
                byte[] rawpkg = parser.packagetobyte(ans);
                logger.trace("Raw Answer: " + Arrays.toString(rawpkg));
                return MessageBuilder.withPayload(rawpkg).build();
            }
        }
        catch(RuntimeException e) {
            logger.warn("Parsing package failed",e);
        }
        return null;
    }

    private String[] filterBaseDomain(String[] labels) {
        String[] base = appProperties.getBasedomain().split("\\.");

        for(int i=1;i<=base.length;i++) {
            if(!labels[labels.length-i].equals(base[base.length-i])) {
                return new String[0];
            }
        }

        return Arrays.copyOfRange(labels,0,labels.length-base.length);
    }

    private byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }
}