package net.runelite.cache;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.runelite.cache.definitions.ItemDefinition;
import net.runelite.cache.definitions.ModelDefinition;
import net.runelite.cache.definitions.NpcDefinition;
import net.runelite.cache.definitions.TextureDefinition;
import net.runelite.cache.definitions.exporters.AllItemColorsExporter;
import net.runelite.cache.fs.Store;
import net.runelite.cache.item.AllItemColors;
import net.runelite.cache.item.GenderItemColors;
import net.runelite.cache.item.ItemColorInfo;
import net.runelite.cache.item.ItemColors;
import net.runelite.cache.models.JagexColor;
import net.runelite.cache.util.Triangle3D;

public class ColorDataDumper
{
	private final Store store;
	private final TextureManager textureManager;
	private final AllItemColors allColors = new AllItemColors(new HashMap<>());

	public ColorDataDumper(Store store)
	{
		this.store = store;
		this.textureManager = new TextureManager(store);
	}

	public void load() throws IOException
	{
		ModelManager modelManager = new ModelManager(store);
		modelManager.load();
		ItemManager itemManager = new ItemManager(store);
		itemManager.load();
		NpcManager npcManager = new NpcManager(store);
		npcManager.load();
		textureManager.load();

		for (ItemDefinition def : itemManager.getItems())
		{
			ModelDefinition m0 = modelManager.getModel(def.maleModel0);
			ModelDefinition m1 = modelManager.getModel(def.maleModel1);
			ModelDefinition m2 = modelManager.getModel(def.maleModel2);
			doRecolor(def, m0);
			doRetexture(def, m0);
			doRecolor(def, m1);
			doRetexture(def, m1);
			doRecolor(def, m2);
			doRetexture(def, m2);
			Map<Integer, Double> mAreas = computeColorAreas(m0);
			addAreas(mAreas, computeColorAreas(m1));
			addAreas(mAreas, computeColorAreas(m2));
			ItemColors m = itemColorsFromArea(mAreas);
			undoRecolor(def, m0);
			undoRetexture(def, m0);
			undoRecolor(def, m1);
			undoRetexture(def, m1);
			undoRecolor(def, m2);
			undoRetexture(def, m2);

			ModelDefinition f0 = modelManager.getModel(def.femaleModel0);
			ModelDefinition f1 = modelManager.getModel(def.femaleModel1);
			ModelDefinition f2 = modelManager.getModel(def.femaleModel2);
			doRecolor(def, f0);
			doRetexture(def, f0);
			doRecolor(def, f1);
			doRetexture(def, f1);
			doRecolor(def, f2);
			doRetexture(def, f2);
			Map<Integer, Double> fAreas = computeColorAreas(f0);
			addAreas(fAreas, computeColorAreas(f1));
			addAreas(fAreas, computeColorAreas(f2));
			ItemColors f = itemColorsFromArea(fAreas);
			undoRecolor(def, f0);
			undoRetexture(def, f0);
			undoRecolor(def, f1);
			undoRetexture(def, f1);
			undoRecolor(def, f2);
			undoRetexture(def, f2);

			if (fAreas.isEmpty() && mAreas.isEmpty())
			{
				continue;
			}
			GenderItemColors genderColors = new GenderItemColors();
			if (m.closeEnoughTo(f))
			{
				genderColors.a = m;
			}
			else
			{
				genderColors.f = f;
				genderColors.m = m;
			}
			allColors.itemToColors.put(def.id, genderColors);
		}
	}

	public void export(File colorDir) throws IOException
	{
		colorDir.mkdirs();
		File file = new File(colorDir, "colors.json");
		AllItemColorsExporter exporter = new AllItemColorsExporter(allColors);
		exporter.exportTo(file);
	}

	private Map<Integer, Double> computeColorAreas(ModelDefinition m)
	{
		Map<Integer, Double> result = new HashMap<>();
		if (m == null)
		{
			return result;
		}
		for (int i = 0; i < m.faceCount; i++)
		{
			int a = m.faceIndices1[i];
			int xa = m.vertexX[a];
			int ya = m.vertexY[a];
			int za = m.vertexZ[a];
			int b = m.faceIndices2[i];
			int xb = m.vertexX[b];
			int yb = m.vertexY[b];
			int zb = m.vertexZ[b];
			int c = m.faceIndices3[i];
			int xc = m.vertexX[c];
			int yc = m.vertexY[c];
			int zc = m.vertexZ[c];
			Triangle3D triangle = new Triangle3D(xa, xb, xc, ya, yb, yc, za, zb, zc);
			double area = triangle.computeArea();

			int rgb = -1;

			if (m.faceTextures != null)
			{
				short textureId = m.faceTextures[i];
				if (textureId != -1)
				{
					TextureDefinition tx = textureManager.findTexture(textureId);
					if (tx != null)
					{
						rgb = tx.missingColor;
						double currentValue = result.getOrDefault(rgb, 0.0);
						result.put(rgb, currentValue + area);
					}
				}
			}

			if (rgb == -1)
			{
				short hsl = m.faceColors[i];
				rgb = JagexColor.HSLtoRGB(hsl, JagexColor.BRIGHTNESS_LOW);
				double currentValue = result.getOrDefault(rgb, 0.0);
				result.put(rgb, currentValue + area);
			}
		}
		return result;
	}

	private static ItemColors itemColorsFromArea(Map<Integer, Double> areas)
	{
		double totalArea = areas.values().stream().reduce(Double::sum).orElse(Double.MAX_VALUE);
		List<ItemColorInfo> infos = areas.entrySet().stream()
			.map(e -> new ItemColorInfo(e.getKey(), e.getValue() / totalArea))
			// anything < 1% total area should be ignored
			.filter(info -> info.pct > 0.01)
			.collect(Collectors.toList());
		return new ItemColors(infos);
	}

	private static void addAreas(Map<Integer, Double> to, Map<Integer, Double> from)
	{
		from.forEach((k, v) -> {
			double current = to.getOrDefault(k, 0.0);
			to.put(k, v + current);
		});
	}

	private static void doRecolor(ItemDefinition def, ModelDefinition model)
	{
		if (model == null)
		{
			return;
		}
		short[] finds = def.colorFind;
		short[] replaces = def.colorReplace;
		recolor(finds, replaces, model);
	}

	private static void undoRecolor(ItemDefinition def, ModelDefinition model)
	{
		if (model == null)
		{
			return;
		}
		short[] finds = def.colorFind;
		short[] replaces = def.colorReplace;
		recolor(replaces, finds, model);
	}

	private static void recolor(short[] finds, short[] replaces, ModelDefinition model)
	{
		if (finds == null || replaces == null)
		{
			return;
		}
		for (int i = 0; i < finds.length && i < replaces.length; i++)
		{
			model.recolor(finds[i], replaces[i]);
		}
	}

	private static void doRetexture(ItemDefinition def, ModelDefinition model)
	{
		if (model == null)
		{
			return;
		}
		short[] finds = def.textureFind;
		short[] replaces = def.textureReplace;
		retexture(finds, replaces, model);
	}

	private static void undoRetexture(ItemDefinition def, ModelDefinition model)
	{
		if (model == null)
		{
			return;
		}
		short[] finds = def.textureFind;
		short[] replaces = def.textureReplace;
		retexture(replaces, finds, model);
	}

	private static void retexture(short[] finds, short[] replaces, ModelDefinition model)
	{
		if (finds == null || replaces == null)
		{
			return;
		}
		for (int i = 0; i < finds.length && i < replaces.length; i++)
		{
			model.retexture(finds[i], replaces[i]);
		}
	}
}
