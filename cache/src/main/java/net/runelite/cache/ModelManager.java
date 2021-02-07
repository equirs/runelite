package net.runelite.cache;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import net.runelite.cache.definitions.ModelDefinition;
import net.runelite.cache.definitions.loaders.ModelLoader;
import net.runelite.cache.fs.Archive;
import net.runelite.cache.fs.Index;
import net.runelite.cache.fs.Storage;
import net.runelite.cache.fs.Store;

public class ModelManager
{
	private final Store store;
	private final Map<Integer, ModelDefinition> models = new HashMap<>();

	public ModelManager(Store store)
	{
		this.store = store;
	}

	public void load() throws IOException
	{
		Storage storage = store.getStorage();
		Index index = store.getIndex(IndexType.MODELS);

		for (Archive archive : index.getArchives())
		{
			byte[] contents = archive.decompress(storage.loadArchive(archive));

			ModelLoader loader = new ModelLoader();
			ModelDefinition def = loader.load(archive.getArchiveId(), contents);
			models.put(def.getId(), def);
		}
	}

	public Collection<ModelDefinition> getModels()
	{
		return Collections.unmodifiableCollection(models.values());
	}

	public ModelDefinition getModel(int modelId)
	{
		return models.get(modelId);
	}
}
