package plataforma.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import java.io.ByteArrayInputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class QrCodeGeneratorTest {

    @Test
    void geraPngLegivelComOConteudoInformado() throws Exception {
        var url = "https://plataforma.test/api/v1/public/certificates/abc?hash=" + "a".repeat(64);

        byte[] png = new QrCodeGenerator().png(url);

        var image = ImageIO.read(new ByteArrayInputStream(png));
        assertThat(image.getWidth()).isEqualTo(QrCodeGenerator.SIZE);
        int[] pixels = image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
        var bitmap = new BinaryBitmap(new HybridBinarizer(
                new RGBLuminanceSource(image.getWidth(), image.getHeight(), pixels)));
        assertThat(new QRCodeReader().decode(bitmap).getText()).isEqualTo(url);
    }
}
