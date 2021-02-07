package net.runelite.cache.definitions.exporters;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import net.runelite.cache.item.AllItemColors;
import net.runelite.cache.item.ItemColors;

public class AllItemColorsExporter
{
	private final Gson gson;
	private final AllItemColors allItemColors;

	public AllItemColorsExporter(AllItemColors allItemColors)
	{
		this.allItemColors = allItemColors;

		GsonBuilder builder = new GsonBuilder();
		builder.registerTypeAdapter(Double.class, (JsonSerializer<Double>) (src, typeOfSrc, context) -> {
			DecimalFormat df = new DecimalFormat("#.#####");
			df.setRoundingMode(RoundingMode.CEILING);
			return new JsonPrimitive(Double.parseDouble(df.format(src)));
		});
		builder.registerTypeAdapter(ItemColors.class, new ItemColors.Serializer());
		gson = builder.create();
	}

	public String export()
	{
		return gson.toJson(allItemColors.itemToColors);
	}

	public void exportTo(File file) throws IOException
	{
		try (FileWriter fw = new FileWriter(file))
		{
			fw.write(export());
		}
	}
}
