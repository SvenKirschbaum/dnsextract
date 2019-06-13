package de.elite12.dnsextract.protocol;

import de.elite12.dnsextract.util.SafeByteArrayInputStream;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.LinkedList;

@Service
public class DNSPackageParser {

    public DNSPackage parsePackage(byte[] rawpkg) {
        final SafeByteArrayInputStream stream = new SafeByteArrayInputStream(rawpkg);

        DNSPackage pkg = new DNSPackage();

        //Header
        byte tmp;
        pkg.setHeader(new DNSPackage.Header());
        pkg.getHeader().setId((short) ((stream.read() << 8) | stream.read()));
        tmp = (byte) stream.read();
        pkg.getHeader().setQr((tmp >> 7 & 0b1) == 0b1);
        pkg.getHeader().setOpcode((byte) (tmp >> 3 & 0b1111));
        pkg.getHeader().setAa((tmp >> 2 & 0b1) == 1);
        pkg.getHeader().setTc((tmp >> 1 & 0b1) == 1);
        pkg.getHeader().setRd((tmp & 0b1) == 1);
        tmp = (byte) stream.read();
        pkg.getHeader().setRa((tmp >> 7 & 0b1) == 1);
        pkg.getHeader().setZ((byte) (tmp >> 4 & 0b111));
        pkg.getHeader().setRcode((byte) (tmp & 0b1111));
        pkg.getHeader().setQdcount((short) (stream.read() << 8 | stream.read()));
        pkg.getHeader().setAncount((short) (stream.read() << 8 | stream.read()));
        pkg.getHeader().setNscount((short) (stream.read() << 8 | stream.read()));
        pkg.getHeader().setArcount((short) (stream.read() << 8 | stream.read()));

        //Question section
        DNSPackage.Question[] questions = new DNSPackage.Question[pkg.getHeader().getQdcount()];
        for(int i=0; i<pkg.getHeader().getQdcount(); i++) {
            questions[i] = new DNSPackage.Question();

            questions[i].setLabels(this.readLabels(stream));

            questions[i].setQtype((short) (stream.read() << 8 | stream.read()));
            questions[i].setQclass((short) (stream.read() << 8 | stream.read()));
        }
        pkg.setQuestions(questions);

        //Answer section
        pkg.setAnswers(this.readRRs(stream,pkg.getHeader().getAncount()));

        //Authority section
        pkg.setAuthority(this.readRRs(stream,pkg.getHeader().getNscount()));

        //Additional section
        pkg.setAdditional(this.readRRs(stream,pkg.getHeader().getArcount()));

        return pkg;
    }

    public byte[] packagetobyte(DNSPackage pkg) {
        final ByteArrayOutputStream stream = new ByteArrayOutputStream();

        //Header
        byte tmp;
        stream.write(pkg.getHeader().getId() >> 8 & 0xFF);
        stream.write(pkg.getHeader().getId() & 0xFF);
        tmp = 0;
        tmp |= (pkg.getHeader().isQr() ? 1 : 0) << 7;
        tmp |= (pkg.getHeader().getOpcode() << 3) & 0b01111000;
        tmp |= (pkg.getHeader().isAa() ? 1 : 0) << 2;
        tmp |= (pkg.getHeader().isTc() ? 1 : 0) << 1;
        tmp |= (pkg.getHeader().isRd() ? 1 : 0);
        stream.write(tmp);
        tmp = 0;
        tmp |= (pkg.getHeader().isRa() ? 1 : 0) << 7;
        tmp |= (pkg.getHeader().getZ() << 4) & 0b01110000;
        tmp |= (pkg.getHeader().getRcode()) & 0xF;
        stream.write(tmp);
        stream.write(pkg.getHeader().getQdcount() >> 8 & 0xFF);
        stream.write(pkg.getHeader().getQdcount() & 0xFF);
        stream.write(pkg.getHeader().getAncount() >> 8 & 0xFF);
        stream.write(pkg.getHeader().getAncount() & 0xFF);
        stream.write(pkg.getHeader().getNscount() >> 8 & 0xFF);
        stream.write(pkg.getHeader().getNscount() & 0xFF);
        stream.write(pkg.getHeader().getArcount() >> 8 & 0xFF);
        stream.write(pkg.getHeader().getArcount() & 0xFF);


        //Question section
        for(int i=0; i<pkg.getHeader().getQdcount(); i++) {
            this.writeLabels(stream, pkg.getQuestions()[i].getLabels());

            stream.write(pkg.getQuestions()[i].getQtype() >> 8 & 0xFF);
            stream.write(pkg.getQuestions()[i].getQtype() & 0xFF);
            stream.write(pkg.getQuestions()[i].getQclass() >> 8 & 0xFF);
            stream.write(pkg.getQuestions()[i].getQclass() & 0xFF);
        }

        //Answer section
        this.writeRRs(stream,pkg.getAnswers());

        //Authority section
        this.writeRRs(stream,pkg.getAuthority());

        //Additional section
        this.writeRRs(stream,pkg.getAdditional());


        return stream.toByteArray();
    }

    private String[] readLabels(SafeByteArrayInputStream stream) {
        byte length;
        LinkedList<String> labels = new LinkedList<>();
        do {
            length = (byte) stream.read();
            //Check if pointer
            if((length >> 6 & 0b11) == 0b11) {
                short offset = (short) ((length & 0b00111111) << 8 | stream.read());
                int pos = stream.getPos();
                stream.reset();
                stream.skip(offset);
                String[] pointervalue = this.readLabels(stream);
                labels.addAll(Arrays.asList(pointervalue));
                labels.add("");//Because last one will be removed below
                stream.reset();
                stream.skip(pos);
                break;
            }
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < length; j++) {
                sb.append((char) stream.read());
            }
            labels.add(sb.toString());
        } while(length != 0);

        labels.removeLast(); // Remove null length last label

        return labels.toArray(String[]::new);
    }

    private DNSPackage.ResourceRecord[] readRRs(SafeByteArrayInputStream stream, int count) {
        DNSPackage.ResourceRecord[] rrs = new DNSPackage.ResourceRecord[count];
        for(int i=0; i < count; i++) {
            rrs[i] = new DNSPackage.ResourceRecord();

            rrs[i].setLabels(this.readLabels(stream));

            rrs[i].setType((short) (stream.read() << 8 | stream.read()));
            rrs[i].set_class((short) (stream.read() << 8 | stream.read()));
            rrs[i].setTtl(stream.read() << 24 | stream.read() << 16 | stream.read() << 8 | stream.read());
            rrs[i].setRdlength((short) (stream.read() << 8 | stream.read()));
            byte[] rdata = new byte[rrs[i].getRdlength()];
            for(int j=0; j<rrs[i].getRdlength(); j++) {
                rdata[j] = (byte) stream.read();
            }
            rrs[i].setRdata(rdata);
        }
        return rrs;
    }

    private void writeRRs(ByteArrayOutputStream stream, DNSPackage.ResourceRecord[] rrs) {
        for(DNSPackage.ResourceRecord r:rrs) {
            if(!r.labelpointer) this.writeLabels(stream, r.getLabels());
            else {
                stream.write(0b11000000);
                stream.write((byte)12);
            }

            stream.write(r.getType() >> 8 & 0xFF);
            stream.write(r.getType() & 0xFF);
            stream.write(r.get_class() >> 8 & 0xFF);
            stream.write(r.get_class() & 0xFF);
            stream.write(r.getTtl() >> 24 & 0xFF);
            stream.write(r.getTtl() >> 16 & 0xFF);
            stream.write(r.getTtl() >> 8 & 0xFF);
            stream.write(r.getTtl() & 0xFF);
            stream.write(r.getRdlength() >> 8 & 0xFF);
            stream.write(r.getRdlength() & 0xFF);

            for(byte b:r.getRdata()) {
                stream.write(b);
            }
        }
    }

    private void writeLabels(ByteArrayOutputStream stream, String[] labels) {
        for(String s:labels) {
            stream.write(s.length());
            s.chars().forEach(stream::write);
        }
        stream.write(0);
    }
}
