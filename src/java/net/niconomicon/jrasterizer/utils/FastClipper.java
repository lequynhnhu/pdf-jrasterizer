/**
 * 
 */
package net.niconomicon.jrasterizer.utils;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferDouble;
import java.awt.image.DataBufferFloat;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferShort;
import java.awt.image.DataBufferUShort;
import java.awt.image.Raster;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream.GetField;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;

/**
 * This class provides static methods to extract a rectangular part of a given image.
 * @author Nicolas Hoibian
 * 
 */
public class FastClipper {

	/** Kept for historical/cut-and-paste purpose : */
	private static BufferedImage slowClip(BufferedImage image, Rectangle clip) {
		Raster ras = image.getData(clip);
		BufferedImage ret = new BufferedImage(clip.width, clip.height, image.getType());
		ret.getRaster().setRect(-clip.x, -clip.y, ras);
		return ret;
	}

	/**
	 * inspired from :
	 * http://stackoverflow.com/questions/2825837/java-how-to-do-fast-copy-of-a-bufferedimages-pixels-unit-test-included
	 * Does an arraycopy of the rasters .
	 * 
	 * @param src source image.
	 * @param clip the part of the image that you want.
	 * @return an image which is identical to the part of the image of src in the area described by clip.
	 */
	public static BufferedImage fastClip(final BufferedImage src, Rectangle clip) {
		return fastClip(src, clip, false);
	}


	/**
	 * @param src source image.
	 * @param clip the part of the image that you want.
	 * @param flipVertically should the image be flipped vertically ?
	 * @return an independent copy of the part of the image described by clip, flipped vertically according to 'flipVertically'. 
	 */
	public static BufferedImage fastClip(final BufferedImage src, Rectangle clip, boolean flipVertically) {
		BufferedImage dst = new BufferedImage(clip.width, clip.height, src.getType());

		Object srcbuf = null;
		Object dstbuf = null;

		int mpx = src.getWidth() * src.getHeight();
		int factor = 1;
		DataBuffer buff = src.getRaster().getDataBuffer();
		/**
		 * Different type of image have different type of underlying buffer. Each type has a different number of cells
		 * dedicated to a single pixel.
		 */
		if (buff instanceof DataBufferByte) {
			srcbuf = ((DataBufferByte) buff).getData();
			dstbuf = ((DataBufferByte) dst.getRaster().getDataBuffer()).getData();
			factor = ((DataBufferByte) buff).getData().length / mpx;
		}
		if (buff instanceof DataBufferDouble) {
			srcbuf = ((DataBufferDouble) buff).getData();
			dstbuf = ((DataBufferDouble) dst.getRaster().getDataBuffer()).getData();
			factor = ((DataBufferDouble) buff).getData().length / mpx;
		}
		if (buff instanceof DataBufferFloat) {
			srcbuf = ((DataBufferFloat) buff).getData();
			dstbuf = ((DataBufferFloat) dst.getRaster().getDataBuffer()).getData();
			factor = ((DataBufferFloat) buff).getData().length / mpx;
		}
		if (buff instanceof DataBufferInt) {
			srcbuf = ((DataBufferInt) buff).getData();
			dstbuf = ((DataBufferInt) dst.getRaster().getDataBuffer()).getData();
			factor = ((DataBufferInt) buff).getData().length / mpx;
		}
		if (buff instanceof DataBufferShort) {
			srcbuf = ((DataBufferShort) buff).getData();
			dstbuf = ((DataBufferShort) dst.getRaster().getDataBuffer()).getData();
			factor = ((DataBufferShort) buff).getData().length / mpx;
		}
		if (buff instanceof DataBufferUShort) {
			srcbuf = ((DataBufferUShort) buff).getData();
			dstbuf = ((DataBufferUShort) dst.getRaster().getDataBuffer()).getData();
			factor = ((DataBufferUShort) buff).getData().length / mpx;
		}

		int srcOffset, dstOffset = 0;

		int dstLineOffset, srcLineOffset = 0;

		final int copyLength = clip.width * factor;

		if (flipVertically) {
			// reading backward
			srcOffset = src.getWidth() * (clip.y + clip.height - 1) * factor + clip.x * factor;
			dstLineOffset = clip.width * factor;
			srcLineOffset = -src.getWidth() * factor;

		} else {
			// reading forward
			srcOffset = src.getWidth() * clip.y * factor + clip.x * factor;

			dstLineOffset = clip.width * factor;
			srcLineOffset = src.getWidth() * factor;
		}
		for (int y = 0; y < clip.height; y++) {
			System.arraycopy(srcbuf, srcOffset, dstbuf, dstOffset, copyLength);

			srcOffset += srcLineOffset;
			dstOffset += dstLineOffset;
		}

		return dst;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws IOException {
		File file = new File(args[0]);
		BufferedImage src = ImageIO.read(file);
		int clipWidth = 200;
		int clipHeight = 10;
		Rectangle clip = new Rectangle((src.getWidth() - clipWidth) / 2, (src.getHeight() - clipHeight) / 2, clipWidth, clipHeight);

		if (args.length > 1) {
			showClips(src, clip);
			return;
		}
		long start, stop;
		int m = 10;
		int n = 1000;
		for (int k = 0; k < m; k++) {
			start = System.currentTimeMillis();
			for (int i = 0; i < n; i++) {
				FastClipper.fastClip(src, clip);
			}
			stop = System.currentTimeMillis();
			System.out.println("fastClip :" + n + " times = " + (stop - start) + " ms");
		}
		for (int k = 0; k < m; k++) {
			start = System.currentTimeMillis();
			for (int i = 0; i < n; i++) {
				FastClipper.fastClip(src, clip, true);
			}
			stop = System.currentTimeMillis();
			System.out.println("flipClip :" + n + " times = " + (stop - start) + " ms");
		}

		for (int k = 0; k < m; k++) {
			start = System.currentTimeMillis();
			for (int i = 0; i < n; i++) {
				FastClipper.slowClip(src, clip);
			}
			stop = System.currentTimeMillis();
			System.out.println("slowClip :" + n + " times = " + (stop - start) + " ms");
		}
	}

	public static void showClips(BufferedImage src, Rectangle clip) {
		int n = 9;
		int rowLength = 3;
		JFrame frame = new JFrame();
		JPanel slowClip = new JPanel(new GridLayout(0, rowLength));
		JPanel fastClip = new JPanel(new GridLayout(0, rowLength));
		JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		JScrollPane left = new JScrollPane(slowClip);
		JScrollPane right = new JScrollPane(fastClip);
		Dimension min = new Dimension(rowLength * clip.width, clip.height * (n / rowLength));
		slowClip.setMinimumSize(min);
		slowClip.setPreferredSize(min);
		fastClip.setMinimumSize(min);
		fastClip.setPreferredSize(min);
		split.add(left);
		split.add(right);
		frame.setContentPane(split);
		long start, stop;
		start = System.currentTimeMillis();
		for (int i = 0; i < n; i++) {
			JLabel l = new JLabel(new ImageIcon(FastClipper.fastClip(src, clip, false)));
			fastClip.add(l);
		}
		stop = System.currentTimeMillis();
		System.out.println("fast : stop - start = " + (stop - start) + " ms");

		start = System.currentTimeMillis();
		for (int i = 0; i < n; i++) {
			JLabel l = new JLabel(new ImageIcon(FastClipper.fastClip(src, clip, true)));
			slowClip.add(l);
		}
		stop = System.currentTimeMillis();
		System.out.println("slow : stop - start = " + (stop - start) + " ms");

		frame.pack();
		frame.setVisible(true);

	}
}
