package de.elite12.dnsextract.protocol;

import java.util.stream.Stream;

public class TXTRecord extends DNSPackage.ResourceRecord {
    public TXTRecord(String label, String data) {
        this(data);
        this.labels = label.split("\\.");
        this.labelpointer = false;
    }

    /*
        Creates a TXTRecord with the given value and a label which points to the first question label
     */
    public TXTRecord(String data) {
        this.labelpointer = true;
        this.type = 16;
        this._class = 1;
        this.ttl = 0;
        this.rdlength = (short) (data.length()+1);
        Integer[] tmp = Stream.concat(Stream.of(data.length()), data.chars().boxed()).toArray(Integer[]::new);
        this.rdata = new byte[tmp.length];
        for(int i = 0;i<rdata.length;i++) {
            this.rdata[i] = (byte) (int) tmp[i];
        }
    }
}
