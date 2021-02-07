package net.runelite.cache.item;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Value;

@Value
public class ItemColors
{
	public List<ItemColorInfo> colorInfo;

	public static class Serializer implements JsonSerializer<ItemColors>
	{
		private static final DecimalFormat doubleFormat;

		static
		{
			doubleFormat = new DecimalFormat("#.#####");
			doubleFormat.setRoundingMode(RoundingMode.CEILING);
		}

		@Override
		public JsonElement serialize(ItemColors itemColors, Type type, JsonSerializationContext jsonSerializationContext)
		{
			JsonArray rgbs = new JsonArray();
			itemColors.colorInfo.stream()
				.map(ItemColorInfo::getRgb)
				.forEach(rgbs::add);
			JsonArray pcts = new JsonArray();
			itemColors.colorInfo.stream()
				.map(ItemColorInfo::getPct)
				.map(doubleFormat::format)
				.map(Double::parseDouble)
				.forEach(pcts::add);
			JsonArray outer = new JsonArray();
			outer.add(rgbs);
			outer.add(pcts);
			return outer;
		}
	}

	public boolean closeEnoughTo(ItemColors other)
	{
		Map<Integer, Double> m0 = colorInfo.stream()
			.collect(Collectors.toMap(i -> i.rgb, i -> i.pct));
		Map<Integer, Double> m1 = other.colorInfo.stream()
			.collect(Collectors.toMap(i -> i.rgb, i -> i.pct));
		return m0.keySet().equals(m1.keySet()) && m0.entrySet().stream()
			.noneMatch(e -> Math.abs(m1.getOrDefault(e.getKey(), Double.MIN_VALUE) - e.getValue()) > 0.05);
	}
}
