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
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

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

  public StreamFactory(byte[] base) throws IOException {
    this.base = base;
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    GZIPOutputStream gos = new GZIPOutputStream(baos, true);
    gos.write(base);
    gos.flush();
    output = baos.toByteArray();
    ignore = new byte[base.length];
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
    GZIPOutputStream gos = new GZIPOutputStream(fos, true);
    gos.write(base);
    gos.flush();
    return gos;
  }

  public InputStream wrapInputStream(InputStream inputStream) throws IOException {
    FilterInputStream fis = new FilterInputStream(inputStream) {
      ByteArrayInputStream bis = new ByteArrayInputStream(output);

      @Override
      public int read() throws IOException {
        if (bis != null) {
          int read = bis.read();
          if (read == -1) bis = null;
          return read;
        }
        return in.read();
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
        return read + additional;
      }
    };
    GZIPInputStream gis = new GZIPInputStream(fis);
    int length = ignore.length;
    while (length > 0) {
      length -= gis.read(ignore, ignore.length - length, length);
    }
    return gis;
  }
}
