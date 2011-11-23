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
    String corpus = new BufferedReader(new FileReader("links.txt")).readLine();
    StreamFactory sf = new StreamFactory(corpus.getBytes());
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    OutputStream outputStream = sf.wrapOutputStream(baos);
    String test = "http://pic.twitter.com/GJKZLU6z";
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
