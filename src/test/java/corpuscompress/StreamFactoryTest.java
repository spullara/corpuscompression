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

import org.junit.Test;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Verify that our roundtrips work, also verify that it is better than without the corpus.
 * <p/>
 * User: sam
 * Date: 11/22/11
 * Time: 9:17 PM
 */
public class StreamFactoryTest {
  @Test
  public void works() throws IOException {
    String corpus = "this is a test of the emergency broadcast system. this is only a test.";
    StreamFactory sf = new StreamFactory(corpus.getBytes());
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    OutputStream outputStream = sf.wrapOutputStream(baos);
    String test = "testing... emergency! but I'm broadcasting.";
    outputStream.write(test.getBytes());
    outputStream.close();
    DataInputStream dis = new DataInputStream(sf.wrapInputStream(new ByteArrayInputStream(baos.toByteArray())));
    String s = dis.readLine();
    assertEquals(test, s);
    System.out.println(baos.size() + " < " + test.length());
    assertTrue(baos.size() < test.length());
  }

  @Test
  public void links() throws IOException {
    link("links.txt", "http://twitpic.com/7i7o21");
    link("links.txt", "https://plus.google.com/u/0/111091089527727420853/posts/joQC6qnJJ2c");
    link("links2.txt", "http://twitpic.com/7i7o21");
    link("links2.txt", "https://plus.google.com/u/0/111091089527727420853/posts/joQC6qnJJ2c");
    link("links3.txt", "http://twitpic.com/7i7o21");
    link("links3.txt", "https://plus.google.com/u/0/111091089527727420853/posts/joQC6qnJJ2c");
    link("links4.txt", "http://twitpic.com/7i7o21");
    link("links4.txt", "https://plus.google.com/u/0/111091089527727420853/posts/joQC6qnJJ2c");
  }

  private void link(String fileName, String test) throws IOException {
    String corpus = new BufferedReader(new FileReader(fileName)).readLine();
    StreamFactory sf = new StreamFactory(corpus.getBytes());
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    OutputStream outputStream = sf.wrapOutputStream(baos);
    outputStream.write(test.getBytes());
    outputStream.close();
    DataInputStream dis = new DataInputStream(sf.wrapInputStream(new ByteArrayInputStream(baos.toByteArray())));
    String s = dis.readLine();
    assertEquals(test, s);
    System.out.println(baos.size() + " < " + test.length());
    assertTrue(baos.size() < test.length());

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    GZIPOutputStream gos = new GZIPOutputStream(out);
    gos.write(test.getBytes());
    gos.close();
    System.out.println(baos.size() + " < " + out.size());
    assertTrue(baos.size() < out.size());
  }

}
