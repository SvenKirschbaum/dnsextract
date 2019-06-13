package de.elite12.dnsextract.util;

import java.io.ByteArrayInputStream;

public class SafeByteArrayInputStream extends ByteArrayInputStream {

    public SafeByteArrayInputStream(byte[] buf) {
        super(buf);
    }

    public SafeByteArrayInputStream(byte[] buf, int offset, int length) {
        super(buf, offset, length);
    }

    @Override
    public int read() {
        int tmp = super.read();
        if(tmp == -1) {
            throw new IndexOutOfBoundsException();
        }
        return tmp;
    }

    public int getPos() {
        return this.pos;
    }
}
