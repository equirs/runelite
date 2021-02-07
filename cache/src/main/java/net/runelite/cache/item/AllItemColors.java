package net.runelite.cache.item;

import java.util.Map;
import lombok.Value;

@Value
public class AllItemColors
{
	public Map<Integer, GenderItemColors> itemToColors;
}
