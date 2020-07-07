package de.ecconia.java.opentung;

import de.ecconia.java.opentung.components.CompLabel;
import de.ecconia.java.opentung.libwrap.LabelTextureWrapper;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import javax.imageio.ImageIO;

public class LabelToolkit
{
	public static LabelTextureWrapper generateUploadTexture(String text, float textSize)
	{
		String[] lines = text.split("\n");
		
		int side = Settings.labelTexturePixelResolution;
		//Generate image:
		BufferedImage image = new BufferedImage(side, side, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		g.setColor(java.awt.Color.white);
		g.fillRect(0, 0, side, side);
		
		g.setColor(java.awt.Color.black);
		Font f = g.getFont();
		f = f.deriveFont(f.getStyle() | Font.BOLD);
		f = f.deriveFont(textSize / 11f * (float) side);
		g.setFont(f);
		
		FontMetrics m = g.getFontMetrics(f);
		int height = lines.length * m.getHeight();
		int lineHeight = m.getHeight();
		
		int offsetY = side / 2 - height / 2;
		for(int i = 0; i < lines.length; i++)
		{
			String lineText = lines[i];
			offsetY += lineHeight;
			g.drawString(lineText, 0, offsetY);
		}
		
		g.dispose();
		
		return new LabelTextureWrapper(image);
	}
	
	private Map<LabelContainer, List<CompLabel>> map = new HashMap<>();
	
	public void startProcessing(BlockingQueue<GPUTask> gpuTasks, List<CompLabel> labelsToRender)
	{
		if(labelsToRender.isEmpty())
		{
			return;
		}
		
		LabelTextureWrapper loading;
		try
		{
			BufferedImage image = ImageIO.read(LabelTextureWrapper.class.getClassLoader().getResourceAsStream("Loading.png"));
			loading = new LabelTextureWrapper(image);
			loading.upload();
		}
		catch(IOException e)
		{
			e.printStackTrace();
			System.exit(1);
			throw new RuntimeException("Tilt."); //Yes Java, this really means I stopped here.
		}
		
		for(CompLabel label : labelsToRender)
		{
			label.setTexture(loading);
			LabelContainer labelHash = new LabelContainer(label.getText(), label.getFontSize());
			List<CompLabel> list = map.get(labelHash);
			if(list == null)
			{
				list = new ArrayList<>();
				map.put(labelHash, list);
			}
			list.add(label);
		}
		
		int threadCount = Runtime.getRuntime().availableProcessors();
		//Leave two unused threads, for other and self-usage. Wire-Linking and graphic threads are running too.
		threadCount -= 2;
		if(map.size() < 20 || threadCount < 2) //Dunno if 20 is fine.
		{
			Thread labelThread = new Thread(() -> {
				List<Map.Entry<LabelContainer, List<CompLabel>>> daList = new ArrayList<>(map.entrySet());
				for(int i = 0; i < daList.size(); i++)
				{
					Map.Entry<LabelContainer, List<CompLabel>> entry = daList.get(i);
					processEntry(gpuTasks, entry);
					if(i % 100 == 0)
					{
						System.out.println("Generated " + (i + 1) + "/" + map.size() + " labels.");
					}
				}
				System.out.println("Finished generating labels.");
			}, "LabelThread");
			System.out.println("Starting to generate " + map.size() + " labels.");
			labelThread.setDaemon(true);
			labelThread.start();
		}
		else
		{
			LinkedBlockingQueue<Map.Entry<LabelContainer, List<CompLabel>>> queue = new LinkedBlockingQueue<>();
			try
			{
				for(Map.Entry<LabelContainer, List<CompLabel>> entry : map.entrySet())
				{
					queue.put(entry);
				}
			}
			catch(InterruptedException e)
			{
				e.printStackTrace();
				return; //Abort, should not happen.
			}
			
			if(threadCount > map.size())
			{
				//Wow over 20 threads :O :O
				threadCount = map.size(); //Safety first.
			}
			System.out.println("Rendering labels with " + threadCount + " threads.");
			for(int i = 0; i < threadCount; i++)
			{
				Thread t = new Thread(() -> {
					Map.Entry<LabelContainer, List<CompLabel>> entry;
					while((entry = queue.poll()) != null)
					{
						processEntry(gpuTasks, entry);
					}
					System.out.println(Thread.currentThread().getName() + " has finished.");
				}, "LabelRenderThread#" + i);
				t.setDaemon(true);
				t.start();
			}
		}
	}
	
	private void processEntry(BlockingQueue<GPUTask> gpuTasks, Map.Entry<LabelContainer, List<CompLabel>> entry)
	{
		LabelTextureWrapper texture = generateUploadTexture(entry.getKey().text, entry.getKey().fontSize);
		try
		{
			gpuTasks.put((unused) -> {
				texture.upload();
			});
		}
		catch(InterruptedException e)
		{
			//Should never happen.
			e.printStackTrace();
			return;
		}
		
		for(CompLabel label : entry.getValue())
		{
			label.setTexture(texture);
		}
	}
	
	private static class LabelContainer
	{
		private final String text;
		private final float fontSize;
		
		public LabelContainer(String text, float fontSize)
		{
			this.text = text;
			this.fontSize = fontSize;
		}
		
		@Override
		public boolean equals(Object o)
		{
			if(this == o)
			{
				return true;
			}
			if(o == null || getClass() != o.getClass())
			{
				return false;
			}
			LabelContainer that = (LabelContainer) o;
			return Float.compare(that.fontSize, fontSize) == 0 &&
					Objects.equals(text, that.text);
		}
		
		@Override
		public int hashCode()
		{
			return Objects.hash(text, fontSize);
		}
	}
}
