package net.runelite.cache.item;

import java.util.Map;
import lombok.Value;
import net.runelite.cache.definitions.ItemSlotsDefinition;

@Value
public class AllItemSlots
{
	public Map<Integer, ItemSlotsDefinition> items;
}
