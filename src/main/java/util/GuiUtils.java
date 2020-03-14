package util;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import util.io.FileUtils;

public class GuiUtils {

    public static void exportToImageFile(JFrame jFrame, File file, Dimension preferredSize) {
        exportToImageFile(jFrame.getContentPane(), file, preferredSize);
    }

    public static void exportToImageFile(Component content, File file, Dimension preferredSize) {
        try {
			int width;
			int height;
			if (preferredSize != null) {
			    width = preferredSize.width;
			    height = preferredSize.height;
			} else {
			    width = content.getWidth();
			    height = content.getHeight();
			}

			BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
			content.paint(bufferedImage.getGraphics());

			if (file.exists()) {
			    file.delete();
			} else {
				FileUtils.mkDirsForFile(file);
			}
			ImageIO.write(bufferedImage, "PNG", file);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
    }

    public static void centerOnWindow(JFrame jFrame) {
        Dimension dim = getScreenSize();
        jFrame.setLocation(dim.width / 2 - jFrame.getSize().width / 2, dim.height / 2 - jFrame.getSize().height / 2);
    }

    private static Dimension getScreenSize() {
        return Toolkit.getDefaultToolkit().getScreenSize();
    }

    public static void fitView(JFrame jFrame) {
        Dimension preferredSize = jFrame.getPreferredSize();
        Dimension screenSize = getScreenSize();

        if (preferredSize.width > screenSize.width || preferredSize.height > screenSize.height) {
            jFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        } else {
            jFrame.setSize(preferredSize);
            centerOnWindow(jFrame);
        }
    }
}
