package net.minecraft.data;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import net.minecraft.util.registry.Bootstrap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DataGenerator {
   private static final Logger LOGGER = LogManager.getLogger();
   private final Collection<Path> inputFolders;
   private final Path outputFolder;
   private final List<IDataProvider> providers = Lists.newArrayList();

   public DataGenerator(Path p_i48266_1_, Collection<Path> p_i48266_2_) {
      this.outputFolder = p_i48266_1_;
      this.inputFolders = p_i48266_2_;
   }

   public Collection<Path> getInputFolders() {
      return this.inputFolders;
   }

   public Path getOutputFolder() {
      return this.outputFolder;
   }

   public void run() throws IOException {
      DirectoryCache directorycache = new DirectoryCache(this.outputFolder, "cache");
      directorycache.keep(this.getOutputFolder().resolve("version.json"));
      Stopwatch stopwatch = Stopwatch.createStarted();
      Stopwatch stopwatch1 = Stopwatch.createUnstarted();

      for(IDataProvider idataprovider : this.providers) {
         LOGGER.info("Starting provider: {}", (Object)idataprovider.getName());
         stopwatch1.start();
         idataprovider.run(directorycache);
         stopwatch1.stop();
         LOGGER.info("{} finished after {} ms", idataprovider.getName(), stopwatch1.elapsed(TimeUnit.MILLISECONDS));
         stopwatch1.reset();
      }

      LOGGER.info("All providers took: {} ms", (long)stopwatch.elapsed(TimeUnit.MILLISECONDS));
      directorycache.purgeStaleAndWrite();
   }

   public void addProvider(IDataProvider p_200390_1_) {
      this.providers.add(p_200390_1_);
   }

   static {
      Bootstrap.bootStrap();
   }
}