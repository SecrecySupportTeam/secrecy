/**
 * Copyright (c) 2000 - 2013 The Legion of the Bouncy Castle Inc. (http://www.bouncycastle.org)

 Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 associated documentation files (the "Software"), to deal in the Software without restriction,
 including without limitation the rights to use, copy, modify, merge, publish, distribute,
 sublicense, and/or sell copies of the Software, and to permit persons to whom the Software
 is furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all
 copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 IN THE SOFTWARE.
 */

package com.doplgangr.secrecy.filesystem.encryption;

import com.doplgangr.secrecy.Config;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.crypto.Cipher;

/**
 * The SecrecyCipherInputStream is a modified version of Bouncy Castles
 * CipherInputStream.
 */
public class SecrecyCipherInputStream extends FilterInputStream {
    private final Cipher cipher;
    private final byte[] buf;
    private final byte[] inBuf;
    private int bufOff;
    private int maxBuf;
    private boolean finalized;

    public SecrecyCipherInputStream(InputStream is, Cipher cipher) {
        super(is);
        this.cipher = cipher;

        buf = new byte[cipher.getOutputSize(Config.BUFFER_SIZE)];
        inBuf = new byte[Config.BUFFER_SIZE];
    }

    private int nextChunk() throws IOException {
        int available = super.available();

        // must always try to read 1 byte!
        // some buggy InputStreams return < 0!
        if (available <= 0) {
            available = 1;
        }

        if (available > inBuf.length) {
            available = super.read(inBuf, 0, inBuf.length);
        } else {
            available = super.read(inBuf, 0, available);
        }

        if (available < 0) {
            if (finalized) {
                return -1;
            }

            try {
                maxBuf = cipher.doFinal(buf, 0);
            } catch (Exception e) {
                throw new IOException("error processing stream: " + e.toString());
            }

            bufOff = 0;
            finalized = true;

            if (bufOff == maxBuf) {
                return -1;
            }
        } else {
            bufOff = 0;

            try {
                maxBuf = cipher.update(inBuf, 0, available, buf, 0);
            } catch (Exception e) {
                throw new IOException("error processing stream: " + e.toString());
            }

            // not enough bytes read for first block...
            if (maxBuf == 0) {
                return nextChunk();
            }
        }
        return maxBuf;
    }

    @Override
    public int read() throws IOException {
        if (bufOff == maxBuf) {
            if (nextChunk() < 0) {
                return -1;
            }
        }
        return buf[bufOff++] & 0xff;
    }

    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (bufOff == maxBuf) {
            if (nextChunk() < 0) {
                return -1;
            }
        }
        int available = maxBuf - bufOff;

        if (len > available) {
            System.arraycopy(buf, bufOff, b, off, available);
            bufOff = maxBuf;

            return available;
        } else {
            System.arraycopy(buf, bufOff, b, off, len);
            bufOff += len;

            return len;
        }
    }

    @Override
    public long skip(long n) throws IOException {
        if (n <= 0) {
            return 0;
        }

        int available = maxBuf - bufOff;

        if (n > available) {
            bufOff = maxBuf;
            return available;
        } else {
            bufOff += (int) n;
            return (int) n;
        }
    }

    @Override
    public int available() throws IOException {
        return maxBuf - bufOff;
    }

    @Override
    public boolean markSupported() {
        return false;
    }
}
