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

import org.iq80.snappy.SnappyOutputStream;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

/**
 * Verify that our roundtrips work, also verify that it is better than without the corpus.
 * <p/>
 * User: sam
 * Date: 11/22/11
 * Time: 9:17 PM
 */
public class StreamFactoryTest {
  @BeforeClass
  public static void setup() {
    header();
  }

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
    assertTrue(baos.size() < test.length());
  }

  @Test
  public void binary() throws IOException {
    File corpusFile = new File("thrifttweet.bin");
    byte[] corpus = new byte[(int) corpusFile.length()];
    new DataInputStream(new FileInputStream(corpusFile)).readFully(corpus);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    StreamFactory sf = new StreamFactory(corpus);
    File targetFile = new File("thrifttweettarget.bin");
    byte[] target = new byte[(int) targetFile.length()];
    new DataInputStream(new FileInputStream(targetFile)).readFully(target);
    OutputStream os = sf.wrapOutputStream(baos);
    os.write(target);
    os.close();
    byte[] check = new byte[(int) targetFile.length()];
    new DataInputStream(sf.wrapInputStream(new ByteArrayInputStream(baos.toByteArray()))).readFully(check);
    assertArrayEquals(target, check);

    report(target.length, snap(target).size(), gzip(target).size(), baos.size());
  }

  @Test
  public void links() throws IOException {
    System.out.println("Links with different corpuses");
    test("links.txt", "http://twitpic.com/7i7o21");
    test("links.txt", "https://plus.google.com/u/0/111091089527727420853/posts/joQC6qnJJ2c");
    test("links2.txt", "http://twitpic.com/7i7o21");
    test("links2.txt", "https://plus.google.com/u/0/111091089527727420853/posts/joQC6qnJJ2c");
    test("links3.txt", "http://twitpic.com/7i7o21");
    test("links3.txt", "https://plus.google.com/u/0/111091089527727420853/posts/joQC6qnJJ2c");
    test("links4.txt", "http://twitpic.com/7i7o21");
    test("links4.txt", "https://plus.google.com/u/0/111091089527727420853/posts/joQC6qnJJ2c");
    test("tweets.txt", new BufferedReader(new FileReader("targettweet.txt")).readLine());
  }

  private void test(String fileName, String test) throws IOException {
    String corpus = new BufferedReader(new FileReader(fileName)).readLine();
    StreamFactory sf = new StreamFactory(corpus.getBytes());
    ByteArrayOutputStream corpused = new ByteArrayOutputStream();
    OutputStream outputStream = sf.wrapOutputStream(corpused);
    outputStream.write(test.getBytes());
    outputStream.close();
    DataInputStream dis = new DataInputStream(sf.wrapInputStream(new ByteArrayInputStream(corpused.toByteArray())));
    String s = dis.readLine();
    assertEquals(test, s);
    assertTrue(corpused.size() < test.length());

    ByteArrayOutputStream gzippped = gzip(test.getBytes());
    assertTrue(corpused.size() < gzippped.size());

    report(test.length(), snap(test.getBytes()).size(), gzippped.size(), corpused.size());
  }

  private ByteArrayOutputStream snap(byte[] bytes) throws IOException {
    ByteArrayOutputStream snapped = new ByteArrayOutputStream();
    SnappyOutputStream sos = new SnappyOutputStream(snapped);
    sos.write(bytes);
    sos.close();
    return snapped;
  }

  private ByteArrayOutputStream gzip(byte[] bytes) throws IOException {
    ByteArrayOutputStream gzippped = new ByteArrayOutputStream();
    GZIPOutputStream gos = new GZIPOutputStream(gzippped);
    gos.write(bytes);
    gos.close();
    return gzippped;
  }

  private static void header() {
    System.out.println("original,snappy,gzip,corpus");
  }

  private void report(int original, int snappy, int gzip, int corpus) {
    System.out.println(original + "," + snappy + "," + gzip + "," + corpus);
  }

}
