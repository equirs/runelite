/*
 * Copyright (c) 2016-2017, Adam <Adam@sigterm.info>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.cache;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Objects;
import javax.annotation.Nullable;
import net.runelite.cache.definitions.ItemDefinition;
import net.runelite.cache.definitions.ItemSlotsDefinition;
import net.runelite.cache.definitions.exporters.ItemSlotsExporter;
import net.runelite.cache.definitions.loaders.ItemLoader;
import net.runelite.cache.fs.Archive;
import net.runelite.cache.fs.ArchiveFiles;
import net.runelite.cache.fs.FSFile;
import net.runelite.cache.fs.Index;
import net.runelite.cache.fs.Storage;
import net.runelite.cache.fs.Store;
import net.runelite.cache.item.AllItemSlots;

public class ItemSlotDumper
{
	private final Store store;
	private final AllItemSlots allItemSlots = new AllItemSlots(new HashMap<>());

	public ItemSlotDumper(Store store)
	{
		this.store = store;
	}

	public void load() throws IOException
	{
		ItemLoader loader = new ItemLoader();

		Storage storage = store.getStorage();
		Index index = store.getIndex(IndexType.CONFIGS);
		Archive archive = index.getArchive(ConfigType.ITEM.getId());

		byte[] archiveData = storage.loadArchive(archive);
		ArchiveFiles files = archive.getFiles(archiveData);

		for (FSFile f : files.getFiles())
		{
			ItemDefinition def = loader.load(f.getFileId(), f.getContents());
			ItemSlotsDefinition slotDef = convert(def);
			if (slotDef == null)
			{
				continue;
			}
			allItemSlots.items.put(def.id, slotDef);
		}
	}

	@Nullable
	private ItemSlotsDefinition convert(ItemDefinition def)
	{
		if (def.maleModel0 < 0 && def.maleModel1 < 0 && def.maleModel2 < 0 && def.femaleModel0 < 0 && def.femaleModel1 < 0 && def.femaleModel2 < 0)
		{
			return null;
		}
		if (Objects.equals(def.name.toLowerCase(), "null"))
		{
			return null;
		}
		// wearpos must be in range of KitType (e.g., can skip rings with wp 12)
		if (def.wearPos1 < 0 || def.wearPos1 > 11)
		{
			return null;
		}
		// if wearpos 2 and 3 are negative/zero, they don't hide anything
		Integer wp2 = def.wearPos2 > 0 ? def.wearPos2 : null;
		Integer wp3 = def.wearPos3 > 0 ? def.wearPos3 : null;
		return new ItemSlotsDefinition(def.wearPos1, wp2, wp3);
	}

	public Collection<ItemSlotsDefinition> getItems()
	{
		return Collections.unmodifiableCollection(allItemSlots.items.values());
	}

	public ItemSlotsDefinition getItem(int itemId)
	{
		return allItemSlots.items.get(itemId);
	}

	public void export(File out) throws IOException
	{
		out.mkdirs();
		File file = new File(out, "slots.json");
		ItemSlotsExporter exporter = new ItemSlotsExporter(allItemSlots);
		exporter.exportTo(file);
	}
}
