package zemberek.lm.apps;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.junit.Test;

import com.google.common.io.ByteSink;
import com.google.common.io.ByteSource;
import com.google.common.io.Files;
import com.google.common.io.Resources;

import junit.framework.Assert;
import zemberek.lm.compression.SmoothLm;

public class ConvertToSmoothLmTest {

    URL TINY_ARPA_URL = Resources.getResource("tiny.arpa");

    File getTinyArpaFile() throws IOException {
        File tmp = File.createTempFile("tiny", ".arpa");
        ByteSource byteSource = Resources.asByteSource(TINY_ARPA_URL);
        ByteSink byteSink = Files.asByteSink(tmp);
        byteSource.copyTo(byteSink);
        return tmp;
    }

    @Test
    public void testConversion() throws IOException {
        File arpaFile = getTinyArpaFile();
        File sm = new File(System.currentTimeMillis() + "-test-lm.smooth");
        sm.deleteOnExit();
        new ConvertToSmoothLm().execute(
                "-arpaFile",
                arpaFile.getAbsolutePath(),
                "-smoothFile",
                sm.getAbsolutePath(),
                "-spaceUsage", "16-16-16");
        Assert.assertTrue(sm.exists());
        SmoothLm lm = SmoothLm.builder(sm).build();
        Assert.assertEquals(3, lm.getOrder());
    }
}
