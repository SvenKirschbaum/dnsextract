package de.elite12.dnsextract.protocol;

import lombok.*;

@Getter
@Setter
@ToString
public class DNSPackage {

    private Header header;
    private Question[] questions;
    private ResourceRecord[] answers;
    private ResourceRecord[] authority;
    private ResourceRecord[] additional;

    public DNSPackage() {
        this.header = new Header();
        this.questions = new Question[0];
        this.answers = new ResourceRecord[0];
        this.authority = new ResourceRecord[0];
        this.additional = new ResourceRecord[0];
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @ToString
    public static class Header {
        short id;
        boolean qr;
        byte opcode;
        boolean aa;
        boolean tc;
        boolean rd;
        boolean ra;
        byte z;
        byte rcode;
        short qdcount;
        short ancount;
        short nscount;
        short arcount;

        public boolean isQuery() {
            return !qr;
        }
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @ToString
    public static class Question {
        String[] labels;
        short qtype;
        short qclass;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @ToString
    public static class ResourceRecord {
        String[] labels;
        boolean labelpointer = false;
        short type;
        short _class;
        int ttl;
        short rdlength;
        byte[] rdata;
    }
}
