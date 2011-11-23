/**

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.

 */
package corpuscompress;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.CRC32;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.Inflater;

/**
 * This creates input and output streams warmed with the content you specify.
 * <p/>
 * User: sam
 * Date: 11/22/11
 * Time: 9:04 PM
 */
public class StreamFactory {
  private final byte[] base;
  private final byte[] output;
  private final byte[] ignore;
  private AtomicReference<CRC32> finalcrc;
  private byte[] expected;

  public StreamFactory(byte[] base) throws IOException {
    this.base = base;
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    finalcrc = new AtomicReference<CRC32>();
    GZIPOutputStream gos = new GZIPOutputStream(baos, true) {
      @Override
      public void flush() throws IOException {
        super.flush();
        finalcrc.set(crc);
      }
    };
    gos.write(base);
    gos.flush();
    output = baos.toByteArray();
    ignore = new byte[base.length];
    gos.close();
  }

  public OutputStream wrapOutputStream(OutputStream outputStream) throws IOException {
    FilterOutputStream fos = new FilterOutputStream(outputStream) {
      boolean first = true;
      int total = output.length;

      @Override
      public void write(byte[] b, int off, int len) throws IOException {
        if (total > 0) {
          if (total >= len) {
            total -= len;
            return;
          } else {
            int diff = len - total;
            off += diff;
            len -= diff;
            total = 0;
          }
        }
        out.write(b, off, len);
      }

      @Override
      public void write(int b) throws IOException {
        if (total-- > 0) {
          return;
        }
        super.write(b);
      }
    };
    GZIPOutputStream gos = new GZIPOutputStream(fos, true) {
      @Override
      public void finish() throws IOException {
        super.flush();
        out.write(3);
        out.write(0);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        out = baos;
        expected = new byte[8];
        super.finish();
        System.arraycopy(baos.toByteArray(), 2, expected, 0, 8);
      }
    };
    gos.write(base);
    gos.flush();
    return gos;
  }

  /*
  * Writes integer in Intel byte order to a byte array, starting at a
  * given offset.
  */
  private void writeInt(int i, byte[] buf, int offset) throws IOException {
    writeShort(i & 0xffff, buf, offset);
    writeShort((i >> 16) & 0xffff, buf, offset + 2);
  }

  /*
  * Writes short integer in Intel byte order to a byte array, starting
  * at a given offset
  */
  private void writeShort(int s, byte[] buf, int offset) throws IOException {
    buf[offset] = (byte) (s & 0xff);
    buf[offset + 1] = (byte) ((s >> 8) & 0xff);
  }

  public InputStream wrapInputStream(InputStream inputStream) throws IOException {
    final AtomicReference<CRC32> readCRC = new AtomicReference<CRC32>();
    final AtomicReference<Inflater> readInflater = new AtomicReference<Inflater>();
    FilterInputStream fis = new FilterInputStream(inputStream) {
      ByteArrayInputStream bis = new ByteArrayInputStream(output);
      byte[] trailer = null;
      int trailerPos = 0;

      @Override
      public int read() throws IOException {
        if (bis != null) {
          int read = bis.read();
          if (read == -1) bis = null;
          return read;
        }
        int read = in.read();
        if (read == -1) {
          if (trailer == null) {
            trailer = new byte[8];
            writeInt((int) readCRC.get().getValue(), trailer, 0);
            writeInt((int) readInflater.get().getBytesWritten(), trailer, 4);
          }
          return trailerPos == 8 ? -1 : trailer[trailerPos++] & 0xFF;
        }
        return read;
      }

      @Override
      public int read(byte[] b, int off, int len) throws IOException {
        int additional = 0;
        if (bis != null) {
          int read = bis.read(b, off, len);
          if (read == -1) bis = null;
          if (read < len) {
            off = read;
            len -= read;
            additional = read;
            bis = null;
          } else {
            return read;
          }
        }
        int read = in.read(b, off, len);
        return read + (read == -1 ? 0 : additional);
      }
    };
    GZIPInputStream gis = new GZIPInputStream(fis) {{
      readCRC.set(crc);
      readInflater.set(inf);
    }};
    int length = ignore.length;
    while (length > 0) {
      length -= gis.read(ignore, ignore.length - length, length);
    }
    return gis;
  }
}
