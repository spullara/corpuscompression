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
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

/**
 * This creates input and output streams warmed with the content you specify.
 * <p/>
 * User: sam
 * Date: 11/22/11
 * Time: 9:04 PM
 */
public class StreamFactory {
  private final byte[] base;

  public StreamFactory(byte[] base) throws IOException {
    this.base = base;
  }

  public OutputStream wrapOutputStream(OutputStream outputStream) throws IOException {
    Deflater def = new Deflater();
    def.setDictionary(base);
    return new DeflaterOutputStream(outputStream, def);
  }

  public InputStream wrapInputStream(InputStream inputStream) throws IOException {
    final Inflater inflater = new Inflater();
    return new InflaterInputStream(inputStream, inflater) {
      private boolean check() {
        if (inflater.needsDictionary()) {
          inflater.setDictionary(base);
          return true;
        }
        return false;
      }

      @Override
      public int read() throws IOException {
        int read = super.read();
        if (read == -1 && check()) return read();
        return read;
      }

      @Override
      public int read(byte[] b, int off, int len) throws IOException {
        int read = super.read(b, off, len);
        if (read == -1 && check()) return read(b, off, len);
        return read;
      }

      @Override
      public int read(byte[] b) throws IOException {
        int read = super.read(b);
        if (read == -1 && check()) return read(b);
        return read;
      }
    };
  }


}
