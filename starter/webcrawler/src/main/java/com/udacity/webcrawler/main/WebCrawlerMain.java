package com.udacity.webcrawler.main;

import com.google.inject.Guice;
import com.udacity.webcrawler.WebCrawler;
import com.udacity.webcrawler.WebCrawlerModule;
import com.udacity.webcrawler.json.ConfigurationLoader;
import com.udacity.webcrawler.json.CrawlResult;
import com.udacity.webcrawler.json.CrawlResultWriter;
import com.udacity.webcrawler.json.CrawlerConfiguration;
import com.udacity.webcrawler.profiler.Profiler;
import com.udacity.webcrawler.profiler.ProfilerModule;

import javax.inject.Inject;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.nio.file.Path;
import java.util.Objects;

public final class WebCrawlerMain {

  private final CrawlerConfiguration config;

  private WebCrawlerMain(CrawlerConfiguration config) {
    this.config = Objects.requireNonNull(config);
  }

  @Inject
  private WebCrawler crawler;

  @Inject
  private Profiler profiler;

  private void run() throws Exception {
    Guice.createInjector(new WebCrawlerModule(config), new ProfilerModule()).injectMembers(this);

    CrawlResult result = crawler.crawl(config.getStartPages());
    CrawlResultWriter resultWriter = new CrawlResultWriter(result);

    /**
     * Write the crawl results to a JSON file (or System.out if the file name is empty)
     * check the value of config.getResultPath()
     * if is non-empty, create a Path as tile name and pass path as the argument
     * if is empty, printout result in standard output
     */
    if (!config.getResultPath().isEmpty()) {
      System.out.println("Writing result into the given path ...");
      Path path = Path.of(config.getResultPath());
      resultWriter.write(path);
    } else {
      System.out.println("Printing result to standard output ...");
      BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(System.out));
      resultWriter.write(bufferedWriter);
    }

    /**
     * Write the profile data to a text file (or System.out if the file name is empty)
     * CrawlerConfiguration#profileOutputPath
     */
    if (!config.getProfileOutputPath().isEmpty()) {
      profiler.writeData(Path.of(config.getProfileOutputPath()));
    } else {
      profiler.writeData(new OutputStreamWriter(System.out));
    }
  }

  public static void main(String[] args) throws Exception {
    if (args.length != 1) {
      System.out.println("Usage: WebCrawlerMain [starting-url]");
      return;
    }

    CrawlerConfiguration config = new ConfigurationLoader(Path.of(args[0])).load();
    new WebCrawlerMain(config).run();
  }
}
